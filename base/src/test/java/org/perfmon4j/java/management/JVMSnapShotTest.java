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

package org.perfmon4j.java.management;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.perfmon4j.SQLTest;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;
import org.perfmon4j.java.management.JVMSnapShot.JVMData;
import org.perfmon4j.util.JDBCHelper;

public class JVMSnapShotTest extends SQLTest {
    final String DERBY_CREATE_1 = "CREATE TABLE p4j.P4JVMSnapShot(\r\n" +
	"	SystemID INT NOT NULL,\r\n" +
	"	StartTime TIMESTAMP NOT NULL,\r\n" +
	"	EndTime TIMESTAMP NOT NULL,\r\n" +
	"	Duration INT NOT NULL,\r\n" +
	"	CurrentClassLoadCount INT NOT NULL,\r\n" +
	" 	ClassLoadCountInPeriod INT NOT NULL,\r\n" +
	" 	ClassLoadCountPerMinute DECIMAL(18,2) NOT NULL,\r\n" +
	"	ClassUnloadCountInPeriod INT NOT NULL,\r\n" +
	"	ClassUnloadCountPerMinute DECIMAL(18,2) NOT NULL,\r\n" +
	" 	PendingClassFinalizationCount INT NOT NULL,\r\n" +  
	" 	CurrentThreadCount INT NOT NULL,\r\n" +
	" 	CurrentDaemonThreadCount INT NOT NULL,\r\n" +
	" 	ThreadStartCountInPeriod INT NOT NULL,\r\n" +
	"  	ThreadStartCountPerMinute DECIMAL(18,2) NOT NULL,\r\n" +
	"  	HeapMemUsedMB  DECIMAL(18,2)  NOT NULL,\r\n" +
	"  	HeapMemCommitedMB DECIMAL(18,2) NOT NULL,\r\n" +
	"  	HeapMemMaxMB DECIMAL(18,2) NOT NULL,\r\n" +
	"  	NonHeapMemUsedMB  DECIMAL(18,2)  NOT NULL,\r\n" +
	" 	NonHeapMemCommittedUsedMB  DECIMAL(18,2) NOT NULL,\r\n" +  
	" 	NonHeapMemMaxUsedMB  DECIMAL(18,2) NOT NULL,\r\n" +  
	"  	SystemLoadAverage DECIMAL(5, 2),\r\n" +
	" 	CompilationMillisInPeriod  INT,\r\n" + // (Can be null based on getCompilationTimeActive())
	" 	CompilationMillisPerMinute DECIMAL(18,2)\r\n" + // (Can be null based on getCompilationTimeActive())
	")\r\n";

    final String DERBY_DROP_1 = "DROP TABLE p4j.P4JVMSnapShot";
    private Connection conn;

	
	public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public JVMSnapShotTest(String name) {
        super(name);
    }
	protected void setUp() throws Exception {
		super.setUp();
		
		conn = appender.getConnection();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(DERBY_CREATE_1);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
	}

	protected void tearDown() throws Exception {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(DERBY_DROP_1);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
		super.tearDown();
	}
    
    
    /*----------------------------------------------------------------------------*/    
    public void testValidateDataInterface() throws Exception {
    	Bundle bundle = SnapShotGenerator.generateBundle(JVMSnapShot.class);
    	SnapShotData data = bundle.newSnapShotData();
    	
    	assertTrue("Should implement interface", data instanceof 
    			JVMSnapShot.JVMData);
    	
    	JVMSnapShot.JVMData d = (JVMSnapShot.JVMData)data;
    	Method methods[] = JVMSnapShot.JVMData.class.getDeclaredMethods();
    	for (int i = 0; i < methods.length; i++) {
    		try {
    			methods[i].invoke(d, new Object[]{});
    		} catch (InvocationTargetException it) {
    			fail("Invalid method on data interface.  method name: " + methods[i].getName());
    		}
		}
    }
  
