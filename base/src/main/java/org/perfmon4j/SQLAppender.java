/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.ThresholdCalculator;
import org.perfmon4j.util.MedianCalculator.MedianResult;
import org.perfmon4j.util.ThresholdCalculator.ThresholdResult;

public abstract class SQLAppender extends Appender {
	private static final Logger logger = LoggerFactory.initLogger(SQLAppender.class);
	private String dbSchema = null;
	private String insertCategoryPS = null;
	private String selectCategoryPS = null;
	private String insertIntervalPS = null;
	private String insertThresholdPS = null;
	
	public SQLAppender(AppenderID id) {
		super(id);
	}

	protected abstract Connection getConnection() throws SQLException;
	protected abstract void releaseConnection(Connection conn);
	protected abstract void resetConnection();
   
	
	@Override
	public void outputData(PerfMonData data) {
		Connection conn = null;
		try {
			conn = getConnection();
			if (data instanceof IntervalData) {
				outputIntervalData(conn, (IntervalData)data);
			} else {
				logger.logWarn("Data type not supported by appender: " + data.getClass().getName());
			}
		} catch(SQLException ex) { 
			resetConnection();
			logger.logError("Error in output data", ex);
		} finally {
			if (conn != null) {
				releaseConnection(conn);
			}
		}
	}

	public String getInsertCategoryPS() {
		if (insertCategoryPS == null) {
			insertCategoryPS = String.format(INSERT_CATEGORY_PS, dbSchema == null ? "" : (dbSchema + "."));
		}
		return insertCategoryPS;
	}

	public String getSelectCategoryPS() {
		if (selectCategoryPS == null) {
			selectCategoryPS = String.format(SELECT_CATEGORY_PS, dbSchema == null ? "" : (dbSchema + "."));
		}
		return selectCategoryPS;
	}

	public String getInsertIntervalPS() {
		if (insertIntervalPS == null) {
			insertIntervalPS = String.format(INSERT_INTERVAL_PS, dbSchema == null ? "" : (dbSchema + "."));
		}
		return insertIntervalPS;
	}

	public String getInsertThresholdPS() {
		if (insertThresholdPS == null) {
			insertThresholdPS = String.format(INSERT_THRESHOLD_PS, dbSchema == null ? "" : (dbSchema + "."));
		}
		return insertThresholdPS;
	}

	private final static String INSERT_CATEGORY_PS = "INSERT INTO %sP4JCategory (CategoryName) VALUES(?)";
	private final static String SELECT_CATEGORY_PS = "SELECT CategoryID FROM %sP4JCategory WHERE CategoryName=?";
	private final static String INSERT_INTERVAL_PS = "INSERT INTO %sP4JIntervalData (CategoryID, StartTime, EndTime, " +
		"TotalHits, TotalCompletions, MaxActiveThreads, MaxActiveThreadsSet, MaxDuration, " +
		"MaxDurationSet, MinDuration, MinDurationSet, averageDuration, standardDeviation,  " +
		"normalizedThroughputPerMinute, medianDuration) VALUES(?, ?, ?, ?, ?, ?, ?, " +
		"?, ?, ?, ?, ?, ?, ?, ?)";
	private final static String INSERT_THRESHOLD_PS = "INSERT INTO %sP4JIntervalThreshold (CategoryID, StartTime, EndTime, ThresholdMillis, " +
		"CompletionsOver, PercentOver) VALUES(?, ?, ?, ?, ?, ?)";

