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

public class ThreadTraceDataTest extends SQLTest {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    final long MIDNIGHT = (new GregorianCalendar(2007, 0, 1)).getTimeInMillis();
    final long SECOND = 1000;
    final long MINUTE = SECOND * 60;
    final long HOUR = MINUTE * 60;
    
/*----------------------------------------------------------------------------*/
    public ThreadTraceDataTest(String name) {
        super(name);
    }
    
    final String DERBY_CREATE_1 = "CREATE TABLE p4j.P4JCategory(\r\n" +
		"CategoryID INT NOT NULL GENERATED ALWAYS AS IDENTITY,\r\n" +
		"CategoryName VARCHAR(450) NOT NULL\r\n" +
	")";

    final String DERBY_CREATE_2 = "CREATE TABLE p4j.P4JThreadTrace(\r\n" +
    	"	RowID INT NOT NULL GENERATED ALWAYS AS IDENTITY,\r\n" +
    	"	ParentRowID INT,\r\n" +
    	"	CategoryID INT NOT NULL,\r\n" +
    	"	StartTime TIMESTAMP NOT NULL,\r\n" +
    	"	EndTime TIMESTAMP NOT NULL,\r\n" +
    	"	Duration INT NOT NULL)\r\n";

	final String DERBY_DROP_1 = "DROP TABLE p4j.P4JCategory";
	final String DERBY_DROP_2 = "DROP TABLE p4j.P4JThreadTrace";

	private Connection conn;

	protected void setUp() throws Exception {
		super.setUp();
		
		conn = appender.getConnection();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(DERBY_CREATE_1);
			stmt.execute(DERBY_CREATE_2);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
	}

	protected void tearDown() throws Exception {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(DERBY_DROP_2);
			stmt.execute(DERBY_DROP_1);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
		super.tearDown();
	}
    
    
/*----------------------------------------------------------------------------*/
    public void testSimpleToAppenderString() throws Exception {
        ThreadTraceData data = new ThreadTraceData("com.perfmon4j.Test.test", MIDNIGHT + HOUR + MINUTE + (SECOND) + 1);
        data.setEndTime(MIDNIGHT + HOUR + MINUTE + MINUTE);
    
        assertEquals("\r\n********************************************************************************\r\n" +
            "+-01:01:01:001 (58999) com.perfmon4j.Test.test\r\n" +
            "+-01:02:00:000 com.perfmon4j.Test.test\r\n" +
            "********************************************************************************", data.toAppenderString());
    }

/*----------------------------------------------------------------------------*/
    public void testNestedToAppenderString() throws Exception {
        ThreadTraceData data = new ThreadTraceData("com.perfmon4j.Test.test", MIDNIGHT + HOUR + MINUTE);
        data.setEndTime(MIDNIGHT + HOUR + MINUTE + MINUTE);
    
        ThreadTraceData child = new ThreadTraceData("com.perfmon4j.MiscHelper.formatString", data, data.getStartTime() + SECOND);
        child.setEndTime(data.getEndTime() - SECOND);
        
        assertEquals("\r\n********************************************************************************\r\n" +
            "+-01:01:00:000 (60000) com.perfmon4j.Test.test\r\n" +
            "|\t+-01:01:01:000 (58000) com.perfmon4j.MiscHelper.formatString\r\n" +
            "|\t+-01:01:59:000 com.perfmon4j.MiscHelper.formatString\r\n" +
            "+-01:02:00:000 com.perfmon4j.Test.test\r\n" +
            "********************************************************************************", data.toAppenderString());
    }

    public void testNestedToSQL() throws Exception {
        ThreadTraceData data = new ThreadTraceData("com.perfmon4j.Test.test", MIDNIGHT + HOUR + MINUTE);
        data.setEndTime(MIDNIGHT + HOUR + MINUTE + MINUTE);
    
        ThreadTraceData child = new ThreadTraceData("com.perfmon4j.MiscHelper.formatString", data, data.getStartTime() + SECOND);
        child.setEndTime(data.getEndTime() - SECOND);
        
        data.writeToSQL(conn, "p4j");
        
System.out.println(JDBCHelper.dumpQuery(conn, "SELECT c.categoryName, tt.*\r\n" + 
				"	FROM p4j.P4JThreadTrace tt\r\n" +
				"	JOIN p4j.P4JCategory c ON c.CategoryID = tt.CategoryID\r\n"));

        
		long parentCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*)\r\n" + 
			"	FROM p4j.P4JThreadTrace tt\r\n" +
			"	JOIN p4j.P4JCategory c ON c.CategoryID = tt.CategoryID\r\n" +
			"	WHERE tt.RowID = 1\r\n" +
			"	AND tt.ParentRowID IS NULL\r\n" +
			"	AND c.CategoryName = 'com.perfmon4j.Test.test'\r\n" +
			"	AND tt.StartTime = '2007-01-01-01.01.00.0'\r\n" +
			"	AND tt.EndTime = '2007-01-01-01.02.00.0'\r\n" +
			"	AND tt.Duration = 60000");
		
		long childCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*)\r\n" + 
				"	FROM p4j.P4JThreadTrace tt\r\n" +
				"	JOIN p4j.P4JCategory c ON c.CategoryID = tt.CategoryID\r\n" +
				"	WHERE tt.RowID = 2\r\n" +
				"	AND tt.ParentRowID = 1\r\n" +
				"	AND c.CategoryName = 'com.perfmon4j.MiscHelper.formatString'\r\n" +
				"	AND tt.StartTime = '2007-01-01-01.01.01.0'\r\n" +
				"	AND tt.EndTime = '2007-01-01-01.01.59.0'\r\n" +
				"	AND tt.Duration = 58000");
		
		assertEquals("Should have inserted main row", 1, parentCount);
		assertEquals("Should have inserted child row", 1, childCount);
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {ThreadTraceDataTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(ThreadTraceDataTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
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
            newSuite = new TestSuite(ThreadTraceDataTest.class);
        }

        return(newSuite);
    }
}
