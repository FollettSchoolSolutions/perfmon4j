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

package web.org.perfmon4j.restdatasource.dataproviders.thirdparty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import web.org.perfmon4j.restdatasource.dataproviders.PercentProviderField;
import web.org.perfmon4j.restdatasource.dataproviders.ProviderField;
import web.org.perfmon4j.restdatasource.util.DateTimeHelper;
import web.org.perfmon4j.restdatasource.util.SeriesField;

public class FSSFetchThreadPoolDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "FSSFetchThreadPool";
	private final FSSFetchThreadPool categoryTemplate;
	static final String REQUIRED_DATABASE_CHANGESET = "FSS-FSSFetchThreadPoolSnapshot-tableCreate";
	private static final Logger logger = LoggerFactory.initLogger(FSSFetchThreadPoolDataProvider.class);
	private final DateTimeHelper dateTimeHelper = new DateTimeHelper();
	
	public FSSFetchThreadPoolDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new FSSFetchThreadPool(TEMPLATE_NAME);
	}

	@Override
	public void processResults(Connection conn, RegisteredDatabaseConnections.Database db, ResultAccumulator accumulator, SeriesField[] fields, long start, 
			long end) throws SQLException {
		if (JDBCHelper.databaseChangeSetExists(conn, db.getSchema(), REQUIRED_DATABASE_CHANGESET)) {
			String schemaPrefix = fixupSchema(db.getSchema());
			Set<String> selectList = buildSelectListAndPopulateAccumulators(accumulator, fields);
			
			selectList.add("endTime");
			
			String query =
				"SELECT " + commaSeparate(selectList) + "\r\n"
				+ "FROM " + schemaPrefix + "FSSFetchThreadPoolSnapshot pid\r\n"
				+ "WHERE pid.systemID IN " + buildSystemIDSet(fields) + "\r\n"
				+ "AND pid.EndTime >= " + dateTimeHelper.formatDateTimeForSQL(start) + "\r\n"
				+ "AND pid.EndTime <= " + dateTimeHelper.formatDateTimeForSQL(end) + "\r\n";
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				stmt.setFetchSize(5000);
				rs = stmt.executeQuery(query);
				accumulator.handleResultSet(TEMPLATE_NAME, rs);			
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
		
		if (JDBCHelper.databaseChangeSetExists(conn, database.getSchema(), REQUIRED_DATABASE_CHANGESET)) {
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String schema = fixupSchema(database.getSchema());
				
				String SQL = "SELECT SystemID, SystemName "
					+ " FROM " + schema + "P4JSystem s "
					+ " WHERE EXISTS (SELECT SystemID " 
					+ " FROM " + schema + "FSSFetchThreadPoolSnapshot pid WHERE pid.SystemID = s.SystemID "
					+ "	AND pid.EndTime >= " + dateTimeHelper.formatDateTimeForSQL(start)  
					+ " AND pid.EndTime <= " + dateTimeHelper.formatDateTimeForSQL(end) + ")";
				if (logger.isDebugEnabled()) {
					logger.logDebug("getSystems SQL: " + SQL);
				}
				
				stmt = conn.createStatement();
				
				rs = stmt.executeQuery(SQL);
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
		
			if (JDBCHelper.databaseChangeSetExists(conn, db.getSchema(), REQUIRED_DATABASE_CHANGESET)) {
				PreparedStatement stmt = null;	
				try {
					String schema = fixupSchema(db.getSchema());
					
					String SQL = "SELECT COUNT(*) "
						+ " FROM " + schema + "FSSFetchThreadPoolSnapshot pid "
						+ "	WHERE pid.EndTime >=  " + dateTimeHelper.formatDateTimeForSQL(start)
						+ " AND pid.EndTime <= "  + dateTimeHelper.formatDateTimeForSQL(end)
						+ " AND pid.SystemID IN " + buildInArrayForSystems(systems);
					if (logger.isDebugEnabled()) {
						logger.logDebug("getSystems SQL: " + SQL);
					}
					stmt = conn.prepareStatement(SQL);
					if (JDBCHelper.getQueryCount(stmt) > 0) {
						result.add(new Category(TEMPLATE_NAME, TEMPLATE_NAME));
					}
				} finally {
					JDBCHelper.closeNoThrow(stmt);
				}
			}
		
		return result;
	}

	private static class FSSFetchThreadPool extends CategoryTemplate {

		private FSSFetchThreadPool(String templateName) {
			super(templateName, buildFields());
		}
		
		private static final Field[] buildFields() {
			List<Field> fields = new ArrayList<Field>();
			
			fields.add(new ProviderField("minThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM, "MINTHREADS", false));
			fields.add(new ProviderField("maxThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM, "MAXTHREADS", false));
			fields.add(new ProviderField("activeThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM, "ACTIVETHREADS", false));
			fields.add(new ProviderField("poolThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM, "POOLTHREADS", false));
			fields.add(new ProviderField("peakThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PEAKTHREADS", false));
			fields.add(new ProviderField("queueCapacity", AggregationMethod.DEFAULT, AggregationMethod.SUM, "QUEUECAPACITY", false));
			fields.add(new ProviderField("queueRemaining", AggregationMethod.DEFAULT, AggregationMethod.SUM, "QUEUEREMAINING", false));
			fields.add(new ProviderField("tasksPending", AggregationMethod.DEFAULT, AggregationMethod.SUM, "TASKSPENDING", false));
			fields.add(new ProviderField("tasksCompleted", AggregationMethod.DEFAULT, AggregationMethod.SUM, "TASKSCOMPLETED", false));
			fields.add(new ProviderField("tasksRejected", AggregationMethod.DEFAULT, AggregationMethod.SUM, "TASKSREJECTED", false));
			
//			<column name="ratioMaxThreadsInUse" type="DECIMAL(18,2)">
			fields.add(new PercentProviderField("maxThreadsInUsePercent", "SystemID", "ACTIVETHREADS", "MAXTHREADS").makePrimary());

//			<column name="ratioCurrentThreadsInUse" type="DECIMAL(18,2)">
			fields.add(new PercentProviderField("currentThreadsInUsePercent", "SystemID", "ACTIVETHREADS", "POOLTHREADS").makePrimary());
			
//			<column name="ratioQueueInUse" type="DECIMAL(18,2)">
			fields.add(new PercentProviderField("queueInUsePercent", "SystemID", "TASKSPENDING", "QUEUECAPACITY").makePrimary());
			
			return fields.toArray(new Field[]{});
		}
	}
}
