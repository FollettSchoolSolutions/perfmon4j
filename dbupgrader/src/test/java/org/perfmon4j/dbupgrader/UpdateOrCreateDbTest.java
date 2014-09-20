/*
 *	Copyright 2008-2011 Follett Software Company 
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
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j.dbupgrader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.TestCase;

import org.perfmon4j.dbupgrader.UpdateOrCreateDb.Parameters;

public class UpdateOrCreateDbTest extends TestCase {
	public static final String JDBC_URL = "jdbc:derby:memory:mydb"; 
	public static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private Connection conn = null;
	
	public UpdateOrCreateDbTest(String name) {
		super(name);
	}
	
	protected void setUp() throws Exception {
		conn = UpdaterUtil.createConnection(JDBC_DRIVER, null, JDBC_URL + ";create=true", null, null);
		
		super.setUp();
	}

	protected void tearDown() throws Exception {
		UpdaterUtil.closeNoThrow(conn);
		
		try {
			UpdaterUtil.createConnection(JDBC_DRIVER, null, "jdbc:derby:;shutdown=true", null, null);
		} catch (SQLException sn) {
		}
		
		super.tearDown();
	}
	
	private static String rsRowToString(ResultSet rs) throws SQLException {
		String result = "";
		
		ResultSetMetaData d = rs.getMetaData();
		int count = d.getColumnCount();
		for (int i = 1; i <= count; i++) {
			result += d.getColumnLabel(i) + "=" + rs.getString(i) + "\r\n";
		}
		
		return result;
	}
	
	private static String dumpQuery(Connection conn, String SQL) throws SQLException {
		String result = "";
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while (rs.next()) {
				result += rsRowToString(rs);
				result += "*********************************************\r\n";
			}
			
		} finally {
			UpdaterUtil.closeNoThrow(rs);
			UpdaterUtil.closeNoThrow(stmt);
		}
		
		return result;
	}	
	public void testPopulateDatabase() throws Exception { 
		// Start with an empty database...
		UpdateOrCreateDb.main(new String[]{"driverClass=org.apache.derby.jdbc.EmbeddedDriver",
				"jdbcURL=" + JDBC_URL,
				"driverJarFile=EMBEDDED",
				"schema=test1"});
		
		assertTrue("Should have a P4JSystem table", UpdaterUtil.doesTableExist(conn, "test1", "P4JSystem"));
		
		System.out.println(dumpQuery(conn, "SELECT * FROM test1.DATABASECHANGELOG"));
	}
	
	public void testParseParameters() throws Exception {
		String args[] = {
				"userName=dave",
				"password=pw",
				"jdbcURL=my.jdbc.url",
				"driverClass=myDriver",
				"driverJarFile=c:/mydriver.jar",
				"schema=dbo"
		};
		
		Parameters params = UpdateOrCreateDb.getParameters(args);
		assertNotNull(params);
		assertEquals("dave", params.getUserName());
		assertEquals("pw", params.getPassword());
		
		assertEquals("my.jdbc.url", params.getJdbcURL());
		assertEquals("myDriver", params.getDriverClass());
		assertEquals("c:/mydriver.jar", params.getDriverJarFile());
		assertEquals("dbo", params.getSchema());
		assertTrue(params.isValid());
	}
	

	public void testInsufficientParameters() throws Exception {
		String args[] = {};
		
		Parameters params = UpdateOrCreateDb.getParameters(args);
		assertTrue("InsufficentParameters", params.isInsufficentParameters());
		assertFalse("isValid", params.isValid());
	}
	
	public void testBadParameters() throws Exception {
		String args[] = {
				"userName=dave",
				"password=pw",
				"jdbcURL=my.jdbc.url",
				"driverClass=myDriver",
				"driverJarFile=c:/mydriver.jar",
				"somethingElse=5"
		};
		
		Parameters params = UpdateOrCreateDb.getParameters(args);
		assertEquals(1, params.getBadParameters().size());
		assertEquals("somethingElse=5", params.getBadParameters().get(0));
		assertFalse(params.isValid());
	}

	
	public void testInstallBaseChangeLogs() throws Exception {
		// Create a P4JIntervalData table...  We will use the existence of this table 
		// to indicate that this database was initialized prior to the change over to
		// Liquibase.
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute("CREATE TABLE test2.P4JIntervalData(id INT)");
			// Start with a database that contain the base tables,
			// but does not contain the liquibase change logs.. 
			// Should install change.logs and any additional upgrades.
			UpdateOrCreateDb.main(new String[]{"driverClass=org.apache.derby.jdbc.EmbeddedDriver",
					"jdbcURL=" + JDBC_URL,
					"driverJarFile=EMBEDDED",
					"schema=test2"});
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
		assertTrue("Should have a changelog", UpdaterUtil.doesTableExist(conn, "test2", "DATABASECHANGELOG"));
	}
	

}
