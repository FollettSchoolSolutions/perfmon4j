/*
 *	Copyright 2018 Follett School Solutions 
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

public class HystrixThreadPoolDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "HystrixThreadPool";
	private final HystrixThreadPoolTemplate categoryTemplate;
	private static final Logger logger = LoggerFactory.initLogger(HystrixThreadPoolDataProvider.class);
	private static final double REQUIRED_DB_VERSION = 7.0;
	
	public HystrixThreadPoolDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new HystrixThreadPoolTemplate(TEMPLATE_NAME);
	}
	

	@Override
	public AggregatorFactory wrapWithCategoryLevelFilter(AggregatorFactory factory, String subCategoryName) {
		return  new ColumnValueFilterFactory(factory, "KeyName", new String[]{subCategoryName});
	}	

	@Override
	public void processResults(Connection conn, RegisteredDatabaseConnections.Database db, ResultAccumulator accumulator, SeriesField[] fields, long start, 
			long end) throws SQLException {
		
		if (JDBCHelper.getDatabaseVersion(conn, db.getSchema()) >= REQUIRED_DB_VERSION) { 
			String schemaPrefix = fixupSchema(db.getSchema());
			Set<String> selectList = buildSelectListAndPopulateAccumulators(accumulator, fields);
			
			selectList.add("endTime");
			
			String query =
				"SELECT " + commaSeparate(selectList) + "\r\n"
				+ "FROM " + schemaPrefix + "P4JHystrixThreadPool pid\r\n"
				+ "JOIN P4JHystrixKey k ON k.keyID = pid.keyID\r\n "
				+ "		AND k.keyName IN "+ buildSubCategoryNameSet(fields) + "\r\n"
				+ "WHERE pid.systemID IN " + buildSystemIDSet(fields) + "\r\n"
				+ "AND pid.EndTime >= ?\r\n"
				+ "AND pid.EndTime <= ?\r\n";
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
	}

	@Override
	public CategoryTemplate getCategoryTemplate() {
		return categoryTemplate;
	}

	@Override
	public Set<MonitoredSystem> lookupMonitoredSystems(Connection conn, RegisteredDatabaseConnections.Database database, 
			long start, long end) throws SQLException {
		Set<MonitoredSystem> result = new HashSet<MonitoredSystem>();
		if (JDBCHelper.getDatabaseVersion(conn, database.getSchema()) >= REQUIRED_DB_VERSION) {  
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				String schema = fixupSchema(database.getSchema());
				
				String SQL = "SELECT SystemID, SystemName "
					+ " FROM " + schema + "P4JSystem s "
					+ " WHERE EXISTS (SELECT SystemID " 
					+ " FROM " + schema + "P4JHystrixThreadPool pid WHERE pid.SystemID = s.SystemID "
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
		}
		
		return result;
	}

	@Override
	public Set<Category> lookupMonitoredCategories(Connection conn,
		Database db, SystemID[] systems, long start, long end)
		throws SQLException {
		Set<Category> result = new HashSet<Category>();
		if (JDBCHelper.getDatabaseVersion(conn, db.getSchema()) >= REQUIRED_DB_VERSION) {  
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				String schema = fixupSchema(db.getSchema());
				
				String SQL = "SELECT DISTINCT(KeyName) "
					+ " FROM " + schema + "P4JHystrixThreadPool pid "
					+ " JOIN " + schema + "P4JHystrixKey k ON pid.keyID = k.keyID"
					+ "	WHERE pid.EndTime >= ? "
					+ " AND pid.EndTime <= ?"
					+ " AND pid.SystemID IN " + buildInArrayForSystems(systems);
				if (logger.isDebugEnabled()) {
					logger.logDebug("getCategories SQL: " + SQL);
				}
				stmt = conn.prepareStatement(SQL);
				stmt.setTimestamp(1, new Timestamp(start));
				stmt.setTimestamp(2, new Timestamp(end));
				rs = stmt.executeQuery();
				while (rs.next()) {
					result.add(new Category(TEMPLATE_NAME + "." + rs.getString(1).trim(), TEMPLATE_NAME));
				}
			} finally {
				JDBCHelper.closeNoThrow(rs);
				JDBCHelper.closeNoThrow(stmt);
			}
		}
		
		return result;
	}

	private static class HystrixThreadPoolTemplate extends CategoryTemplate {

		private HystrixThreadPoolTemplate(String templateName) {
			super(templateName, buildFields());
		}
		
		private static final Field[] buildFields() {
			List<Field> fields = new ArrayList<Field>();

			
			fields.add(new ProviderField("currentPoolSize", AggregationMethod.DEFAULT, AggregationMethod.SUM, "CURRENTPOOLSIZE", false));
			fields.add(new ProviderField("currentQueueSize", AggregationMethod.DEFAULT, AggregationMethod.SUM, "CURRENTQUEUESIZE", false));
			fields.add(new ProviderField("maxActiveThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM, "MAXACTIVETHREADS", false));
			fields.add(new NaturalPerMinuteProviderField("rejectedThreadCount","systemID", "startTime", "endTime", "REJECTEDTHREADCOUNT"));
			fields.add(new NaturalPerMinuteProviderField("executedThreadCount","systemID", "startTime", "endTime", "EXECUTEDTHREADCOUNT"));
			fields.add(new NaturalPerMinuteProviderField("scheduledTaskCount","systemID", "startTime", "endTime", "SCHEDULEDTASKCOUNT"));
			fields.add(new NaturalPerMinuteProviderField("completedTaskCount","systemID", "startTime", "endTime", "COMPLETEDTASKCOUNT").makePrimary());

			return fields.toArray(new Field[]{});
		}
	}
}
