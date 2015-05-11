package org.perfmon4j.restdatasource.util.aggregators.decorator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;

public class ColumnValueFilterFactory implements AggregatorFactory {
	private final AggregatorFactory delegate;
	private final String columnName;
	private final Set<String> values = new HashSet<String>();
	
	public ColumnValueFilterFactory(AggregatorFactory delegate,
			String columnName, String[] values) {
		super();
		this.delegate = delegate;
		this.columnName = columnName;
		this.values.addAll(Arrays.asList(values));
	}

	@Override
	public Aggregator newAggregator() {
		Aggregator d = delegate.newAggregator();
		return new ColumnValueFilter(d);
	}

	private class ColumnValueFilter implements Aggregator {
		private final Aggregator delegate;
		
		public ColumnValueFilter(Aggregator delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			String filterValue = rs.getString(columnName);
			if (values.contains(filterValue)) {		
				delegate.aggreagate(rs);
			}
		}

		@Override
		public Number getResult() {
			return delegate.getResult();
		}
	}
	
}
