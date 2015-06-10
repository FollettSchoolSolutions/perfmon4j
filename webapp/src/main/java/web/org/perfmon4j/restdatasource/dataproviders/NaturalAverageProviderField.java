package web.org.perfmon4j.restdatasource.dataproviders;

import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.NaturalAverageAggregatorFactory;

public class NaturalAverageProviderField extends ProviderField {
	protected String numeratorColumn;
	protected String denominatorColumn;

	public NaturalAverageProviderField(String name,
			AggregationMethod[] aggregationMethods,
			String databaseColumn, 
			String numeratorColumn,
			String denominatorColumn,
			boolean floatingPoint) {
		super(name, aggregationMethods, AggregationMethod.NATURAL, databaseColumn,
				floatingPoint);
		this.numeratorColumn = numeratorColumn;
		this.denominatorColumn = denominatorColumn;
	}

	@Override
	public AggregatorFactory buildFactory(AggregationMethod method) {
		if (method.equals(AggregationMethod.NATURAL)) {
			return new NaturalAverageAggregatorFactory(numeratorColumn, denominatorColumn);
		} else {
			return super.buildFactory(method);
		}
	}
}
