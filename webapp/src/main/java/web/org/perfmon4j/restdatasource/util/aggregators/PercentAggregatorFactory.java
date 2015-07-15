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

public class PercentAggregatorFactory implements AggregatorFactory {
	private final String databaseColumnNumerator;
	private final String databaseColumnDenominator;
	private final int percentMultiplier;
	
	public PercentAggregatorFactory(String databaseColumnNumerator, String databaseColumnDenominator) {
		this(databaseColumnNumerator, databaseColumnDenominator, false);
	}

	public PercentAggregatorFactory(String databaseColumnNumerator, String databaseColumnDenominator, boolean displayAsRatio) {
		this.databaseColumnNumerator = databaseColumnNumerator;
		this.databaseColumnDenominator = databaseColumnDenominator;
		percentMultiplier = displayAsRatio ? 1 : 100;
	}
	
	
	
	@Override
	public Aggregator newAggregator() {
		return new PercentAggregator();
	}


	@Override
	public String[] getDatabaseColumns() {
		return new String[]{databaseColumnNumerator, databaseColumnDenominator};
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
					result = Double.valueOf(accumulatorNumerator.divide(accumulatorDenominator, 4, RoundingMode.HALF_UP).doubleValue()  * percentMultiplier);
				} else {
					result = Double.valueOf(0.0);
				}
			}
			
			return result;
		}
	}
}
