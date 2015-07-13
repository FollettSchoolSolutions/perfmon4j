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
import java.sql.Timestamp;

public class NaturalPerMinuteAggregatorFactory implements AggregatorFactory {
	private final String databaseColumnStartTime;
	private final String databaseColumnEndTime;
	private final String databaseColumnCounter;
	
	
	public NaturalPerMinuteAggregatorFactory(String databaseColumnStartTime, String databaseColumnEndTime, 
			String databaseColumnCounter) {
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

		private long startTime = Long.MAX_VALUE;
		private long endTime = 0;
		
//		private BigDecimal accumulatorMillis = new BigDecimal(0);
		private BigDecimal accumulatorCounter = new BigDecimal(0);
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			Timestamp tsStart = rs.getTimestamp(databaseColumnStartTime);
			Timestamp tsEnd = rs.getTimestamp(databaseColumnEndTime);
			if (tsStart != null && tsEnd != null) {
				long counter = rs.getLong(databaseColumnCounter);
				if (!rs.wasNull()) {
					hasValue =  true;
					long start = tsStart.getTime();
					long end = tsEnd.getTime();

					if (start < startTime) {
						startTime = start;
					}
				
					if (end > endTime) {
						endTime = end;
					}
					accumulatorCounter = accumulatorCounter.add(new BigDecimal(counter));
				}
			}
		}

		@Override
		public Number getResult() {
			Double result = null;
			
			if (hasValue) {
				double minutes = ((endTime - startTime)/60000.0);
				if (minutes != 0.0) {
					result = Double.valueOf(accumulatorCounter.divide(new BigDecimal(minutes), 4, RoundingMode.HALF_UP).doubleValue());
				} else {
					result = Double.valueOf(0);
				}
			}
			return result;
		}
	}
}
