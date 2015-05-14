	package org.perfmon4j.restdatasource.dataproviders;

import org.jboss.resteasy.spi.BadRequestException;
import org.perfmon4j.restdatasource.data.AggregationMethod;
import org.perfmon4j.restdatasource.data.Field;
import org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import org.perfmon4j.restdatasource.util.aggregators.AverageAggregatorFactory;
import org.perfmon4j.restdatasource.util.aggregators.MaxAggregatorFactory;
import org.perfmon4j.restdatasource.util.aggregators.MinAggregatorFactory;
import org.perfmon4j.restdatasource.util.aggregators.SumAggregatorFactory;


public class ProviderField extends Field {
	protected boolean floatingPoint = false;
	protected String databaseColumn = null;
	
	public ProviderField(String name, AggregationMethod[] aggregationMethods,
			AggregationMethod defaultAggregationMethod, String databaseColumn, boolean floatingPoint) {
		super(name, aggregationMethods, defaultAggregationMethod);
		this.databaseColumn = databaseColumn;
		this.floatingPoint = floatingPoint;
	}

	AggregatorFactory buildFactory(AggregationMethod method) {
		AggregatorFactory factory = null;
		
		switch (method) {
			case SUM:
				factory = new SumAggregatorFactory(databaseColumn, floatingPoint);
				break;
				
			case MAX:
				factory = new MaxAggregatorFactory(databaseColumn, floatingPoint);
				break;
				
			case MIN:
				factory = new MinAggregatorFactory(databaseColumn, floatingPoint);
				break;
				
			case AVERAGE:
				factory = new AverageAggregatorFactory(databaseColumn, floatingPoint);
				break;
				
			case NATURAL:
			default:
				throw new BadRequestException("Aggregation method not supported for field");	
		}

		return factory;
	}
}