	private long getOrCreateCategory(Connection conn, String categoryName) throws SQLException {
		long result = 0;
		PreparedStatement stmtQuery = null;
		PreparedStatement stmtInsert = null;
		ResultSet rs = null;
		
		try {
			stmtQuery = conn.prepareStatement(getSelectCategoryPS());
			stmtQuery.setString(1, categoryName);
			rs = stmtQuery.executeQuery();
			if (!rs.next()) {
				JDBCHelper.closeNoThrow(rs);
				rs = null;
				
				stmtInsert = conn.prepareStatement(getInsertCategoryPS());
				stmtInsert.setString(1, categoryName);
				stmtInsert.execute();
				
				rs = stmtQuery.executeQuery();
				rs.next();
			}
			result = rs.getLong(1);
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmtQuery);
			JDBCHelper.closeNoThrow(stmtInsert);
		}
		return result;
	}
	
	private Timestamp buildTimestampOrNull(long time) {
		return time > 0 ? new Timestamp(time) : null;
	}
	
	private void outputIntervalData(Connection conn, IntervalData data) throws SQLException {
		if (data.getTimeStart() <= 0 || data.getTimeStop() <= 0) {
			logger.logWarn("Skipping SQL insert for data timeStart and/or timeEnd missing");
			return;
		}
		boolean autoCommit = conn.getAutoCommit();
		boolean doRollback = false;
		
		PreparedStatement insertIntervalStmt = null;
		PreparedStatement insertThresholdStmt = null;
		try {
			if (autoCommit) {
				conn.setAutoCommit(false);
			}
			long categoryID = getOrCreateCategory(conn, data.getOwner().getName());
			long startTime = data.getTimeStart();
			long endTime = data.getTimeStop();
			long totalHits = data.getTotalHits();
			long totalCompletions = data.getTotalCompletions();
			long maxActiveThreads = data.getMaxActiveThreadCount();
			Timestamp maxActiveThreadsSet = buildTimestampOrNull(data.getTimeMaxActiveThreadCountSet());
			long maxDuration = data.getMaxDuration();
			Timestamp maxDurationSet = buildTimestampOrNull(data.getTimeMaxDurationSet());
			long minDuration = data.getMinDuration();
			Timestamp minDurationSet = buildTimestampOrNull(data.getTimeMaxDurationSet());
			double averageDuration = data.getAverageDuration();
			double standardDeviation = data.getStdDeviation();
			double normalizedThroughputPerMinute = data.getThroughputPerMinute();
			Double medianDuration = null;
			
			MedianCalculator calc = data.getMedianCalculator();
			MedianResult medianResult = null;
			if (calc != null) {
				medianResult = calc.getMedian();
			}
			if (medianResult != null && (medianResult.getOverflowFlag() == 0)) {
				medianDuration = new Double(medianResult.getResult());
			}
			insertIntervalStmt = conn.prepareStatement(getInsertIntervalPS());

			insertIntervalStmt.setLong(1, categoryID);
			insertIntervalStmt.setTimestamp(2, new Timestamp(startTime));
			insertIntervalStmt.setTimestamp(3, new Timestamp(endTime));
			insertIntervalStmt.setLong(4, totalHits);
			insertIntervalStmt.setLong(5, totalCompletions);
			insertIntervalStmt.setLong(6, maxActiveThreads);
			insertIntervalStmt.setObject(7, maxActiveThreadsSet, Types.TIMESTAMP);
			insertIntervalStmt.setLong(8, maxDuration);
			insertIntervalStmt.setObject(9, maxDurationSet, Types.TIMESTAMP);
			insertIntervalStmt.setLong(10, minDuration);
			insertIntervalStmt.setObject(11, minDurationSet, Types.TIMESTAMP);
			insertIntervalStmt.setDouble(12, averageDuration);
			insertIntervalStmt.setDouble(13, standardDeviation);
			insertIntervalStmt.setDouble(14, normalizedThroughputPerMinute);
			insertIntervalStmt.setObject(15, medianDuration, Types.DOUBLE);
		
			insertIntervalStmt.execute();
			
			// Now write out the thresholds...
			ThresholdCalculator thCalc = data.getThresholdCalculator();
			if (thCalc != null) {
				insertThresholdStmt = conn.prepareStatement(getInsertThresholdPS());
				
				long thresholds[] = thCalc.getThresholdMillis();
				for (int i = 0; i < thresholds.length; i++) {
					ThresholdResult thResult = thCalc.getResult(thresholds[i]);
					
					insertThresholdStmt.setLong(1, categoryID);
					insertThresholdStmt.setTimestamp(2, new Timestamp(startTime));
					insertThresholdStmt.setTimestamp(3, new Timestamp(endTime));
					insertThresholdStmt.setLong(4, thresholds[i]);
					insertThresholdStmt.setLong(5, thResult.getCountOverThreshold());
					insertThresholdStmt.setFloat(6, thResult.getPercentOverThreshold());
					
					insertThresholdStmt.execute();
				}
			}
		} catch (SQLException se){
			doRollback = true;
			throw se;
		} finally {
			try {
				JDBCHelper.closeNoThrow(insertThresholdStmt);
				JDBCHelper.closeNoThrow(insertIntervalStmt);
				if (doRollback) {
					JDBCHelper.rollbackNoThrow(conn);
				} else {
					conn.commit();
				}
			} finally {
				if (autoCommit) {
					conn.setAutoCommit(true);
				}
			}
		}
	}

	public String getDbSchema() {
		return dbSchema;
	}

	public void setDbSchema(String dbSchema) {
		this.dbSchema = dbSchema;

		// Clear out so they will be regenerated with the new schema
		this.insertCategoryPS = null;
		this.insertIntervalPS = null;
		this.insertThresholdPS = null;
		this.selectCategoryPS = null;
	}
}
