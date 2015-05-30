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

package war.org.perfmon4j.restdatasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.perfmon4j.RegisteredDatabaseConnections;

import war.org.perfmon4j.restdatasource.RestImpl.SystemID;
import war.org.perfmon4j.restdatasource.data.Category;
import war.org.perfmon4j.restdatasource.data.CategoryTemplate;
import war.org.perfmon4j.restdatasource.data.MonitoredSystem;
import war.org.perfmon4j.restdatasource.data.query.advanced.ResultAccumulator;
import war.org.perfmon4j.restdatasource.dataproviders.ProviderField;
import war.org.perfmon4j.restdatasource.util.SeriesField;
import war.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import war.org.perfmon4j.restdatasource.util.aggregators.decorator.ColumnValueFilterFactory;

public abstract class DataProvider {
	private final String templateName;
	
	protected DataProvider(String templateName) {
		this.templateName = templateName;
	}
	
	public abstract Set<MonitoredSystem> lookupMonitoredSystems(Connection conn, RegisteredDatabaseConnections.Database database, 
			long startTime, long endTime) throws SQLException;	
	
	public abstract Set<Category> lookupMonitoredCategories(Connection conn, RegisteredDatabaseConnections.Database db, 
			SystemID systems[], long startTime, long endTime) throws SQLException;
	
	public abstract void processResults(Connection conn, RegisteredDatabaseConnections.Database db, ResultAccumulator accumulator, 
			SeriesField[] fields, long start, long end) throws SQLException;

	/**
	 * If your Data provider has a "sub-category" for example an instance name, use this to include
	 * a filter on the aggregatorFactory to limit the calculation to the specified category.
	 * @param factory
	 * @param subCategoryName
	 * @return
	 */
	protected AggregatorFactory wrapWithCategoryLevelFilter(AggregatorFactory factory, String subCategoryName) {
		return factory;
	}	

	
	protected Set<String> buildSelectListAndPopulateAccumulators(ResultAccumulator accumulator, SeriesField []fields) {
		Set<String> databaseColumnsToSelect = new HashSet<String>();

		for (SeriesField seriesField : fields) {
			Set<String> systemIDString = new HashSet<String>();
			for (SystemID id : seriesField.getSystems()) {
				systemIDString.add(Long.toString(id.getID()));
			}
			ProviderField providerField = (ProviderField)seriesField.getField();
			
			AggregatorFactory aggregator = providerField.buildFactory(seriesField.getAggregationMethod());
			AggregatorFactory categoryFilter = wrapWithCategoryLevelFilter(aggregator, getSubCategoryName(seriesField.getCategory().getName()));
			AggregatorFactory systemIDFilter = new ColumnValueFilterFactory(categoryFilter, "SystemID", systemIDString.toArray(new String[]{}));
			databaseColumnsToSelect.addAll(Arrays.asList(systemIDFilter.getDatabaseColumns()));
			
			seriesField.setFactory(systemIDFilter);
			accumulator.addSeries(seriesField);
		}
		
		return databaseColumnsToSelect;
	}
	
	
	public abstract CategoryTemplate getCategoryTemplate();

	public String getTemplateName() {
		return templateName;
	}
	
	protected String fixupSchema(String schema) {
		return schema == null ? "" : schema + ".";
	}	
	
	protected String buildInArrayForSystems(SystemID systems[]) {
		StringBuilder builder = new StringBuilder();
		builder.append("( ");

		for (SystemID id: systems) {
			if (builder.length() > 2) {
				builder.append(", ");
			}
			builder.append(id.getID());	
		}
		builder.append(" )");
		
		return builder.toString();
	}
	
	/*
	 *  Returns the sub-category of a category template.
	 *  For example for the category "Interval.WebRequest.browse" in the "Interval" template.
	 *  The sub-category is "WebRequest.browse".
	 *  
	 *  If there is no sub-category this method will return null.
	 */
	protected String getSubCategoryName(String category) {
		String result = null;
		
		if (category != null) {
			category = category.trim();
			if (category.startsWith(templateName + ".")) {
				result = category.replaceFirst(templateName + "\\.", "");
			}
		}
		return result;
	}
	
	
	protected String toSQLSet(Set<String> values, boolean quoteForSQL) {
		boolean firstElement = true;
		StringBuilder builder = new StringBuilder("( ");
		
		for(String s : values) {
			if (!firstElement) {
				builder.append(", ");
			}
			firstElement = false;
			if (quoteForSQL) {
				builder.append("'");
			}
			builder.append(s);
			if (quoteForSQL) {
				builder.append("'");
			}
		}
		builder.append(" )");
		
		return builder.toString();
	}
	
	protected String buildSystemIDSet(SeriesField fields[]) {
		Set<String> systemIDs = new HashSet<String>();
		
		for (SeriesField field : fields) {
			for (SystemID id : field.getSystems()) {
				systemIDs.add(Long.toString(id.getID()));
			}
		}
		return toSQLSet(systemIDs, false);
	}
	
	protected String buildCategoryNameSet(SeriesField[] fields) {
		Set<String> categories = new HashSet<String>();
		
		for (SeriesField field : fields) {
			String shortenedCategoryName = getSubCategoryName(field.getCategory().getName());
			categories.add(shortenedCategoryName);
		}
		
		return toSQLSet(categories, true);
	}
	
	protected String commaSeparate(Set<String> set) {
		boolean first = true;
		StringBuilder builder = new StringBuilder();
		
		for (String value : set) {
			if (!first) {
				builder.append(", ");
			}
			first = false;
			builder.append(value);
		}
		
		return builder.toString();
	}
}
