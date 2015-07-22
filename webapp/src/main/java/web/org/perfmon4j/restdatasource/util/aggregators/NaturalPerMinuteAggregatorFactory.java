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
import java.util.HashMap;
import java.util.Map;

public class NaturalPerMinuteAggregatorFactory implements AggregatorFactory {
	private final String databaseColumnStartTime;
	private final String databaseColumnEndTime;
	private final String databaseColumnCounter;
	private final String databaseColumnSystemID;
	
	
	public NaturalPerMinuteAggregatorFactory(String databaseColumnSystemID, String databaseColumnStartTime, String databaseColumnEndTime, 
			String databaseColumnCounter) {
		this.databaseColumnSystemID = databaseColumnSystemID;
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
		return new String[]{databaseColumnSystemID, databaseColumnStartTime, databaseColumnEndTime, databaseColumnCounter};
	}
	
	// For an average we will always return a floating point value.
	private final class FloatingPoint implements Aggregator {
		private boolean hasValue = false;

		Map<Long, StartStopTime> durationTracker = new HashMap<Long, NaturalPerMinuteAggregatorFactory.StartStopTime>();
		private BigDecimal accumulatorCounter = new BigDecimal(0);
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			Timestamp tsStart = rs.getTimestamp(databaseColumnStartTime);
			Timestamp tsEnd = rs.getTimestamp(databaseColumnEndTime);
			if (tsStart != null && tsEnd != null) {
				long counter = rs.getLong(databaseColumnCounter);
				if (!rs.wasNull()) {
					Long systemID = Long.valueOf(rs.getLong(databaseColumnSystemID));
					StartStopTime tracker = durationTracker.get(systemID);
					if (tracker == null) {
						tracker = new StartStopTime();
						durationTracker.put(systemID, tracker);
					}
					hasValue =  true;
					long start = tsStart.getTime();
					long end = tsEnd.getTime();
					tracker.update(start, end);

					accumulatorCounter = accumulatorCounter.add(new BigDecimal(counter));
				}
			}
		}

		@Override
		public Number getResult() {
			Double result = null;
			
			if (hasValue) {
				long duration = 0;
				for (StartStopTime tracker : durationTracker.values()) {
					long newDuration = tracker.getDuration();
					if (newDuration > duration) {
						duration = newDuration;
					}
				}
				double minutes = duration/60000.0;
				if (minutes != 0.0) {
					result = Double.valueOf(accumulatorCounter.divide(new BigDecimal(minutes), 4, RoundingMode.HALF_UP).doubleValue());
				} else {
					result = Double.valueOf(0);
				}
			}
			return result;
		}
	}
	
	private static class StartStopTime {
		long startTime = Long.MAX_VALUE;
		long endTime = 0;
		
		void update(long newStartTime, long newEndTime) {
			if (newStartTime < startTime) {
				startTime = newStartTime;
			}
			
			if (newEndTime > endTime) {
				endTime = newEndTime;
			}
		}
		
		long getDuration() {
			return endTime - startTime;
		}
	}
}
