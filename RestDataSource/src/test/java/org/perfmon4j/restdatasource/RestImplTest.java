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
        MockHttpResponse response = getDatabases();

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
		
		MockHttpResponse response = getDatabases();
        assertEquals("",  HttpServletResponse.SC_OK,  response.getStatus());

        Database[] result = responseToObject(response, Database[].class);
        assertEquals("Should not have had any databases", 0, result.length);
	}
	
	public void testGetSystems() throws Exception {
		String databaseID = JDBCHelper.getDatabaseIdentity(connection, null);
		
		MockHttpResponse response = getSystems("BADID");
		assertEquals("No database registerd with BADID", 404, response.getStatus());
		
		response = getSystems("default");
		assertEquals("default should be found", 200, response.getStatus());
		
		response = getSystems(databaseID);
		assertEquals("There is a database registered with databaseID", 200, response.getStatus());
		assertEquals("database does not contain any systems with observations", 0, responseToObject(response, MonitoredSystem[].class).length);
		
		// Add an observation to the "Default" system...
		addInterval(1L, 1L, "now");
		
		response = getSystems(databaseID);
		MonitoredSystem systems[] = responseToObject(response, MonitoredSystem[].class);
		
		assertEquals("The default system has an observation", 1, systems.length);

		// Now add another system with an observation WAY in the past.
		long systemID = addSystem("Production");
		addInterval(systemID, 1L, "now-100h");

		response = getSystems(databaseID);
		systems = responseToObject(response, MonitoredSystem[].class);
		assertEquals("Still only 1 system has an observation within default start/end time", 1, systems.length);

		// Now change timeStart to include the observation.
		response = getSystems(databaseID, "?timeStart=now-101h");
		systems = responseToObject(response, MonitoredSystem[].class);
		assertEquals("Should now have 2 systems", 2, systems.length);
System.out.println(systems[0]);		
System.out.println(systems[1]);		
	}
	
	private MockHttpResponse getSystems(String databaseID) throws URISyntaxException {
		return getSystems(databaseID, "");
	}
	
	private MockHttpResponse getSystems(String databaseID, String queryParams) throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/datasource/databases/" + databaseID + "/systems" + queryParams);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
        return response;
	}
	
	private MockHttpResponse getDatabases() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/datasource/databases");
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
        return response;
	}
}
