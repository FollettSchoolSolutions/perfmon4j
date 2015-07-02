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
import java.sql.Timestamp;

import junit.framework.TestCase;

import org.mockito.Mockito;

public class NaturalPerMinuteAggregatorFactoryTest extends TestCase {


	public NaturalPerMinuteAggregatorFactoryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testSingleSystem() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		long now = System.currentTimeMillis();
		long minuteAgo = now - 60000;
		long twoMinutesAgo = minuteAgo - 60000;
		long threeMinutesAgo = twoMinutesAgo - 60000;
		
		Mockito.when(rs.getString("systemID")).thenReturn("1");
		
		Aggregator ag = new NaturalPerMinuteAggregatorFactory("startTime", "endTime", "counter").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(threeMinutesAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(twoMinutesAgo));
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(800));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(twoMinutesAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(minuteAgo));
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(900));
		ag.aggreagate(rs);

		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(minuteAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(now));
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(1000));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 900, result.longValue());
	}


	
	/**
	 * For multiple systems the throughput per minute for each system
	 * should be summed together.  
	 * For example if you have 4 systems and each processed 10 requests per/minute should
	 * return 40 request per minute.	
	 * 
	 * @throws Exception
	 */
	public void testMultipleSystem() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		long now = System.currentTimeMillis();
		long minuteAgo = now - 60000;
		
		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(minuteAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(now));
		
		Aggregator ag = new NaturalPerMinuteAggregatorFactory("startTime", "endTime", "counter").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		// System1
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(800));
		ag.aggreagate(rs);
		
		// System2
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(900));
		ag.aggreagate(rs);

		// System3
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(1000));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 2700, result.longValue());
	}


}
