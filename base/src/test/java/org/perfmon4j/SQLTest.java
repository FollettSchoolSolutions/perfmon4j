package org.perfmon4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.TestCase;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.JDBCHelper.DriverCache;
import org.slf4j.LoggerFactory;

public abstract class SQLTest extends TestCase {

	protected JDBCSQLAppender appender = null;

	public SQLTest() {
		super();
	}

	public SQLTest(String name) {
		super(name);
	}
    
    public static final String SCHEMA_NAME = "mydb";
    public static final String DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";
    public static final String JDBC_URL = "jdbc:derby:memory:derbyDB";
    public static final String DATABASE_ID = "ABCD-EFGH";
    
    
	protected void setUp() throws Exception {
		super.setUp();
	
		appender = new JDBCSQLAppender(AppenderID.getAppenderID(JDBCSQLAppender.class.getName()));
		appender.setDbSchema("mydb");
		appender.setDriverClass("org.apache.derby.jdbc.EmbeddedDriver");
		appender.setJdbcURL(JDBC_URL + ";create=true");
		
		createTables();
	}

    private void createTables() throws Exception {
		Connection conn = appender.getConnection();

		/** Quiet down Liquibase **/
    	ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("liquibase");
    	logger.setLevel(ch.qos.logback.classic.Level.WARN);
		
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
		db.setDefaultSchemaName(SCHEMA_NAME);

		Liquibase updater = new Liquibase("org/perfmon4j/update-change-master-log.xml", new ClassLoaderResourceAccessor(), db);
		updater.setChangeLogParameter("DatabaseIdentifier", "ABCDEFG");
		updater.update((String)null);

		// The liquibase updater turns off autocommit...  Our appenders require it
		// so turn it back on.
		conn.setAutoCommit(true);
    }
    
	protected void tearDown() throws Exception {
		appender.deInit();
		appender = null;
		
		try {
			JDBCHelper.createJDBCConnection(DriverCache.DEFAULT, DRIVER_CLASS, null, JDBC_URL + ";drop=true", null, null);
		} catch (SQLException sn) {
			// The drop in-memory database throws a SQLException even when successful
		}
		
		super.tearDown();
	}
	
    public static double r(double x, int precision) {
    	double ret;
    	if (precision == 0) {
    		ret = (long)x;
    	} else {
    		ret = (long)(x * Math.pow(10, precision));
    		ret = ret/(Math.pow(10, precision));
    	}
    	return ret;
    }	
    
    public void addVersionLabel(Connection conn, String label, boolean removeOtherLabels) throws SQLException {
    	Statement stmt = null;
    	if (removeOtherLabels) {
    		try {
    			stmt = conn.createStatement();
    			stmt.execute("DELETE FROM mydb.DATABASECHANGELOG");
    		} finally {
    			JDBCHelper.closeNoThrow(stmt);
    		}
    	}
    	try {
    		stmt = conn.createStatement();
    		stmt.execute("INSERT INTO mydb.DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype) VALUES('" 
    				+ label + "' , 'databaseLabel', 'test.xml', '2018-04-24-00.00.00', 1, 'unittest')");
    	} finally {
			JDBCHelper.closeNoThrow(stmt);
    	}
    	conn.commit();
    }
    
}