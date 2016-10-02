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
import web.org.perfmon4j.restdatasource.DataSourceRestImpl.SystemID;
import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.data.Category;
import web.org.perfmon4j.restdatasource.data.CategoryTemplate;
import web.org.perfmon4j.restdatasource.data.Field;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.query.advanced.ResultAccumulator;
import web.org.perfmon4j.restdatasource.util.SeriesField;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.decorator.ColumnValueFilterFactory;

public class CacheDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "Cache";
	private final CacheTemplate categoryTemplate;
	private static final Logger logger = LoggerFactory.initLogger(CacheDataProvider.class);
	
	public CacheDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new CacheTemplate(TEMPLATE_NAME);
	}
	

	@Override
	public AggregatorFactory wrapWithCategoryLevelFilter(AggregatorFactory factory, String subCategoryName) {
		String[] subCats = splitSubCategory(subCategoryName);
		
		factory = new ColumnValueFilterFactory(factory, "CacheType", new String[]{subCats[0]}); 
		return  new ColumnValueFilterFactory(factory, "InstanceName", new String[]{subCats[1]});
	}	

	private Set<String> handledCalculatedField(Set<String> selectColumns) {
		Set<String> result = new HashSet<String>();
		for (String s : selectColumns) {
			if ("CalculatedHitsPlusMisses".equals(s)) {
				s = "(HitCount + MissCount) AS " + s;
			}
			result.add(s);
		}
		return result;
	}
	
	
	@Override
	public void processResults(Connection conn, RegisteredDatabaseConnections.Database db, ResultAccumulator accumulator, SeriesField[] fields, long start, 
			long end) throws SQLException {
		String schemaPrefix = fixupSchema(db.getSchema());
		Set<String> selectList = buildSelectListAndPopulateAccumulators(accumulator, fields);
		selectList = handledCalculatedField(selectList);
		
		selectList.add("endTime");
		String str[] = buildSubSubCategoryNameSets(fields);
		
		String query =
			"SELECT " + commaSeparate(selectList) + "\r\n"
			+ "FROM " + schemaPrefix + "P4JCache pid\r\n"
			+ "WHERE pid.systemID IN " + buildSystemIDSet(fields) + "\r\n"
			+ "AND pid.CacheType IN "+ str[0] + "\r\n"
			+ "AND pid.InstanceName IN "+ str[1] + "\r\n"
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
				+ " FROM " + schema + "P4JCache pid WHERE pid.SystemID = s.SystemID "
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
			ResultSet rs = null;
			try {
				String schema = fixupSchema(db.getSchema());
				
				String SQL = "SELECT CacheType, InstanceName "
					+ " FROM " + schema + "P4JCache pid "
					+ "	WHERE pid.EndTime >= ? "
					+ " AND pid.EndTime <= ?"
					+ " AND pid.SystemID IN " + buildInArrayForSystems(systems)
					+ " GROUP BY CacheType, InstanceName";
				if (logger.isDebugEnabled()) {
					logger.logDebug("getCategories SQL: " + SQL);
				}
				stmt = conn.prepareStatement(SQL);
				stmt.setTimestamp(1, new Timestamp(start));
				stmt.setTimestamp(2, new Timestamp(end));
				rs = stmt.executeQuery();
				while (rs.next()) {
					String cacheType = rs.getString(1).trim();
					String instanceName = rs.getString(2).trim();
					result.add(new Category(TEMPLATE_NAME + "." + cacheType + "." + instanceName, TEMPLATE_NAME));
				}
			} finally {
				JDBCHelper.closeNoThrow(rs);
				JDBCHelper.closeNoThrow(stmt);
			}
		
		return result;
	}
	
	
	private static class CacheTemplate extends CategoryTemplate {

		private CacheTemplate(String templateName) {
			super(templateName, buildFields());
		}
		
		private static final Field[] buildFields() {
			List<Field> fields = new ArrayList<Field>();

			fields.add(new ProviderField("hitCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "HitCount", false));
			fields.add(new ProviderField("missCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "MissCount", false));
			fields.add(new ProviderField("putCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PutCount", false));
			fields.add(new PercentProviderField("hitPercent", "systemID", "HitCount", "CalculatedHitsPlusMisses").makePrimary());
			
			return fields.toArray(new Field[]{});
		}
	}
}
