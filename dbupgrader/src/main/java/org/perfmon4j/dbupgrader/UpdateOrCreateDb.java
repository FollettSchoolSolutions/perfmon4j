/*
 *	Copyright 2008-2014 Follett School Solutions 
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

package org.perfmon4j.dbupgrader;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class UpdateOrCreateDb {
	private static final String EMBEDDED_JAR_FILE = "EMBEDDED";
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		Parameters params = getParameters(args);
		if (params.isValid()) {
			Connection conn = null;
			try {
				// Embedded indicates jdbcDriver can be found within the application classpath.
				if (EMBEDDED_JAR_FILE.equals(params.getDriverJarFile())) {
					params.setDriverJarFile(null);
				}
				conn = UpdaterUtil.createConnection(params.getDriverClass(), params.getDriverJarFile(), params.getJdbcURL(), params.getUserName(), params.getPassword());
				Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
				String schema = params.getSchema();
				if (schema != null) {
					db.setDefaultSchemaName(schema);
				}
				
				boolean hasLiquibaseChangeLogs =  UpdaterUtil.doesTableExist(conn, schema, "DATABASECHANGELOG"); 
				boolean hasPerfmon4jTables = UpdaterUtil.doesTableExist(conn, schema, "P4JIntervalData");
				if (hasPerfmon4jTables && !hasLiquibaseChangeLogs) {
					// Install the change logs..
				
					Liquibase initializer = new Liquibase("org/perfmon4j/initial-change-log.xml", new ClassLoaderResourceAccessor(), db);
					initializer.changeLogSync((String)null);
				}
				Liquibase updater = new Liquibase("org/perfmon4j/update-change-master-log.xml", new ClassLoaderResourceAccessor(), db);
				updater.update((String)null);
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				closeNoThrow(conn);
			}
		} else {
			System.err.println("Usage: UpdateOrCreateDb driverClass=my.jdbc.Driver jdbcURL=jdbc://myurl driverJarFile=./myjdbc.jar userName=[dbuser] password=[dbpassword] schema=[myschema]");
			if (params.insufficentParameters && args.length > 1) {
				System.err.println("\tError: Insufficient parameters");
			}
			for (String param : params.badParameters) {
				System.err.println("\tError: Unexpected parameter: " + param);
			}
		}
	}
	
	 
	static private String[] splitArg(String arg) {
		String result[] = arg.split("=", 2);
		if (result.length != 2) {
			result = null;
		}
		return result;
	}
	

	static Parameters getParameters(String args[]) {
		Parameters result = new Parameters();
		
		for (String arg : args) {
			boolean badArg = false;
			String split[] = splitArg(arg);
			if (split == null) {
				badArg = true;
			} else {
				if ("userName".equals(split[0])) {
					result.setUserName(split[1]);
				} else if ("driverClass".equals(split[0])) {
					result.setDriverClass(split[1]);
				} else if ("driverJarFile".equals(split[0])) {
					result.setDriverJarFile(split[1]);
				} else if ("jdbcURL".equals(split[0])) {
					result.setJdbcURL(split[1]);
				} else if ("password".equals(split[0])) {
					result.setPassword(split[1]);
				} else if ("schema".equals(split[0])) {
					result.setSchema(split[1]);
				} else {
					badArg = true;
				}
			}
			if (badArg) {
				result.addBadParameter(arg);
			}
		}
		
		if (result.getDriverClass() == null ||
				result.getJdbcURL() == null ||
				result.getDriverJarFile() == null) {
			result.setInsufficentParameters(true);
		}
		
		// Both userName and password are optional, however if we have
		// a password we must have a userName.
		if (result.getPassword() != null && result.getUserName() == null) {
			result.setInsufficentParameters(true);
		}
		
		return result;
	}

	
	public static class Parameters {
		private String userName;
		private String password;
		private String jdbcURL;
		private String driverClass;
		private String driverJarFile;
		private String schema;
		private final List<String> badParameters = new ArrayList<String>();
		private boolean insufficentParameters = false;
	
		public String getUserName() {
			return userName;
		}
		public void setUserName(String userName) {
			this.userName = userName;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getJdbcURL() {
			return jdbcURL;
		}
		public void setJdbcURL(String jdbcURL) {
			this.jdbcURL = jdbcURL;
		}
		public String getDriverClass() {
			return driverClass;
		}
		public void setDriverClass(String driverClass) {
			this.driverClass = driverClass;
		}
		public String getDriverJarFile() {
			return driverJarFile;
		}
		public void setDriverJarFile(String driverJarFile) {
			this.driverJarFile = driverJarFile;
		}
		
		public void addBadParameter(String parameter) {
			badParameters.add(parameter);
		}
		public boolean isInsufficentParameters() {
			return insufficentParameters;
		}
		public void setInsufficentParameters(boolean insufficentParameters) {
			this.insufficentParameters = insufficentParameters;
		}
		public List<String> getBadParameters() {
			return badParameters;
		}
		
		public boolean isValid() {
			return !insufficentParameters && badParameters.isEmpty();
		}
		public String getSchema() {
			return schema;
		}
		public void setSchema(String schema) {
			this.schema = schema;
		}
	}
	
	static void closeNoThrow(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}