package web.org.perfmon4j.restdatasource.util.aggregators;

public interface AggregatorFactory {
	public Aggregator newAggregator();
	public String[] getDatabaseColumns();
}