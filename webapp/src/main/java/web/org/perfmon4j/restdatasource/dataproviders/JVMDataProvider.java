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
import java.sql.Statement;
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
import web.org.perfmon4j.restdatasource.util.DateTimeHelper;
import web.org.perfmon4j.restdatasource.util.SeriesField;

public class JVMDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "JVM";
	private final JVMTemplate categoryTemplate;
	private static final Logger logger = LoggerFactory.initLogger(JVMDataProvider.class);
	private final DateTimeHelper dateTimeHelper = new DateTimeHelper();
	
	public JVMDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new JVMTemplate(TEMPLATE_NAME);
	}

	@Override
	public void processResults(Connection conn, RegisteredDatabaseConnections.Database db, ResultAccumulator accumulator, SeriesField[] fields, long start, 
			long end) throws SQLException {
		String schemaPrefix = fixupSchema(db.getSchema());
		Set<String> selectList = buildSelectListAndPopulateAccumulators(accumulator, fields);
		
		selectList.add("endTime");
		
		String query =
			"SELECT " + commaSeparate(selectList) + "\r\n"
			+ "FROM " + schemaPrefix + "P4JVMSnapShot pid\r\n"
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

	@Override
	public CategoryTemplate getCategoryTemplate() {
		return categoryTemplate;
	}

	@Override
	public Set<MonitoredSystem> lookupMonitoredSystems(Connection conn, RegisteredDatabaseConnections.Database database, 
			long start, long end) throws SQLException {
		Set<MonitoredSystem> result = new HashSet<MonitoredSystem>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			String schema = fixupSchema(database.getSchema());
			
			String SQL = "SELECT SystemID, SystemName "
				+ " FROM " + schema + "P4JSystem s "
				+ " WHERE EXISTS (SELECT SystemID " 
				+ " FROM " + schema + "P4JVMSnapShot pid WHERE pid.SystemID = s.SystemID "
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
		
			PreparedStatement stmt = null;	
			try {
				String schema = fixupSchema(db.getSchema());
				
				String SQL = "SELECT COUNT(*) "
					+ " FROM " + schema + "P4JVMSnapShot pid "
					+ "	WHERE pid.EndTime >= ? "
					+ " AND pid.EndTime <= ?"
					+ " AND pid.SystemID IN " + buildInArrayForSystems(systems);
				if (logger.isDebugEnabled()) {
					logger.logDebug("getSystems SQL: " + SQL);
				}
				stmt = conn.prepareStatement(SQL);
				stmt.setTimestamp(1, new Timestamp(start));
				stmt.setTimestamp(2, new Timestamp(end));
				if (JDBCHelper.getQueryCount(stmt) > 0) {
					result.add(new Category(TEMPLATE_NAME, TEMPLATE_NAME));
				}
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		
		return result;
	}

	private static class JVMTemplate extends CategoryTemplate {

		private JVMTemplate(String templateName) {
			super(templateName, buildFields());
		}
		
		private static final Field[] buildFields() {
			List<Field> fields = new ArrayList<Field>();

			fields.add(new ProviderField("currentClassLoadCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "CurrentClassLoadCount", false));
			fields.add(new ProviderField("pendingClassFinalizationCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PendingClassFinalizationCount", false));
			fields.add(new ProviderField("currentThreadCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "CurrentThreadCount", false).makePrimary());
			fields.add(new ProviderField("currentDaemonThreadCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "CurrentDaemonThreadCount", false));
			fields.add(new ProviderField("heapMemUsedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "HeapMemUsedMB", true));
			fields.add(new ProviderField("heapMemCommittedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "HeapMemCommitedMB", true));
			fields.add(new ProviderField("heapMemMaxMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "HeapMemMaxMB", true));
			fields.add(new PercentProviderField("percentHeapMemInUse", "systemID", "HeapMemUsedMB", "HeapMemMaxMB").makePrimary());
			fields.add(new PercentProviderField("percentHeapMemCommitted", "systemID", "HeapMemCommitedMB", "HeapMemMaxMB"));
			fields.add(new ProviderField("nonHeapMemUsedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "NonHeapMemUsedMB", true));
			fields.add(new ProviderField("nonHeapMemCommittedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "NonHeapMemCommittedUsedMB", true));
			fields.add(new ProviderField("nonHeapMemMaxMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "NonHeapMemMaxUsedMB", true));
			fields.add(new PercentProviderField("percentNonHeapMemInUse", "systemID", "nonHeapMemUsedMB", "NonHeapMemMaxUsedMB").makePrimary());
			fields.add(new PercentProviderField("percentNonHeapMemCommitted", "systemID", "NonHeapMemCommittedUsedMB", "NonHeapMemMaxUsedMB"));
			fields.add(new ProviderField("systemCpuLoad", AggregationMethod.DEFAULT, AggregationMethod.MAX, "systemCpuLoad", true).makePrimary());
			fields.add(new ProviderField("processCpuLoad", AggregationMethod.DEFAULT, AggregationMethod.MAX, "processCpuLoad", true).makePrimary());
			fields.add(new NaturalPerMinuteProviderField("classLoadCountPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, "ClassLoadCountPerMinute", "systemID", "startTime", "endTime", "ClassLoadCountInPeriod", true));
			fields.add(new NaturalPerMinuteProviderField("classUnloadCountPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, "ClassUnloadCountPerMinute", "systemID", "startTime", "endTime", "ClassUnloadCountInPeriod", true));
			fields.add(new NaturalPerMinuteProviderField("threadStartCountPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, "ThreadStartCountPerMinute", "systemID", "startTime", "endTime", "ThreadStartCountInPeriod", true));
			fields.add(new NaturalPerMinuteProviderField("compilationMillisPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, "CompilationMillisPerMinute", "systemID", "startTime", "endTime", "CompilationMillisInPeriod", true));
			
			return fields.toArray(new Field[]{});
		}
	}
}
