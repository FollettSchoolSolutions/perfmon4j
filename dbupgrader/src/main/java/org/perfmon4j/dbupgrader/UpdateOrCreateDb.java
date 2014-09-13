package org.perfmon4j.dbupgrader;

import java.sql.Connection;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class UpdateOrCreateDb {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		Parameters params = getParameters(args);
		Connection conn = null;
		try {
			conn = UpgraderUtil.createConnection(params.getDriverClass(), params.getDriverJarFile(), params.getJdbcURL(), params.getUserName(), params.getPassword());
			Liquibase updater = new Liquibase("org/perfmon4j/update-change-master-log.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(conn));
			updater.update((String)null);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeNoThrow(conn);
		}
	}

	static private Parameters getParameters(String args[]) {
		Parameters result = new Parameters();
		
		result.setDriverClass("org.apache.derby.jdbc.EmbeddedDriver");
		result.setJdbcURL("jdbc:derby:memory:derbyDB;create=true");
		
		return result;
	}

	
	public static class Parameters {
		private String userName;
		private String password;
		private String jdbcURL;
		private String driverClass;
		private String driverJarFile;
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
