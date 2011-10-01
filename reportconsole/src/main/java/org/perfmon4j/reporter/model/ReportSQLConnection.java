package org.perfmon4j.reporter.model;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.util.JDBCHelper;


public class ReportSQLConnection extends P4JConnection {
	private final String userName;
	private final String password;
	private final String schema;
	private final String driverClass;
	private final String jarFileName;
	private final String jdbcURL;
	private static final JDBCHelper.DriverCache driverCache = new JDBCHelper.DriverCache();
	
//	private static Map<File, WeakReference<ClassLoader>> cachedJARLoaders = new HashMap<File, WeakReference<ClassLoader>>();
	
	public ReportSQLConnection(String jdbcURL, String userName, String password, String schema, String driverClass, String jarFileName) {
		super(jdbcURL);
		this.userName = userName;
		this.password = password;
		this.schema = schema;
		this.driverClass = driverClass;
		this.jarFileName = jarFileName;
		this.jdbcURL = jdbcURL;
	}
	
	
	public Connection createJDBCConnection() throws SQLException {
		return JDBCHelper.createJDBCConnection(driverCache, driverClass, jarFileName, jdbcURL, userName, password);
	}
	

	public void refresh() throws SQLException {
		Connection conn = createJDBCConnection();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			clearCategories();
			
			IntervalCategory c = new IntervalCategory("Interval Timers", null, this);
			this.addCategory(c);
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT CategoryID, CategoryName FROM P4JCategory");
			while (rs.next()) {
				Long databaseID = new Long(rs.getLong(1));
				String name = rs.getString(2);
				
				IntervalCategory.getOrCreate(c, name, databaseID, this);
			}
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmt);
			JDBCHelper.closeNoThrow(conn);
		}
	}
	
	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getSchema() {
		return schema;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public String getJarFileName() {
		return jarFileName;
	}
}
