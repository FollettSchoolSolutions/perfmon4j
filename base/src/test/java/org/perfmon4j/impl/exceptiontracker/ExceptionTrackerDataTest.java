/*
 *	Copyright 2021 Follett School Solutions, LLC 
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
 * 	ddeucher@follett.com
 * 	David Deuchert
 * 	Follett School Solutions, LLC
 * 	1340 Ridgeview Dr
 * 	McHenry, IL 60050
*/
package org.perfmon4j.impl.exceptiontracker;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.instrument.snapshot.Delta;

import junit.framework.TestCase;

public class ExceptionTrackerDataTest extends TestCase {
	private final String FIELD_NAME = "measurement";
	private final long START_TIME = TestHelper.getTimeForTest();
	private final long END_TIME = TestHelper.addMinutes(START_TIME, 1);
	
	public void testToAppenderString() {
		Set<DeltaElement> deltas = new HashSet<DeltaElement>();
		deltas.add(TestHelper.getMockDeltaElement(FIELD_NAME, 60.0));
		
		ExceptionTrackerData data = new ExceptionTrackerData("WebRequest", START_TIME, END_TIME, deltas);
		final String EXPECTED = "\r\n********************************************************************************\r\n"
				+ "WebRequest\r\n"
				+ "00:00:00:000 -> 00:01:00:000\r\n"
				+ " measurement............................. 60.00 per minute\r\n"
				+ "********************************************************************************";
		assertEquals("Expected appenderString", EXPECTED, data.toAppenderString());
	}
	
	public void testToAppenderStringWithSQL() {
		Set<DeltaElement> deltas = new HashSet<DeltaElement>();
		deltas.add(TestHelper.getMockDeltaElement(FIELD_NAME, 60.0, 10.0));
		
		ExceptionTrackerData data = new ExceptionTrackerData("WebRequest", START_TIME, END_TIME, deltas);
		final String EXPECTED = "\r\n********************************************************************************\r\n"
				+ "WebRequest\r\n"
				+ "00:00:00:000 -> 00:01:00:000\r\n"
				+ " measurement............................. 60.00 per minute\r\n"
				+ " measurement(SQL)........................ 10.00 per minute\r\n"
				+ "********************************************************************************";
		assertEquals("Current implementation does NOT include SQL counts", EXPECTED, data.toAppenderString());
	}

	private Map<String, PerfMonObservableDatum<?>> toMap(Set<PerfMonObservableDatum<?>> observations) {
		Map<String, PerfMonObservableDatum<?>> result = new HashMap<String, PerfMonObservableDatum<?>>();
		
		Iterator<PerfMonObservableDatum<?>> itr = observations.iterator();
		while (itr.hasNext()) {
			PerfMonObservableDatum<?> datum = itr.next();
			result.put(datum.getFieldName(), datum);
		}
		
		return result;
	}

	public void testGetObservations_NoSQL() {
		Set<DeltaElement> deltas = new HashSet<DeltaElement>();
		deltas.add(TestHelper.getMockDeltaElement(FIELD_NAME, 60.0));
		
		ExceptionTrackerData data = new ExceptionTrackerData("WebRequest", START_TIME, END_TIME, deltas);
		
		Set<PerfMonObservableDatum<?>> observations = data.getObservations();
		assertNotNull(observations);
		assertEquals("Expected number of observations (includes startTime and stopTime)", 3, observations.size());
	
		Map<String, PerfMonObservableDatum<?>> datumMap = toMap(observations);
		PerfMonObservableDatum<Delta> count = (PerfMonObservableDatum<Delta>)datumMap.get(FIELD_NAME);
		PerfMonObservableDatum<Delta> sqlCount = (PerfMonObservableDatum<Delta>)datumMap.get(FIELD_NAME + "(SQL)");
	
		assertEquals("Expected count", 60, Math.round(count.getComplexObject().getDeltaPerMinute()));
		assertNull("Not expected to return SQLCount",sqlCount);
	}
	
	
	@SuppressWarnings("unchecked")
	public void testGetObservations() {
		Set<DeltaElement> deltas = new HashSet<DeltaElement>();
		deltas.add(TestHelper.getMockDeltaElement(FIELD_NAME, 60.0, 10.0));
		
		ExceptionTrackerData data = new ExceptionTrackerData("WebRequest", START_TIME, END_TIME, deltas);
		
		Set<PerfMonObservableDatum<?>> observations = data.getObservations();
		assertNotNull(observations);
		assertEquals("Expected number of observations (includes startTime and stopTime)", 4, observations.size());
	
		Map<String, PerfMonObservableDatum<?>> datumMap = toMap(observations);
		PerfMonObservableDatum<Delta> count = (PerfMonObservableDatum<Delta>)datumMap.get(FIELD_NAME);
		PerfMonObservableDatum<Delta> sqlCount = (PerfMonObservableDatum<Delta>)datumMap.get(FIELD_NAME + "(SQL)");
	
		assertEquals("Expected count", 60, Math.round(count.getComplexObject().getDeltaPerMinute()));
		assertEquals("Expected sqlCount", 10, Math.round(sqlCount.getComplexObject().getDeltaPerMinute()));
	}
}
