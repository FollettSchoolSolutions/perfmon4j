package org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AverageAggregatorFactory implements AggregatorFactory {
	private final String databaseColumn;
	private final boolean floatingPoint;
	
	
	public AverageAggregatorFactory(String databaseColumn, boolean floatingPoint) {
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
		private double accumulator = 0;
		private long numValues = 0;
	
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			double d = rs.getDouble(databaseColumn);
			if (!rs.wasNull()) {
				hasValue = true;
				accumulator += d;
				numValues++;
			}
		}

		@Override
		public Number getResult() {
			if (hasValue) {
				return Double.valueOf(accumulator/numValues);
			} else {
				return null;
			}
		}
	}
	
	private final class FixedPoint implements Aggregator {
		private boolean hasValue = false;
		private long accumulator = 0;
		private long numValues = 0;
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			long l = rs.getLong(databaseColumn);
			if (!rs.wasNull()) {
				hasValue = true;
				accumulator += l;
				numValues++;
			}
		}

		@Override
		public Number getResult() {
			if (hasValue) {
				return numValues > 1 ? Double.valueOf(((double)accumulator)/numValues) : Long.valueOf(accumulator);
			} else {
				return null;
			}
		}
	}

}
