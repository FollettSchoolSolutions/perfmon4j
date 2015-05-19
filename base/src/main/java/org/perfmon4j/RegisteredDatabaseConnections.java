/*
 *	Copyright 2015 Follett School Solutions 
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
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.JDBCHelper.DriverCache;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public final class RegisteredDatabaseConnections {
	private static final Logger logger = LoggerFactory.initLogger(RegisteredDatabaseConnections.class);
	private static final Object lockToken = new Object();
	private static final Map<String, Database> databaseByName = new HashMap<String, RegisteredDatabaseConnections.Database>();
	private static final Map<String, Database> databaseByID = new HashMap<String, RegisteredDatabaseConnections.Database>();
	private static Database defaultDatabase = null;
	
	private static final String DB_SCHEMA = "dbSchema";
	private static String DRIVER_PATH = "driverPath";
	private static String DRIVER_CLASS = "driverClass"; 
	private static String JDBC_URL = "jdbcURL";
	private static String USER_NAME = "userName";
	private static String PASSWORD = "password";
	
	static boolean mockIdentityAndVersionForTest = false;
	
	private static String buildSignature(String dbSchema, String driverPath, String driverClass, String jdbcURL, String userName, String password) {
		return dbSchema + "|" + driverPath + "|" + driverClass + "|" + jdbcURL + "|" + userName + "|" + password;
	}

	
	static public void config(PerfMonConfiguration config) {
		Map<String, Properties> configDBs = getDatabaseProperties(config);
		
		for (Map.Entry<String, Properties> entry : configDBs.entrySet()) {
			String name = entry.getKey();
			Properties props = entry.getValue();
			
			String dbSchema = props.getProperty(DB_SCHEMA);
			String driverPath = props.getProperty(DRIVER_PATH); 
			String driverClass = props.getProperty(DRIVER_CLASS);
			String jdbcURL = props.getProperty(JDBC_URL);
			String userName = props.getProperty(USER_NAME);
			String password = props.getProperty(PASSWORD);
			
			boolean addDatabase = true;
			Database db = getDatabaseByName(name);
			if (db != null) {
				String newSignature = buildSignature(dbSchema, driverPath, driverClass, jdbcURL, userName, password);
				if (newSignature.equals(db.getSignature())) {
					addDatabase = false;
				} else {
					RegisteredDatabaseConnections.removeDatabase(name);
				}
			}
			if (addDatabase) {
				try {
					RegisteredDatabaseConnections.addJDBCDatabase(name, false, driverClass, driverPath, jdbcURL, dbSchema, userName, password);
				} catch (InvalidConfigException e) {
					logger.logWarn("Error registering database: " + name, e);
				}
			}
		}
		
		// Walk through and remove any databases that no longer exist.
		for (Database db : RegisteredDatabaseConnections.getAllDatabases()) {
			if (!configDBs.keySet().contains(db.getName())) {
				RegisteredDatabaseConnections.removeDatabase(db.getName());
			}
		}
	}
	
	static private Map<String, Properties> getDatabaseProperties(PerfMonConfiguration config) {
		Map<String, Properties> result = new HashMap<String, Properties>();

		for (String name : config.getAppenderNames()) {
			AppenderID id = config.getAppenderForName(name);
			
			if (id.getClassName().equals(JDBCSQLAppender.class.getName())) {
				result.put(name, id.getAttributes());
			}
		}
		
		return result;
	}
	
	
	static public void addJDBCDatabase(String name, boolean isDefault, String driverClassName, String jarFileName,
	    String jdbcURL, String schema, String userName, String password) throws InvalidConfigException {
		
		Connection conn = null;
		try {
			String databaseIdentity;
			double databaseVersion;

			if (mockIdentityAndVersionForTest) {
				databaseIdentity = Long.toString(System.nanoTime());
				databaseVersion = 1.0;
			} else {
				conn = JDBCHelper.createJDBCConnection(DriverCache.DEFAULT, driverClassName, jarFileName, jdbcURL, userName, password);
				databaseIdentity = JDBCHelper.getDatabaseIdentity(conn, schema);
				databaseVersion = JDBCHelper.getDatabaseVersion(conn, schema);
			}

			Database database = new JDBCDatabase();
			database.setDefault(isDefault);
			database.setId(databaseIdentity);
			database.setName(name);
			database.setSchema(schema);
			database.setDatabaseVersion(databaseVersion);
			
			JDBCDatabase jdbcDatabase = (JDBCDatabase)database;
			jdbcDatabase.setDriverClassName(driverClassName);
			jdbcDatabase.setJarFileName(jarFileName);
			jdbcDatabase.setJdbcURL(jdbcURL);
			jdbcDatabase.setUserName(userName);
			jdbcDatabase.setPassword(password);
			
			synchronized (lockToken) {
				if (getDatabaseByName(name) != null) {
					throw new InvalidConfigException("Unable to register duplicate database name: " + name);
				}
				
				if (getDatabaseByID(databaseIdentity) != null) {
					throw new InvalidConfigException("Unable to register database with duplicate ID: " + databaseIdentity);
				}
				
				if (isDefault) {
					if (defaultDatabase != null) {
						defaultDatabase.setDefault(false);
					}
				} else {
					database.setDefault(defaultDatabase == null);
				}
				
				if (database.isDefault()) {
					defaultDatabase = database;
				}
				
				databaseByID.put(databaseIdentity, database);
				databaseByName.put(name, database);
			} // synchronized
		} catch (SQLException se) { 
			throw new InvalidConfigException("SQLError registering database: " + name, se);
		} finally {
			JDBCHelper.closeNoThrow(conn);
		}
	}
	
	static public void removeDatabase(String name) {
		synchronized (lockToken) {
			Database databaseToRemove = databaseByName.remove(name);
			if (databaseToRemove != null) {
				databaseByID.remove(databaseToRemove.getID());
				if (databaseToRemove.isDefault()) {
					Database[] d = getAllDatabases();
					if (d.length > 0) {
						d[0].setDefault(true);
						defaultDatabase = d[0];
					} else {
						defaultDatabase = null;
					}
				}
			}
		}
	}
	
	static public Database[] getAllDatabases() {
		Database[] result;
		
		synchronized (lockToken) {
			result = databaseByID.values().toArray(new Database[]{});
		}
		
		return result;
	}
	
	static public Database getDatabaseByID(String id) {
		Database result = null;
		
		synchronized (lockToken) {
			result = databaseByID.get(id);
		}
		
		return result;
	}

	static public Database getDatabaseByName(String name) {
		Database result = null;
		
		synchronized (lockToken) {
			result = databaseByName.get(name);
		}
		
		return result;
	}
	
	static public Database getDefaultDatabase() {
		Database result = null;
		
		synchronized (lockToken) {
			result = defaultDatabase;
		}
		
		return result;
	}

	public static abstract class Database {
		private boolean defaultDatabase = false;
		private String id = null;
		private String name = null;
		private String schema = null;
		private double databaseVersion = 0.0;
		
		public abstract Connection openConnection() throws SQLException ; 

		
		public boolean isDefault() {
			return defaultDatabase;
		}
		
		public String getID() {
			return id;
		}
		
		public String getName() {
			return name;
		}

		public String getSchema() {
			return schema;
		}

		public double getDatabaseVersion() {
			return databaseVersion;
		}

		private void setDefault(boolean defaultDatabase) {
			this.defaultDatabase = defaultDatabase;
		}

		private void setId(String id) {
			this.id = id;
		}

		private void setName(String name) {
			this.name = name;
		}

		private void setSchema(String schema) {
			this.schema = schema;
		}

		private void setDatabaseVersion(double databaseVerson) {
			this.databaseVersion = databaseVerson;
		}
		
		abstract String getSignature();
	
	}
	
	private static class JDBCDatabase extends Database {
		private String driverClassName;
		private String jarFileName;
	    private String jdbcURL; 
	    private String userName; 
	    private String password;
	    
		@Override
		public Connection openConnection() throws SQLException {
			return JDBCHelper.createJDBCConnection(DriverCache.DEFAULT, driverClassName, jarFileName, jdbcURL, userName, password);
		}

		private void setDriverClassName(String driverClassName) {
			this.driverClassName = driverClassName;
		}

		private void setJarFileName(String jarFileName) {
			this.jarFileName = jarFileName;
		}

		private void setJdbcURL(String jdbcURL) {
			this.jdbcURL = jdbcURL;
		}

		private void setUserName(String userName) {
			this.userName = userName;
		}

		private void setPassword(String password) {
			this.password = password;
		}

		@Override
		String getSignature() {
			return buildSignature(this.getSchema(), jarFileName, driverClassName, jdbcURL, userName, password);
		}
	}
}
