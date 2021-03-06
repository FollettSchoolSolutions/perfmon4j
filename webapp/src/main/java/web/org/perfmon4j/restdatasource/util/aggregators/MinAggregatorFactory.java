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

import java.sql.ResultSet;
import java.sql.SQLException;

public class MinAggregatorFactory implements AggregatorFactory {
	private final String databaseColumn;
	private final boolean floatingPoint;
	
	
	public MinAggregatorFactory(String databaseColumn, boolean floatingPoint) {
		this.databaseColumn = databaseColumn;
		this.floatingPoint = floatingPoint;
	}
	
	@Override
	public String[] getDatabaseColumns() {
		return new String[]{databaseColumn};
	}
			
	
	@Override
	public Aggregator newAggregator() {
		return floatingPoint ? new FloatingPoint() : new FixedPoint();
	}
	
	private final class FloatingPoint implements Aggregator {
		private boolean hasValue = false;
		private double minValue = 0;
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			double d = rs.getDouble(databaseColumn);
			if (!rs.wasNull()) {
				if (!hasValue || d < minValue) {
					minValue = d;
				}
				hasValue = true;
			}
		}

		@Override
		public Number getResult() {
			if (hasValue) {
				return Double.valueOf(minValue);
			} else {
				return null;
			}
		}
	}
	
	private final class FixedPoint implements Aggregator {
		private boolean hasValue = false;
		private long minValue = Long.MAX_VALUE;
		
		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			long l = rs.getLong(databaseColumn);
			if (!rs.wasNull()) {
				hasValue = true;
				if (l < minValue) {
					minValue = l;
				}
			}
		}

		@Override
		public Number getResult() {
			if (hasValue) {
				return Long.valueOf(minValue);
			} else {
				return null;
			}
		}
	}

}
