/*
 *	Copyright 2022 Follett School Solutions, LLC 
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
 *  Follett School Solutions, LLC
 *  1340 Ridgeview Drive
 *  McHenry, IL 60050
 *
 */
package org.perfmon4j.influxdb;

import java.util.HashSet;
import java.util.Set;

import org.mockito.Mockito;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.util.SubCategorySplitter;

import junit.framework.TestCase;

public class InfluxAppenderTest extends TestCase {

	public InfluxAppenderTest(String name) {
		super(name);
	}

	InfluxAppender appender = null;
	PerfMonObservableData mockData = null;
	
	@SuppressWarnings("boxing")
	protected void setUp() throws Exception {
		super.setUp();
		appender = new InfluxAppender(AppenderID.getAppenderID(InfluxAppender.class.getName()));
		
		mockData = Mockito.mock(PerfMonObservableData.class);
		Mockito.when(mockData.getDataCategory()).thenReturn("MyCategory");  //Comma, space should be escaped, but not the equals sign,
		Mockito.when(mockData.getTimestamp()).thenReturn(Long.valueOf(1000));
	
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
 		set.add(PerfMonObservableDatum.newDatum("throughput", 25));
 		
 		Mockito.when(mockData.getObservations()).thenReturn(set);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testBuildPostURL() {
		appender.setBaseURL("http://localhost:8086");
		appender.setDatabase("perfmon4j");
		
		assertEquals("baseURL", "http://localhost:8086/write?db=perfmon4j&precision=s",
			appender.buildPostURL());
	}

	public void testBuildPostURLDoesNotAddExtraPathSep() {
		appender.setBaseURL("http://localhost:8086/");
		appender.setDatabase("perfmon4j");
		
		assertEquals("baseURL", "http://localhost:8086/write?db=perfmon4j&precision=s",
			appender.buildPostURL());
	}
	
	public void testBuildPostURLAddsUserNameAndPassword() {
		appender.setBaseURL("http://localhost:8086/");
		appender.setDatabase("perfmon4j");
		appender.setUserName("dave");
		appender.setPassword("fish");
		
		assertEquals("baseURL", 
			"http://localhost:8086/write?db=perfmon4j&precision=s&u=dave&p=fish",
			appender.buildPostURL());
	}

	public void testBuildPostURLAddsOptionalRetentionPolicy() {
		appender.setBaseURL("http://localhost:8086/");
		appender.setDatabase("perfmon4j");
		appender.setRetentionPolicy("3months");
		
		assertEquals("baseURL", 
			"http://localhost:8086/write?db=perfmon4j&precision=s&rp=3months",
			appender.buildPostURL());
	}
	
	public void testBuildDataLine() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("My =\\Group"); //Comma (Perfmon4j does not allow comma in group name), space AND the equals sign should be escaped
		
		Mockito.when(mockData.getDataCategory()).thenReturn("My, \\=Category");  //Comma and space should be escaped, but not the equals sign,

		assertEquals("My\\,\\ \\\\=Category,system=MySystemName,group=My\\ \\=\\\\Group throughput=25i 1",
			appender.buildPostDataLine(mockData));
	}

	public void testBuildDataLine_NoData() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("MyGroup");
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
 		Mockito.when(mockData.getObservations()).thenReturn(set);
		
		assertNull("There are no data elements to report so should return null", appender.buildPostDataLine(mockData));
	}
	
	
	public void testBuildDataLine_IgnoresDataumWithNullInput() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("MyGroup");
		
		Number nullValue = null;
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
 		set.add(PerfMonObservableDatum.newDatum("this was null input and shouldn't be sent to influxDb", nullValue));
		
		Mockito.when(mockData.getObservations()).thenReturn(set);
 		
		
		assertNull("There are no data elements to report so should return null", appender.buildPostDataLine(mockData));
	}
	
	
	public void testBuildDataLine_OnlyNonNumericData() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("MyGroup");
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
		set.add(PerfMonObservableDatum.newDatum("stringData", "This is a string value"));
 		Mockito.when(mockData.getObservations()).thenReturn(set);
		
		assertNotNull("numericOnly is false so should return a data line", appender.buildPostDataLine(mockData));
		
		// Now set numericOnly=true on the appender
		appender.setNumericOnly(true);
		assertNull("numericOnly is true should return null", appender.buildPostDataLine(mockData));
	}
	
	public void testBuildDataLineNoGroup() {
		appender.setSystemNameBody("MySystemName");
		
		assertEquals("MyCategory,system=MySystemName throughput=25i 1",
			appender.buildPostDataLine(mockData));
	}
	
	public void testDecorateBooleanForOutput() {
		assertEquals("true", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", true)));
		assertEquals("false", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", false)));
	}
	
	public void testDecorateShortForOutput() {
		short value = 10;
		assertEquals("10i", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", value)));
	}
	
	public void testDecorateIntegerForOutput() {
		int value = 10;
		assertEquals("10i", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", value)));
	}
	
	public void testDecorateLongForOutput() {
		long value = 10L;
		assertEquals("10i", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", value)));
	}

	public void testDecorateFloatForOutput() {
		float value = 10.0f;
		assertEquals("10.000", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", value)));
	}

	public void testDecorateDoubleForOutput() {
		double value = 10.0d;
		assertEquals("10.000", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", value)));
	}

	public void testDecorateFieldValueForOutput() {
		assertEquals("\"QuoteThis\"", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", "QuoteThis")));

		// Must escape nested quotes in field values
		assertEquals("\"Quote\\\"This\"", appender.decorateDatumForInflux(PerfMonObservableDatum.newDatum("fieldName", "Quote\"This")));
	}

	/**
	 * Review https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_tutorial/ for information
	 * on escaping characters.
	 */
	public void testDecorateMeasurementForOutput() {
		assertEquals("Should escape spaces", "Hits\\ per\\ second", appender.decorateMeasurementForInflux("Hits per second"));
		assertEquals("Should escape commas", "Hits\\,per\\,second", appender.decorateMeasurementForInflux("Hits,per,second"));
		assertEquals("Should escape the escape character", "Hits\\\\per\\\\second", appender.decorateMeasurementForInflux("Hits\\per\\second"));
	}
	
	public void testDecorateTagKeyTagValuesAndFieldKeys() {
		assertEquals("Should escape spaces", "DAP\\ CLUSTER", appender.decorateTagKeyTagValueFieldKeyForInflux("DAP CLUSTER"));
		assertEquals("Should escape commas", "DAP\\,DD", appender.decorateTagKeyTagValueFieldKeyForInflux("DAP,DD"));
		assertEquals("Should escape equals", "DAP\\=TODAY", appender.decorateTagKeyTagValueFieldKeyForInflux("DAP=TODAY"));
		assertEquals("Should escape the escape character", "DAP\\\\TODAY", appender.decorateTagKeyTagValueFieldKeyForInflux("DAP\\TODAY"));
	}

	public void testBuildDataLineWithSubCategory() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("MyGroup");
		appender.setSubCategorySplitter(new SubCategorySplitter("DistrictResource\\.(.*)"));
		
		Mockito.when(mockData.getDataCategory()).thenReturn("DistrictResource.dist_1234");

		assertEquals("DistrictResource,system=MySystemName,group=MyGroup,subCategory=dist_1234 throughput=25i 1",
			appender.buildPostDataLine(mockData));
	}
}
