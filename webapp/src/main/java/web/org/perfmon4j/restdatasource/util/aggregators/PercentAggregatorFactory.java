/*
 *	Copyright 2015 Follett School Solutions 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package web.org.perfmon4j.restdatasource.util.aggregators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import web.org.perfmon4j.restdatasource.data.AggregationMethod;

public class PercentAggregatorFactory implements AggregatorFactory {
	private final String databaseColumnNumerator;
	private final String databaseColumnDenominator;
	private final String databaseColumnSystemID;
	private final boolean invertRatio;

	private final int percentMultiplier;
	private final AggregationMethod aggregationMethod;
	
	public PercentAggregatorFactory(String databaseColumnSystemID, String databaseColumnNumerator, String databaseColumnDenominator) {
		this(databaseColumnSystemID, databaseColumnNumerator, databaseColumnDenominator, AggregationMethod.NATURAL, false, false);
	}
	
	public PercentAggregatorFactory(String databaseColumnSystemID, String databaseColumnNumerator, String databaseColumnDenominator, 
			AggregationMethod aggregationMethod) {
		this(databaseColumnSystemID, databaseColumnNumerator, databaseColumnDenominator, aggregationMethod, false, false);
	}

	public PercentAggregatorFactory(String databaseColumnSystemID, String databaseColumnNumerator, String databaseColumnDenominator, AggregationMethod aggregationMethod,  boolean displayAsRatio, boolean invertRatio) {
		this.databaseColumnSystemID = databaseColumnSystemID;
		this.databaseColumnNumerator = databaseColumnNumerator;
		this.databaseColumnDenominator = databaseColumnDenominator;
		percentMultiplier = displayAsRatio ? 1 : 100;
		this.aggregationMethod = aggregationMethod;
		this.invertRatio = invertRatio;
		
		if (aggregationMethod != AggregationMethod.NATURAL 
			&& aggregationMethod != AggregationMethod.MAX
			&& aggregationMethod != AggregationMethod.MIN) {

			throw new RuntimeException("Unsupported Aggregation method for PercentAggregator: " + aggregationMethod);
		}
	}
	
	@Override
	public Aggregator newAggregator() {
		if (aggregationMethod == AggregationMethod.MAX) {
			return new PercentAggregatorMaxOrMin(true);
		} else if (aggregationMethod == AggregationMethod.MIN) {
			return new PercentAggregatorMaxOrMin(false);
		} else {
			return new PercentAggregator();
		}
	}

	@Override
	public String[] getDatabaseColumns() {
		return new String[]{databaseColumnSystemID, databaseColumnNumerator, databaseColumnDenominator};
	}
	
	// For an average we will always return a floating point value.
	private final class PercentAggregator implements Aggregator {
		private boolean hasValue = false;

		private BigDecimal accumulatorNumerator = new BigDecimal(0);
		private BigDecimal accumulatorDenominator = new BigDecimal(0);
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			long numerator = rs.getLong(databaseColumnNumerator);
			if (!rs.wasNull()) { // If numerator is null don't use.
				long denominator = rs.getLong(databaseColumnDenominator);
				if (!rs.wasNull()) { // If denominator is null don't use.
					hasValue = true;
					accumulatorNumerator = accumulatorNumerator.add(new BigDecimal(numerator));
					accumulatorDenominator = accumulatorDenominator.add(new BigDecimal(denominator));
				}
			}
		}

		@Override
		public Number getResult() {
			Double result = null;

			if (hasValue) {
				if (accumulatorDenominator.longValue() != 0) {
					result = Double.valueOf(accumulatorNumerator.divide(accumulatorDenominator, 2, RoundingMode.HALF_UP).doubleValue()  * percentMultiplier);
				} else {
					result = Double.valueOf(0.0);
				}
				if (invertRatio) {
					result = Double.valueOf((1.0 * percentMultiplier) - result.doubleValue());
				}
			}
			
			return result;
		}
	}
	
	// For an average we will always return a floating point value.
	private final class PercentAggregatorMaxOrMin implements Aggregator {
		private final Map<Long, PercentAggregator> systems = new HashMap<Long, PercentAggregatorFactory.PercentAggregator>(); 
		private final boolean useMax;
		
		PercentAggregatorMaxOrMin(boolean useMax) {
			this.useMax = useMax;
		}
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			long denominator = rs.getLong(databaseColumnDenominator);
			if (!rs.wasNull() && denominator > 0) {  // Ignore any column that does not have a positive denominator.
				Long systemID = Long.valueOf(rs.getLong(databaseColumnSystemID));
				PercentAggregator aggregator = systems.get(systemID);
				if (aggregator == null) {
					aggregator = new PercentAggregator();
					systems.put(systemID, aggregator);
				}
				aggregator.aggreagate(rs);
			}
		}

		@Override
		public Number getResult() {
			Double result = null;

			for (PercentAggregator agg : systems.values()) {
				if (agg.hasValue) {
					Double aggResult = (Double)agg.getResult();
					if (result == null) {
						result = aggResult;
					} else if (useMax && (aggResult.doubleValue() > result.doubleValue())) {
						result = aggResult;
					} else if (!useMax && (aggResult.doubleValue() < result.doubleValue())) {
						result = aggResult;
					}
				}
			}
			
			return result;
		}
	}
	
	
}
