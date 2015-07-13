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

import junit.framework.TestCase;

import org.mockito.Mockito;

import web.org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.SumAggregatorFactory;

public class ColumnValueFilterFactoryTest extends TestCase {

	public ColumnValueFilterFactoryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFilter() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(3));
		Mockito.when(rs.getString("Category")).thenReturn("WebRequest");
		
		// Create sum aggregator factory.
		AggregatorFactory delegate = new SumAggregatorFactory("myColumn", false);
		
		// Wrap it with a factory that will filter calls to the SumAggregator based on the result set containing a specific category.
		AggregatorFactory factory = new ColumnValueFilterFactory(delegate, "Category", new String[]{"WebRequest"});
		
		Aggregator ag = factory.newAggregator();
		
		ag.aggreagate(rs);
		
		
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(5));
		Mockito.when(rs.getString("Category")).thenReturn("SomeOtherCategory");
		ag.aggreagate(rs);
		
		Number result = ag.getResult();
		assertTrue("Should be a Long value", result instanceof Long);
		assertEquals("should have filtered out the request for SomeOtherCategory", 3, result.intValue());
	}
	
}
