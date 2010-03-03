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
package org.perfmon4j.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCHelper {
	private static final Logger logger = LoggerFactory.initLogger(JDBCHelper.class);
	
	 public static void rollbackNoThrow(Connection conn) {
			try {
				if (conn != null) {
					conn.rollback();
				}
			} catch (SQLException se) {
				logger.logDebug("Error rolling back connection", se);
		}
	}
	
	public static void closeNoThrow(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException se) {
			logger.logDebug("Error closing connection", se);
		}
	 }
	
	public static void closeNoThrow(Statement stmt) {
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (SQLException se) {
			logger.logDebug("Error closing statement", se);
		}
	}
	
	public static void executeNoThrow(Statement stmt, String sql) {
		try {
			if (stmt != null) {
				stmt.execute(sql);
			}
		} catch (SQLException se) {
			logger.logDebug("Error executing statement", se);
		}
	}
	
	
	public static void closeNoThrow(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException se) {
			logger.logDebug("Error closing result set", se);
		}
	}
	
	public static long getQueryCount(Connection conn, String sql) throws SQLException {
		long result = 0;
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				result = rs.getLong(1);
			}
		} finally {
			closeNoThrow(rs);
			closeNoThrow(stmt);
		}
		
		return result;
	}
}
