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

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.UserAgentVO;

public class UserAgentSnapShotMonitorTest extends SQLTest {
    
	final String DERBY_CREATE_VIEW = "CREATE VIEW mydb.P4JUserAgentView AS\r\n" +
		"SELECT\r\n" +
		"	oc.SystemID" + 
		"	,oc.CollectionDate\r\n" +
		"	,b.BrowserName\r\n" +
		"	,bv.BrowserVersion\r\n" +
		"	,os.OSName\r\n" +
		"	,osv.OSVersion\r\n" +
		"	,oc.RequestCount\r\n" +
		"FROM mydb.P4JUserAgentOccurance oc\r\n" +
		"JOIN mydb.P4JUserAgentBrowser b ON b.BrowserID = oc.BrowserID\r\n" +
		"LEFT JOIN mydb.P4JUserAgentBrowserVersion bv ON bv.BrowserVersionID = oc.BrowserVersionID\r\n" +
		"LEFT JOIN mydb.P4JUserAgentOS os ON os.OSID = oc.OSID\r\n" +
		"LEFT JOIN mydb.P4JUserAgentOSVersion osv ON osv.OSVersionID = oc.OSVersionID\r\n";
//	
	final String DERBY_DROP_VIEW = "DROP VIEW mydb.P4JUserAgentView";
    
    private Connection conn;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		conn = appender.getConnection();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(DERBY_CREATE_VIEW);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
	}

	protected void tearDown() throws Exception {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(DERBY_DROP_VIEW);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
		super.tearDown();
	}
	
	public void testWriteMasterRecords() throws Exception {
		UserAgentSnapShotMonitor mon = new UserAgentSnapShotMonitor("test");
	
		UserAgentSnapShotMonitor.UserAgentData d = new UserAgentSnapShotMonitor.UserAgentData(System.currentTimeMillis());
		UserAgentVO vo = new UserAgentVO("IE", "7.0", "XP", "3.0", null); 
		UserAgentSnapShotMonitor.Counter counter = new UserAgentSnapShotMonitor.Counter(); 
		counter.setCount(1);
		
		d.getUserAgentMap().put(vo, counter);
		appender.outputData(d);
		
		long browserCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
				"FROM mydb.P4JUserAgentBrowser WHERE BrowserName='IE'");
		long browserVersionCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
			"FROM mydb.P4JUserAgentBrowserVersion WHERE BrowserVersion='7.0'");
		long osCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
		"FROM mydb.P4JUserAgentOS WHERE OSName='XP'");
		long osVersionCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
			"FROM mydb.P4JUserAgentOSVersion WHERE OSVersion='3.0'");
		long requestCount = JDBCHelper.getQueryCount(conn, "SELECT requestCount " +
		"FROM mydb.P4JUserAgentView " +
		"WHERE BrowserName='IE' " +
		"	AND BrowserVersion='7.0' " +
		"	AND OSName='XP' " +
		"	AND OSVersion='3.0'");
		
		assertEquals("Browser Count", 1, browserCount);
		assertEquals("Browser Version Count", 1, browserVersionCount);
		assertEquals("OS Count", 1, osCount);
		assertEquals("OS Version Count", 1, osVersionCount);
		assertEquals("requestCount", 1, requestCount);

		// Should be use an existing master record (Should not add duplicate ROWS!)
		appender.outputData(d);
		
		browserCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
			"FROM mydb.P4JUserAgentBrowser WHERE BrowserName='IE'");
		browserVersionCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
			"FROM mydb.P4JUserAgentBrowserVersion WHERE BrowserVersion='7.0'");
		osCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
			"FROM mydb.P4JUserAgentOS WHERE OSName='XP'");
		osVersionCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
			"FROM mydb.P4JUserAgentOSVersion WHERE OSVersion='3.0'");
		requestCount = JDBCHelper.getQueryCount(conn, "SELECT requestCount " +
				"FROM mydb.P4JUserAgentView " +
				"WHERE BrowserName='IE' " +
				"	AND BrowserVersion='7.0' " +
				"	AND OSName='XP' " +
				"	AND OSVersion='3.0'");
		
		assertEquals("Browser Count", 1, browserCount);
		assertEquals("Browser Version Count", 1, browserVersionCount);
		assertEquals("OS Count", 1, osCount);
		assertEquals("OS Version Count", 1, osVersionCount);
		assertEquals("Should have incremented requestCount", 2, requestCount);
	}

	public void testFillInMissingRecordsWithUnkown() throws Exception {
		UserAgentSnapShotMonitor mon = new UserAgentSnapShotMonitor("test");
	
		UserAgentSnapShotMonitor.UserAgentData d = new UserAgentSnapShotMonitor.UserAgentData(System.currentTimeMillis());
		UserAgentVO vo = new UserAgentVO("I don't know how to translate this useragent string"); 
		UserAgentSnapShotMonitor.Counter counter = new UserAgentSnapShotMonitor.Counter(); 
		counter.setCount(1);
		
		d.getUserAgentMap().put(vo, counter);
		appender.outputData(d);
		
		long browserCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
				"FROM mydb.P4JUserAgentBrowser WHERE BrowserName='[UNKNOWN]'");
		long browserVersionCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
			"FROM mydb.P4JUserAgentBrowserVersion WHERE BrowserVersion='[UNKNOWN]'");
		long osCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
		"FROM mydb.P4JUserAgentOS WHERE OSName='[UNKNOWN]'");
		long osVersionCount = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*) " +
			"FROM mydb.P4JUserAgentOSVersion WHERE OSVersion='[UNKNOWN]'");
		long requestCount = JDBCHelper.getQueryCount(conn, "SELECT requestCount " +
		"FROM mydb.P4JUserAgentView " +
		"WHERE BrowserName='[UNKNOWN]' " +
		"	AND BrowserVersion='[UNKNOWN]' " +
		"	AND OSName='[UNKNOWN]' " +
		"	AND OSVersion='[UNKNOWN]'");
		
		assertEquals("Browser Count", 1, browserCount);
		assertEquals("Browser Version Count", 1, browserVersionCount);
		assertEquals("OS Count", 1, osCount);
		assertEquals("OS Version Count", 1, osVersionCount);
		assertEquals("requestCount", 1, requestCount);
	}
}
