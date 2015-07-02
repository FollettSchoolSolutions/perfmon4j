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

package web.org.perfmon4j.restdatasource.util.aggregators.decorator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import web.org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;

public class ColumnValueFilterFactory implements AggregatorFactory {
	private final AggregatorFactory delegate;
	private final String columnName;
	private final Set<String> values = new HashSet<String>();
	
	public ColumnValueFilterFactory(AggregatorFactory delegate,
			String columnName, String[] values) {
		super();
		this.delegate = delegate;
		this.columnName = columnName;
		this.values.addAll(Arrays.asList(values));
	}

	@Override
	public Aggregator newAggregator() {
		Aggregator d = delegate.newAggregator();
		return new ColumnValueFilter(d);
	}

	@Override
	public String[] getDatabaseColumns() {
		Set<String> result = new HashSet<String>(Arrays.asList(delegate.getDatabaseColumns()));
		
		result.add(columnName);
		
		return result.toArray(new String[]{});
	}
	
	private class ColumnValueFilter implements Aggregator {
		private final Aggregator delegate;
		
		public ColumnValueFilter(Aggregator delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public void aggreagate(ResultSet rs) throws SQLException {
			String filterValue = rs.getString(columnName);
			if (filterValue != null) {
				filterValue = filterValue.trim();
				if (values.contains(filterValue)) {		
					delegate.aggreagate(rs);
				}
			}
		}

		@Override
		public Number getResult() {
			return delegate.getResult();
		}
	}
}
