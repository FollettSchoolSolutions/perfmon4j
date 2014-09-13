package org.perfmon4j.dbupgrader;

import java.sql.Connection;

import junit.framework.TestCase;

public class UpdateOrCreateDbTest extends TestCase {
	public static final String JDBC_URL = "jdbc:derby:memory:derbyDB;create=true";
	public static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private Connection conn = null;
	
	
	public UpdateOrCreateDbTest(String name) {
		super(name);
	}
	
	
	protected void setUp() throws Exception {
		conn = UpgraderUtil.createConnection(JDBC_DRIVER, null, JDBC_URL, null, null);
		
		super.setUp();
	}

	protected void tearDown() throws Exception {
		UpgraderUtil.closeNoThrow(conn);
		
		super.tearDown();
	}
	
	public void testPopulateDatabase() { 
		// Start with an empty database...
		UpdateOrCreateDb.main(new String[]{});
		
		assertTrue("Should have a P4JSystem table", UpgraderUtil.doesTableExists(conn, "P4JSystem"));
	}
	
	public void testInstallBaseChangeLogs() {
		// Start with a database that contain the base tables,
		// but does not contain the liquibase change logs.. 
		// Should install change.logs and any additional upgrades.
		UpdateOrCreateDb.main(new String[]{});
		
		// End up with a full database.
	}
	

}
