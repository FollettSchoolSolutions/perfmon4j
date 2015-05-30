package war.org.perfmon4j.restdatasource.dataproviders;

import org.jboss.resteasy.spi.BadRequestException;

import war.org.perfmon4j.restdatasource.data.AggregationMethod;
import war.org.perfmon4j.restdatasource.data.Field;
import war.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import war.org.perfmon4j.restdatasource.util.aggregators.AverageAggregatorFactory;
import war.org.perfmon4j.restdatasource.util.aggregators.MaxAggregatorFactory;
import war.org.perfmon4j.restdatasource.util.aggregators.MinAggregatorFactory;
import war.org.perfmon4j.restdatasource.util.aggregators.SumAggregatorFactory;


public class ProviderField extends Field {
	protected boolean floatingPoint = false;
	protected String databaseColumn = null;
	
	public ProviderField(String name, AggregationMethod[] aggregationMethods,
			AggregationMethod defaultAggregationMethod, String databaseColumn, boolean floatingPoint) {
		super(name, aggregationMethods, defaultAggregationMethod);
		this.databaseColumn = databaseColumn;
		this.floatingPoint = floatingPoint;
	}

	public AggregatorFactory buildFactory(AggregationMethod method) {
		AggregatorFactory factory = null;
		
		if (method.equals(AggregationMethod.SUM)) {
			factory = new SumAggregatorFactory(databaseColumn, floatingPoint);
		} else if (method.equals(AggregationMethod.MAX)) {
			factory = new MaxAggregatorFactory(databaseColumn, floatingPoint);
		} else if (method.equals(AggregationMethod.MIN)) {
			factory = new MinAggregatorFactory(databaseColumn, floatingPoint);
		} else if (method.equals(AggregationMethod.AVERAGE)) {
			factory = new AverageAggregatorFactory(databaseColumn, floatingPoint);
		} else {
			throw new BadRequestException("Aggregation method not supported for field");	
		}

		return factory;
	}
}
