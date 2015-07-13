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

public class PercentAggregatorFactoryTest extends TestCase {


	public PercentAggregatorFactoryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testSimple() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		
		Mockito.when(rs.getString("systemID")).thenReturn("1");
		
		Aggregator ag = new PercentAggregatorFactory("numerator", "denominator").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getLong("numerator")).thenReturn(Long.valueOf(4));
		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf(10));
		ag.aggreagate(rs);

		Mockito.when(rs.getLong("numerator")).thenReturn(Long.valueOf(6));
		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf(10));
		ag.aggreagate(rs);
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("should be 50 percent", 50, result.longValue());
	}

	public void testDontDivideByZero() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		
		Mockito.when(rs.getString("systemID")).thenReturn("1");
		
		Aggregator ag = new PercentAggregatorFactory("numerator", "denominator").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getLong("numerator")).thenReturn(Long.valueOf(0));
		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf(0));
		ag.aggreagate(rs);

		Number result = ag.getResult();
		assertNull("Denominator is 0, we should return null", ag.getResult());
	}
}