    public JVMData createMockJVMData() {
    	JVMData data = Mockito.mock(JVMData.class);

    	long start = System.currentTimeMillis();
    	long end = start + 60000;
    	
    	Mockito.when(data.getStartTime()).thenReturn(new Long(start));
    	Mockito.when(data.getStartTime()).thenReturn(new Long(end));
    	Mockito.when(data.getClassesLoaded()).thenReturn(new Integer(500000));
       	Mockito.when(data.getDuration()).thenReturn(new Long(60000));
       	Mockito.when(data.getTotalLoadedClassCount()).thenReturn(new Delta(500, 1000, 60000));
       	Mockito.when(data.getUnloadedClassCount()).thenReturn(new Delta(550, 1100, 60000));
       	Mockito.when(data.getCompilationTime()).thenReturn(new Delta(60000, 200000, 60000));
       	Mockito.when(data.getCompilationTimeActive()).thenReturn(Boolean.TRUE);
       	Mockito.when(data.getHeapMemUsed()).thenReturn(new Long(1000 * 255)); 
       	Mockito.when(data.getHeapMemCommitted()).thenReturn(new Long(1000 * 612));
       	Mockito.when(data.getHeapMemMax()).thenReturn(new Long(1000 * 750));
       	Mockito.when(data.getNonHeapMemUsed()).thenReturn(new Long(1000 * 60));
       	Mockito.when(data.getNonHeapMemCommitted()).thenReturn(new Long(1000 * 70));
       	Mockito.when(data.getNonHeapMemMax()).thenReturn(new Long(1000 * 100));
       	Mockito.when(data.getPendingFinalization()).thenReturn(new Long(512));
       	Mockito.when(data.getSystemLoadAverage()).thenReturn(new Double(43.65));
       	Mockito.when(data.getThreadCount()).thenReturn(new Integer(712));
       	Mockito.when(data.getDaemonThreadCount()).thenReturn(new Integer(212));
       	Mockito.when(data.getThreadsStarted()).thenReturn(new Delta(654, 712, 60000));
       	
    	return data;
    }

    public void testSnapShotInfoToSQL() throws Exception {
    	JVMSnapShot.SQLWriter writer = new JVMSnapShot.SQLWriter();
    	JVMData data = createMockJVMData();
    	
    	writer.writeToSQL(conn, "p4j", data, 1);

        final String VALIDATE_SQL = "SELECT " +
    		" COUNT(*) " +
    		" FROM p4j.P4JVMSnapShot\r\n" +
    		" WHERE SystemID=1\r\n" +
    		" AND StartTime=?\r\n" +
    		" AND EndTime=?\r\n" +
    		
    		" AND Duration=?\r\n" +
	    	" AND CurrentClassLoadCount=?\r\n" +
	    	" AND ClassLoadCountInPeriod=?\r\n" +
	    	" AND ClassLoadCountPerMinute=?\r\n" +
	    	" AND ClassUnloadCountInPeriod=?\r\n" +
	    	" AND ClassUnloadCountPerMinute=?\r\n" +
	    	" AND PendingClassFinalizationCount=?\r\n" +
	    	
	    	" AND CurrentThreadCount=?\r\n" +
	    	" AND CurrentDaemonThreadCount=?\r\n" +
	    	" AND ThreadStartCountInPeriod=?\r\n" +
	    	" AND ThreadStartCountPerMinute=?\r\n" +
	    	
	    	" AND HeapMemUsedMB=?\r\n" +
	    	" AND HeapMemCommitedMB=?\r\n" +
	    	" AND HeapMemMaxMB=?\r\n" +
	    	" AND NonHeapMemUsedMB=?\r\n" +
	    	" AND NonHeapMemCommittedUsedMB=?\r\n" +  
	    	" AND NonHeapMemMaxUsedMB=?\r\n" +  
	    	
	    	" AND SystemLoadAverage=?\r\n" +
	    	" AND CompilationMillisInPeriod=?\r\n" +
	    	" AND CompilationMillisPerMinute=?\r\n" + 
	    	"";
    	
        PreparedStatement stmt = null;
        try {
        	stmt = conn.prepareStatement(VALIDATE_SQL);
        	stmt.setTimestamp(1, new Timestamp(data.getStartTime()));
        	stmt.setTimestamp(2, new Timestamp(data.getEndTime()));
        	stmt.setLong(3, data.getDuration());
        	stmt.setInt(4, data.getClassesLoaded());
        	stmt.setLong(5, data.getTotalLoadedClassCount().getDelta());
        	stmt.setDouble(6, data.getTotalLoadedClassCount().getDeltaPerMinute());
        	stmt.setLong(7, data.getUnloadedClassCount().getDelta());
        	stmt.setDouble(8, data.getUnloadedClassCount().getDeltaPerMinute());
        	stmt.setLong(9, data.getPendingFinalization());
        	stmt.setLong(10, data.getThreadCount());
        	stmt.setLong(11, data.getDaemonThreadCount());
        	stmt.setLong(12, data.getThreadsStarted().getDelta());
        	stmt.setDouble(13, data.getThreadsStarted().getDeltaPerMinute());

        	
        	stmt.setDouble(14, r(data.getHeapMemUsed() / (double)1024, 2));
        	stmt.setDouble(15, r(data.getHeapMemCommitted() / (double)1024, 2));
        	stmt.setDouble(16, r(data.getHeapMemMax() / (double)1024, 2));
        	
        	stmt.setDouble(17, r(data.getNonHeapMemUsed() / (double)1024, 2));
        	stmt.setDouble(18, r(data.getNonHeapMemCommitted() / (double)1024, 2));
        	stmt.setDouble(19, r(data.getNonHeapMemMax() / (double)1024, 2));
        	
        	stmt.setDouble(20, data.getSystemLoadAverage());
        	stmt.setLong(21, data.getCompilationTime().getDelta());
        	stmt.setDouble(22, data.getCompilationTime().getDeltaPerMinute());
        	
        	long resultCount = JDBCHelper.getQueryCount(stmt);
        	assertEquals("Should have inserted row", 1, resultCount);
        } finally {
        	JDBCHelper.closeNoThrow(stmt);
        }
    }    

