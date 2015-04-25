package org.perfmon4j.restdatasource.data;

import java.util.ArrayList;
import java.util.List;

public class IntervalTemplate extends CategoryTemplate {

	public IntervalTemplate() {
		super("Interval", buildFields());
	}
	
	private static final Field[] buildFields() {
		List<Field> fields = new ArrayList<Field>();
		
		fields.add(new Field("maxActiveThreads", AggregationType.DEFAULT, AggregationType.SUM));
		fields.add(new Field("maxDuration", AggregationType.DEFAULT, AggregationType.MAX));
		fields.add(new Field("minDuration", AggregationType.DEFAULT, AggregationType.MIN));
		fields.add(new Field("throughputPerMinute", AggregationType.DEFAULT_WITH_NATURAL));
		fields.add(new Field("averageDuration", AggregationType.DEFAULT_WITH_NATURAL));
		fields.add(new Field("medianDuration", AggregationType.DEFAULT, AggregationType.AVERAGE));
		fields.add(new Field("standardDeviation", AggregationType.DEFAULT_WITH_NATURAL));
		fields.add(new Field("sqlMaxDuration", AggregationType.DEFAULT, AggregationType.MAX));
		fields.add(new Field("sqlLMinDuration", AggregationType.DEFAULT, AggregationType.MIN));
		fields.add(new Field("sqlAverageDuration", AggregationType.DEFAULT_WITH_NATURAL));
		fields.add(new Field("sqlStandardDeviation", AggregationType.DEFAULT_WITH_NATURAL));
		
		return fields.toArray(new Field[]{});
	}
}
