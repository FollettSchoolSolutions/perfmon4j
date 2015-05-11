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
package org.perfmon4j.restdatasource.data.query.advanced;

import java.sql.ResultSet;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.perfmon4j.restdatasource.util.DateTimeHelper;
import org.perfmon4j.restdatasource.util.aggregators.SumAggregatorFactory;

public class ResultAccumulatorTest extends TestCase {
	private final DateTimeHelper helper = new DateTimeHelper();
	
	public ResultAccumulatorTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testNoDataFound() throws Exception {
		ResultAccumulator accumulator = new ResultAccumulator();
		
		accumulator.addSeries("MyTemplate", new SumAggregatorFactory("average", false), "Average" , 
				"ABCD-EFGH.1", "Person", "average", "SUM");

		AdvancedQueryResult results = accumulator.buildResults();
		assertNotNull("results", results);
		
		String[] dateTime = results.getDateTime();
		assertNotNull("dateTime", dateTime);
		assertEquals("Should have had 0 minutes of observations", 0, dateTime.length);
		
		assertNotNull("Series", results.getSeries());
		assertEquals("Should have 1 series",1, results.getSeries().length);
		
		Series series = results.getSeries()[0];
		assertEquals("Alias", "Average", series.getAlias());
		assertEquals("Category", "Person", series.getCategory());
		assertEquals("fieldName", "average", series.getFieldName());
		assertEquals("aggregationMethod", "SUM", series.getAggregationMethod());
		assertEquals("systemID", "ABCD-EFGH.1", series.getSystemID());
	
		Number[] values = series.getValues();
		assertEquals("Should have no values", 0, values.length);
	}
	
	public void testSingleDataSeries() throws Exception {
		final String myTemplate = "MyTemplate";
		ResultAccumulator accumulator = new ResultAccumulator();
		

		accumulator.addSeries(myTemplate, new SumAggregatorFactory("average", false), "Average" , 
				"ABCD-EFGH.1", "Person", "average", "SUM");

		ResultSet rs = Mockito.mock(ResultSet.class);
		Mockito.when(rs.wasNull()).thenReturn(Boolean.FALSE);
		
		// Both these should roll up to the same minute.  The end time should be truncated and bucketed into 9:30
		long first = helper.parseDateTime("2015-01-01T09:30").getTimeForStart();
		long second = helper.parseDateTime("2015-01-01T09:30").getTimeForEnd();
		
		long third = helper.parseDateTime("2015-01-01T09:31").getTimeForStart();
		long forth = helper.parseDateTime("2015-01-01T09:31").getTimeForEnd();
		
		Mockito.when(rs.getLong("average")).thenReturn(Long.valueOf(2));
		accumulator.getAggregators(myTemplate, first)[0].aggreagate(rs);  // Add 2 to first minute
		accumulator.getAggregators(myTemplate, second)[0].aggreagate(rs); // Add 2 more to first minute

		Mockito.when(rs.getLong("average")).thenReturn(Long.valueOf(3));
		accumulator.getAggregators(myTemplate, third)[0].aggreagate(rs); // Add 3 to second minute
		accumulator.getAggregators(myTemplate, forth)[0].aggreagate(rs); // Add 3 more to second minute
		
		AdvancedQueryResult results = accumulator.buildResults();
		assertNotNull("results", results);
		
		String[] dateTime = results.getDateTime();
		assertNotNull("dateTime", dateTime);
		assertEquals("Should have had 2 minutes of observations", 2, dateTime.length);
		assertEquals("first minute", "2015-01-01T09:30", dateTime[0]);
		assertEquals("second minute", "2015-01-01T09:31", dateTime[1]);
		
		assertNotNull("Series", results.getSeries());
		assertEquals("Should have 1 series",1, results.getSeries().length);
		
		Series series = results.getSeries()[0];
		assertEquals("Alias", "Average", series.getAlias());
		assertEquals("Category", "Person", series.getCategory());
		assertEquals("fieldName", "average", series.getFieldName());
		assertEquals("aggregationMethod", "SUM", series.getAggregationMethod());
		assertEquals("systemID", "ABCD-EFGH.1", series.getSystemID());
	
		Number[] values = series.getValues();
		assertEquals("Number of values", 2, values.length);
		assertEquals("First value", 4, values[0].longValue());
		assertEquals("Second value", 6, values[1].longValue());
	}
}
