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
import java.util.GregorianCalendar;
import java.util.List;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.ThresholdCalculator;
import org.perfmon4j.util.vo.ResponseInfo;

/* todo: this test needs a LOT of work!!! */
public class JDBCSQLAppenderTest extends SQLTest {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    final long MIDNIGHT = (new GregorianCalendar(2007, 0, 0)).getTimeInMillis();
    final long SECOND = 1000;
    final long MINUTE = SECOND * 60;
    final long HOUR = MINUTE * 60;
    
    /*----------------------------------------------------------------------------*/
    public JDBCSQLAppenderTest(String name) {
        super(name);
    }


    boolean originalSQLTimeEnabled;
    public void setUp() throws Exception {
    	super.setUp();
    	originalSQLTimeEnabled = SQLTime.isEnabled();
    }
    
    
    public void tearDown() throws Exception {
    	SQLTime.setEnabled(originalSQLTimeEnabled);
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
    	d.stop(100, 10000, now, 0, 0);
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
    	d.stop(100, 10000, now, 0, 0);
		d.setTimeStop(now + 1000);
    	appender.outputData(d);
		
		assertEquals("Should have record with durationSumOfSquares", 1, JDBCHelper.getQueryCount(conn, count));
    }    

    public void testWriteSQLMontiorColumnsWithSQLTimeDisabled() throws Exception {
    	SQLTime.setEnabled(false);
    	long now = System.currentTimeMillis();
    	
    	Connection conn = appender.getConnection();
    	String count = "SELECT COUNT(*) " +
    		"FROM mydb.P4JIntervalData t " +
    		"WHERE t.SQLMaxDuration IS NULL " +
    		"AND t.SQLMaxDurationSet IS NULL " +
    		"AND t.SQLMinDuration IS NULL " +
    		"AND t.SQLMinDurationSet IS NULL " +
    		"AND t.SQLAverageDuration IS NULL  " +
    		"AND t.SQLStandardDeviation IS NULL  " + 
    		"AND t.SQLDurationSum IS NULL  " +
    		"AND t.SQLDurationSumOfSquares IS NULL\r\n";
    	
    	IntervalData d = new IntervalData(PerfMon.getMonitor("a.b.c"), now);
    	d.start(0, now);
    	d.stop(555, 55555, now, 101, 101*101, true);
    	
    	d.start(0, now);
    	d.stop(555, 55555, now, 202, 202*202, true);
		
    	d.start(0, now);
    	d.stop(555, 55555, now, 303, 303*303, true);

    	d.setTimeStop(now + 1000);
    	appender.outputData(d);
    	
System.out.println(JDBCHelper.dumpQuery(conn, "SELECT * FROM mydb.P4JIntervalData t"));
		assertEquals("record count", 1, JDBCHelper.getQueryCount(conn, count));
    }    
    
    public void testWriteSQLMontiorColumnsWithSQLTimeEnabled() throws Exception {
    	SQLTime.setEnabled(true);
    	long now = System.currentTimeMillis();
    	
    	Connection conn = appender.getConnection();
    	String count = "SELECT COUNT(*) " +
    		"FROM mydb.P4JIntervalData t " +
    		"WHERE t.SQLMaxDuration = 303 " +
    		"AND t.SQLMaxDurationSet IS NOT NULL " +
    		"AND t.SQLMinDuration = 101 " +
    		"AND t.SQLMinDurationSet IS NOT NULL " +
    		"AND t.SQLAverageDuration = 202.0 " +
    		"AND t.SQLStandardDeviation = 101.0 " + 
    		"AND t.SQLDurationSum IS NOT NULL " +
    		"AND t.SQLDurationSumOfSquares IS NOT NULL\r\n";
    	
    	IntervalData d = new IntervalData(PerfMon.getMonitor("a.b.c"), now);
    	d.start(0, now);
    	d.stop(555, 55555, now, 101, 101*101, true);
    	
    	d.start(0, now);
    	d.stop(555, 55555, now, 202, 202*202, true);
		
    	d.start(0, now);
    	d.stop(555, 55555, now, 303, 303*303, true);

    	d.setTimeStop(now + 1000);
    	appender.outputData(d);
    	
System.out.println(JDBCHelper.dumpQuery(conn, "SELECT * FROM mydb.P4JIntervalData t"));
		assertEquals("record count", 1, JDBCHelper.getQueryCount(conn, count));
    }    

