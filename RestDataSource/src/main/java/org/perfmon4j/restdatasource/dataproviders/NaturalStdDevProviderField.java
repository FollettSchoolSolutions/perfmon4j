package org.perfmon4j.restdatasource.dataproviders;

import org.perfmon4j.restdatasource.data.AggregationMethod;
import org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import org.perfmon4j.restdatasource.util.aggregators.NaturalStdDevAggregatorFactory;

public class NaturalStdDevProviderField extends ProviderField {
	protected String numeratorColumn;
	protected String sumOfSquaresColumn;
	protected String denominatorColumn;

	public NaturalStdDevProviderField(String name,
			AggregationMethod[] aggregationMethods,
			String databaseColumn, 
			String numeratorColumn,
			String sumOfSquaresColumn,
			String denominatorColumn,
			boolean floatingPoint) {
		super(name, aggregationMethods, AggregationMethod.NATURAL, databaseColumn,
				floatingPoint);
		this.numeratorColumn = numeratorColumn;
		this.sumOfSquaresColumn = sumOfSquaresColumn;
		this.denominatorColumn = denominatorColumn;
	}

	@Override
	public AggregatorFactory buildFactory(AggregationMethod method) {
		if (method.equals(AggregationMethod.NATURAL)) {
			return new NaturalStdDevAggregatorFactory(numeratorColumn, sumOfSquaresColumn, denominatorColumn);
		} else {
			return super.buildFactory(method);
		}
	}
}
