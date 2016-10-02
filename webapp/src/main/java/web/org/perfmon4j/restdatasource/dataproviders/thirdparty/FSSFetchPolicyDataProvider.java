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
import web.org.perfmon4j.restdatasource.DataSourceRestImpl.SystemID;
import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.data.Category;
import web.org.perfmon4j.restdatasource.data.CategoryTemplate;
import web.org.perfmon4j.restdatasource.data.Field;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.query.advanced.ResultAccumulator;
import web.org.perfmon4j.restdatasource.dataproviders.PercentProviderField;
import web.org.perfmon4j.restdatasource.dataproviders.ProviderField;
import web.org.perfmon4j.restdatasource.util.SeriesField;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.decorator.ColumnValueFilterFactory;

public class FSSFetchPolicyDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "FSSFetchPolicy";
	private final FSSFetchPolicy categoryTemplate;
	static final String REQUIRED_DATABASE_CHANGESET = "FSS-P4JFetchPolicySnapshot-tableCreate";
	static final String ADD_ASYNC_COLUMN_CHANGESET = "FSS-addProviderAsyncCountColumn";
	private static final Logger logger = LoggerFactory.initLogger(FSSFetchPolicyDataProvider.class);
	
	public FSSFetchPolicyDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new FSSFetchPolicy(TEMPLATE_NAME);
	}
	

	@Override
	public AggregatorFactory wrapWithCategoryLevelFilter(AggregatorFactory factory, String subCategoryName) {
		return  new ColumnValueFilterFactory(factory, "InstanceName", new String[]{subCategoryName});
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
				+ "FROM " + schemaPrefix + "FSSFetchPolicySnapshot pid\r\n"
				+ "WHERE pid.systemID IN " + buildSystemIDSet(fields) + "\r\n"
				+ "AND pid.InstanceName IN "+ buildSubCategoryNameSet(fields) + "\r\n"
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
		
		if (JDBCHelper.databaseChangeSetExists(conn, database.getSchema(), REQUIRED_DATABASE_CHANGESET)) {
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				String schema = fixupSchema(database.getSchema());
				
				String SQL = "SELECT SystemID, SystemName "
					+ " FROM " + schema + "P4JSystem s "
					+ " WHERE EXISTS (SELECT SystemID " 
					+ " FROM " + schema + "FSSFetchPolicySnapshot pid WHERE pid.SystemID = s.SystemID "
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
		
			if (JDBCHelper.databaseChangeSetExists(conn, db.getSchema(), REQUIRED_DATABASE_CHANGESET)) {
				PreparedStatement stmt = null;
				ResultSet rs = null;
				try {
					String schema = fixupSchema(db.getSchema());
					
					String SQL = "SELECT DISTINCT(InstanceName) "
						+ " FROM " + schema + "FSSFetchPolicySnapshot pid "
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

	private static class FSSFetchPolicy extends CategoryTemplate {
		private FSSFetchPolicy(String templateName) {
			super(templateName, buildFields());
		}
		
		private static final Field[] buildFields() {
			List<Field> fields = new ArrayList<Field>();
			
			fields.add(new ProviderField("L2CacheCumulativeTime", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L2CACHECUMULATIVETIME", false));
			fields.add(new ProviderField("L2CacheQueryCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L2CACHEQUERYCOUNT", false));
			fields.add(new ProviderField("L2CacheHitCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L2CACHEHITCOUNT", false));
			fields.add(new ProviderField("L2CacheMissCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L2CACHEMISSCOUNT", false));
			fields.add(new ProviderField("L2CachePutCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L2CACHEPUTCOUNT", false));
			fields.add(new ProviderField("L2CacheDeleteCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L2CACHEDELETECOUNT", false));
			fields.add(new PercentProviderField("L2CacheHitPercent", "SystemID", "L2CACHEHITCOUNT", "L2CACHEQUERYCOUNT").makePrimary());
			fields.add(new ProviderField("L2CacheTimePerQuery", AggregationMethod.DEFAULT, AggregationMethod.AVERAGE, "L2TIMEPERQUERY", true));
			
			fields.add(new ProviderField("L3CacheCumulativeTime", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L3CACHECUMULATIVETIME", false));
			fields.add(new ProviderField("L3CacheQueryCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L3CACHEQUERYCOUNT", false));
			fields.add(new ProviderField("L3CacheHitCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L3CACHEHITCOUNT", false));
			fields.add(new ProviderField("L3CacheMissCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L3CACHEMISSCOUNT", false));
			fields.add(new ProviderField("L3CachePutCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L3CACHEPUTCOUNT", false));
			fields.add(new ProviderField("L3CacheDeleteCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "L3CACHEDELETECOUNT", false));
			fields.add(new PercentProviderField("L3CacheHitPercent", "SystemID", "L3CACHEHITCOUNT", "L3CACHEQUERYCOUNT").makePrimary());
			fields.add(new ProviderField("L3CacheTimePerQuery", AggregationMethod.DEFAULT, AggregationMethod.AVERAGE, "L3TIMEPERQUERY", true));

			fields.add(new ProviderField("providerCumulativeTime", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROVIDERCUMULATIVETIME", false));
			fields.add(new ProviderField("providerQueryCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROVIDERQUERYCOUNT", false));
			fields.add(new ProviderField("providerHitCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROVIDERHITCOUNT", false));
			fields.add(new ProviderField("providerMissCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROVIDERMISSCOUNT", false));
			fields.add(new ProviderField("providerWaiveCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROVIEDRWAIVECOUNT", false));
			fields.add(new ProviderField("providerErrorCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROVIDERERRORCOUNT", false));
			fields.add(new PercentProviderField("providerHitPercent", "SystemID", "PROVIDERHITCOUNT", "PROVIDERQUERYCOUNT").makePrimary());
			fields.add(new PercentProviderField("providerWaivePercent", "SystemID", "PROVIEDRWAIVECOUNT", "PROVIDERQUERYCOUNT").makePrimary());
			fields.add(new ProviderField("providerTimePerQuery", AggregationMethod.DEFAULT, AggregationMethod.AVERAGE, "PROVIDERTIMEPERQUERY", true));
			
			fields.add(new ProviderField("promiseBlockCumulativeTime", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROMISEBLOCKCULULATIVETIME", false));
			fields.add(new ProviderField("promiseOwnCumulativeTime", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROMISEOWNCUMULATIVETIME", false));
			fields.add(new ProviderField("promiseBlockedThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROMISEBLOCKEDTHREADS", false).makePrimary());
			fields.add(new ProviderField("promiseExpirations", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROMISEEXPIRATIONS", false));
			fields.add(new ProviderField("promiseCancellations", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROMISECANCELLATIONS", false));
		
			fields.add(new ProviderField("providerAsyncCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PROVIDERASYNCCOUNT", false)
				.requiresChangeSet(ADD_ASYNC_COLUMN_CHANGESET)
				.makePrimary());
			
			return fields.toArray(new Field[]{});
		}
	}
}
