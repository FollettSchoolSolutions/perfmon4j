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
package org.perfmon4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.ThresholdCalculator;

public class JDBCSQLAppender extends SQLAppender {
	private static final Logger logger = LoggerFactory.initLogger(JDBCSQLAppender.class);
	private Connection conn = null;
	private String driverPath = null;
	private String driverClass = null; 
	private String jdbcURL = null;
	private String userName = null;
	private String password = null;

	public JDBCSQLAppender(AppenderID id) {
		super(id);
	}
	
    public void deInit() {
    	if (conn != null) {
    		logger.logDebug("Closing connection");
    		try {
				conn.close();
				conn = null;
			} catch (SQLException e) {
				logger.logWarn("Unable to close connection", e);
			}
    	}
    	super.deInit();
    }
    
    public synchronized Connection getConnection() throws SQLException {
    	if (conn == null) {
    		try {
    			if (driverClass != null) {
    				ClassLoader loader = null;
    				if (driverPath != null) {
    					File driverFile = new File(driverPath);
    					if (!driverFile.exists()) {
    						throw new SQLException("JDBCDriver file not found in path: " + driverPath);
    					}
    					loader = new URLClassLoader(new URL[]{driverFile.toURL()}, Thread.currentThread().getContextClassLoader());
    				} else {
    					loader = PerfMon.getClassLoader();
    				}
    				Driver driver = (Driver)Class.forName(driverClass, true, loader).newInstance();
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
			} catch (ClassNotFoundException e) {
				throw new SQLException("Unable to load driver class: " + driverClass + ": " + e.getMessage());
			} catch (IllegalAccessException aec) {
				throw new SQLException("Unable to access driver class: " + driverClass + ": " + aec.getMessage());
			} catch (InstantiationException e) {
				throw new SQLException("Unable to instantiate driver class: " + driverClass + ": " + e.getMessage());
			} catch (MalformedURLException e) {
				throw new SQLException("Malformed URL for driver path" + ": " + e.getMessage());
			}
    	}
    	return conn;
    }
    
	protected void resetConnection() {
		JDBCHelper.closeNoThrow(conn);
		conn = null;
	}
    
    public void releaseConnection(Connection conn) {
    	// Only clear the connection in deInit!
    }
    
	public String getDriverClass() {
		return driverClass;
	}

	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	public String getJdbcURL() {
		return jdbcURL;
	}

	public void setJdbcURL(String jdbcURL) {
		this.jdbcURL = jdbcURL;
	}

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
	
	public String getDriverPath() {
		return driverPath;
	}

	public void setDriverPath(String driverPath) {
		this.driverPath = driverPath;
	}
}
