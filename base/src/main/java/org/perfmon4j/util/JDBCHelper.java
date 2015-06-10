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

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.perfmon4j.PerfMon;
import org.perfmon4j.util.vo.ResponseInfo;
import org.perfmon4j.util.vo.ResponseInfoImpl;

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
	
	public static long getQueryCount(PreparedStatement stmt) throws SQLException {
		long result = 0;
		
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getLong(1);
			}
		} finally {
			closeNoThrow(rs);
		}
		
		return result;
	}
	
	public static String getDatabaseIdentity(Connection conn, String schema) throws SQLException {
		String result = null;
		Statement stmt = null;
		ResultSet rs = null;
		final String tableName = (schema != null ? schema + "." : "") + "P4JDatabaseIdentity";
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT DatabaseID FROM " + tableName);
			if (rs.next()) {
				result = rs.getString(1);
			}
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmt);
		}
		
		return result;
	}
	
	public static double getDatabaseVersion(Connection conn, String dbSchema) throws SQLException {
		double result = 0.0;
		
		if (conn != null) {
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String s = (dbSchema == null) ? "" : (dbSchema + ".");
				String sql = "SELECT ID FROM " + s + "DATABASECHANGELOG WHERE author = 'databaseLabel' ORDER BY ID DESC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if (rs.next()) {
					try {
						result = Double.parseDouble(rs.getString(1));
					} catch (NumberFormatException nfe) {
						// Nothing to do... Just return default version..
					}
				}
			} finally {
				JDBCHelper.closeNoThrow(rs);
				JDBCHelper.closeNoThrow(stmt);
			}
		}
		
		return result;
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
	
	public static long simpleGetOrCreate(Connection conn, 
			String tableName, String idColumnName, String descColumnName, String desc) throws SQLException {
		long result = 0;
		final boolean oracleConnection = isOracleConnection(conn);
		final String selectSQL = "SELECT " + idColumnName + " FROM " +
			tableName + " WHERE " + descColumnName + "=?";
		PreparedStatement stmtQuery = null;
		PreparedStatement stmtInsert = null;
		ResultSet rs = null;
		
		try {
			stmtQuery = conn.prepareStatement(selectSQL);
			stmtQuery.setString(1, desc);
			rs = stmtQuery.executeQuery();
			if (!rs.next()) {
				JDBCHelper.closeNoThrow(rs);
				final String insertSQL = "INSERT INTO " +
					tableName + " (" + descColumnName + ")" +
					" VALUES(?)";
				rs = null;
				
				if (oracleConnection) {
					stmtInsert = conn.prepareStatement(insertSQL, new int[]{1});
				} else {
					stmtInsert = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
				}
				stmtInsert.setString(1, desc);
				stmtInsert.execute();
				
				rs = stmtInsert.getGeneratedKeys();
				rs.next();
			}
			result = rs.getLong(1);
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmtQuery);
			JDBCHelper.closeNoThrow(stmtInsert);
		}
		return result;
	}
	
	public static String buildEqualOrIsNULL(String fieldName, Number value) {
		if (value == null) {
			return fieldName + "IS NULL";
		} else {
			return fieldName + "=" + value.toString();
		}
	}
	
	public static String rsRowToString(ResultSet rs) throws SQLException {
		String result = "";
		
		ResultSetMetaData d = rs.getMetaData();
		int count = d.getColumnCount();
		for (int i = 1; i <= count; i++) {
			result += d.getColumnLabel(i) + "=" + rs.getString(i) + "\r\n";
		}
		
		return result;
	}
	
	public static String dumpQuery(Connection conn, String SQL) throws SQLException {
		String result = "";
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while (rs.next()) {
				result += rsRowToString(rs);
				result += "*********************************************\r\n";
			}
			
		} finally {
			closeNoThrow(rs);
			closeNoThrow(stmt);
		}
		
		return result;
	}

	public static boolean isOracleConnection(Connection conn) throws SQLException {
		boolean result = false;
		
		if (conn != null) {
			result = conn.getMetaData().getDriverName().equalsIgnoreCase("Oracle JDBC driver");
		}
		
		return result;
	}
	
	public static final class DriverCache {
		public static final DriverCache DEFAULT = new DriverCache();
		
		
		private final Map<String, WeakReference<Driver>> cache =
			new HashMap<String, WeakReference<Driver>>();
		
		private String buildKey(String driverClassName, String jarFileName) {
			StringBuilder builder = new StringBuilder();
			
			builder.append("D:")
				.append(driverClassName)
				.append("J:")
				.append(jarFileName);
			
			return builder.toString();
		}
		
		public synchronized Driver get(String driverClassName, String jarFileName) {
			Driver result = null;
			String key = buildKey(driverClassName, jarFileName);
			WeakReference<Driver> driverRef = cache.get(key);
			if (driverRef != null) {
				result = driverRef.get();
				if (result == null) {
					cache.remove(key);
				}
			}
			
			return result;
		}
		
		public synchronized void put(String driverClassName, String jarFileName, Driver driver) {
			cache.put(buildKey(driverClassName, jarFileName), new WeakReference<Driver>(driver));
		}
	}

	public static Driver loadDriver(String driverClassName) throws FileNotFoundException, ClassNotFoundException, 
		InstantiationException, IllegalAccessException {
	
		return loadDriver(driverClassName, null);
	}

	public static Driver loadDriver(String driverClassName, String jarFileName) throws FileNotFoundException, ClassNotFoundException, 
		InstantiationException, IllegalAccessException {
		Class<Driver> driverClazz;
		
		if (jarFileName != null) {
			File driverFile = new File(jarFileName);
			if (!driverFile.exists()) {
				throw new FileNotFoundException("File: " + jarFileName + " NOT FOUND");
			}
			URL url;
			try {
				// Why would a java file object ever return an invalid URL?  Should not happen
				// if it does just throw a runtime exception.
				url = driverFile.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException("Unable to convert to URL - file: " + jarFileName, e);
			}
			ClassLoader loader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());
			driverClazz = (Class<Driver>)Class.forName(driverClassName, true, loader);
		} else {
			driverClazz = (Class<Driver>)Class.forName(driverClassName, true, PerfMon.getClassLoader());
		}
		
		return driverClazz.newInstance();
	}
	
    public static Connection createJDBCConnection(Driver driver, String jdbcURL, String userName,
    	String password) throws SQLException {
    	Connection conn = null;
		if (driver != null) {
			Properties credentials = new Properties();
			if (userName != null) {
				credentials.setProperty("user", userName);
			}
			if (password != null) {
				credentials.setProperty("password", password);
			}
			conn = driver.connect(jdbcURL, credentials);
		} else {
			conn = DriverManager.getConnection(jdbcURL, userName, password);
		}
		logger.logDebug("Created SQL Connection");
		
    	return conn;
    }

   
    /**
     * 
     * @param cache - Required, you must provide a store to keep from reloading driver class.
     * @param driverClassName - Optional, if this is null we will use the DriverManager to load driver based on URL. 
     * @param jarFileName - Optional, if this is null we will attempt to load driver using the 
     * 		class loader provided by PerfMon.getClassLoader()
     * @param jdbcURL - Required
     * @param userName - Optional
     * @param password - Optional
     * @return
     * @throws SQLException
     */
    public static Connection createJDBCConnection(JDBCHelper.DriverCache cache, String driverClassName, String jarFileName,
    	String jdbcURL, String userName, String password) throws SQLException {
        Driver driver = null;
        
    	if (driverClassName != null) {
           	driver = cache.get(driverClassName, jarFileName);
           	if (driver == null) {
           		try {
					driver = loadDriver(driverClassName, jarFileName);
				} catch (Exception e) {
					throw new SQLException("Unable to load driver - driverClassName: " + driverClassName + " jarFileName: " + jarFileName,
							e);
				} 
           		cache.put(driverClassName, jarFileName, driver);
           	}
		}
    	
    	return createJDBCConnection(driver, jdbcURL, userName, password);
    }
    
    /**
     * NOTE -  This is tested in JDBCSQLAppenderTest class.
     * @param conn
     * @param schema
     * @param monitorID
     * @return
     * @throws SQLException 
     */
    public static List<ResponseInfo> queryResponseInfo(Connection conn, String schema, long categoryID) throws SQLException {
    	List<ResponseInfo> result = new Vector<ResponseInfo>();

    	String intervalTable = (schema != null ?  schema + "." : "")   + "P4JIntervalData";
    	String categoryTable = (schema != null ?  schema + "." : "")   + "P4JCategory";
    	
    	Statement stmt = null;
    	ResultSet rs = null;
    	try {
    		stmt = conn.createStatement();
    		rs = stmt.executeQuery("SELECT cat.categoryName, id.* FROM " + intervalTable + " id " +
    				" JOIN " + categoryTable + " cat ON cat.categoryID = id.categoryID" +
    				" WHERE id.CategoryID=" + categoryID);
    		while (rs.next()) {
    			ResponseInfoImpl impl = new ResponseInfoImpl();
    			
    			impl.setMonitorName(rs.getString("CATEGORYNAME"));
    			impl.setEndTime(rs.getTimestamp("ENDTIME"));
    			impl.setMaxDuration(rs.getLong("MAXDURATION"));
    			impl.setMaxThreads(rs.getLong("MAXACTIVETHREADS"));
    			impl.setMinDuration(rs.getLong("MINDURATION"));
				impl.setStartTime(rs.getTimestamp("STARTTIME"));
				impl.setSum(rs.getLong("DURATIONSUM"));
				impl.setSumOfSquares(rs.getLong("DURATIONSUMOFSQUARES"));
				impl.setThroughput(rs.getDouble("NormalizedThroughputPerMinute"));
				impl.setTotalCompletions(rs.getLong("TOTALCOMPLETIONS"));
				impl.setTotalHits(rs.getLong("TOTALHITS"));
				
    			result.add(impl);
    		}
    	} finally {
    		JDBCHelper.closeNoThrow(rs);
    		JDBCHelper.closeNoThrow(stmt);
    	}
    	return result;
    }
}
