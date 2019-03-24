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

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
		super.setUp();
		
		NoCloseDerbyDatabase.initLiquibaseNoCloseDerbyDatabase();
		
		conn = UpdaterUtil.createConnection(JDBC_DRIVER, null, JDBC_URL + ";create=true", null, null);
		conn.setAutoCommit(true);
		executeUpdate("CREATE SCHEMA " + SCHEMA);
	}

	protected void tearDown() throws Exception {
		UpdaterUtil.closeNoThrow(conn);
		
		try {
			UpdaterUtil.createConnection(JDBC_DRIVER, null, JDBC_URL + ";drop=true", null, null);
		} catch (SQLException sn) {
		}
		
		NoCloseDerbyDatabase.deInitLiquibaseNoCloseDerbyDatabase();
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
		runUpdater();
		
		assertTrue("Should have a P4JSystem table", UpdaterUtil.doesTableExist(conn, SCHEMA, "P4JSystem"));
		
		System.out.println(dumpQuery(conn, "SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG"));
		assertTrue("Database change log should reflect databaseLabel 0002.0 applied", databaseLabelExistsInChangeLog("0002.0"));
		
		int systemRows = getQueryCount("SELECT count(*) FROM " + SCHEMA 
			+ ".P4JSystem WHERE SystemID=1 AND SystemName='Default'");
		assertEquals("Should have populated default system row", 1, systemRows);
	}

	public void testVersion4Update() throws Exception { 
		// Start with an empty database...
		runUpdater();
		
		int count = getQueryCount("SELECT count(*) FROM " + SCHEMA  
				+ ".DATABASECHANGELOG WHERE author = 'databaseLabel' AND ID = '0004.0'");
		assertEquals("should have installed 4.0 label", 1, count);
		
		try {
			// Select new columns...
			getQueryCount("SELECT count(*) FROM " + SCHEMA  
				+ ".P4JVMSnapshot WHERE systemCpuLoad > 1.0 AND processCpuLoad > 1.0");
		} catch (Exception ex) {
			fail("Should have added systemCpuLoad and processCpuLoad columns to the P4JVMSnapshot table");
		}
				
	}

	
	public void testVersion5Update() throws Exception { 
		// Start with an empty database...
		runUpdater();
		int count = getQueryCount("SELECT count(*) FROM " + SCHEMA  
				+ ".DATABASECHANGELOG WHERE author = 'databaseLabel' AND ID = '0005.0'");
		assertEquals("should have installed 5.0 label", 1, count);
		
		try {
			// Select new columns...
			count = getQueryCount("SELECT count(*) FROM " + SCHEMA  
				+ ".P4JDatabaseIdentity WHERE DatabaseID IS NOT NULL");
			assertEquals("Should have populated database identity", 1, count);
		} catch (Exception ex) {
			fail("Should have added database identity table");
		}
				
	}
	
	/**
	 * 4/23/2018
	 * 
	 * After 2.5 years of monitoring many systems we overflowed the INTEGER column
	 * primary key of the P4JIntervalData table and the referenced column in the 
	 * P4JIntervalThreshold table.  
	 * 
	 * Based on the SQL hoops (dropping indexes and constraints) that would be required for an 
	 * automated upgrade we decided not to upgrade existing tables to a BIGINT. 
	 * However, new databases will be created with a BIGINT column instead of a INTEGER Column.  
	 * For those with existing tables they will require a manual update of the columns.
	 * @throws Exception
	 */
	public void testIntervalIDIsCreatedAsABigInt() throws Exception { 
		// Start with an empty database...
		runUpdater();
		
		String dataType = UpdaterUtil.getColumnDataType(conn, SCHEMA, "P4JIntervalData", "IntervalID");
		assertEquals("P4JIntervalData.IntervalID column should be a BIGINT", "BIGINT", dataType.toUpperCase());

		dataType = UpdaterUtil.getColumnDataType(conn, SCHEMA, "P4JIntervalThreshold", "IntervalID");
		assertEquals("P4JIntervalThreshold.IntervalID column should be a BIGINT", "BIGINT", dataType.toUpperCase());
	}

	public void testVersion6Update() throws Exception { 
		// Start with an empty database...
		runUpdater();
		int count = getQueryCount("SELECT count(*) FROM " + SCHEMA  
				+ ".DATABASECHANGELOG WHERE author = 'databaseLabel' AND ID = '0006.0'");
		assertEquals("should have installed 6.0 label", 1, count);
		
		boolean groupExists = UpdaterUtil.doesTableExist(conn, SCHEMA, "P4JGroup");
		boolean joinExists = UpdaterUtil.doesTableExist(conn, SCHEMA, "P4JGroupSystemJoin");
		
		assertTrue("New P4JGroup table should exist", groupExists);
		assertTrue("New P4JGroupSystemJoin table should exist", joinExists);
	}
	
	public void testVersion7Update_Hystrix() throws Exception { 
		// Start with an empty database...
		runUpdater();
		int count = getQueryCount("SELECT count(*) FROM " + SCHEMA  
				+ ".DATABASECHANGELOG WHERE author = 'databaseLabel' AND ID = '0007.0'");
		assertEquals("should have installed 7.0 label", 1, count);
		
		boolean hystrixKeyExists = UpdaterUtil.doesTableExist(conn, SCHEMA, "P4JGroup");
		boolean hystrixCommandExists = UpdaterUtil.doesTableExist(conn, SCHEMA, "P4JGroupSystemJoin");
		boolean hystrixThreadPoolExists = UpdaterUtil.doesTableExist(conn, SCHEMA, "P4JGroupSystemJoin");
		
		assertTrue("New P4JHystrixKey table should exist", hystrixKeyExists);
		assertTrue("New P4JHystrixCommand table should exist", hystrixCommandExists);
		assertTrue("New P4JHystrixThreadPool table should exist", hystrixThreadPoolExists);
	}

	public void testVersion7Update_Performance() throws Exception { 
		runUpdater();
		assertTrue("should have installed 7.0 label", databaseLabelExistsInChangeLog("0007.0"));
		assertTrue("Changelog entry created",  
				changeLogEntryExistsWithID("P4J-AddIndexesForP4JReports"));

		
		assertTrue("Should have added control table", UpdaterUtil.doesTableExist(conn, SCHEMA, 
				"P4JAppenderControl"));
		assertTrue("Should have added control table", UpdaterUtil.doesColumnExist(conn, SCHEMA, 
				"P4JAppenderControl", "pauseAppenderMinutes"));
		assertTrue("Changelog entry created",  
				changeLogEntryExistsWithID("P4J-CreateAppenderControlTable"));
		
		
		boolean indexExists = UpdaterUtil.doesIndexExist(conn, SCHEMA, "P4JIntervalData", "P4JIntervalData_SystemEndTime");
		assertTrue("New P4JIntervalData_SystemEndTime index should exist", indexExists);
		
		// I fought with Liquibase either closing the entire in-memory
		// database or not releasing its database lock for far too long.
		// Don't mess with the rest of this test unless we figure out 
		// a way around this.
		boolean weGetPastProblemOfLiquibaseNotReleasingDatabaseLock = false;
		if (weGetPastProblemOfLiquibaseNotReleasingDatabaseLock) {
			// Demonstrate index will be skipped if it already exists..
			deleteChangeLogEntyWithID("P4J-AddIndexesForP4JReports");
	
			assertFalse("Make sure change log entry was deleted, this will cause Liquibase to run it again",  
				changeLogEntryExistsWithID("P4J-AddIndexesForP4JReports"));
			
			// Rerun update.  Change log should get re-applied, but skip because index already exists.
			runUpdater();
			
			assertTrue("P4JIntervalData_SystemEndTime entry should have been restored", 
					changeLogEntryExistsWithID("P4J-AddIndexesForP4JReports"));
		}
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
		assertEquals(0, params.getThirdPartyExtensions().length);
		assertTrue(params.isValid());
	}

	public void testParse3rdPartyIncludesMultiple() throws Exception {
		String args[] = {
				"userName=dave",
				"password=pw",
				"jdbcURL=my.jdbc.url",
				"driverClass=myDriver",
				"driverJarFile=c:/mydriver.jar",
				"schema=dbo",
				"thirdPartyExtensions=Follett,Other,YetAnother"
		};
		
		Parameters params = UpdateOrCreateDb.getParameters(args);
		String thirdPartyExtensions[] = params.getThirdPartyExtensions();
		assertEquals("Should have 3 thirdPartyExtensions", 3, thirdPartyExtensions.length);
		List<String> extensions = Arrays.asList(thirdPartyExtensions);
		
		assertTrue("Should have Follett Extension", extensions.contains("Follett"));
		assertTrue("Should have Other Extension", extensions.contains("Other"));
		assertTrue("Should have YetAnother Extension", extensions.contains("YetAnother"));
	}

	public void testParse3rdPartyIncludesSingle() throws Exception {
		String args[] = {
				"userName=dave",
				"password=pw",
				"jdbcURL=my.jdbc.url",
				"driverClass=myDriver",
				"driverJarFile=c:/mydriver.jar",
				"schema=dbo",
				"thirdPartyExtensions=Follett"
		};
		
		Parameters params = UpdateOrCreateDb.getParameters(args);
		String thirdPartyExtensions[] = params.getThirdPartyExtensions();
		assertEquals("Should have 3 thirdPartyExtensions", 1, thirdPartyExtensions.length);
		List<String> extensions = Arrays.asList(thirdPartyExtensions);
		
		assertTrue("Should have Follett Extension", extensions.contains("Follett"));
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
			runUpdater();
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
			runUpdater();
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
			runUpdater();
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
		assertTrue("Should have a changelog", UpdaterUtil.doesTableExist(conn, SCHEMA, "DATABASECHANGELOG"));
		System.out.println(dumpQuery(conn, "SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG"));

		assertTrue("Database change log should reflect databaseLabel 0003.0 applied", databaseLabelExistsInChangeLog("0003.0"));
	}
	
	public void testApplyThirdPartyChanges() throws Exception { 
		// Start with an empty database...
		runUpdater(new String[]{"thirdPartyExtensions=FSS"});
		
		assertTrue("Should have a FSSFetchThreadPoolSnapshot table", UpdaterUtil.doesTableExist(conn, SCHEMA, "FSSFetchThreadPoolSnapshot"));
		assertTrue("Should have a FSSFetchPolicySnapshot table", UpdaterUtil.doesTableExist(conn, SCHEMA, "FSSFetchPolicySnapshot"));
		
//		System.out.println(dumpQuery(conn, "SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG"));
		assertTrue("Database change log should reflect databaseLabel 0002.0 applied", databaseLabelExistsInChangeLog("0002.0"));
	}	
	
	public void testWriteSQLScript() throws Exception { 
		File sqlFile = new File(System.getProperty("java.io.tmpdir"), new Random().nextInt(10000) +  ".sql");

		try {
			System.out.println(sqlFile.getCanonicalPath());
			// Start with an empty database...
			runUpdater("thirdPartyExtensions=FSS"
					,"sqlOutputScript=" + sqlFile.getCanonicalPath());
			
			assertFalse("Should NOT have created database.  We just asked for a srcipt", 
					UpdaterUtil.doesTableExist(conn, SCHEMA, "FSSFetchPolicySnapshot"));

			assertTrue("Should have created SQL Script", sqlFile.exists());
		} finally {
			if (sqlFile.exists()) {
				sqlFile.delete();
			}
		}
	}		
	
	public void X_testLivePostgres() {
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
		DatabaseFactory.reset();
		
		String [] args = new String[] {
			"driverJarFile=/media/sf_shared/tools/common/JDBCDrivers/sqljdbc4.jar", 
			"driverClass=com.microsoft.sqlserver.jdbc.SQLServerDriver", 
			"jdbcURL=jdbc:sqlserver://10.0.2.2:1433;databaseName=TestUpgrade", 
			"userName=perfmonwriter", 
			"password=perfmon",
			"thirdPartyExtensions=FSS",
			"sqlOutputScript=/media/sf_shared/tools/common/create.sql" 
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
	
	public void X_testLiveOracle() {
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

	private void runUpdater(String... extraParameters) throws Exception {
		List<String> parameters = new ArrayList<String>(Arrays.
			asList(new String[]{"driverClass=org.apache.derby.jdbc.EmbeddedDriver",
			"jdbcURL=" + JDBC_URL,
			"driverJarFile=EMBEDDED",
			"schema=" + SCHEMA}));
		parameters.addAll(Arrays.asList(extraParameters));
		UpdateOrCreateDb.main(parameters.toArray(new String[]{}));
	}
	

	private Database buildLiquibaseDatabaseConnection() throws Exception {
		return buildLiquibaseDatabaseConnection(JDBC_URL);
	}
	
	
	private Database buildLiquibaseDatabaseConnection(String jdbcURL) throws Exception {
		Connection tmpConn = UpdaterUtil.createConnection(JDBC_DRIVER, null, jdbcURL, null, null);
		
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(tmpConn));
		db.setDefaultSchemaName(SCHEMA);
		
		return db;
	}
	
	private void applyChangeLog(String changeLog) throws Exception {
		Database db = buildLiquibaseDatabaseConnection();
		try {
			Liquibase updater = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), db);
			updater.update((String)null);
		} finally {
			db.close();
		}
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

	private boolean changeLogEntryExistsWithID(String changeLogID) throws Exception {
		boolean result = false;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG "
					+ "WHERE ID='" + changeLogID + "'");
			result = rs.next();
		} finally {
			UpdaterUtil.closeNoThrow(rs);
			UpdaterUtil.closeNoThrow(stmt);
		}
		
		return result;
	}
	
	private void deleteChangeLogEntyWithID(String changeLogID) throws Exception {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM " + SCHEMA + ".DATABASECHANGELOG "
					+ "WHERE ID='" + changeLogID + "'");
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
	}
	
	private void executeUpdate(String sql) throws Exception {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
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

	private void dumpChangeLog() throws Exception {
		System.out.println(dumpQuery(conn, "SELECT * FROM " + SCHEMA + ".DATABASECHANGELOG"));
	}
	
	private void dropLiquibaseTables() throws Exception {
		Statement stmt = null;
		
		try {
			stmt = conn.createStatement();
			stmt.execute("DROP TABLE " + SCHEMA + ".DATABASECHANGELOG");
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
	}
}
