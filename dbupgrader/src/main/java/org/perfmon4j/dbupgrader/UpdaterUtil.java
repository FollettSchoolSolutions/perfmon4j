/*
 *	Copyright 2008-2011 Follett Software Company 
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
package org.perfmon4j.dbupgrader;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

class UpdaterUtil {
	static void closeNoThrow(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException se) {
			// Nothing todo.
		}
	}

	static void closeNoThrow(Statement stmt) {
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (SQLException se) {
			// Nothing todo.
		}
	}

	static void closeNoThrow(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException se) {
			// Nothing todo.
		}
	}

	
//	static private boolean isDriverLoaded(String driverClassName) {
//		boolean result = false;
//		
//		Enumeration<Driver> drivers = DriverManager.getDrivers();
//		while (drivers.hasMoreElements() && !result) {
//			result = drivers.nextElement().getClass().getClass().equals(driverClassName);
//		}
//		
//		return result;
//	}
	
	static Connection createConnection(String driverClassName, String jarFileName, String jdbcURL, String userName, String password) throws Exception {
		Driver driver = loadDriver(driverClassName, jarFileName);
		
		Properties credentials = new Properties();
		if (userName != null) {
			credentials.setProperty("user", userName);
		}
		
		if (password != null) {
			credentials.setProperty("password", password);
		}
		
		return driver.connect(jdbcURL, credentials);
	}
	
	@SuppressWarnings("unchecked")
	private static Driver loadDriver(String driverClassName, String jarFileName)
			throws Exception {
		Class<Driver> driverClazz;

		if (jarFileName != null) {
			File driverFile = new File(jarFileName);
			if (!driverFile.exists()) {
				throw new FileNotFoundException("File: " + jarFileName
						+ " NOT FOUND");
			}
			URL url;
			try {
				url = driverFile.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new Exception("Unable to convert to URL - file: "
						+ jarFileName, e);
			}
			ClassLoader loader = new URLClassLoader(new URL[] { url }, Thread
					.currentThread().getContextClassLoader());
			driverClazz = (Class<Driver>) Class.forName(driverClassName, true, loader);
		} else {
			driverClazz = (Class<Driver>) Class.forName(driverClassName, true, Thread.currentThread().getContextClassLoader());
		}

		return driverClazz.newInstance();
	}	
	
//	static Connection createConnection(String driverClassName, String jarFileName, String jdbcURL, String userName, String password) throws Exception {
//		if (!isDriverLoaded(driverClassName)) {
//			Driver driver = loadDriver(driverClassName, jarFileName);
//			DriverManager.registerDriver(driver);
//		}
//		
//		return DriverManager.getConnection(jdbcURL, userName, password);
//	}
//	
//	@SuppressWarnings("unchecked")
//	private static Driver loadDriver(String driverClassName, String jarFileName)
//			throws Exception {
//		Class<Driver> driverClazz;
//
//		if (jarFileName != null) {
//			File driverFile = new File(jarFileName);
//			if (!driverFile.exists()) {
//				throw new FileNotFoundException("File: " + jarFileName
//						+ " NOT FOUND");
//			}
//			URL url;
//			try {
//				url = driverFile.toURI().toURL();
//			} catch (MalformedURLException e) {
//				throw new Exception("Unable to convert to URL - file: "
//						+ jarFileName, e);
//			}
//			ClassLoader loader = new URLClassLoader(new URL[] { url }, Thread
//					.currentThread().getContextClassLoader());
//			driverClazz = (Class<Driver>) Class.forName(driverClassName, true, loader);
//		} else {
//			driverClazz = (Class<Driver>) Class.forName(driverClassName, true, Thread.currentThread().getContextClassLoader());
//		}
//		
//		return driverClazz.newInstance();
//	}
	
	static boolean doesTableExist(Connection conn, String schema, String tableName) {
		boolean result = false;
		
		Statement stmt = null;
		ResultSet rs = null;
		if (schema != null) {
			tableName = schema + "." + tableName;
		}
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM " + tableName);
			result = true;
		} catch (SQLException se) {
			// Assume table does not exist.
		} finally {
			closeNoThrow(stmt);
			closeNoThrow(rs);
		}
		
		return result;
	}
	

}
