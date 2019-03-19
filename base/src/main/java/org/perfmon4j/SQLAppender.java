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
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.MedianCalculator.MedianResult;
import org.perfmon4j.util.ThresholdCalculator;
import org.perfmon4j.util.ThresholdCalculator.ThresholdResult;

public abstract class SQLAppender extends SystemNameAndGroupsAppender {
	private static final Logger logger = LoggerFactory.initLogger(SQLAppender.class);
	private String dbSchema = null;
	private String insertCategoryPS = null;
	private String selectCategoryPS = null;
	private String insertGroupPS = null;
	private String selectGroupPS = null;
	private String joinGroupToSystemPS = null;
	private String groupToSystemJoinExistsPS = null;
	private String insertIntervalPS = null;
	private String pauseIntervalSQL = null;
	private String insertThresholdPS = null;
	private Long systemID = null;
	private final AtomicBoolean groupsHaveChanged = new AtomicBoolean(true);
	private static final AtomicBoolean testOnly_testDurationsForAppenderPause = new AtomicBoolean(false);
	private final AtomicLong pauseUntilTime = new AtomicLong(-1);
	private final AtomicLong nextCheckForPauseFlagTime = new AtomicLong(-1);
	
	public SQLAppender(AppenderID id) {
		super(id);
	}

