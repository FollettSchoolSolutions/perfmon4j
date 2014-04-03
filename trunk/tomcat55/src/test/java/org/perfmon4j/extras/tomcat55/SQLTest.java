package org.perfmon4j.extras.tomcat55;

import org.perfmon4j.JDBCSQLAppender;
import org.perfmon4j.Appender.AppenderID;

import junit.framework.TestCase;


public abstract class SQLTest extends TestCase {
	protected JDBCSQLAppender appender = null;
	
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
