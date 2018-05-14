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

import org.perfmon4j.SQLTest;
import org.perfmon4j.util.JDBCHelper;

public class CacheSnapShotSQLWriterTest extends SQLTest {
		private Connection conn;

		private void createTables() throws SQLException {
			conn = appender.getConnection();
		}
		
		protected void setUp() throws Exception {
			super.setUp();
			createTables();
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
