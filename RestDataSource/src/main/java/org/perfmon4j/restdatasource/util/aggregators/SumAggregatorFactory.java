package org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SumAggregatorFactory implements AggregatorFactory {
	private final String databaseColumn;
	private final boolean floatingPoint;
	
	
	public SumAggregatorFactory(String databaseColumn, boolean floatingPoint) {
		this.databaseColumn = databaseColumn;
		this.floatingPoint = floatingPoint;
	}
	
	@Override
	public Aggregator newAggregator() {
		return floatingPoint ? new FloatingPoint() : new FixedPoint();
	}
	
	private final class FloatingPoint implements Aggregator {
		private boolean hasValue = false;
		private double accumulator = 0;
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			hasValue = true;
			double d = rs.getDouble(databaseColumn);
			if (!rs.wasNull()) {
				accumulator += d;
			}
		}

		@Override
		public Number getResult() {
			if (hasValue) {
				return Double.valueOf(accumulator);
			} else {
				return null;
			}
		}
	}
	
	private final class FixedPoint implements Aggregator {
		private boolean hasValue = false;
		private long accumulator = 0;
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			hasValue = true;
			long l = rs.getLong(databaseColumn);
			if (!rs.wasNull()) {
				accumulator += l;
			}
		}

		@Override
		public Number getResult() {
			if (hasValue) {
				return Long.valueOf(accumulator);
			} else {
				return null;
			}
		}
	}

}
