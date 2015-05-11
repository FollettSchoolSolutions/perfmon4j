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

package org.perfmon4j.restdatasource;


import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.restdatasource.data.Category;
import org.perfmon4j.restdatasource.data.Database;
import org.perfmon4j.restdatasource.data.MonitoredSystem;
import org.perfmon4j.restdatasource.data.query.advanced.AdvancedQueryResult;
import org.perfmon4j.restdatasource.data.query.advanced.Series;
import org.perfmon4j.restdatasource.util.DateTimeHelper;
import org.perfmon4j.restdatasource.util.DateTimeValue;
import org.perfmon4j.util.JDBCHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RestImplTest extends TestCase {
	private final Dispatcher dispatcher;
	private final ObjectMapper mapper;
	private final BaseDatabaseSetup databaseSetup = new BaseDatabaseSetup();
	private final DateTimeHelper helper = new DateTimeHelper();
	
	public RestImplTest(String name) {
		super(name);
		
		dispatcher = MockDispatcherFactory.createDispatcher();

		POJOResourceFactory noDefaults = new POJOResourceFactory(RestImpl.class);
		dispatcher.getRegistry().addResourceFactory(noDefaults);
		mapper = new ObjectMapper();

	}
	
	private static String DATABASE_NAME = "Production";

	void setUpDatabase() throws Exception {
		databaseSetup.setUpDatabase();
		
		databaseSetup.addCategory("WebRequest");
		databaseSetup.addCategory("WebRequest.search");
		RegisteredDatabaseConnections.addJDBCDatabase(DATABASE_NAME, true, BaseDatabaseSetup.JDBC_DRIVER, 
				null, BaseDatabaseSetup.JDBC_URL, null, null, null);
	}

	protected void tearDownDatabase() throws Exception {
		RegisteredDatabaseConnections.removeDatabase(DATABASE_NAME);
		
		databaseSetup.tearDownDatabase();
	}

	private  <T> T  responseToObject(MockHttpResponse response, Class<T> clazz) throws Exception {
		return mapper.readValue(response.getContentAsString(), clazz);
	}
	
	public void testGetDatabasesWithSingleDatabase() throws Exception {
		setUpDatabase();
		try {
	        MockHttpResponse response = getDatabasesThroughRest();
	
	        assertEquals("",  HttpServletResponse.SC_OK,  response.getStatus());
	
	        Database[] result = responseToObject(response, Database[].class);
	        assertEquals("Should not have had one database",1, result.length);
	        Database d = result[0];
	        
	        assertTrue("isDefault", d.isDefault());
	        assertEquals("name", "Production", d.getName());
	        assertEquals("database ID",  JDBCHelper.getDatabaseIdentity(databaseSetup.getConnection(), null), d.getID());
		} finally {
			tearDownDatabase();
		}
	}

	public void testGetDatabasesWithNoDatabases() throws Exception {
		MockHttpResponse response = getDatabasesThroughRest();
        assertEquals("",  HttpServletResponse.SC_OK,  response.getStatus());

        Database[] result = responseToObject(response, Database[].class);
        assertEquals("Should not have had any databases", 0, result.length);
	}
	
	public void testGetSystems() throws Exception {
		setUpDatabase();
		try {
			String databaseID = JDBCHelper.getDatabaseIdentity(databaseSetup.getConnection(), null);
			
			MockHttpResponse response = getSystemsThroughRest("BADID");
			assertEquals("No database registerd with BADID", 404, response.getStatus());
			
			response = getSystemsThroughRest("default");
			assertEquals("default should be found", 200, response.getStatus());
			
			response = getSystemsThroughRest(databaseID);
			assertEquals("There is a database registered with databaseID", 200, response.getStatus());
			assertEquals("database does not contain any systems with observations", 0, responseToObject(response, MonitoredSystem[].class).length);
			
			// Add an observation to the "Default" system...
			databaseSetup.addInterval(1L, 1L, "now");
			
			response = getSystemsThroughRest(databaseID);
			MonitoredSystem systems[] = responseToObject(response, MonitoredSystem[].class);
			
			assertEquals("The default system has an observation", 1, systems.length);
			// Verify fields associated with the monitored systems.
			assertEquals("name", "Default", systems[0].getName());
			assertEquals("ID", databaseID + ".1", systems[0].getID());
			
	
			// Now add another system with an observation WAY in the past.
			long systemID = databaseSetup.addSystem("Production");
			databaseSetup.addInterval(systemID, 1L, "now-100h");
	
			response = getSystemsThroughRest(databaseID);
			systems = responseToObject(response, MonitoredSystem[].class);
			assertEquals("Still only 1 system has an observation within default start/end time", 1, systems.length);
	
			// Now change timeStart to include the observation.
			response = getSystemsThroughRest(databaseID, "timeStart=now-101h");
			systems = responseToObject(response, MonitoredSystem[].class);
			assertEquals("Should now have 2 systems", 2, systems.length);
		} finally {
			tearDownDatabase();
		}
	}
	
	public void testGetCategories() throws Exception {
		setUpDatabase();
		try {
			String databaseID = JDBCHelper.getDatabaseIdentity(databaseSetup.getConnection(), null);
			
			MockHttpResponse response;
			
			response = getCategoriesThroughRest("BADID", "BADDB.1");
			assertEquals("No database registerd with BADID should return 404", 404, response.getStatus());
			
			response = getCategoriesThroughRest(databaseID, "BADDB.1");
			assertEquals("If system ID does not match the database it is a bad request", 400, response.getStatus());
			
			final String defaultSystemID = databaseID + ".1";
			
			response = getCategoriesThroughRest(databaseID, defaultSystemID);
			assertEquals("Default system...  Although no categories, should return empty collection", 200, response.getStatus());
	
			response = getCategoriesThroughRest("default", defaultSystemID);
			assertEquals("Also works if you specify the 'default' database", 200, response.getStatus());
			
			Category categories[] = responseToObject(response, Category[].class);
			assertEquals("No active categories yet", 0, categories.length);
			
			// Add an observation to the Interval.WebRequest category.
			databaseSetup.addInterval(1L, 1L, "now");
			response = getCategoriesThroughRest(databaseID, defaultSystemID);
			categories = responseToObject(response, Category[].class);
	
			assertEquals("Now have one active category", 1, categories.length);
			assertEquals("Category name", "Interval.WebRequest", categories[0].getName());
			assertEquals("Category template", "Interval", categories[0].getTemplateName());
			
			// Add another system, this system will have no observations.
			long systemID = databaseSetup.addSystem("Production");
			final String productionSystemID = databaseID + "." + systemID;
			
			response = getCategoriesThroughRest("default", productionSystemID);
			assertEquals("Request categories for the Production system in the default database", 200, response.getStatus());
	
			categories = responseToObject(response, Category[].class);
			assertEquals("The production system has no active categories yet", 0, categories.length);
			
			// Now get categories from multiple systems.
			// Multiple systems are specified in a tilde separated list.
			final String multipleSystems = defaultSystemID + "~" + productionSystemID;
			
			response = getCategoriesThroughRest("default", multipleSystems);
			assertEquals("Request categories for the Production system and the default systmm in the default database", 200, response.getStatus());
	
			categories = responseToObject(response, Category[].class);
			assertEquals("When specifying multiple systems you get a category if at least one of the systems has an observation", 
					1, categories.length);
		} finally {
			tearDownDatabase();
		}
	}
	
	public void testGetQueryObservations() throws Exception {
		setUpDatabase();
		try {
			String databaseID = JDBCHelper.getDatabaseIdentity(databaseSetup.getConnection(), null);
			
			MockHttpResponse response;
			Series series;
			AdvancedQueryResult result;
			
			response = queryThroughRest("BADID", "BADDB.1~Interval.WebRequest~avgDuration");
			assertEquals("No database registerd with BADID should return 404", 404, response.getStatus());
			
			response = queryThroughRest(databaseID, "BADDB.1~Interval.WebRequest~averageDuration");
			assertEquals("Good database, but the seriesDefinition contains a system that does not match", 400, response.getStatus());
			
			response = queryThroughRest(databaseID, databaseID + ".1~NOTATEMPLATE.WebRequest~averageDuration");
			assertEquals("No template found for Category NOTATEMPLATE", 400, response.getStatus());

			response = queryThroughRest(databaseID, databaseID + ".1~Interval.WebRequest~NoFieldWithThisName");
			assertEquals("Template is OK, but field does not exist", 400, response.getStatus());
			
			// Try a successful request with no observations recorded.
			response = queryThroughRest(databaseID, databaseID + ".1~Interval.WebRequest~averageDuration");
			assertEquals("Valid request", 200, response.getStatus());
			
			result = responseToObject(response, AdvancedQueryResult.class);
			assertNotNull("Should have a result", result);
			
			assertEquals("No data to collect", 0, result.getDateTime().length);
			assertEquals("Should have one series", 1, result.getSeries().length);
			
			series = result.getSeries()[0];
			assertEquals("Should have a default alias", "Series 1", series.getAlias());
			assertEquals("SystemID", databaseID + ".1", series.getSystemID());
			assertEquals("series.getCategory", "Interval.WebRequest", series.getCategory());
			assertEquals("series.getAggregationMethod", "NATURAL", series.getAggregationMethod());
			assertEquals("series.getFieldName", "averageDuration", series.getFieldName());
			assertEquals("Should have no values", 0, series.getValues().length);

			// Try again, only this time add an observation...
			DateTimeValue now = helper.parseDateTime("now");
			
			databaseSetup.addInterval(1L, 1L, now.getFixedDateTime());
			
			response = queryThroughRest(databaseID, databaseID + ".1~Interval.WebRequest~averageDuration");
			assertEquals("Valid request", 200, response.getStatus());
			
			result = responseToObject(response, AdvancedQueryResult.class);
			assertNotNull("Should have a result", result);
			
			assertEquals("Have 1 element", 1, result.getDateTime().length);
			assertEquals("Expected date/time", now.getFixedDateTime(), result.getDateTime()[0]);
			
			
			assertEquals("Should have one series", 1, result.getSeries().length);
			
			series = result.getSeries()[0];
			assertEquals("Should have a default alias", "Series 1", series.getAlias());
			assertEquals("Should have one observation", 1, series.getValues().length);
			assertEquals("Observation Value", "10.3", series.getValues()[0].toString());
		} finally {
			tearDownDatabase();
		}
	}

	public void testMultipleSeriesAcrossCategories() throws Exception {
		setUpDatabase();
		try {
			String databaseID = JDBCHelper.getDatabaseIdentity(databaseSetup.getConnection(), null);
			
			MockHttpResponse response;
			Series series;
			AdvancedQueryResult result;
			
			DateTimeValue now = helper.parseDateTime("now");
			
			// Add one observation for "WebRequest"
			databaseSetup.addInterval(1L, 1L, now.getFixedDateTime()); // Average will be set to 10.3
			
			// Add one observation for WebRequest.search
			databaseSetup.addInterval(1L, 2L, now.getFixedDateTime(), 2); // Average will be set to 20.6
			
			response = queryThroughRest(databaseID, databaseID + ".1~Interval.WebRequest~averageDuration"
					+ "_" + databaseID + ".1~Interval.WebRequest.search~averageDuration");
			assertEquals("Valid request", 200, response.getStatus());
			
			result = responseToObject(response, AdvancedQueryResult.class);
			assertNotNull("Should have a result", result);
			
			assertEquals("Have 1 element", 1, result.getDateTime().length);
			assertEquals("Expected date/time", now.getFixedDateTime(), result.getDateTime()[0]);
			
			assertEquals("Should have two series", 2, result.getSeries().length);
			
			series = result.getSeries()[0];
			assertEquals("Should have a default alias", "Series 1", series.getAlias());
			assertEquals("Should have one observation", 1, series.getValues().length);
			assertEquals("Observation Value", "10.3", series.getValues()[0].toString());
			
			series = result.getSeries()[1];
			assertEquals("Should have a default alias", "Series 2", series.getAlias());
			assertEquals("Should have one observation", 1, series.getValues().length);
			assertEquals("Observation Value", "20.6", series.getValues()[0].toString());
		} finally {
			tearDownDatabase();
		}
	}
	
	
	private MockHttpResponse queryThroughRest(String databaseID, String seriesDefinition) throws URISyntaxException {
		return queryThroughRest(databaseID, seriesDefinition, "");
	}
	
	private MockHttpResponse queryThroughRest(String databaseID, String seriesDefinition, String queryParams) throws URISyntaxException {
		if (queryParams.length() > 0) {
			queryParams = "&" + queryParams;
		}
        MockHttpRequest request = MockHttpRequest.get("/datasource/databases/" + databaseID + "/observations?seriesDefinition=" + seriesDefinition + queryParams);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
//        if (response.getStatus() != 200) {
//        	System.err.println(response.getErrorMessage());
//        }
        
        
        return response;
	}
	
	
	private MockHttpResponse getCategoriesThroughRest(String databaseID, String systemID) throws URISyntaxException {
		return getCategoriesThroughRest(databaseID, systemID, "");
	}
	
	private MockHttpResponse getCategoriesThroughRest(String databaseID, String systemID, String queryParams) throws URISyntaxException {
		if (queryParams.length() > 0) {
			queryParams = "&" + queryParams;
		}
        MockHttpRequest request = MockHttpRequest.get("/datasource/databases/" + databaseID + "/categories?systemID=" + systemID + queryParams);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
        return response;
	}
	
	
	private MockHttpResponse getSystemsThroughRest(String databaseID) throws URISyntaxException {
		return getSystemsThroughRest(databaseID, "");
	}
	
	private MockHttpResponse getSystemsThroughRest(String databaseID, String queryParams) throws URISyntaxException {
		if (queryParams.length() > 0) {
			queryParams = "?" + queryParams;
		}
        MockHttpRequest request = MockHttpRequest.get("/datasource/databases/" + databaseID + "/systems" + queryParams);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
        return response;
	}
	
	private MockHttpResponse getDatabasesThroughRest() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/datasource/databases");
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
        return response;
	}
}
