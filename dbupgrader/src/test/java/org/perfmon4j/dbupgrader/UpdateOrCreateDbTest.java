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
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.perfmon4j.dbupgrader.UpdateOrCreateDb.Parameters;

public class UpdateOrCreateDbTest extends TestCase {
	private static String SCHEMA = "TEST";
	public static final String JDBC_URL = "jdbc:derby:memory:mydb"; 
	public static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private Connection conn = null;
	
	public UpdateOrCreateDbTest(String name) {
		super(name);
	}
	
	protected void setUp() throws Exception {
		conn = UpdaterUtil.createConnection(JDBC_DRIVER, null, JDBC_URL + ";create=true", null, null);
		conn.setAutoCommit(true);
		
		super.setUp();
	}

	protected void tearDown() throws Exception {
		UpdaterUtil.closeNoThrow(conn);
		
		try {
			UpdaterUtil.createConnection(JDBC_DRIVER, null, JDBC_URL + ";drop=true", null, null);
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
				"schema=" + SCHEMA});
		
		assertTrue("Should have a P4JSystem table", UpdaterUtil.doesTableExist(conn, SCHEMA, "P4JSystem"));
		
		System.out.println(dumpQuery(conn, "SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG"));
		assertTrue("Database change log should reflect databaseLabel 0002.0 applied", databaseLabelExistsInChangeLog("0002.0"));
		
		int systemRows = getQueryCount("SELECT count(*) FROM " + SCHEMA 
			+ ".P4JSystem WHERE SystemID=1 AND SystemName='Default'");
		assertEquals("Should have populated default system row", 1, systemRows);
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

