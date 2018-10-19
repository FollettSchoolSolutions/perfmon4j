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

package web.org.perfmon4j.restdatasource.dataproviders;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import junit.framework.TestCase;

import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.util.JDBCHelper;

import web.org.perfmon4j.restdatasource.BaseDatabaseSetup;
import web.org.perfmon4j.restdatasource.DataSourceRestImpl;
import web.org.perfmon4j.restdatasource.data.Category;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.SystemID;
import web.org.perfmon4j.restdatasource.util.DateTimeHelper;

public class JVMDataProviderTest extends TestCase {

	private static String DATABASE_NAME = "Production";
	private final BaseDatabaseSetup databaseSetup = new BaseDatabaseSetup();
	private final JVMDataProvider provider = new JVMDataProvider();
	private Connection conn = null;
	private RegisteredDatabaseConnections.Database database = null;
	private final DateTimeHelper helper = new DateTimeHelper();
	
	void setUpDatabase() throws Exception {
		if (conn != null) {
			tearDownDatabase();
		}
		
		databaseSetup.setUpDatabase();
		RegisteredDatabaseConnections.addDatabase(DATABASE_NAME, true, BaseDatabaseSetup.JDBC_DRIVER, 
				null, BaseDatabaseSetup.JDBC_URL, null, null, null, null, null, null);
		conn = databaseSetup.getConnection();
		database = RegisteredDatabaseConnections.getDatabaseByName(DATABASE_NAME);
	}

	protected void tearDownDatabase() throws Exception {
		RegisteredDatabaseConnections.removeDatabase(DATABASE_NAME);
		databaseSetup.tearDownDatabase();
		
		conn = null;
		database = null;
	}
	
	
	public JVMDataProviderTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		if (conn != null) {
			tearDownDatabase();
		}
		
		super.tearDown();
	}

	public void testLookupMonitoredSystems() throws Exception {
		setUpDatabase();
		try {
			Set<MonitoredSystem> result = provider.lookupMonitoredSystems(conn, database, getStartTime(), getEndTime());
			assertNotNull(result);
			assertEquals("Should have no systems with JVM Results", 0, result.size());
			
			// Add a JVM Observation...
			databaseSetup.addJVMObservation(1, System.currentTimeMillis());
			result = provider.lookupMonitoredSystems(conn, database, getStartTime(), getEndTime());
			assertNotNull(result);
			assertEquals("Should have one system with JVM Results", 1, result.size());
			
			// Now check outside when the observation was recorded...
			databaseSetup.addJVMObservation(1, System.currentTimeMillis());
			result = provider.lookupMonitoredSystems(conn, database, getStartTime("now-10H"), getEndTime("now-8H"));
			assertNotNull(result);
			assertEquals("Should have no systems within the time period JVM Results", 0, result.size());
		} finally {
			tearDownDatabase();
		}
	}

	private SystemID getDefaultSystemID() throws SQLException {
		String databaseID = JDBCHelper.getDatabaseIdentity(databaseSetup.getConnection(), null);
		return new SystemID(databaseID, 1);
	}
	
	
	public void testLookupMonitoredCategories() throws Exception {
		setUpDatabase();
		try {
			SystemID[] systems = new SystemID[]{getDefaultSystemID()};
			
			
			Set<Category> result = provider.lookupMonitoredCategories(conn, database, systems, getStartTime(), getEndTime());
			assertNotNull(result);
			assertEquals("Should have no categories with JVM Results", 0, result.size());
			
			// Add a JVM Observation...
			databaseSetup.addJVMObservation(1, System.currentTimeMillis());
			result = provider.lookupMonitoredCategories(conn, database, systems, getStartTime(), getEndTime());
			assertNotNull(result);
			assertEquals("Should have one category with JVM Results", 1, result.size());
			
			// Now check outside when the observation was recorded...
			databaseSetup.addJVMObservation(1, System.currentTimeMillis());
			result = provider.lookupMonitoredCategories(conn, database, systems, getStartTime("now-10H"), getEndTime("now-8H"));
			assertNotNull(result);
			assertEquals("Should have no category within the time period JVM Results", 0, result.size());
		} finally {
			tearDownDatabase();
		}
	}
	
	public void testQuery() throws Exception{
		setUpDatabase();
		try {
			DataSourceRestImpl impl = new DataSourceRestImpl();
			String seriesDef = 
				TestHelper.buildSeriesDefinitionWithAllFields(getDefaultSystemID(), "JVM", 
					provider.getCategoryTemplate());
					
			
			web.org.perfmon4j.restdatasource.data.query.advanced.AdvancedQueryResult result = 
					impl.getQueryObservations(database.getID(), seriesDef, "now-1H", "now", "");
			assertEquals(provider.getCategoryTemplate().getFields().length, result.getSeries().length);
		} finally {
			tearDownDatabase();
		}
	}	
	
	private long getStartTime() {
		return getStartTime("now-8H");
	}
	
	private long getStartTime(String value) {
		return helper.parseDateTime(value).getTimeForStart();
	}

	private long getEndTime() {
		return getEndTime("now");
	}
	
	private long getEndTime(String value) {
		return helper.parseDateTime(value).getTimeForEnd();
	}

}
