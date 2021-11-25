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
import java.util.Map;
import java.util.Set;

import org.perfmon4j.instrument.snapshot.Delta;

import junit.framework.TestCase;

public class DeltaElementTest extends TestCase {
	private final String FIELD_NAME = "measurement";
	
	public void testConstructorNoSQL() {
		MeasurementElement start = TestHelper.getMockMeasurementElement(FIELD_NAME, 0);
		MeasurementElement end = TestHelper.getMockMeasurementElement(FIELD_NAME, 120);
		
		DeltaElement delta = new DeltaElement(start, end, TestHelper.MINUTE);
		assertEquals("Expected Field Name", delta.getFieldName(), FIELD_NAME);
		
		Delta countDelta = delta.getCount();
		assertNotNull("Should have a count delta", countDelta);
		assertNull("Should NOT have a SQL count delta", delta.getSqlCount());
		
		assertEquals("Excpected delta (end - start)", 120, countDelta.getDelta());
		assertEquals("Expected Duration", Double.valueOf(TestHelper.MINUTE), Double.valueOf(countDelta.getDurationMillis()));
	}

	public void testConstructorWithSQL() {
		MeasurementElement start = TestHelper.getMockMeasurementElement(FIELD_NAME, 0, 0);
		MeasurementElement end = TestHelper.getMockMeasurementElement(FIELD_NAME, 120, 10);
		
		DeltaElement delta = new DeltaElement(start, end, TestHelper.MINUTE);
		assertNotNull("Should have a SQL count delta", delta.getSqlCount());
	}

	public void testCreateDeltaSet() {
		MeasurementElement start = TestHelper.getMockMeasurementElement(FIELD_NAME, 0, 0);
		MeasurementElement end = TestHelper.getMockMeasurementElement(FIELD_NAME, 120, 10);
		
		Map<String, MeasurementElement> startMap = new HashMap<String, MeasurementElement>();
		Map<String, MeasurementElement> endMap = new HashMap<String, MeasurementElement>();
		startMap.put(FIELD_NAME, start);
		endMap.put(FIELD_NAME, end);
		
		Set<DeltaElement> elementSet = DeltaElement.createDeltaSet(startMap, endMap, TestHelper.MINUTE);
		assertNotNull(elementSet);
		assertEquals(1, elementSet.size());
		
		DeltaElement element = elementSet.iterator().next();
		assertEquals(FIELD_NAME, element.getFieldName());
	}
	
}
