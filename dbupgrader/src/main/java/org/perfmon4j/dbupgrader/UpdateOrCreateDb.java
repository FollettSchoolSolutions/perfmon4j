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

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
					// Install the base change logs...
					Liquibase initializer = new Liquibase("org/perfmon4j/initial-change-log.xml", new ClassLoaderResourceAccessor(), db);
					initializer.changeLogSync((String)null);
					
					boolean hasLegacyVersion2Update = UpdaterUtil.doesColumnExist(conn, schema, "P4JIntervalData", "SQLMaxDuration");
					if (hasLegacyVersion2Update) {
						initializer = new Liquibase("org/perfmon4j/version-2-change-log.xml", new ClassLoaderResourceAccessor(), db);
						initializer.changeLogSync((String)null);

						boolean hasLegacyVersion3Tables = UpdaterUtil.doesTableExist(conn, schema, "P4JSystem");
						if (hasLegacyVersion3Tables) {
							initializer = new Liquibase("org/perfmon4j/version-3-change-log.xml", new ClassLoaderResourceAccessor(), db);
							initializer.changeLogSync((String)null);
						}
					}
				}
				Liquibase updater = new Liquibase("org/perfmon4j/update-change-master-log.xml", new ClassLoaderResourceAccessor(), db);
				updater.setChangeLogParameter("DatabaseIdentifier", UpdaterUtil.generateUniqueIdentity());
				if (params.isClearChecksums()) {
					updater.clearCheckSums();
				}
				
				FileWriter writer = null;
				try {
					if (params.getSqlOutputScript() != null) {
						writer = new FileWriter(new File(params.getSqlOutputScript()));
						updater.update((String)null, writer);
						applyThirdPartyChanges(params, db, writer);
						
					} else {
						updater.update((String)null);
						applyThirdPartyChanges(params, db, null);
					}
				} finally {
					if (writer != null) {
						writer.close();
					}
				}
				System.out.println();
				if (params.getSqlOutputScript() != null) {
					System.out.println("Success: Database has not been upgraded but upgrade script created " + params.getSqlOutputScript());
				} else {
					System.out.println("Success: Database upgraded or installed.");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				System.err.println();
				System.err.println("Failure: Database upgrade or install failed.");
			} finally {
				closeNoThrow(conn);
			}
		} else {
			System.err.println("Usage: java -jar perfmon4j-dbupgrader.jar driverClass=my.jdbc.Driver jdbcURL=jdbc://myurl driverJarFile=./myjdbc.jar");
			System.err.println("[userName=dbuser] [password=dbpassword] [schema=myschema] [sqlOutputScript=./upgrade.sql]");
			System.err.println("\tNote: [] indicates optional parameters.");
			System.err.println("\t      * Providing a sqlOutputScript parameter will create an");
			System.err.println("\t        upgrade script that can be used to manually upgrade");
			System.err.println("\t        the database. The database will not be altered.");
			if (params.insufficentParameters && args.length > 0) {
				System.err.println("\tError: Insufficient parameters");
			}
			for (String param : params.badParameters) {
				System.err.println("\tError: Unexpected parameter: " + param);
			}
		}
	}

	static private void applyThirdPartyChanges(Parameters params, Database db, FileWriter writer) throws Exception {
		for (String s : params.getThirdPartyExtensions()) {
			Liquibase updater = new Liquibase("org/perfmon4j/thirdParty/" + s + "/change-log.xml", new ClassLoaderResourceAccessor(), db);
			if (writer != null) {
				updater.update((String)null, writer);
			} else {
				updater.update((String)null);
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
				} else if ("clearChecksums".equals(split[0])) {
					result.setClearChecksums(split[1]);
				} else if ("sqlOutputScript".equals(split[0])) {
					result.setSqlOutputScript(split[1]);
				} else if ("thirdPartyExtensions".equals(split[0])) {
					result.addThirdPartyExtensions(split[1]);
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
		private String sqlOutputScript;
		private boolean clearChecksums = true;
		private final List<String> badParameters = new ArrayList<String>();
		private final Set<String> thirdPartyExtensions = new HashSet<String>(); 
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
		
		public void setClearChecksums(String value) {
			this.clearChecksums = Boolean.valueOf(value).booleanValue();
		}
		
		public boolean isClearChecksums() {
			return clearChecksums;
		}
		public String getSqlOutputScript() {
			return sqlOutputScript;
		}
		public void setSqlOutputScript(String sqlOutputScript) {
			this.sqlOutputScript = sqlOutputScript;
		}
		
		public String[] getThirdPartyExtensions() {
			return thirdPartyExtensions.toArray(new String[]{}); 
		}
		
		private void addThirdPartyExtensions(String extensionString) {
			if (extensionString.contains(",")) {
				for (String s : extensionString.split(",")) {
					thirdPartyExtensions.add(s);
				}
			} else {
				thirdPartyExtensions.add(extensionString);
			}
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