package org.perfmon4j;

import java.sql.Connection;
import java.sql.Statement;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.util.JDBCHelper;

import junit.framework.TestCase;

public abstract class SQLTest extends TestCase {

	protected JDBCSQLAppender appender = null;

	public SQLTest() {
		super();
	}

	public SQLTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		appender = new JDBCSQLAppender(AppenderID.getAppenderID(JDBCSQLAppender.class.getName()));
		appender.setDbSchema("mydb");
		appender.setDriverClass("org.apache.derby.jdbc.EmbeddedDriver");
		appender.setJdbcURL("jdbc:derby:memory:derbyDB;create=true");
	}

	protected void tearDown() throws Exception {
		appender.deInit();
		appender = null;
		
		super.tearDown();
	}

}