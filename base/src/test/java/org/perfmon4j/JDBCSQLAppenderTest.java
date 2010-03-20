/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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
package org.perfmon4j;

import java.sql.Connection;
import java.sql.Statement;
import java.util.GregorianCalendar;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.ThresholdCalculator;

/* todo: this test needs a LOT of work!!! */
public class JDBCSQLAppenderTest extends SQLTest {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    final String DERBY_DROP_CATEGORY = "DROP TABLE mydb.P4JCategory";
    
    
    final String DERBY_CREATE_CATEGORY = "CREATE TABLE mydb.P4JCategory(\r\n" +
    	"CategoryID INT NOT NULL GENERATED ALWAYS AS IDENTITY,\r\n" +
    	"CategoryName varchar(450) NOT NULL\r\n" +
    	")";

    final String DERBY_DROP_INTERVAL_DATA = "DROP TABLE mydb.P4JIntervalData";
    
    final String DERBY_CREATE_INTERVAL_DATA = "CREATE TABLE mydb.P4JIntervalData (\r\n" +
		"CategoryID INT NOT NULL,\r\n" +
		"IntervalID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY," +
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
		"DurationSumOfSquares BIGINT NOT NULL\r\n" +
		")";

    final String DERBY_DROP_THRESHOLD = "DROP TABLE mydb.P4JIntervalThreshold";
    
    final String DERBY_CREATE_THRESHOLD = "CREATE TABLE mydb.P4JIntervalThreshold (\r\n" +
		"IntervalID BIGINT NOT NULL,\r\n" +
		"ThresholdMillis INT NOT NULL,\r\n" +
		"CompletionsOver BIGINT NOT NULL,\r\n" +
		"PercentOver DECIMAL(5, 2) NOT NULL\r\n" +
		")";
    
    
    final long MIDNIGHT = (new GregorianCalendar(2007, 0, 0)).getTimeInMillis();
    final long SECOND = 1000;
    final long MINUTE = SECOND * 60;
    final long HOUR = MINUTE * 60;
    
    /*----------------------------------------------------------------------------*/
    public JDBCSQLAppenderTest(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
    	super.setUp();
    
		Connection conn = appender.getConnection();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_THRESHOLD);
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_INTERVAL_DATA);
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_CATEGORY);
			
			stmt.execute(DERBY_CREATE_CATEGORY);
			stmt.execute(DERBY_CREATE_INTERVAL_DATA);
			stmt.execute(DERBY_CREATE_THRESHOLD);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
			stmt = null;
		}
    }
    
    
    public void tearDown() throws Exception {
		Connection conn = appender.getConnection();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_THRESHOLD);
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_INTERVAL_DATA);
			JDBCHelper.executeNoThrow(stmt, DERBY_DROP_CATEGORY);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
			stmt = null;
		}
		
		super.tearDown();
    }
    
    private IntervalData createIntervalData(String category, long startTime, long duration) {
    	return createIntervalData(category, startTime, duration, null, null);
    }
    
    private IntervalData createIntervalData(String category, long startTime, long duration, String medianConfig, String thresholdConfig) {
		MedianCalculator mCalc = null;
		ThresholdCalculator tCalc = null;
    	
		if (medianConfig != null) {
			mCalc = new MedianCalculator(medianConfig);
		}
		
		if (thresholdConfig != null) {
			tCalc = new ThresholdCalculator(thresholdConfig);
		}
    	
    	IntervalData d = new IntervalData(PerfMon.getMonitor(category), startTime, mCalc, tCalc);
		d.setTimeStop(startTime + duration);
		
		return d;
    }
    
/*----------------------------------------------------------------------------*/
    public void testCreateAndReuseCategory() throws Exception {
    	long now = System.currentTimeMillis();
    	
    	Connection conn = appender.getConnection();
		final String countCategory = "SELECT COUNT(*) FROM mydb.P4JCategory WHERE categoryName = 'a.b.c'";
		final String countInterval = "SELECT COUNT(*)\r\n" +
				" FROM mydb.P4JCategory c\r\n" +
				" JOIN mydb.P4jIntervalData id ON c.categoryID = id.categoryID\r\n" +
				" WHERE categoryName = 'a.b.c'\r\n";

		assertEquals("Should not have category", 0, JDBCHelper.getQueryCount(conn, countCategory));

		appender.outputData(createIntervalData("a.b.c", now, 1000));
		
		assertEquals("Should NOW have category", 1, JDBCHelper.getQueryCount(conn, countCategory));
		assertEquals("Should have 1 interval row",1, JDBCHelper.getQueryCount(conn, countInterval));

		appender.outputData(createIntervalData("a.b.c", now+1000, 1000));
		
		assertEquals("Should STILL have only 1 category", 1, JDBCHelper.getQueryCount(conn, countCategory));
		assertEquals("Should have 2 interval rows", 2, JDBCHelper.getQueryCount(conn, countInterval));
    }

    public void testAddThreshold() throws Exception {
    	long now = System.currentTimeMillis();
    	
    	Connection conn = appender.getConnection();
    	String countThreshold = "SELECT COUNT(*) " +
    			"FROM mydb.P4JIntervalThreshold t " +
    			"JOIN mydb.P4JIntervalData d ON d.intervalID = t.intervalID";
    	
		appender.outputData(createIntervalData("a.b.c", now, 1000, null, "100, 1000"));

		assertEquals("Should have added 2 thresholdRows", 2, JDBCHelper.getQueryCount(conn, countThreshold));
    }

    public void testDurationSum() throws Exception {
    	long now = System.currentTimeMillis();
    	
    	Connection conn = appender.getConnection();
    	String count = "SELECT COUNT(*) " +
    			"FROM mydb.P4JIntervalData t " +
    			"WHERE t.DurationSum = 100";
    	
    	IntervalData d = new IntervalData(PerfMon.getMonitor("a.b.c"), now);
    	d.start(0, now);
    	d.stop(100, 10000, now);
		d.setTimeStop(now + 1000);
    	appender.outputData(d);
		
		assertEquals("Should have record with durationSum", 1, JDBCHelper.getQueryCount(conn, count));
    }    

    public void testDurationSumOfSquares() throws Exception {
    	long now = System.currentTimeMillis();
    	
    	Connection conn = appender.getConnection();
    	String count = "SELECT COUNT(*) " +
    			"FROM mydb.P4JIntervalData t " +
    			"WHERE t.DurationSumOfSquares = 10000";
    	
    	IntervalData d = new IntervalData(PerfMon.getMonitor("a.b.c"), now);
    	d.start(0, now);
    	d.stop(100, 10000, now);
		d.setTimeStop(now + 1000);
    	appender.outputData(d);
		
		assertEquals("Should have record with durationSumOfSquares", 1, JDBCHelper.getQueryCount(conn, count));
    }    
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {JDBCSQLAppenderTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(JDBCSQLAppenderTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new SnapShotManagerTest("testDefineMonitorWithAttributes"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(JDBCSQLAppenderTest.class);
        }

        return(newSuite);
    }
}
