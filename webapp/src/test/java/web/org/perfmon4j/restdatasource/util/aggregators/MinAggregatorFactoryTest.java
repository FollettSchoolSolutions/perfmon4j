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

import junit.framework.TestCase;

import org.mockito.Mockito;

public class MinAggregatorFactoryTest extends TestCase {


	public MinAggregatorFactoryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testFloatingPoint() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		
		Aggregator ag = new MinAggregatorFactory("myColumn", true).newAggregator();
		
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getDouble("myColumn")).thenReturn(Double.valueOf(-1.5));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getDouble("myColumn")).thenReturn(Double.valueOf(-3.5));
		ag.aggreagate(rs);

		Mockito.when(rs.getDouble("myColumn")).thenReturn(Double.valueOf(-2.5));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("min", -3.5, result.doubleValue());
	}

	public void testFixedPoint() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		
		Aggregator ag = new MinAggregatorFactory("myColumn", false).newAggregator();
		
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(-2));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(-4));
		ag.aggreagate(rs);

		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(-3));
		ag.aggreagate(rs);
		
		Number result = ag.getResult();
		assertTrue("Should be a Long value", result instanceof Long);
		assertEquals("min", -4, result.intValue());
	}
}
