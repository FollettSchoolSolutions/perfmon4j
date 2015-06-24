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

package org.perfmon4j;

import java.util.Properties;


public class RegisteredDatabaseConnectionsTest extends SQLTest {
	private final static String JDBC_APPENDER_CLASS_NAME = JDBCSQLAppender.class.getName();
	private final static String POOLED_SQL_APPENDER_CLASS_NAME = PooledSQLAppender.class.getName();
	
	public RegisteredDatabaseConnectionsTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		RegisteredDatabaseConnections.mockIdentityAndVersionForTest = true;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		RegisteredDatabaseConnections.mockIdentityAndVersionForTest = false;
	}
	
	public void testAddRemoveJDBCDatabase() throws Exception {
		RegisteredDatabaseConnections.addDatabase("First", false, SQLTest.DRIVER_CLASS, null, SQLTest.JDBC_URL, SQLTest.SCHEMA_NAME, null, null, null, null, null);
		
		assertNotNull(RegisteredDatabaseConnections.getDefaultDatabase());
		
		RegisteredDatabaseConnections.removeDatabase("First");
		
		assertNull(RegisteredDatabaseConnections.getDefaultDatabase());
	}
	
	public void testConfigureAddJDBCSQLAppender() throws Exception {
		PerfMonConfiguration config = new PerfMonConfiguration();

		Properties attributes = new Properties();
    	attributes.setProperty("driverPath", "c:/driver.path");
    	attributes.setProperty("driverClass", "myDriver");
    	attributes.setProperty("jdbcURL", "jdbc://localhost");
    	attributes.setProperty("userName", "userName");
    	attributes.setProperty("password", "password");
    	
		config.defineAppender("production", JDBC_APPENDER_CLASS_NAME, "1 minute", attributes);
		
		RegisteredDatabaseConnections.config(config);
		
		assertNotNull("Should have registered database", RegisteredDatabaseConnections.getDatabaseByName("production"));
		
		config = new PerfMonConfiguration();
		RegisteredDatabaseConnections.config(config);

		assertNull("Should not have registered database", RegisteredDatabaseConnections.getDatabaseByName("production"));
	}

//	
//	Figure out what to do with this test...  It needs an active jndiBased dataSource in order to pass.
//	public void testConfigureAddDatabasePool() throws Exception {
//		PerfMonConfiguration config = new PerfMonConfiguration();
//
//		Properties attributes = new Properties();
//    	attributes.setProperty("poolName", "productionDS");
//    	
//		config.defineAppender("production", POOLED_SQL_APPENDER_CLASS_NAME, "1 minute", attributes);
//		
//		RegisteredDatabaseConnections.config(config);
//		
//		assertNotNull("Should have registered database", RegisteredDatabaseConnections.getDatabaseByName("production"));
//		
//		config = new PerfMonConfiguration();
//		RegisteredDatabaseConnections.config(config);
//
//		assertNull("Should not have registered database", RegisteredDatabaseConnections.getDatabaseByName("production"));
//	}
//	
	
	public void testReplaceDatabase() throws Exception {
		PerfMonConfiguration config = new PerfMonConfiguration();

		Properties attributes = new Properties();
    	attributes.setProperty("driverPath", "c:/driver.path");
    	attributes.setProperty("driverClass", "myDriver");
    	attributes.setProperty("jdbcURL", "jdbc://localhost");
    	attributes.setProperty("userName", "userName");
    	attributes.setProperty("password", "password");
    	
		config.defineAppender("production", JDBC_APPENDER_CLASS_NAME, "1 minute", attributes);
		
		RegisteredDatabaseConnections.config(config);
		
		String signature = RegisteredDatabaseConnections.getDatabaseByName("production").getSignature();
	
		config = new PerfMonConfiguration();
		// Change the userName
	 	attributes.setProperty("userName", "anotherUserName");
		config.defineAppender("production", JDBC_APPENDER_CLASS_NAME, "1 minute", attributes);
		RegisteredDatabaseConnections.config(config);
	    	
		String newSignature = RegisteredDatabaseConnections.getDatabaseByName("production").getSignature();
		
		assertFalse("Should have updated the database", newSignature.equals(signature));

		RegisteredDatabaseConnections.removeDatabase("production");
	}

}
