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

import junit.framework.TestCase;

import org.perfmon4j.restdatasource.data.AggregationMethod;
import org.perfmon4j.restdatasource.data.CategoryTemplate;
import org.perfmon4j.restdatasource.data.Field;

public class TemplateRegistryTest extends TestCase {

	private TemplateRegistry registry;
	
	public TemplateRegistryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		registry = new TemplateRegistry();
		registry.registerTemplate(new TestCategoryTemplate());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testBasicResolve() {
		ParsedSeriesDefinition def = ParsedSeriesDefinition.parse("ABCD-EFGH.1~Person.student~shoeSize", "ABCD-EFGH")[0];
		
		SeriesField field = registry.resolveField(def);
		assertNotNull(field);
		
		assertEquals("Should be the default aggregation method", AggregationMethod.AVERAGE,  field.getAggregationMethod());
		assertEquals("Category name", "Person.student", field.getCategory().getName());
		assertEquals("Category Template", "Person", field.getCategory().getTemplateName());
		assertEquals("Field name", "shoeSize", field.getField().getName());
		
		def = ParsedSeriesDefinition.parse("ABCD-EFGH.1~Person.student~hatSize", "ABCD-EFGH")[0];
		
		field = registry.resolveField(def);
		assertNotNull(field);
		
		assertEquals("Should be the default aggregation method", AggregationMethod.SUM,  field.getAggregationMethod());
		assertEquals("Category name", "Person.student", field.getCategory().getName());
		assertEquals("Category Template", "Person", field.getCategory().getTemplateName());
		assertEquals("Field name", "hatSize", field.getField().getName());
	}
	

	public void testOverrideDefaultAggregationMethod() {
		ParsedSeriesDefinition def = ParsedSeriesDefinition.parse("MAX~ABCD-EFGH.1~Person.student~shoeSize", "ABCD-EFGH")[0];
		
		SeriesField field = registry.resolveField(def);
		assertEquals("Should be the specified aggregation method", AggregationMethod.MAX,  field.getAggregationMethod());
	}

	public void testUnsupportedAggregationMethod() {
		ParsedSeriesDefinition def = ParsedSeriesDefinition.parse("MIN~ABCD-EFGH.1~Person.student~shoeSize", "ABCD-EFGH")[0];
		
		try {
			registry.resolveField(def);
			fail("Expected an exception, MIN is not supported by field shoeSize");
		} catch (BadRequestException e) {
			assertEquals("Exception message", "Aggregation method MIN not supported for field shoeSize in Person category template", e.getMessage());
		}
	}

	public void testUnregisteredTemplate() {
		ParsedSeriesDefinition def = ParsedSeriesDefinition.parse("ABCD-EFGH.1~Pet.student~shoeSize", "ABCD-EFGH")[0];
		
		try {
			registry.resolveField(def);
			fail("No template registered named Pet");
		} catch (BadRequestException e) {
			assertEquals("Exception message", "Category template Pet not found", e.getMessage());
		}
	}
	
	public void testInvalidField() {
		ParsedSeriesDefinition def = ParsedSeriesDefinition.parse("ABCD-EFGH.1~Person.student~height", "ABCD-EFGH")[0];	
		try {
			registry.resolveField(def);
			fail("Template does not contain a height field");
		} catch (BadRequestException e) {
			assertEquals("Exception message", "Category template Person field height not found", e.getMessage());
		}
	}
	
	private static class TestCategoryTemplate extends CategoryTemplate {
		public TestCategoryTemplate() {
			super.setName("Person");
			
			Field shoeSize = new Field();
			shoeSize.setName("shoeSize");
			shoeSize.setAggregationMethods(new AggregationMethod[]{AggregationMethod.AVERAGE, AggregationMethod.MAX});
			shoeSize.setDefaultAggregationMethod(AggregationMethod.AVERAGE);

			Field hatSize = new Field();
			hatSize.setName("hatSize");
			hatSize.setAggregationMethods(new AggregationMethod[]{AggregationMethod.SUM, AggregationMethod.MIN});
			hatSize.setDefaultAggregationMethod(AggregationMethod.SUM);
			
			this.setFields(new Field[]{shoeSize, hatSize});
		}
	}
}
