package web.org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;

import junit.framework.TestCase;

import org.mockito.Mockito;

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
public class NaturalAverageAggregatorFactoryTest extends TestCase {


	public NaturalAverageAggregatorFactoryTest(String name) {
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
		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf("1"));
		
		Aggregator ag = new NaturalAverageAggregatorFactory("numerator", "denominator").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf("1.5"));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf("2.0"));
		ag.aggreagate(rs);

		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf("2.5"));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 2, result.intValue());
	}

	public void testHandleVERYLargeNumerator() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf("1"));
		
		Aggregator ag = new NaturalAverageAggregatorFactory("numerator", "denominator").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf(Long.MAX_VALUE));
		// We are going to create a numerator of (5 * Long.MAX_VALUE).  The average (divide by 5) should equal Long.MAX_VALUE;  
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", Long.MAX_VALUE, result.longValue());
	}
	
	
}
