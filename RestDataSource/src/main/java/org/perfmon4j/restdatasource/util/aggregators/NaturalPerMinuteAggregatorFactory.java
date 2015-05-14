package org.perfmon4j.restdatasource.util.aggregators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class NaturalPerMinuteAggregatorFactory implements AggregatorFactory {
	private final String databaseColumnStartTime;
	private final String databaseColumnEndTime;
	private final String databaseColumnCounter;
	
	
	public NaturalPerMinuteAggregatorFactory(String databaseColumnStartTime, String databaseColumnEndTime, String databaseColumnCounter) {
		this.databaseColumnStartTime = databaseColumnStartTime;
		this.databaseColumnEndTime = databaseColumnEndTime;
		this.databaseColumnCounter = databaseColumnCounter;
	}
	
	@Override
	public Aggregator newAggregator() {
		return new FloatingPoint();
	}


	@Override
	public String[] getDatabaseColumns() {
		return new String[]{databaseColumnStartTime, databaseColumnEndTime, databaseColumnCounter};
	}
	
	// For an average we will always return a floating point value.
	private final class FloatingPoint implements Aggregator {
		private boolean hasValue = false;

		private BigDecimal accumulatorMillis = new BigDecimal(0);
		private BigDecimal accumulatorCounter = new BigDecimal(0);
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			Timestamp start = rs.getTimestamp(databaseColumnStartTime);
			Timestamp end = rs.getTimestamp(databaseColumnEndTime);
			if (start != null && end != null) {
				long counter = rs.getLong(databaseColumnCounter);
				if (!rs.wasNull()) {
					hasValue =  true;
					long millisToAdd = end.getTime() - start.getTime();
					accumulatorMillis = accumulatorMillis.add(new BigDecimal(millisToAdd));
					accumulatorCounter = accumulatorCounter.add(new BigDecimal(counter));
				}
			}
		}

		@Override
		public Number getResult() {
			Double result = null;
			
			if (hasValue) {
				BigDecimal minutes = accumulatorMillis.divide(new BigDecimal(60000), 4, RoundingMode.HALF_UP);
				if (minutes.doubleValue() != 0.0) {
					result = Double.valueOf(accumulatorCounter.divide(minutes, 4, RoundingMode.HALF_UP).doubleValue());
				}
			}
			return result;
		}
	}
}
