package org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MaxAggregatorFactory implements AggregatorFactory {
	private final String databaseColumn;
	private final boolean floatingPoint;
	
	
	public MaxAggregatorFactory(String databaseColumn, boolean floatingPoint) {
		this.databaseColumn = databaseColumn;
		this.floatingPoint = floatingPoint;
	}
	
	@Override
	public Aggregator newAggregator() {
		return floatingPoint ? new FloatingPoint() : new FixedPoint();
	}
	
	@Override
	public String[] getDatabaseColumns() {
		return new String[]{databaseColumn};
	}
			
	
	private final class FloatingPoint implements Aggregator {
		private boolean hasValue = false;
		private double maxValue = 0;
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			double d = rs.getDouble(databaseColumn);
			if (!rs.wasNull()) {
				if (!hasValue || d > maxValue) {
					maxValue = d;
				}
				hasValue = true;
			}
		}

		@Override
		public Number getResult() {
			if (hasValue) {
				return Double.valueOf(maxValue);
			} else {
				return null;
			}
		}
	}
	
	private final class FixedPoint implements Aggregator {
		private boolean hasValue = false;
		private long maxValue = Long.MIN_VALUE;
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			long l = rs.getLong(databaseColumn);
			if (!rs.wasNull()) {
				hasValue = true;
				if (l > maxValue) {
					maxValue = l;
				}
			}
		}

		@Override
		public Number getResult() {
			if (hasValue) {
				return Long.valueOf(maxValue);
			} else {
				return null;
			}
		}
	}
}
