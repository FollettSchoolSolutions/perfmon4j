package org.perfmon4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.TestCase;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.util.JDBCHelper;

public abstract class SQLTest extends TestCase {

	protected JDBCSQLAppender appender = null;

	public SQLTest() {
		super();
	}

	public SQLTest(String name) {
		super(name);
	}
	
    final String DERBY_DROP_SYSTEM = "DROP TABLE mydb.P4JSystem";
    
    final String DERBY_CREATE_SYSTEM = "CREATE TABLE mydb.P4JSystem (\r\n" +
    		"SystemID INT NOT NULL GENERATED ALWAYS AS IDENTITY,\r\n" +
    		"SystemName varchar(450) NOT NULL\r\n" +
    	")";
    
    final String DERBY_DROP_CATEGORY = "DROP TABLE mydb.P4JCategory";
    
    
    final String DERBY_CREATE_CATEGORY = "CREATE TABLE mydb.P4JCategory(\r\n" +
    	"CategoryID INT NOT NULL GENERATED ALWAYS AS IDENTITY,\r\n" +
    	"CategoryName varchar(450) NOT NULL\r\n" +
    	")";

    final String DERBY_DROP_INTERVAL_DATA = "DROP TABLE mydb.P4JIntervalData";
    
    final String DERBY_CREATE_INTERVAL_DATA = "CREATE TABLE mydb.P4JIntervalData (\r\n" +
		"IntervalID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY," +
		"SystemID INT NOT NULL DEFAULT 1,\r\n" +
		"CategoryID INT NOT NULL,\r\n" +
		"StartTime TIMESTAMP NOT NULL,\r\n" +
		"EndTime TIMESTAMP NOT NULL,\r\n" +
		"TotalHits BIGINT NOT NULL,\r\n" +
		"TotalCompletions BIGINT NOT NULL,\r\n" +
		"MaxActiveThreads BIGINT NOT NULL,\r\n" +
		"MaxActiveThreadsSet TIMESTAMP,\r\n" +
		"MaxDuration int NOT NULL,\r\n" +
		"MaxDurationSet TIMESTAMP,\r\n" +
		"MinDuration int NOT NULL,\r\n" +
		"MinDurationSet TIMESTAMP,\r\n" +
		"AverageDuration DECIMAL(18, 2) NOT NULL,\r\n" +
		"MedianDuration  DECIMAL(18, 2),\r\n " +
		"StandardDeviation DECIMAL(18, 2) NOT NULL,\r\n" +
		"NormalizedThroughputPerMinute DECIMAL(18, 2) NOT NULL," +
		"DurationSum BIGINT NOT NULL," +
		"DurationSumOfSquares BIGINT NOT NULL,\r\n" +

		// All SQL Durations must allow NULL, not all monitors will contain SQL information...
		// These columns are also optional for the table...
		// All columns must exist for data to be written.
		"SQLMaxDuration int,\r\n" +
		"SQLMaxDurationSet TIMESTAMP,\r\n" +
		"SQLMinDuration int,\r\n" +
		"SQLMinDurationSet TIMESTAMP,\r\n" +
		"SQLAverageDuration DECIMAL(18, 2),\r\n" +
		"SQLStandardDeviation DECIMAL(18, 2),\r\n" +
		"SQLDurationSum BIGINT," +
		"SQLDurationSumOfSquares BIGINT\r\n" +
		")";    
    
    final String DERBY_DROP_THRESHOLD = "DROP TABLE mydb.P4JIntervalThreshold";
    
    final String DERBY_CREATE_THRESHOLD = "CREATE TABLE mydb.P4JIntervalThreshold (\r\n" +
		"IntervalID BIGINT NOT NULL,\r\n" +
		"ThresholdMillis INT NOT NULL,\r\n" +
		"CompletionsOver BIGINT NOT NULL,\r\n" +
		"PercentOver DECIMAL(5, 2) NOT NULL\r\n" +
		")";
    

    final String DERBY_CREATE_STUB_CHANGELOG = "CREATE TABLE mydb.DATABASECHANGELOG (id varchar(150) not null, author varchar(150) not null)";
    final String DERBY_DROP_STUB_CHANGELOG = "DROP TABLE mydb.DATABASECHANGELOG ";
    
    
	protected void setUp() throws Exception {
		super.setUp();

		appender = new JDBCSQLAppender(AppenderID.getAppenderID(JDBCSQLAppender.class.getName()));
		appender.setDbSchema("mydb");
		appender.setDriverClass("org.apache.derby.jdbc.EmbeddedDriver");
		appender.setJdbcURL("jdbc:derby:memory:derbyDB;create=true");
		
		createTables();
	}

    private void createTables() throws Exception {
		Connection conn = appender.getConnection();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			
			stmt.execute(DERBY_CREATE_SYSTEM);
			stmt.execute("INSERT INTO mydb.P4JSystem (SystemName) VALUES('default')");
			
			stmt.execute(DERBY_CREATE_CATEGORY);
			stmt.execute(DERBY_CREATE_INTERVAL_DATA);
			stmt.execute(DERBY_CREATE_THRESHOLD);
			stmt.execute(DERBY_CREATE_STUB_CHANGELOG);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
			stmt = null;
		}
    }
    

    private void dropTables() throws Exception {
		Connection conn = appender.getConnection();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_THRESHOLD);
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_INTERVAL_DATA);
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_CATEGORY);
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_SYSTEM);
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_STUB_CHANGELOG);
				} finally {
			JDBCHelper.closeNoThrow(stmt);
			stmt = null;
		}
    }
    	
	
	protected void tearDown() throws Exception {
		dropTables();
		
		appender.deInit();
		appender = null;
		
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
    		stmt.execute("INSERT INTO mydb.DATABASECHANGELOG (id, author) VALUES('" 
    				+ label + "' , 'databaseLabel')");
    	} finally {
			JDBCHelper.closeNoThrow(stmt);
    	}
    	conn.commit();
    }
}