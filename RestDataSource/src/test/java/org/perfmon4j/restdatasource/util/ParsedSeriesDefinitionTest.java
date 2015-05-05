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

package org.perfmon4j.restdatasource.util;

import javax.ws.rs.BadRequestException;

import org.perfmon4j.restdatasource.data.AggregationMethod;
import org.perfmon4j.restdatasource.util.ParsedSeriesDefinition;

import junit.framework.TestCase;

public class ParsedSeriesDefinitionTest extends TestCase {

	public ParsedSeriesDefinitionTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	public void testParserSimpleSeries() {
		String series = "GRSK-VRTS.1~Interval.WebRequest.search~avgDuration";
		ParsedSeriesDefinition def[] = ParsedSeriesDefinition.parse(series);
		
		assertNotNull(def);
		assertEquals("def.length", 1, def.length);
		assertNull("aggregationMethod", def[0].getAggregationMethod());
		assertEquals("numberOfSystems", 1, def[0].getSystems().length);
		assertEquals("System", "GRSK-VRTS.1", def[0].getSystems()[0]);
		assertEquals("Category", "Interval.WebRequest.search", def[0].getCategoryName());
		assertEquals("Field", "avgDuration", def[0].getFieldName());
	}

	public void testParserCompositeSeries() {
		String series = "GRSK-VRTS.1~GRSK-VRTS.2~Interval.WebRequest.search~avgDuration";
		ParsedSeriesDefinition def[] = ParsedSeriesDefinition.parse(series);
		
		assertNotNull(def);
		assertEquals("def.length", 1, def.length);
		assertNull("aggregationMethod", def[0].getAggregationMethod());
		
		assertEquals("numberOfSystems", 2, def[0].getSystems().length);
		assertEquals("System", "GRSK-VRTS.1", def[0].getSystems()[0]);
		assertEquals("System", "GRSK-VRTS.2", def[0].getSystems()[1]);
		
		assertEquals("Category", "Interval.WebRequest.search", def[0].getCategoryName());
		assertEquals("Field", "avgDuration", def[0].getFieldName());
	}

	public void testParserCompositeSeriesWithAggregationMethod() {
		String series = "MAX~GRSK-VRTS.1~GRSK-VRTS.2~Interval.WebRequest.search~avgDuration";
		ParsedSeriesDefinition def[] = ParsedSeriesDefinition.parse(series);
		
		assertNotNull(def);
		assertEquals("def.length", 1, def.length);
		assertEquals("aggregationMethod", AggregationMethod.MAX, def[0].getAggregationMethod());
		
		assertEquals("numberOfSystems", 2, def[0].getSystems().length);
		assertEquals("System", "GRSK-VRTS.1", def[0].getSystems()[0]);
		assertEquals("System", "GRSK-VRTS.2", def[0].getSystems()[1]);
		
		assertEquals("Category", "Interval.WebRequest.search", def[0].getCategoryName());
		assertEquals("Field", "avgDuration", def[0].getFieldName());
	}

	public void testParserMultipleSeries() {
		String series = "GRSK-VRTS.1~Interval.WebRequest.search~avgDuration_MAX~GRSK-VRTS.4~GRSK-VRTS.5~Interval.WebRequest.search~maxActiveThreads_MAX~GRSK-VRTS.4~GRSK-VRTS.5~Interval.WebRequest.search~medianDuration";
		ParsedSeriesDefinition def[] = ParsedSeriesDefinition.parse(series);
		
		assertNotNull(def);
		assertEquals("numberOfSeries", 3, def.length);
	}
	

	public void testNullRequest() {
		try {
			ParsedSeriesDefinition.parse(null);
			fail("Expected a BadRequestException");
		} catch (BadRequestException ex) {
			assertEquals("You must provide a series definition", ex.getMessage());
		}
	}

	public void testEmptyRequest() {
		try {
			ParsedSeriesDefinition.parse("");
			fail("Expected a BadRequestException");
		} catch (BadRequestException ex) {
			assertEquals("You must provide a series definition", ex.getMessage());
		}
	}

	public void testMultipleEmptyRequests() {
		try {
			ParsedSeriesDefinition.parse("__");
			fail("Expected a BadRequestException");
		} catch (BadRequestException ex) {
			assertEquals("You must provide a series definition", ex.getMessage());
		}
	}

	public void testMultipleEmptyRequestsWithWhiteSpace() {
		try {
			ParsedSeriesDefinition.parse("  _  _  ");
			fail("Expected a BadRequestException");
		} catch (BadRequestException ex) {
			assertEquals("You must provide a series definition", ex.getMessage());
		}
	}
	
	public void testInsufficientFieldsInSeriesDefinition() {
		try {
			ParsedSeriesDefinition.parse("Interval.WebRequest.search~avgDuration");
			fail("Expected a BadRequestException");
		} catch (BadRequestException ex) {
			assertEquals("Insufficent fields in series definition", ex.getMessage());
		}
	}
		
	public void testInsufficientFieldsInSeriesDefinitionWithAggregationMethod() {
		try {
			ParsedSeriesDefinition.parse("MAX~Interval.WebRequest.search~avgDuration");
			fail("Expected a BadRequestException");
		} catch (BadRequestException ex) {
			assertEquals("Insufficent fields in series definition", ex.getMessage());
		}
	}

	public void testInvalidAggregationMethod() {
		try {
			ParsedSeriesDefinition.parse("WHATISTHIS~Interval.WebRequest.search~avgDuration");
			fail("Expected a BadRequestException");
		} catch (BadRequestException ex) {
			assertEquals("Invalid aggregation method: WHATISTHIS", ex.getMessage());
		}
	}

}
