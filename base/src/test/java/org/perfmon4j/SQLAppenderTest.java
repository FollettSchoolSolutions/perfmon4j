package org.perfmon4j;

import java.sql.Connection;
import java.sql.Statement;

import org.perfmon4j.util.JDBCHelper;


public class SQLAppenderTest extends SQLTest {

	public SQLAppenderTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	private final long disableCache = 0;
	
	public void testGetDatabaseVersion_NoChangeLog() throws Exception {
		Connection conn = appender.getConnection();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute("DROP TABLE mydb.DATABASECHANGELOG");
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
		
		double version = appender.getDatabaseVersion_TestOnly(disableCache);
		assertEquals("If we have no databasechangelog we should default to version 0.0", 0.0, version);
	}

	public void testGetDatabaseVersion_SingleChangeLogLabel() throws Exception {
		Connection conn = appender.getConnection();
		
		addVersionLabel(conn, "0004.0", true);
		
		double version = appender.getDatabaseVersion_TestOnly(disableCache);
		assertEquals("With single version label we should use value", 4.0, version);
	}

	public void testGetDatabaseVersion_MultipleChangeLogLabels() throws Exception {
		Connection conn = appender.getConnection();
		
		addVersionLabel(conn, "0001.0", true);
		addVersionLabel(conn, "0004.0", false);
		addVersionLabel(conn, "0002.0", false);
		addVersionLabel(conn, "0003.0", false);
		
		double version = appender.getDatabaseVersion_TestOnly(disableCache);
		assertEquals("With single version label we should use value", 4.0, version);
	}

	
	public void testGetDatabaseVersion_CacheDuration() throws Exception {
		Connection conn = appender.getConnection();
	
		// fill the cache
		appender.getDatabaseVersion_TestOnly(disableCache);

		addVersionLabel(conn, "0004.0", true);
		
		double version = appender.getDatabaseVersion_TestOnly(100);
		assertEquals("Should still be reading", 0.0, version);
		
		Thread.sleep(100);
		
		version = appender.getDatabaseVersion_TestOnly(100);
		assertEquals("Cache should have expored", 4.0, version);
		
	}
	
	
}
