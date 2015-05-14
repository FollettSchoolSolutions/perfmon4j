package org.perfmon4j.restdatasource.dataproviders;

import org.perfmon4j.restdatasource.data.AggregationMethod;
import org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import org.perfmon4j.restdatasource.util.aggregators.NaturalPerMinuteAggregatorFactory;

public class NaturalPerMinuteProviderField extends ProviderField {
	protected String startTimeColumn;
	protected String endTimeColumn;
	protected String counterColumn;

	public NaturalPerMinuteProviderField(String name,
			AggregationMethod[] aggregationMethods,
			String databaseColumn, 
			String startTimeColumn,
			String endTimeColumn,
			String counterColumn,
			boolean floatingPoint) {
		super(name, aggregationMethods, AggregationMethod.NATURAL, databaseColumn,
				floatingPoint);
		this.startTimeColumn = startTimeColumn;
		this.endTimeColumn = endTimeColumn;
		this.counterColumn = counterColumn;
	}

	@Override
	AggregatorFactory buildFactory(AggregationMethod method) {
		if (method.equals(AggregationMethod.NATURAL)) {
			return new NaturalPerMinuteAggregatorFactory(startTimeColumn, endTimeColumn, counterColumn);
		} else {
			return super.buildFactory(method);
		}
	}
}
