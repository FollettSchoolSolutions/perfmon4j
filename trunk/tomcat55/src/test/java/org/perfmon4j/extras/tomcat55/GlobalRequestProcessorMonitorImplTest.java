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

package org.perfmon4j.extras.tomcat55;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;

import org.mockito.Mockito;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.util.JDBCHelper;


public class GlobalRequestProcessorMonitorImplTest extends SQLTest {
    final String DERBY_CREATE_1 = "CREATE TABLE mydb.P4JGlobalRequestProcessor(\r\n" +
	"	SystemID INT NOT NULL,\r\n" +
	"	InstanceName VARCHAR(200) NOT NULL,\r\n" +
	"	StartTime TIMESTAMP NOT NULL,\r\n" +
	"	EndTime TIMESTAMP NOT NULL,\r\n" +
	"	Duration INT NOT NULL,\r\n" +
	"	RequestCountInPeriod INT NOT NULL,\r\n" +
	"	RequestCountPerMinute DECIMAL(18,2) NOT NULL,\r\n" +
	"	KBytesSentInPeriod DECIMAL(18, 2) NOT NULL,\r\n" +
	"	KBytesSentPerMinute DECIMAL(18, 2) NOT NULL,\r\n" +
	"	KBytesReceivedInPeriod DECIMAL(18, 2) NOT NULL,\r\n" +
	"	KBytesReceivedPerMinute DECIMAL(18, 2) NOT NULL,\r\n" +
	"	ProcessingMillisInPeriod INT NOT NULL,\r\n" +
	"	ProcessingMillisPerMinute DECIMAL(18, 2) NOT NULL,\r\n" +
	"	ErrorCountInPeriod INT NOT NULL,\r\n" +
	"	ErrorCountPerMinute DECIMAL(18, 2) NOT NULL\r\n" +
	")\r\n";

    final String DERBY_DROP_1 = "DROP TABLE mydb.P4JGlobalRequestProcessor";
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
	
    public void testDoIt() throws Exception {
    	GlobalRequestProcessorMonitorImpl.SQLWriter writer = new GlobalRequestProcessorMonitorImpl.SQLWriter();
    	GlobalRequestProcessorMonitor data = Mockito.mock(GlobalRequestProcessorMonitor.class);
    
    	long start = System.currentTimeMillis();
    	long end = start + 60000;
    	
    	Mockito.when(data.getStartTime()).thenReturn(new Long(start));
    	Mockito.when(data.getStartTime()).thenReturn(new Long(end));
    	Mockito.when(data.getInstanceName()).thenReturn("HTTP");
    	
    	Mockito.when(data.getRequestCount()).thenReturn(new Delta(0, 60000, 60000));
    	Mockito.when(data.getBytesSent()).thenReturn(new Delta(0, 40000 * 1000, 60000));
    	Mockito.when(data.getBytesReceived()).thenReturn(new Delta(0, 60000 * 1000, 60000));
    	Mockito.when(data.getProcessingTimeMillis()).thenReturn(new Delta(0, 320000, 60000));
    	Mockito.when(data.getErrorCount()).thenReturn(new Delta(0, 72, 60000));

    	writer.writeToSQL(conn, "mydb", data, 1);

    	final String VALIDATE_SQL = "SELECT COUNT(*) FROM mydb.P4JGlobalRequestProcessor " +
	    	" WHERE InstanceName=?\r\n" +
	    	" AND StartTime=?\r\n" +
	    	" AND EndTime=?\r\n" +
	    	" AND Duration=?\r\n" +
	    	" AND RequestCountInPeriod=?\r\n" +
	    	" AND RequestCountPerMinute=?\r\n" +
	    	" AND KBytesSentInPeriod=?\r\n" +
	    	" AND KBytesSentPerMinute=?\r\n" +
	    	" AND KBytesReceivedInPeriod=?\r\n" +
	    	" AND KBytesReceivedPerMinute=?\r\n" +
	    	" AND ProcessingMillisInPeriod=?\r\n" +
	    	" AND ProcessingMillisPerMinute=?\r\n" +
	    	" AND ErrorCountInPeriod=?\r\n" +
	    	" AND ErrorCountPerMinute=?\r\n";
    	
    	
    	
        PreparedStatement stmt = null;
        try {
        	stmt = conn.prepareStatement(VALIDATE_SQL);
        	stmt.setString(1, data.getInstanceName());
        	stmt.setTimestamp(2, new Timestamp(data.getStartTime()));
        	stmt.setTimestamp(3, new Timestamp(data.getEndTime()));
        	stmt.setLong(4, data.getDuration());
        	stmt.setLong(5, data.getRequestCount().getDelta());
        	stmt.setDouble(6, data.getRequestCount().getDeltaPerMinute());
        	stmt.setLong(7, (data.getBytesSent().getDelta() / 1024));
        	stmt.setDouble(8, (data.getBytesSent().getDeltaPerMinute() / 1024));
        	stmt.setLong(9, (data.getBytesReceived().getDelta() / 1024));
        	stmt.setDouble(10, (data.getBytesReceived().getDeltaPerMinute() / 1024));
        	stmt.setLong(11, data.getProcessingTimeMillis().getDelta());
        	stmt.setDouble(12, data.getProcessingTimeMillis().getDeltaPerMinute());
        	stmt.setLong(13, data.getErrorCount().getDelta());
        	stmt.setDouble(14, data.getErrorCount().getDeltaPerMinute());
        	
        	
        	long resultCount = JDBCHelper.getQueryCount(stmt);
        	assertEquals("Should have inserted row", 1, resultCount);
        } finally {
        	JDBCHelper.closeNoThrow(stmt);
        }
    }
}
