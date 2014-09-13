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

class UpgraderUtil {
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
	
	static boolean doesTableExists(Connection conn, String tableName) {
		boolean result = false;
		
		Statement stmt = null;
		ResultSet rs = null;
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