	/**
	 * This test verifies we can upgrade a database that was created with
	 * SQL Scripts.  These types of test will NOT be required post
	 * database version 3.0 since the database will be populated with Liquibase
	 * and contain the appropriate change log.
	 * @throws Exception
	 */
	public void testInstallBaseChangeLogsVersion1Db() throws Exception {
		Statement stmt = null;
		try {
			// Simulate a database that was created with the Perfmon4j Version 1.0.2
			// Database scripts.
			applyChangeLog("org/perfmon4j/initial-change-log.xml");
			dropLiquibaseTables();
			assertFalse("Make sure version 2.0 changes have not been applied", UpdaterUtil.doesColumnExist(conn, SCHEMA, "P4JIntervalData", "SQLMaxDuration"));
			
			// Start with a database that contain the base tables,
			// but does not contain the liquibase change logs.. 
			// Should install change.logs and any additional upgrades.
			UpdateOrCreateDb.main(new String[]{"driverClass=org.apache.derby.jdbc.EmbeddedDriver",
					"jdbcURL=" + JDBC_URL,
					"driverJarFile=EMBEDDED",
					"schema=" + SCHEMA});
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
		assertTrue("Should have a changelog", UpdaterUtil.doesTableExist(conn, SCHEMA, "DATABASECHANGELOG"));
		assertTrue("Should have applied version 2.0 changes", UpdaterUtil.doesColumnExist(conn, SCHEMA, "P4JIntervalData", "SQLMaxDuration"));
		assertTrue("Database change log should reflect databaseLabel 0002.0 applied", databaseLabelExistsInChangeLog("0002.0"));
	}

	/**
	 * This test verifies we can upgrade a database that was created with
	 * SQL Scripts.  These types of test will NOT be required post
	 * database version 3.0 since the database will be populated with Liquibase
	 * and contain the appropriate change log.
	 * @throws Exception
	 */
	public void testInstallBaseChangeLogsVersion2Db() throws Exception {
		Statement stmt = null;
		try {
			// Simulate a database that was created with the Perfmon4j Version 1.1.0
			// Database scripts.
			applyChangeLog("org/perfmon4j/initial-change-log.xml");
			applyChangeLog("org/perfmon4j/version-2-change-log.xml");
			dropLiquibaseTables();
			
			// Start with a database that contain the base tables,
			// but does not contain the liquibase change logs.. 
			// Should install change.logs and any additional upgrades.
			UpdateOrCreateDb.main(new String[]{"driverClass=org.apache.derby.jdbc.EmbeddedDriver",
					"jdbcURL=" + JDBC_URL,
					"driverJarFile=EMBEDDED",
					"schema=" + SCHEMA});
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
		assertTrue("Should have a changelog", UpdaterUtil.doesTableExist(conn, SCHEMA, "DATABASECHANGELOG"));
		System.out.println(dumpQuery(conn, "SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG"));

		assertTrue("Database change log should reflect databaseLabel 0002.0 applied", databaseLabelExistsInChangeLog("0002.0"));
	}

	
	/**
	 * This test verifies we can upgrade a database that was created with
	 * SQL Scripts.  These types of test will NOT be required post
	 * database version 3.0 since the database will be populated with Liquibase
	 * and contain the appropriate change log.
	 * @throws Exception
	 */
	public void testInstallBaseChangeLogsVersion3Db() throws Exception {
		Statement stmt = null;
		try {
			// Simulate a database that was created with the Perfmon4j Version 1.2.0
			// Database scripts.
			applyChangeLog("org/perfmon4j/initial-change-log.xml");
			applyChangeLog("org/perfmon4j/version-2-change-log.xml");
			applyChangeLog("org/perfmon4j/version-3-change-log.xml");
			dropLiquibaseTables();
			
			// Start with a database that contain the base tables,
			// but does not contain the liquibase change logs.. 
			// Should install change.logs and any additional upgrades.
			UpdateOrCreateDb.main(new String[]{"driverClass=org.apache.derby.jdbc.EmbeddedDriver",
					"jdbcURL=" + JDBC_URL,
					"driverJarFile=EMBEDDED",
					"schema=" + SCHEMA});
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
		assertTrue("Should have a changelog", UpdaterUtil.doesTableExist(conn, SCHEMA, "DATABASECHANGELOG"));
		System.out.println(dumpQuery(conn, "SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG"));

		assertTrue("Database change log should reflect databaseLabel 0003.0 applied", databaseLabelExistsInChangeLog("0003.0"));
	}
	
	
	public void DontRunAutomatically_testLivePostgres() {
		String [] args = new String[] {
			"driverJarFile=/home/perfmon/host/tools/common/JDBCDrivers/postgresql-9.3-1102.jdbc4.jar", 
			"driverClass=org.postgresql.Driver", 
			"jdbcURL=jdbc:postgresql://10.0.2.2:15432/TestUpgrade", 
			"userName=perfmonwriter", 
			"password=perfmon"
			};
		UpdateOrCreateDb.main(args);
	}
	
	public void X_testLiveSQLServer() {
		String [] args = new String[] {
			"driverJarFile=/home/perfmon/host/tools/common/JDBCDrivers/sqljdbc4.jar", 
			"driverClass=com.microsoft.sqlserver.jdbc.SQLServerDriver", 
			"jdbcURL=jdbc:sqlserver://10.0.2.2:1433;databaseName=TestUpgrade", 
			"userName=perfmonwriter", 
			"password=perfmon"
			};
		
		UpdateOrCreateDb.main(args);
	}

	public void X_testLiveMySQL() {
		String [] args = new String[] {
			"driverJarFile=/home/perfmon/jdbc-drivers/mysql-connector-java-5.1.32-bin.jar", 
			"driverClass=com.mysql.jdbc.Driver", 
			"jdbcURL=jdbc:mysql://localhost:3306/TestUpgrade", 
			"userName=perfmonwriter", 
			"password=perfmon"
			};
		
		UpdateOrCreateDb.main(args);
	}
	
	public void ORACLE_NOT_SUPPORTED_testLiveOracle() {
		String [] args = new String[] {
			"driverJarFile=/home/perfmon/host/tools/common/JDBCDrivers/ojdbc6.jar", 
			"driverClass=oracle.jdbc.driver.OracleDriver", 
			"jdbcURL=jdbc:oracle:thin:@10.0.2.2:1521/xe", 
			"userName=perfmonwriter", 
			"password=perfmon",
			"clearChecksums=true"
			};
		
		UpdateOrCreateDb.main(args);
	}
	
	
	private boolean databaseLabelExistsInChangeLog(String label) throws Exception {
		boolean result = false;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG "
					+ "WHERE ID='" + label + "' AND AUTHOR='databaseLabel'");
			result = rs.next();
		} finally {
			UpdaterUtil.closeNoThrow(rs);
			UpdaterUtil.closeNoThrow(stmt);
		}
		
		return result;
	}
	
	private int getQueryCount(String query) throws Exception {
		int result = 0;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			rs.next();
			result = rs.getInt(1);
		} finally {
			UpdaterUtil.closeNoThrow(rs);
			UpdaterUtil.closeNoThrow(stmt);
		}
		return result;
	}
	
	private void applyChangeLog(String changeLog) throws Exception {
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
		db.setDefaultSchemaName(SCHEMA);
		
		Liquibase updater = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), db);
		updater.update((String)null);
	}
	
	private void dropLiquibaseTables() throws Exception {
		Statement stmt = null;
		
		try {
			stmt = conn.createStatement();
			stmt.execute("DROP TABLE " + SCHEMA + ".DATABASECHANGELOG");
			conn.commit();
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
	}
}
