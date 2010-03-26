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

import java.lang.management.MemoryPoolMXBean;
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
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;
import org.perfmon4j.java.management.MemoryPoolSnapShot.MemoryPoolData;
import org.perfmon4j.util.JDBCHelper;

public class MemoryPoolSnapShotTest extends SQLTest {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public MemoryPoolSnapShotTest(String name) {
        super(name);
    }
    
    final String DERBY_CREATE_1 = "CREATE TABLE p4j.P4JMemoryPool(\r\n" +
	"	InstanceName VARCHAR(200) NOT NULL,\r\n" +
	"	StartTime TIMESTAMP NOT NULL,\r\n" +
	"	EndTime TIMESTAMP NOT NULL,\r\n" +
	"	Duration INT NOT NULL,\r\n" +
	"	InitialMB DECIMAL(18,2) NOT NULL,\r\n" +
	"	UsedMB DECIMAL(18,2) NOT NULL,\r\n" +
	"	CommittedMB DECIMAL(18,2) NOT NULL,\r\n" +
	"	MaxMB DECIMAL(18,2) NOT NULL,\r\n" +
	"	MemoryType VARCHAR(50)\r\n" +
	")\r\n";

    
    final String DERBY_DROP_1 = "DROP TABLE p4j.P4JMemoryPool";
    private Connection conn;

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
    public void testGetMemoryPools() throws Exception {
    	MemoryPoolMXBean mpBeans[] = MemoryPoolSnapShot.getAllMemoryPools();
    	assertNotNull(mpBeans);
    	assertTrue("should have at least 1 memory pool", mpBeans.length >= 1);

    	// Now try to get a single garbage collector by name...
    	MemoryPoolMXBean bean = MemoryPoolSnapShot.getMemoryPool("SHOULD NOT EXIST");
    	assertNull("Should not exist", bean);
    	
    	String mpName = mpBeans[0].getName();
    	bean = MemoryPoolSnapShot.getMemoryPool(mpName);
    	assertNotNull("Should exist", bean);
    	
    	assertEquals("poolName should match", mpName, bean.getName());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testValidateDataInterface() throws Exception {
    	Bundle bundle = SnapShotGenerator.generateBundle(MemoryPoolSnapShot.class);
    	SnapShotData data = bundle.newSnapShotData();
    	
    	assertTrue("Should implement interface", data instanceof MemoryPoolSnapShot.MemoryPoolData);
    	
    	MemoryPoolSnapShot.MemoryPoolData d = (MemoryPoolSnapShot.MemoryPoolData)data;
    	Method methods[] = MemoryPoolSnapShot.MemoryPoolData.class.getDeclaredMethods();
    	for (int i = 0; i < methods.length; i++) {
    		try {
    			methods[i].invoke(d, new Object[]{});
    		} catch (InvocationTargetException it) {
    			fail("Invalid method on data interface.  method name: " + methods[i].getName());
    		}
		}
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testMemoryPoolInfoToSQL() throws Exception {
    	MemoryPoolSnapShot.SQLWriter writer = new MemoryPoolSnapShot.SQLWriter();
    	MemoryPoolData data = Mockito.mock(MemoryPoolData.class);
    
    	long start = System.currentTimeMillis();
    	long end = start + 60000;
    	
    	Mockito.when(data.getStartTime()).thenReturn(new Long(start));
    	Mockito.when(data.getStartTime()).thenReturn(new Long(end));
    	Mockito.when(data.getInstanceName()).thenReturn("Heap");
    	Mockito.when(data.getInit()).thenReturn(new Long(1000 * 32));
    	Mockito.when(data.getUsed()).thenReturn(new Long(1000 * 24));
    	Mockito.when(data.getCommitted()).thenReturn(new Long(1000 * 36));
    	Mockito.when(data.getMax()).thenReturn(new Long(1000 * 64));
    	Mockito.when(data.getType()).thenReturn("My type");
    	
    	writer.writeToSQL(conn, "p4j", data);

        final String VALIDATE_SQL = "SELECT " +
    		" COUNT(*) " +
    		" FROM p4j.P4JMemoryPool\r\n" +
    		" WHERE InstanceName='Heap'\r \n" +
    		" AND StartTime=?\r\n" +
    		" AND EndTime=?\r\n" +
    		" AND Duration=?\r\n" +
    		" AND InitialMB=?\r\n" +
    		" AND UsedMB=?\r\n" +
    		" AND CommittedMB=?\r\n" +
    		" AND MaxMB=?\r\n" +
    		" AND MemoryType=?\r\n" +
    		"";
        PreparedStatement stmt = null;
        try {
        	stmt = conn.prepareStatement(VALIDATE_SQL);
        	stmt.setTimestamp(1, new Timestamp(data.getStartTime()));
        	stmt.setTimestamp(2, new Timestamp(data.getEndTime()));
        	stmt.setLong(3, data.getDuration());
        	stmt.setDouble(4, r(data.getInit()/1024, 2));
        	stmt.setDouble(5, r(data.getUsed()/1024, 2));
        	stmt.setDouble(6, r(data.getCommitted()/1024, 2));
        	stmt.setDouble(7, r(data.getMax()/1024, 2));
        	stmt.setString(8, data.getType());
        	
        	long resultCount = JDBCHelper.getQueryCount(stmt);
        	assertEquals("Should have inserted row", 1, resultCount);
        } finally {
        	JDBCHelper.closeNoThrow(stmt);
        }
    }
     
  
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(MemoryPoolSnapShotTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {MemoryPoolSnapShotTest.class.getName()};

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
            newSuite = new TestSuite(MemoryPoolSnapShotTest.class);
        }

        return( newSuite);
    }
}
