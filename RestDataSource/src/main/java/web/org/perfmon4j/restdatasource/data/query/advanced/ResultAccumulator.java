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
package web.org.perfmon4j.restdatasource.data.query.advanced;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import web.org.perfmon4j.restdatasource.RestImpl.SystemID;
import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.util.DateTimeHelper;
import web.org.perfmon4j.restdatasource.util.SeriesField;
import web.org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;

public class ResultAccumulator {
	private final Set<Long> times = new TreeSet<Long>();
	private final Map<String, Set<SeriesWrapper>> seriesMap = new HashMap<String, Set<SeriesWrapper>>();
	private final List<Series> allSeries = new ArrayList<Series>();
	private final DateTimeHelper helper = new DateTimeHelper();

	public ResultAccumulator() {
		super();
	}
	
	public void addSeries(SeriesField series) {
		String aggregationMethodDisplayName = null;
		AggregationMethod method = series.getAggregationMethod();
		if (method != null) {
			aggregationMethodDisplayName = method.toString();
		}
		
		this.addSeries(series.getProvider().getTemplateName(), series.getFactory(), series.getAlias(), SystemID.toString(series.getSystems()), series.getCategory().getName(), 
				series.getField().getName(), aggregationMethodDisplayName);
	}
	
	// Package level to make unit testing easier
	void addSeries(String templateName, AggregatorFactory factory, String alias, String systemID, String category, 
			String fieldName, String aggregationMethod) {
		Series series = new Series();
		series.setAlias(alias);
		series.setAggregationMethod(aggregationMethod);
		series.setCategory(category);
		series.setFieldName(fieldName);
		series.setSystemID(systemID);
		
		getSeriesForProvider(templateName).add(new SeriesWrapper(series, factory));
		allSeries.add(series);
	}
	
	public void accumulateResults(String templateName, ResultSet rs) throws SQLException {
		long time = rs.getTimestamp("EndTime").getTime();
		
		Aggregator[] aggregators = getAggregators(templateName, time);
		for (Aggregator a : aggregators) {
			a.aggreagate(rs);
		}
	}

	public Aggregator[] getAggregators(String templateName, long time) {
		Set<SeriesWrapper> series = seriesMap.get(templateName);
		Aggregator[] result = new Aggregator[series.size()];
		
		Long timeKey = Long.valueOf(helper.truncateToMinute(time));
		times.add(timeKey);
		
		int i = 0;
		for (SeriesWrapper w : series) {
			Aggregator a = w.map.get(timeKey);
			if (a == null) {
				a = w.factory.newAggregator();
				w.map.put(timeKey, a);
			}
			result[i++] = a;
		}
		
		return result;
	}
	
	public AdvancedQueryResult buildResults() {
		AdvancedQueryResult result = new AdvancedQueryResult();
		
		result.setDateTime(getFormattedTimes());
		result.setSeries(allSeries.toArray(new Series[]{}));
		for (Set<SeriesWrapper> set : seriesMap.values()) {
			for (SeriesWrapper w : set) {
				w.populateValues(times);
			}
		}
		return result;
	}
	
	private String[] getFormattedTimes() {
		String result[] = new String[times.size()];
		
		int i = 0;
		for (Long t : times) {
			result[i++] =  helper.formatDate(t.longValue());
		}
		
		return result;
	}
	
	
	private Set<SeriesWrapper> getSeriesForProvider(String templateName) {
		Set<SeriesWrapper> result = seriesMap.get(templateName);

		if (result == null) {
			result = new HashSet<ResultAccumulator.SeriesWrapper>();
			seriesMap.put(templateName, result);
		}
		
		return result;
	}

	private static class SeriesWrapper {
		private final Series series;
		private final AggregatorFactory factory;
		private final Map<Long, Aggregator> map = new HashMap<Long, Aggregator>();

		public SeriesWrapper(Series series, AggregatorFactory factory) {
			this.series = series;
			this.factory = factory;
		}
		
		public void populateValues(Set<Long> times) {
			Number seriesData[] = new Number[times.size()];
			int i = 0;
			for (Long time : times) {
				Aggregator a = map.get(time);
				if (a != null) {
					seriesData[i] = a.getResult();
				}
				i++;
			}
			series.setValues(seriesData);
		}
	}
}
