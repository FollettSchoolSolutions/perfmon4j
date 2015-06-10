package org.perfmon4j.dbupgrader;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class UpdaterUtilTest extends TestCase {
	public static final String JDBC_URL = "jdbc:derby:memory:mydb"; 
	public static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";	
	private Connection conn;

	public UpdaterUtilTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		conn = UpdaterUtil.createConnection(JDBC_DRIVER, null, JDBC_URL + ";create=true", null, null);
	}

	protected void tearDown() throws Exception {
		UpdaterUtil.closeNoThrow(conn);
		
		try {
			UpdaterUtil.createConnection(JDBC_DRIVER, null, JDBC_URL + ";drop=true", null, null);
		} catch (SQLException sn) {
		}
		super.tearDown();
	}

	public void testTableExist() throws Exception {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute("CREATE TABLE MyTable(ID int)");
			
			assertTrue("Default schema and correct table name should match", UpdaterUtil.doesTableExist(conn, null, "MyTable"));
			assertTrue("Explicit schema name and correct table name should match", UpdaterUtil.doesTableExist(conn, "app", "MyTable"));
			assertFalse("Incorrect schema and correct table name should NOT match", UpdaterUtil.doesTableExist(conn, "db", "MyTable"));
			assertFalse("Default schema and incorrect table name should NOT match", UpdaterUtil.doesTableExist(conn, null, "MyOtherTable"));
			assertFalse("Explicit schema name and incorrect table name should NOT match", UpdaterUtil.doesTableExist(conn, "app", "MyOtherTable"));
			
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
	}
	
	public void testColumnExist() throws Exception {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute("CREATE TABLE MyTable(ID int)");
			
			assertTrue("Correct column", UpdaterUtil.doesColumnExist(conn, null, "MyTable", "ID"));
			assertFalse("incorrect column", UpdaterUtil.doesColumnExist(conn, null, "MyTable", "NotID"));
			
		} finally {
			UpdaterUtil.closeNoThrow(stmt);
		}
	}
	
	
	public void testGenerateUniqueIdentity() throws Exception {
		String identity = UpdaterUtil.generateUniqueIdentity();
		assertNotNull("Should have created and Identity", identity);
		assertTrue("Should match pattern", identity.matches("[A-Z]{4}-[A-Z]{4}"));
		
		Set<String> identities = new HashSet<String>();
		
		final int numToCreate = 20000;
		for (int i = 0; i < numToCreate; i++) {
			identity = UpdaterUtil.generateUniqueIdentity();
			identities.add(identity);
		}
 		assertEquals("Should never create a duplicate identity", numToCreate, identities.size());
	}

	
	
	
}
