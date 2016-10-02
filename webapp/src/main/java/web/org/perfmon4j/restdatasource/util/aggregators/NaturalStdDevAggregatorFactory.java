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

public class NaturalStdDevAggregatorFactory implements AggregatorFactory {
	private final String databaseColumnNumerator;
	private final String databaseColumnSumOfSquares;
	private final String databaseColumnDenominator;
	
	
	public NaturalStdDevAggregatorFactory(String databaseColumnNumerator, String databaseColumnSumOfSquares,  String databaseColumnDenominator) {
		this.databaseColumnNumerator = databaseColumnNumerator;
		this.databaseColumnSumOfSquares = databaseColumnSumOfSquares;
		this.databaseColumnDenominator = databaseColumnDenominator;
	}
	
	@Override
	public Aggregator newAggregator() {
		return new FloatingPoint();
	}

	@Override
	public String[] getDatabaseColumns() {
		return new String[]{databaseColumnNumerator, databaseColumnSumOfSquares, databaseColumnDenominator};
	}
	
	// For an Standard deviation we will always return a floating point value.
	private final class FloatingPoint implements Aggregator {
		private boolean hasValue = false;
		
		private BigDecimal accumulatorNumerator = new BigDecimal(0);
		private BigDecimal accumulatorSumOfSquares = new BigDecimal(0);
		private long accumulatorDenominator = 0;
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			double numerator = rs.getDouble(databaseColumnNumerator);
			if (!rs.wasNull()) {
				long denominator = rs.getLong(databaseColumnDenominator);
				if (!rs.wasNull()) {
					double sumOfSquares = rs.getDouble(databaseColumnSumOfSquares);
					if (!rs.wasNull()) {
						hasValue = true;
						accumulatorNumerator = accumulatorNumerator.add(new BigDecimal(numerator));
						accumulatorSumOfSquares = accumulatorSumOfSquares.add(new BigDecimal(sumOfSquares));
						accumulatorDenominator += denominator;
					}
				}
			}
		}

		@Override
		public Number getResult() {
			if (hasValue && accumulatorDenominator > 0) {
				return Double.valueOf(calcStdDeviation(accumulatorDenominator, accumulatorNumerator, accumulatorSumOfSquares));
			} else {
				return hasValue ? Double.valueOf(0) : null;
			}
		}
	}
	
	/*----------------------------------------------------------------------------*/    
    private static double calcVariance(long sampleCount, BigDecimal total, BigDecimal sumOfSquares) {
        double result = 0;
        
        if (sampleCount > 1) {
        	total = total.multiply(total);
        	total = total.divide(new BigDecimal(sampleCount), 4, RoundingMode.HALF_UP);
        	BigDecimal numerator = sumOfSquares.subtract(total);
        	result = numerator.divide(new BigDecimal(sampleCount - 1), 2, RoundingMode.HALF_UP).doubleValue();
        }
        
        return result;
    }
    
    private static double calcStdDeviation(long sampleCount, BigDecimal total, BigDecimal sumOfSquares) {
        double result = 0;
        
        double variance = calcVariance(sampleCount, total, sumOfSquares);
        if (variance > 0) {
            result = Math.round(Math.sqrt(variance) * 1000)/1000.0;
        }
        
        return result;
    }
}