    public void testJDBCHelperQueryResponseInfo() throws Exception {
    	long now = System.currentTimeMillis();
    	
    	Connection conn = appender.getConnection();
    	
    	IntervalData d = new IntervalData(PerfMon.getMonitor("dave"), now);
    	d.start(0, now);
    	d.stop(100, 10000, now, 0, 0);
		d.setTimeStop(now + 1000);
    	appender.outputData(d);
    	
    	long monitorID = JDBCHelper.getQueryCount(conn, "SELECT CategoryID FROM mydb.P4JCategory WHERE categoryName='dave'");
    	List<ResponseInfo> list = JDBCHelper.queryResponseInfo(conn, "mydb", monitorID);
    	assertEquals(1, list.size());
    	
    	ResponseInfo element = list.get(0);
    	assertEquals("dave", element.getMonitorName());
    }    

    
    public void testGetOrCreateSystem() throws Exception {
    	Connection conn = appender.getConnection();

    	String systemName = MiscHelper.getDefaultSystemName();
    	final String SQL_COUNT = "SELECT COUNT(*) FROM mydb.P4JSystem WHERE SystemName = '" + systemName + "'";
    	
    	long count = JDBCHelper.getQueryCount(conn, SQL_COUNT);
    	assertEquals("Should not have system row", 0, count);
    	
    	long id = appender.getSystemID();
    	assertEquals("Should be the second systemID", 2, id);
    	
    	count = JDBCHelper.getQueryCount(conn, SQL_COUNT);
    	assertEquals("Should have added system row", 1, count);
    }    
    
    public void testGetOrCreateSystemWithoutCWDHash() throws Exception {
    	Connection conn = appender.getConnection();
    	
    	appender.setExcludeCWDHashFromSystemName(true);
    	String systemNameWithoutCWDHash = MiscHelper.getDefaultSystemName(false);
    	
    	final String SQL_COUNT = "SELECT COUNT(*) FROM mydb.P4JSystem WHERE SystemName = '" + systemNameWithoutCWDHash + "'";
    	
    	long count = JDBCHelper.getQueryCount(conn, SQL_COUNT);
    	assertEquals("Should not have system row", 0, count);
    	
    	long id = appender.getSystemID();
    	assertEquals("Should be the second systemID", 2, id);
    	
    	count = JDBCHelper.getQueryCount(conn, SQL_COUNT);
    	assertEquals("Should have added system row", 1, count);
    }      
    
    
    public void testOverrideSystemName() throws Exception {
    	Connection conn = appender.getConnection();
    	String systemName = "My System -- testOverrideSystemName";
    	
    	appender.setSystemNameBody(systemName);
    	
    	long id = appender.getSystemID();
    	assertEquals("Should be the second systemID", 2, id);
    	
    	final String SQL_COUNT = "SELECT COUNT(*) FROM mydb.P4JSystem WHERE SystemName = '" + systemName + "'";
    	long count = JDBCHelper.getQueryCount(conn, SQL_COUNT);
    	assertEquals("Should have added system row", 1, count);
    }    

    public void testPrefixAppendedToSystemName() throws Exception {
    	Connection conn = appender.getConnection();
    	
    	appender.setSystemNamePrefix("dave");
    	
    	long id = appender.getSystemID();
    	assertEquals("Should be the second systemID", 2, id);
    	
    	final String SQL_COUNT = "SELECT COUNT(*) FROM mydb.P4JSystem WHERE SystemName = 'dave" + appender.getSystemNameBody() + "'";
    	long count = JDBCHelper.getQueryCount(conn, SQL_COUNT);
    	assertEquals("Should have added system row", 1, count);
    }    
    
    public void testSuffexAddedToSystemName() throws Exception {
    	Connection conn = appender.getConnection();
    	
    	appender.setSystemNameSuffix("dave");
    	
    	long id = appender.getSystemID();
    	assertEquals("Should be the second systemID", 2, id);
    	
    	final String SQL_COUNT = "SELECT COUNT(*) FROM mydb.P4JSystem WHERE SystemName = '" + appender.getSystemNameBody() + "dave'";
    	long count = JDBCHelper.getQueryCount(conn, SQL_COUNT);
    	assertEquals("Should have added system row", 1, count);
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
//        newSuite.addTest(new JDBCSQLAppenderTest("testWriteToLegacySchema"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(JDBCSQLAppenderTest.class);
        }

        return(newSuite);
    }
}
