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

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.restdatasource.data.Category;
import org.perfmon4j.restdatasource.data.Database;
import org.perfmon4j.restdatasource.data.MonitoredSystem;
import org.perfmon4j.util.JDBCHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RestImplTest extends BaseDatabaseTest {
	private final Dispatcher dispatcher;
	private final ObjectMapper mapper;
	
	public RestImplTest(String name) {
		super(name);
		
		dispatcher = MockDispatcherFactory.createDispatcher();

		POJOResourceFactory noDefaults = new POJOResourceFactory(RestImpl.class);
		dispatcher.getRegistry().addResourceFactory(noDefaults);
		mapper = new ObjectMapper();

	}
	
	private static String DATABASE_NAME = "Production";
	

	protected void setUp() throws Exception {
		super.setUp();
		
		addCategory("WebRequest");
		addCategory("WebRequest.search");
		RegisteredDatabaseConnections.addJDBCDatabase(DATABASE_NAME, true, BaseDatabaseTest.JDBC_DRIVER, 
				null, BaseDatabaseTest.JDBC_URL, null, null, null);
	}

	protected void tearDown() throws Exception {
		RegisteredDatabaseConnections.removeDatabase(DATABASE_NAME);
		
		super.tearDown();
	}

	private  <T> T  responseToObject(MockHttpResponse response, Class<T> clazz) throws Exception {
		return mapper.readValue(response.getContentAsString(), clazz);
	}
	
	public void testGetDatabasesWithSingleDatabase() throws Exception {
        MockHttpResponse response = getDatabasesThroughRest();

        assertEquals("",  HttpServletResponse.SC_OK,  response.getStatus());

        Database[] result = responseToObject(response, Database[].class);
        assertEquals("Should not have had one database",1, result.length);
        Database d = result[0];
        
        assertTrue("isDefault", d.isDefault());
        assertEquals("name", "Production", d.getName());
        assertEquals("database ID",  JDBCHelper.getDatabaseIdentity(connection, null), d.getID());
	}

	public void testGetDatabasesWithNoDatabases() throws Exception {
		RegisteredDatabaseConnections.removeDatabase(DATABASE_NAME);
		
		MockHttpResponse response = getDatabasesThroughRest();
        assertEquals("",  HttpServletResponse.SC_OK,  response.getStatus());

        Database[] result = responseToObject(response, Database[].class);
        assertEquals("Should not have had any databases", 0, result.length);
	}
	
	public void testGetSystems() throws Exception {
		String databaseID = JDBCHelper.getDatabaseIdentity(connection, null);
		
		MockHttpResponse response = getSystemsThroughRest("BADID");
		assertEquals("No database registerd with BADID", 404, response.getStatus());
		
		response = getSystemsThroughRest("default");
		assertEquals("default should be found", 200, response.getStatus());
		
		response = getSystemsThroughRest(databaseID);
		assertEquals("There is a database registered with databaseID", 200, response.getStatus());
		assertEquals("database does not contain any systems with observations", 0, responseToObject(response, MonitoredSystem[].class).length);
		
		// Add an observation to the "Default" system...
		addInterval(1L, 1L, "now");
		
		response = getSystemsThroughRest(databaseID);
		MonitoredSystem systems[] = responseToObject(response, MonitoredSystem[].class);
		
		assertEquals("The default system has an observation", 1, systems.length);
		// Verify fields associated with the monitored systems.
		assertEquals("name", "Default", systems[0].getName());
		assertEquals("ID", databaseID + ".1", systems[0].getID());
		

		// Now add another system with an observation WAY in the past.
		long systemID = addSystem("Production");
		addInterval(systemID, 1L, "now-100h");

		response = getSystemsThroughRest(databaseID);
		systems = responseToObject(response, MonitoredSystem[].class);
		assertEquals("Still only 1 system has an observation within default start/end time", 1, systems.length);

		// Now change timeStart to include the observation.
		response = getSystemsThroughRest(databaseID, "timeStart=now-101h");
		systems = responseToObject(response, MonitoredSystem[].class);
		assertEquals("Should now have 2 systems", 2, systems.length);
	}
	
	
	public void testGetCategories() throws Exception {
		String databaseID = JDBCHelper.getDatabaseIdentity(connection, null);
		
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
		addInterval(1L, 1L, "now");
		response = getCategoriesThroughRest(databaseID, defaultSystemID);
		categories = responseToObject(response, Category[].class);

		assertEquals("Now have one active category", 1, categories.length);
		assertEquals("Category name", "Interval.WebRequest", categories[0].getName());
		assertEquals("Category template", "Interval", categories[0].getTemplateName());
		
		// Add another system, this system will have no observations.
		long systemID = addSystem("Production");
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
