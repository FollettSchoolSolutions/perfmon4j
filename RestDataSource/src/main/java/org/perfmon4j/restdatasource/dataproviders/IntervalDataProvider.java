package org.perfmon4j.restdatasource.dataproviders;

import java.util.ArrayList;
import java.util.List;

import org.perfmon4j.restdatasource.DataProvider;
import org.perfmon4j.restdatasource.data.AggregationMethod;
import org.perfmon4j.restdatasource.data.CategoryTemplate;
import org.perfmon4j.restdatasource.data.Field;
import org.perfmon4j.restdatasource.util.SeriesField;

public class IntervalDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "Interval";
	private final IntervalTemplate categoryTemplate;
	
	public IntervalDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new IntervalTemplate(TEMPLATE_NAME);
	}

	@Override
	public String buildQueryString(long[] systemID, String category, SeriesField[] fields) {
		// TODO Auto-generated method stub
		return null;
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
			
			fields.add(new Field("maxActiveThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM));
			fields.add(new Field("maxDuration", AggregationMethod.DEFAULT, AggregationMethod.MAX));
			fields.add(new Field("minDuration", AggregationMethod.DEFAULT, AggregationMethod.MIN));
			fields.add(new Field("throughputPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL));
			fields.add(new Field("averageDuration", AggregationMethod.DEFAULT_WITH_NATURAL));
			fields.add(new Field("medianDuration", AggregationMethod.DEFAULT, AggregationMethod.AVERAGE));
			fields.add(new Field("standardDeviation", AggregationMethod.DEFAULT_WITH_NATURAL));
			fields.add(new Field("sqlMaxDuration", AggregationMethod.DEFAULT, AggregationMethod.MAX));
			fields.add(new Field("sqlLMinDuration", AggregationMethod.DEFAULT, AggregationMethod.MIN));
			fields.add(new Field("sqlAverageDuration", AggregationMethod.DEFAULT_WITH_NATURAL));
			fields.add(new Field("sqlStandardDeviation", AggregationMethod.DEFAULT_WITH_NATURAL));
			
			return fields.toArray(new Field[]{});
		}
	}
}
