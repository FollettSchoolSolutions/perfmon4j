package org.perfmon4j.restdatasource.data;

import java.util.ArrayList;
import java.util.List;

public class IntervalTemplate extends CategoryTemplate {

	public IntervalTemplate() {
		super("Interval", buildFields());
	}
	
	private static final Field[] buildFields() {
		List<Field> fields = new ArrayList<Field>();
		
		fields.add(new Field("MaxActiveThreads", AggregationType.DEFAULT, AggregationType.SUM));
		fields.add(new Field("MaxDuration", AggregationType.DEFAULT, AggregationType.MAX));
		fields.add(new Field("MinDuration", AggregationType.DEFAULT, AggregationType.MIN));
		fields.add(new Field("ThroughputPerMinute", AggregationType.DEFAULT_WITH_NATURAL));
		fields.add(new Field("AverageDuration", AggregationType.DEFAULT_WITH_NATURAL));
		fields.add(new Field("MedianDuration", AggregationType.DEFAULT, AggregationType.AVERAGE));
		fields.add(new Field("StandardDeviation", AggregationType.DEFAULT_WITH_NATURAL));
		fields.add(new Field("SQLMaxDuration", AggregationType.DEFAULT, AggregationType.MAX));
		fields.add(new Field("SQLMinDuration", AggregationType.DEFAULT, AggregationType.MIN));
		fields.add(new Field("SQLAverageDuration", AggregationType.DEFAULT_WITH_NATURAL));
		fields.add(new Field("SQLStandardDeviation", AggregationType.DEFAULT_WITH_NATURAL));
		
		return fields.toArray(new Field[]{});
	}
}
