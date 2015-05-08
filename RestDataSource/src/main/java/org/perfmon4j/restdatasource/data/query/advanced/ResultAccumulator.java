package org.perfmon4j.restdatasource.data.query.advanced;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.perfmon4j.restdatasource.RestImpl.SystemID;
import org.perfmon4j.restdatasource.util.DateTimeHelper;
import org.perfmon4j.restdatasource.util.SeriesField;
import org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;

public class ResultAccumulator {
	private final Set<Long> times = new TreeSet<Long>();
	private final Map<String, Set<SeriesWrapper>> seriesMap = new HashMap<String, Set<SeriesWrapper>>();
	private final Set<Series> allSeries = new HashSet<Series>();
	private final DateTimeHelper helper = new DateTimeHelper();

	public void addSeries(SeriesField series, AggregatorFactory factory) {
		this.addSeries(series.getProvider().getTemplateName(), factory, series.getAlias(), SystemID.toString(series.getSystems()), series.getCategory().getName(), 
				series.getField().getName(), series.getAggregationMethod().toString());
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
