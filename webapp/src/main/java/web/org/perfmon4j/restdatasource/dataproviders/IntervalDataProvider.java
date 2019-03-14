/*
 *	Copyright 2015 Follett School Solutions 
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
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package web.org.perfmon4j.restdatasource.dataproviders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.RegisteredDatabaseConnections.Database;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import web.org.perfmon4j.restdatasource.DataProvider;
import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.data.Category;
import web.org.perfmon4j.restdatasource.data.CategoryTemplate;
import web.org.perfmon4j.restdatasource.data.Field;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.SystemID;
import web.org.perfmon4j.restdatasource.data.query.advanced.ResultAccumulator;
import web.org.perfmon4j.restdatasource.util.SeriesField;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.decorator.ColumnValueFilterFactory;

public class IntervalDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "Interval";
	private final IntervalTemplate categoryTemplate;
	private static final Logger logger = LoggerFactory.initLogger(IntervalDataProvider.class);
	
	public IntervalDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new IntervalTemplate(TEMPLATE_NAME);
	}

	

	@Override
	public AggregatorFactory wrapWithCategoryLevelFilter(AggregatorFactory factory, String subCategoryName) {
		return  new ColumnValueFilterFactory(factory, "CategoryName", new String[]{subCategoryName});
	}
	

	@Override
	public void processResults(Connection conn, RegisteredDatabaseConnections.Database db, ResultAccumulator accumulator, SeriesField[] fields, long start, 
			long end) throws SQLException {
		String schemaPrefix = fixupSchema(db.getSchema());
		Set<String> selectList = buildSelectListAndPopulateAccumulators(accumulator, fields);
		
		selectList.add("endTime");
		
		String query =
			"SELECT " + commaSeparate(selectList) + "\r\n"
			+ "FROM " + schemaPrefix + "P4JIntervalData pid\r\n"
			+ "JOIN " + schemaPrefix + "P4JCategory cat ON cat.categoryID = pid.CategoryID\r\n"
			+ "WHERE pid.systemID IN " + buildSystemIDSet(fields) + "\r\n"
			+ "AND cat.categoryName IN "+ buildSubCategoryNameSet(fields) + "\r\n"
			+ "AND pid.EndTime >= ?\r\n"
			+ "AND pid.EndTime <= ?\r\n";
		
		if (logger.isDebugEnabled()) {
			logger.logDebug("processResults SQL: " + query);
		}		
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(query);
			stmt.setTimestamp(1, new Timestamp(start));
			stmt.setTimestamp(2, new Timestamp(end));
			rs = stmt.executeQuery();
			while (rs.next()) {
				accumulator.accumulateResults(TEMPLATE_NAME, rs);
			}
		} finally {
			JDBCHelper.closeNoThrow(stmt);
			JDBCHelper.closeNoThrow(rs);
		}
	}

	@Override
	public CategoryTemplate getCategoryTemplate() {
		return categoryTemplate;
	}
	
	
	private static class IntervalTemplate extends CategoryTemplate {

		private IntervalTemplate(String templateName) {
			super(templateName, buildFields());
		}
		
		private static final Field[] buildFields() {
			List<Field> fields = new ArrayList<Field>();
			
			fields.add(new ProviderField("maxActiveThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM, "MaxActiveThreads", false).makePrimary());
			fields.add(new ProviderField("maxDuration", AggregationMethod.DEFAULT, AggregationMethod.MAX, "MaxDuration", false));
			fields.add(new ProviderField("minDuration", AggregationMethod.DEFAULT, AggregationMethod.MIN, "MinDuration", false));
			fields.add(new NaturalPerMinuteProviderField("throughputPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, 
					"NormalizedThroughputPerMinute", "systemID", "startTime", "endTime", "TotalCompletions", true).makePrimary());
			fields.add(new NaturalAverageProviderField("averageDuration", AggregationMethod.DEFAULT_WITH_NATURAL, "AverageDuration", 
					"DurationSum", "TotalCompletions",  true).makePrimary());
			fields.add(new ProviderField("medianDuration", AggregationMethod.DEFAULT, AggregationMethod.AVERAGE, "MedianDuration", true));
			fields.add(new NaturalStdDevProviderField("standardDeviation", AggregationMethod.DEFAULT_WITH_NATURAL, "StandardDeviation", "DurationSum", "DurationSumOfSquares", "TotalCompletions", true));
			fields.add(new ProviderField("sqlMaxDuration", AggregationMethod.DEFAULT, AggregationMethod.MAX, "SQLMaxDuration", false));
			fields.add(new ProviderField("sqlLMinDuration", AggregationMethod.DEFAULT, AggregationMethod.MIN, "SQLMinDuration", false));
			fields.add(new NaturalAverageProviderField("sqlAverageDuration", AggregationMethod.DEFAULT_WITH_NATURAL, 
					"SQLAverageDuration", "SQLDurationSum", "TotalCompletions", true));
			fields.add(new NaturalStdDevProviderField("sqlStandardDeviation", AggregationMethod.DEFAULT_WITH_NATURAL, "StandardDeviation", "SQLDurationSum", "SQLDurationSumOfSquares", "TotalCompletions", true));
			
			return fields.toArray(new Field[]{});
		}
	}


	@Override
	public Set<MonitoredSystem> lookupMonitoredSystems(Connection conn, RegisteredDatabaseConnections.Database database, 
			long start, long end) throws SQLException {
		Set<MonitoredSystem> result = new HashSet<MonitoredSystem>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		String indexHint = "";
		if (database.isMSSQL() && database.getDatabaseVersion() >= 7.0) {
			indexHint = " WITH(INDEX(P4JIntervalData_SystemEndTime)) ";
		}
		
		try {
			String schema = fixupSchema(database.getSchema());
			
			String SQL = "SELECT SystemID, SystemName "
				+ " FROM " + schema + "P4JSystem s "
				+ " WHERE EXISTS (SELECT IntervalID " 
				+ " FROM " + schema + "P4JIntervalData pid"
				+ indexHint
				+ " WHERE pid.SystemID = s.SystemID "
				+ "	AND pid.EndTime >= ? AND pid.EndTime <= ?)";
			if (logger.isDebugEnabled()) {
				logger.logDebug("getSystems SQL: " + SQL);
			}

			
			stmt = conn.prepareStatement(SQL);
			stmt.setTimestamp(1, new Timestamp(start));
			stmt.setTimestamp(2, new Timestamp(end));
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				MonitoredSystem ms = new MonitoredSystem(rs.getString("SystemName").trim(), database.getID() + "." + rs.getLong("SystemID"));
				result.add(ms);
			}
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmt);
		}
		
		return result;
	}

	@Override
	public Set<Category> lookupMonitoredCategories(Connection conn,
			Database db, SystemID[] systems, long start, long end)
			throws SQLException {
		Set<Category> result = new HashSet<Category>(); 

		String indexHint = "";
		String categoryName = "CategoryName";
		if (db.isMSSQL()  && db.getDatabaseVersion() >= 7.0) {
			indexHint = " WITH(INDEX(P4JIntervalData_SystemCatEndTime)) ";
			categoryName = "RTRIM(CategoryName) AS CategoryName";
		}
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			String schema = fixupSchema(db.getSchema());
			
			String SQL = "SELECT " + categoryName
				+ " FROM " + schema + "P4JCategory cat "
				+ " WHERE EXISTS (SELECT IntervalID " 
				+ " FROM " + schema + "P4JIntervalData pid"
				+ 	indexHint
				+ " WHERE pid.categoryId = cat.categoryID "
				+ " AND pid.systemID IN " + buildInArrayForSystems(systems)
				+ "	AND pid.EndTime >= ? AND pid.EndTime <= ?)";
			
			if (logger.isDebugEnabled()) {
				logger.logDebug("getIntervalCategories SQL: " + SQL);
			}
			
			stmt = conn.prepareStatement(SQL);
			stmt.setTimestamp(1, new Timestamp(start));
			stmt.setTimestamp(2, new Timestamp(end));
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				Category cat = new Category("Interval." + rs.getString("CategoryName").trim(), "Interval");
				result.add(cat);
			}
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmt);
		}
		
		return result;
	}
}
