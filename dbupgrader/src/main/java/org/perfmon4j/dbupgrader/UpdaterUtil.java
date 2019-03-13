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
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;

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
	
	static Connection createConnection(String driverClassName, String jarFileName, String jdbcURL, String userName, String password) throws Exception {
		Driver driver = loadDriver(driverClassName, jarFileName);
		
		Properties credentials = new Properties();
		if (userName != null) {
			credentials.setProperty("user", userName);
		}
		
		if (password != null) {
			credentials.setProperty("password", password);
		}
		Connection conn = driver.connect(jdbcURL, credentials); 
		
		if (conn == null) {
			throw new SQLException("Unabled to connect with jdbcURL: " + jdbcURL);
		} 
		
		return conn;
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
	
	static boolean doesTableExist(Connection conn, String schema, String tableName) throws Exception {
		boolean nullSchema = (schema == null);
		boolean result = false;
		DatabaseMetaData dbMetaData = null;
		ResultSet rs = null;
		try {
			dbMetaData = conn.getMetaData();
			rs = dbMetaData.getTables(null, "%", "%", new String[]{"TABLE"});
			while (rs.next() && !result) {
				final String n = rs.getString("TABLE_NAME");
				final String s = rs.getString("TABLE_SCHEM");
				
				boolean schemaMatches = ((s == null) && nullSchema)
						|| schema.equalsIgnoreCase(s);
				result = schemaMatches && tableName.equalsIgnoreCase(n);
			}
		} finally {
			closeNoThrow(rs);
		}
		
		return result;
	}

	static boolean doesIndexExist(Connection conn, String schema, String tableName, String indexName) throws Exception {
		boolean nullSchema = (schema == null);
		
		boolean result = false;
		DatabaseMetaData dbMetaData = null;
		ResultSet rs = null;
		try {
			dbMetaData = conn.getMetaData();
			rs = dbMetaData.getIndexInfo(null, null, tableName.toUpperCase(), false, false);
			while (rs.next() && !result) {
				final String n = rs.getString("INDEX_NAME");
				final String s = rs.getString("TABLE_SCHEM");
				
				boolean schemaMatches = ((s == null) && nullSchema)
						|| schema.equalsIgnoreCase(s);
				result = schemaMatches && indexName.equalsIgnoreCase(n);
			}
		} finally {
			closeNoThrow(rs);
		}
		
		return result;
	}

	
	
	static String getColumnDataType(Connection conn, String schema, String tableName, String columnName) throws Exception {
		String result = null;
		
		if (doesColumnExist(conn, schema, tableName, columnName)) {
			tableName = schema == null ? tableName : schema + "." + tableName;
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery("SELECT " + columnName + " FROM " + tableName + " WHERE 1=0");
				ResultSetMetaData md = rs.getMetaData();
				result = md.getColumnTypeName(1);
			} finally {
				closeNoThrow(rs);
				closeNoThrow(stmt);
			}
		}
		
		return result;
	}
	
	
	static boolean doesColumnExist(Connection conn, String schema, String tableName, String column) throws Exception {
		boolean result = doesTableExist(conn, schema, tableName);
		
		if (result) {
			Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
			tableName = db.escapeTableName(null, schema, tableName);
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery("SELECT  * FROM " + tableName + " WHERE 1=0");
				ResultSetMetaData md = rs.getMetaData();
				for (int i = 0; i < md.getColumnCount(); i++) {
					result = column.equalsIgnoreCase(md.getColumnName(i+1));
					if (result) {
						break;
					}
				}
			} finally {
				closeNoThrow(rs);
				closeNoThrow(stmt);
			}
		}
		return result;
	}
	
	private final static String IDENTITY_CHARS = "BCDFGHJKLMNPRSTVWXYZ";
	private final static Random random = new Random();
	
	private static char nextChar() {
		int offset = random.nextInt(IDENTITY_CHARS.length());
		return IDENTITY_CHARS.charAt(offset);
	}
	
	
	public static String generateUniqueIdentity() {
		StringBuilder builder = new StringBuilder();
		
		for (int j = 0; j < 2; j++) {
			for(int i = 0; i < 4; i++) {
				builder.append(nextChar());
			}
			if (j == 0) {
				builder.append('-');
			}
		}
		return builder.toString();
	}
}