	protected abstract Connection getConnection() throws SQLException;
	protected abstract void releaseConnection(Connection conn);
	protected abstract void resetConnection();
   
	
	@Override
	public void outputData(PerfMonData data) {
		Connection conn = null;
		double databaseVersion = 0.0;
		boolean needDatabaseVersion = 
			(data instanceof SQLWriteableWithDatabaseVersion) 
			|| (data instanceof IntervalData)
			|| groupsHaveChanged.get();
				
		if (needDatabaseVersion) {
			databaseVersion = getDatabaseVersion();
		}
		
		try {
			long systemIDToUse = getSystemID();
			conn = getConnection();

			if (conn != null) {
				if (databaseVersion < 7.0 || !isAppenderPaused(conn)) {
					if (groupsHaveChanged.get() && databaseVersion >= 6.0) {
						long systemID = getSystemID();
						for (String groupName : getGroupsAsArray()) {
							long groupID = getOrCreateGroup(conn, groupName);
							joinGroupToSystemIfNotExists(conn, groupID, systemID);
						}
						groupsHaveChanged.set(false);
					}
					if (data instanceof IntervalData) {
						outputIntervalData(conn, (IntervalData)data);
					} else if (data instanceof SQLWriteable){
						((SQLWriteable)data).writeToSQL(conn, dbSchema, getSystemID());
					} else if (data instanceof SQLWriteableWithDatabaseVersion){
						((SQLWriteableWithDatabaseVersion)data).writeToSQL(conn, dbSchema, getSystemID(), databaseVersion);
					} else {
						logger.logWarn("SKIPPING! Data type not supported by appender: " + data.getClass().getName());
					}
				}
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

	public String getPauseIntervalSQL() {
		if (pauseIntervalSQL == null) {
			pauseIntervalSQL = String.format(PAUSE_INTERVAL_SQL, dbSchema == null ? "" : (dbSchema + "."));
		}
		return pauseIntervalSQL;
	}
	
	public String getInsertGroupPS() {
		if (insertGroupPS == null) {
			insertGroupPS = String.format(INSERT_GROUP_PS, dbSchema == null ? "" : (dbSchema + "."));
		}
		return insertGroupPS;
	}

	
	public String getSelectGroupPS() {
		if (selectGroupPS == null) {
			selectGroupPS = String.format(SELECT_GROUP_PS, dbSchema == null ? "" : (dbSchema + "."));
		}
		return selectGroupPS;
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

	public String getJoinGroupToSystemPS() {
		if (joinGroupToSystemPS == null) {
			joinGroupToSystemPS = String.format(JOIN_GROUP_TO_SYSTEM_PS, dbSchema == null ? "" : (dbSchema + "."));
		}
		return joinGroupToSystemPS;
	}	
	
	public String getGroupToSystemJoinExistsPS() {
		if (groupToSystemJoinExistsPS == null) {
			groupToSystemJoinExistsPS = String.format(GROUP_TO_SYSTEM_JOIN_EXISTS_PS, dbSchema == null ? "" : (dbSchema + "."));
		}
		return groupToSystemJoinExistsPS;
	}	

	
	private final static String INSERT_CATEGORY_PS = "INSERT INTO %sP4JCategory (CategoryName) VALUES(?)";
	private final static String SELECT_CATEGORY_PS = "SELECT CategoryID FROM %sP4JCategory WHERE CategoryName=?";

	private final static String INSERT_GROUP_PS = "INSERT INTO %sP4JGroup (GroupName) VALUES(?)";
	private final static String SELECT_GROUP_PS = "SELECT GroupID FROM %sP4JGroup WHERE GroupName=?";
	
	
	private final static String INSERT_INTERVAL_PS = "INSERT INTO %sP4JIntervalData (SystemID, CategoryID, StartTime, EndTime, " +
		"TotalHits, TotalCompletions, MaxActiveThreads, MaxActiveThreadsSet, MaxDuration, " +
		"MaxDurationSet, MinDuration, MinDurationSet, averageDuration, standardDeviation,  " +
		"normalizedThroughputPerMinute, durationSum, durationSumOfSquares, medianDuration, " +
		"SQLMaxDuration, SQLMaxDurationSet, SQLMinDuration, SQLMinDurationSet, " +
		"SQLAverageDuration, SQLStandardDeviation, SQLDurationSum, SQLDurationSumOfSquares) " +
		"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
		"?, ?, ?, ?)";

	private final static String PAUSE_INTERVAL_SQL = "SELECT pauseAppenderMinutes FROM %sP4JAppenderControl";
	
	private final static String INSERT_THRESHOLD_PS = "INSERT INTO %sP4JIntervalThreshold (intervalID, ThresholdMillis, " +
		"CompletionsOver, PercentOver) VALUES(?, ?, ?, ?)";

	private final static String JOIN_GROUP_TO_SYSTEM_PS = "INSERT INTO %sP4JGroupSystemJoin (groupID, systemID) VALUES(?, ?)";
	private final static String GROUP_TO_SYSTEM_JOIN_EXISTS_PS = "SELECT COUNT(*) FROM %sP4JGroupSystemJoin WHERE GroupID=? AND SystemID=?";

	private void joinGroupToSystemIfNotExists(Connection conn, long groupID, long systemID) throws SQLException {
		PreparedStatement stmtQuery = null;
		PreparedStatement stmtInsert = null;

		try {
			stmtQuery = conn.prepareStatement(getGroupToSystemJoinExistsPS());
			stmtQuery.setLong(1, groupID);
			stmtQuery.setLong(2, systemID);
			boolean rowExists = JDBCHelper.getQueryCount(stmtQuery) != 0;
			if (!rowExists) {
				// We use generic SQL so we can't reliably have a single statement 
				// that only inserts if the row does not already exist. Still since all 
				// appenders operate in a single thread, the possibility of
				// a race condition where the insert fails is minor.
				stmtInsert = conn.prepareStatement(getJoinGroupToSystemPS());
				stmtInsert.setLong(1, groupID);
				stmtInsert.setLong(2, systemID);
				
				stmtInsert.execute();
			}
		} finally {
			JDBCHelper.closeNoThrow(stmtQuery);
			JDBCHelper.closeNoThrow(stmtInsert);
		}
	}

	private long getOrCreateCategory(Connection conn, String categoryName) throws SQLException {
		long result = 0;
		final boolean oracleConnection = JDBCHelper.isOracleConnection(conn);
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
		
				if (oracleConnection) {
					stmtInsert = conn.prepareStatement(getInsertCategoryPS(), new int[]{1});
				} else {
					stmtInsert = conn.prepareStatement(getInsertCategoryPS(), Statement.RETURN_GENERATED_KEYS);
				}
				stmtInsert.setString(1, categoryName);
				stmtInsert.execute();
				
				rs = stmtInsert.getGeneratedKeys();
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

	private long getOrCreateGroup(Connection conn, String groupName) throws SQLException {
		long result = 0;
		final boolean oracleConnection = JDBCHelper.isOracleConnection(conn);
		PreparedStatement stmtQuery = null;
		PreparedStatement stmtInsert = null;
		ResultSet rs = null;
		
		try {
			stmtQuery = conn.prepareStatement(getSelectGroupPS());
			stmtQuery.setString(1, groupName);
			rs = stmtQuery.executeQuery();
			if (!rs.next()) {
				JDBCHelper.closeNoThrow(rs);
				rs = null;
		
				if (oracleConnection) {
					stmtInsert = conn.prepareStatement(getInsertGroupPS(), new int[]{1});
				} else {
					stmtInsert = conn.prepareStatement(getInsertGroupPS(), Statement.RETURN_GENERATED_KEYS);
				}
				stmtInsert.setString(1, groupName);
				stmtInsert.execute();
				
				rs = stmtInsert.getGeneratedKeys();
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
	
	private static long CACHED_DATABASE_VERISON_MINUTES = Long.getLong(SQLAppender.class.getName() + ".CACHED_DATABASE_VERISON_MINUTES", 10).longValue();
	private long lastDatabaseVersionCheck = 0;
	private double cachedDatabaseVersion = 0.0;
	
	public double getDatabaseVersion() {
		return getDatabaseVersion_TestOnly(CACHED_DATABASE_VERISON_MINUTES * 60 * 1000); 
	}
	
	private boolean isAppenderPaused(Connection conn) throws SQLException {
		long now = System.currentTimeMillis();
		boolean paused = now < pauseUntilTime.longValue();
	
		// Check to see if we are paused already..
		if (!paused && (now > nextCheckForPauseFlagTime.longValue())) {
			long pausePeriod = JDBCHelper.getQueryCount(conn, getPauseIntervalSQL());
			if (pausePeriod > 0) {
				logger.logInfo("Found appender pause flag set -- Database logging will be suspended. "
						+ "Will check again in " 
						+ pausePeriod + " minute(s).");
				
				pausePeriod = pausePeriod * 1000;
				if (!testOnly_testDurationsForAppenderPause.get()) {
					pausePeriod *= 60;
				}
				pauseUntilTime.set(now + pausePeriod);
				paused = true;
			} else {
				// Delay for next check 
				int delayForNextCheck = 10;  // In test check every 10 milliseconds
				if (!testOnly_testDurationsForAppenderPause.get()) {
					delayForNextCheck *= 1000; // In production check every 10 seconds
				}
				nextCheckForPauseFlagTime.set(now + delayForNextCheck);
			}
		}
		
		return paused;
	}
	
	public double getDatabaseVersion_TestOnly(long cacheDuration) {
		if (lastDatabaseVersionCheck + cacheDuration < System.currentTimeMillis()) {
			Connection conn = null;
			try {
				conn = getConnection();
				cachedDatabaseVersion = JDBCHelper.getDatabaseVersion(conn, dbSchema);
			} catch(SQLException se) { 
				resetConnection();
			} finally {
				if (conn != null) {
					releaseConnection(conn);
				}
			}
			lastDatabaseVersionCheck = System.currentTimeMillis();
		}
		return cachedDatabaseVersion;
	}
	
// This did NOT work wiht a default (NULL) schema on SQL server...
//	static private boolean doesVersionTableExists(Connection conn, String schema) throws SQLException {
//		// If we don't have an explicit schema check the schema associated with the connection.
//		schema = (schema == null) ? conn.getSchema() : null; 
//		
//		boolean nullSchema = (schema == null);
//
//		
//		boolean result = false;
//		DatabaseMetaData dbMetaData = null;
//		ResultSet rs = null;
//		try {
//			dbMetaData = conn.getMetaData();
//			rs = dbMetaData.getTables(null, "%", "%", new String[]{"TABLE"});
//			while (rs.next() && !result) {
//				final String n = rs.getString("TABLE_NAME");
//				final String s = rs.getString("TABLE_SCHEM");
//				
//				boolean schemaMatches = ((s == null) && nullSchema)
//						|| schema.equalsIgnoreCase(s);
//				result = schemaMatches && "DATABASECHANGELOG".equalsIgnoreCase(n);
//			}
//		} finally {
//			JDBCHelper.closeNoThrow(rs);
//		}
//		
//		return result;
//	}
	
	private void outputIntervalData(Connection conn, IntervalData data) throws SQLException {
		if (data.getTimeStart() <= 0 || data.getTimeStop() <= 0) {
			logger.logWarn("Skipping SQL insert for data timeStart and/or timeEnd missing");
			return;
		}
		final boolean oracleConnection = JDBCHelper.isOracleConnection(conn);
		boolean autoCommit = conn.getAutoCommit();
		boolean doRollback = false;
		
		PreparedStatement insertIntervalStmt = null;
		PreparedStatement insertThresholdStmt = null;
		ResultSet generatedKeys = null;
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
			long durationSum = data.getTotalDuration();
			long durationSumOfSquares = data.getSumOfSquares();
			Double medianDuration = null;

			long maxSQLDuration = data.getMaxSQLDuration();
			Timestamp maxSQLDurationSet = buildTimestampOrNull(data.getTimeMaxSQLDurationSet());
			long minSQLDuration = data.getMinSQLDuration();
			Timestamp minSQLDurationSet = buildTimestampOrNull(data.getTimeMinSQLDurationSet());
			double averageSQLDuration = data.getAverageSQLDuration();
			double sqlStdDeviation = data.getSQLStdDeviation();
			long totalSQLDuration = data.getTotalSQLDuration();
			long sumOfSQLSquares = data.getSumOfSQLSquares();
			
			MedianCalculator calc = data.getMedianCalculator();
			MedianResult medianResult = null;
			if (calc != null) {
				medianResult = calc.getMedian();
			}
			if (medianResult != null && medianResult.getOverflowFlag() == 0) {
				Double d = medianResult.getResult();
				if (d == null) {
					d = Double.valueOf(0.0);
				}
				medianDuration = d;
			}
			
			boolean includeOptionalCols = (
					SQLTime.isEnabled() 
					&& !data.isSQLMonitor());
			
			if (oracleConnection) {
				insertIntervalStmt = conn.prepareStatement(getInsertIntervalPS(), new int[]{1});
			} else {
				insertIntervalStmt = conn.prepareStatement(getInsertIntervalPS(), Statement.RETURN_GENERATED_KEYS);
			}
			int index = 1;
			insertIntervalStmt.setLong(index++, getSystemID());
			insertIntervalStmt.setLong(index++, categoryID);
			insertIntervalStmt.setTimestamp(index++, new Timestamp(startTime));
			insertIntervalStmt.setTimestamp(index++, new Timestamp(endTime));
			insertIntervalStmt.setLong(index++, totalHits);
			insertIntervalStmt.setLong(index++, totalCompletions);
			insertIntervalStmt.setLong(index++, maxActiveThreads);
			insertIntervalStmt.setObject(index++, maxActiveThreadsSet, Types.TIMESTAMP);
			insertIntervalStmt.setLong(index++, maxDuration);
			insertIntervalStmt.setObject(index++, maxDurationSet, Types.TIMESTAMP);
			insertIntervalStmt.setLong(index++, minDuration);
			insertIntervalStmt.setObject(index++, minDurationSet, Types.TIMESTAMP);
			insertIntervalStmt.setDouble(index++, averageDuration);
			insertIntervalStmt.setDouble(index++, standardDeviation);
			insertIntervalStmt.setDouble(index++, normalizedThroughputPerMinute);
			insertIntervalStmt.setLong(index++, durationSum);
			insertIntervalStmt.setLong(index++, durationSumOfSquares);
			insertIntervalStmt.setObject(index++, medianDuration, Types.DOUBLE);
			if (includeOptionalCols) {
				insertIntervalStmt.setLong(index++, maxSQLDuration);
				insertIntervalStmt.setObject(index++, maxSQLDurationSet, Types.TIMESTAMP);
				insertIntervalStmt.setLong(index++, minSQLDuration);
				insertIntervalStmt.setObject(index++, minSQLDurationSet, Types.TIMESTAMP);
				insertIntervalStmt.setDouble(index++, averageSQLDuration);
				insertIntervalStmt.setDouble(index++, sqlStdDeviation);
				insertIntervalStmt.setLong(index++, totalSQLDuration);
				insertIntervalStmt.setLong(index++, sumOfSQLSquares);
			} else {
				insertIntervalStmt.setObject(index++, null, Types.INTEGER);
				insertIntervalStmt.setObject(index++, null, Types.TIMESTAMP);
				insertIntervalStmt.setObject(index++, null, Types.INTEGER);
				insertIntervalStmt.setObject(index++, null, Types.TIMESTAMP);
				insertIntervalStmt.setObject(index++, null, Types.DOUBLE);
				insertIntervalStmt.setObject(index++, null, Types.DOUBLE);
				insertIntervalStmt.setObject(index++, null, Types.INTEGER);
				insertIntervalStmt.setObject(index++, null, Types.INTEGER);
			}
			insertIntervalStmt.execute();
			generatedKeys = insertIntervalStmt.getGeneratedKeys();
			generatedKeys.next();
			long intervalID = generatedKeys.getLong(1);
			
			// Now write out the thresholds...
			ThresholdCalculator thCalc = data.getThresholdCalculator();
			if (thCalc != null) {
				insertThresholdStmt = conn.prepareStatement(getInsertThresholdPS());
				
				long thresholds[] = thCalc.getThresholdMillis();
				for (int i = 0; i < thresholds.length; i++) {
					ThresholdResult thResult = thCalc.getResult(thresholds[i]);
					
					insertThresholdStmt.setLong(1, intervalID);
					insertThresholdStmt.setLong(2, thresholds[i]);
					insertThresholdStmt.setLong(3, thResult.getCountOverThreshold());
					insertThresholdStmt.setFloat(4, thResult.getPercentOverThreshold());
					
					insertThresholdStmt.execute();
				}
			}
		} catch (SQLException se){
			doRollback = true;
			throw se;
		} finally {
			try {
				JDBCHelper.closeNoThrow(generatedKeys);
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
	
	public long getSystemID() throws SQLException {
		long result;
		
		if (systemID != null) {
			result = systemID.longValue();
		} else {
			String s = (dbSchema == null) ? "" : (dbSchema + ".");
			result = JDBCHelper.simpleGetOrCreate(this.getConnection(), s + "P4JSystem", 
					"SystemID", "SystemName", getSystemName());
			systemID = new Long(result);
		}
		
		return result;
	}
	
	public void setSystemNameBody(String systemName) {
		super.setSystemNameBody(systemName);
		this.systemID = null;
	}
	

	public void setSystemNamePrefix(String systemNamePrefix) {
		super.setSystemNamePrefix(systemNamePrefix);
		this.systemID = null;
	}

	public void setSystemNameSuffix(String systemNameSuffix) {
		super.setSystemNameSuffix(systemNameSuffix);
		this.systemID = null;
	}
	
	public void setGroups(String csvGroups) {
		super.setGroups(csvGroups);
		groupsHaveChanged.set(true);
	}
	
}
