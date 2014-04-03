/*
 *	Copyright 2008-2014 Follett Software Company 
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
package org.perfmon4j.instrument.snapshot;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.perfmon4j.SQLTest;
import org.perfmon4j.util.JDBCHelper;

public class CacheSnapShotSQLWriterTest extends SQLTest {
	   final String DERBY_CREATE_1 = "CREATE TABLE mydb.P4JCache(\r\n" +
		"	SystemID INT NOT NULL,\r\n" +
		"	CacheType VARCHAR(100) NOT NULL,\r\n" +
		"	InstanceName VARCHAR(100) NOT NULL,\r\n" +
		"	StartTime TIMESTAMP NOT NULL,\r\n" +
		"	EndTime TIMESTAMP NOT NULL,\r\n" +
		"	Duration INT NOT NULL,\r\n" +
		"	HitRatio DOUBLE NOT NULL,\r\n" +
		"	HitCount INT NOT NULL,\r\n" +
		"	MissCount INT NOT NULL,\r\n" +
		"	PutCount INT NOT NULL)\r\n";

		final String DERBY_DROP_1 = "DROP TABLE mydb.P4JCache";

		private Connection conn;

		private void createTables() throws SQLException {
			conn = appender.getConnection();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.execute(DERBY_CREATE_1);
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		}

		private void dropTables() throws SQLException {
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.execute(DERBY_DROP_1);
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		}
		
		protected void setUp() throws Exception {
			super.setUp();
			createTables();
		}

		protected void tearDown() throws Exception {
			dropTables();
			super.tearDown();
		}
		
		
	    public void testWriteToSQL() throws Exception {
	    	MyData data = new MyData();
	    	
	    	CacheSnapShotSQLWriter writer = new CacheSnapShotSQLWriter();
	    	
	    	writer.writeToSQL(conn, "mydb", data, 1);
	    	
	    	long rows = JDBCHelper.getQueryCount(conn, "SELECT COUNT(*)\r\n" + 
					"	FROM mydb.P4JCache tt\r\n"); 
	    	
	    	assertEquals("row count", 1, rows);
	    }
	    
	    private static class MyData implements CacheSnapShotData {
			public String getCacheType() {
				return "MyCacheType";
			}

			public Delta getHitCount() {
				return new Delta(0, 3, 60000);
			}

			public Ratio getHitRatio() {
				return new Ratio(3, 10);
			}

			public String getInstanceName() {
				return "MyInstance";
			}

			public Delta getMissCount() {
				return new Delta(0, 7, 60000);
			}

			public Delta getPutCount() {
				return new Delta(0, 8, 60000);
			}

			public long getDuration() {
				return 60000;
			}

			public long getEndTime() {
				return System.currentTimeMillis();
			}

			public String getName() {
				return "MyName";
			}

			public long getStartTime() {
				// TODO Auto-generated method stub
				return System.currentTimeMillis();
			}

			public String toAppenderString() {
				return "";
			}
	    	
	    }
}