    public void testSnapShotInfoCompilationMillisActive() throws Exception {
    	JVMSnapShot.SQLWriter writer = new JVMSnapShot.SQLWriter();
    	JVMData data = createMockJVMData();
    	
       	Mockito.when(data.getCompilationTimeActive()).thenReturn(Boolean.FALSE);
    	writer.writeToSQL(conn, "p4j", data, 1);

        final String VALIDATE_SQL = "SELECT " +
    		" COUNT(*) " +
    		" FROM p4j.P4JVMSnapShot\r\n" +
	    	" WHERE CompilationMillisInPeriod IS NULL\r\n" +
	    	" AND CompilationMillisPerMinute IS NULL\r\n" + 
	    	"";
    	long resultCount = JDBCHelper.getQueryCount(conn, VALIDATE_SQL);
    	assertEquals("Should have inserted row", 1, resultCount);
    }    
    

    public void testNullWhenNoLoadAverage() throws Exception {
    	JVMSnapShot.SQLWriter writer = new JVMSnapShot.SQLWriter();
    	JVMData data = createMockJVMData();
    	
       	Mockito.when(data.getSystemLoadAverage()).thenReturn(new Double(-1));
    	writer.writeToSQL(conn, "p4j", data, 1);

        final String VALIDATE_SQL = "SELECT " +
    		" COUNT(*) " +
    		" FROM p4j.P4JVMSnapShot\r\n" +
	    	" WHERE SystemLoadAverage IS NULL\r\n" +
	    	"";
    	long resultCount = JDBCHelper.getQueryCount(conn, VALIDATE_SQL);
    	assertEquals("Should have inserted row", 1, resultCount);
    }    
    
    
/*----------------------------------------------------------------------------*/    
    
    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(JVMSnapShotTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {JVMSnapShotTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new GarbageCollectorSnapShotTest("testGetGarbageCollectors"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(JVMSnapShotTest.class);
        }

        return( newSuite);
    }
}
