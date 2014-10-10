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

import java.sql.Connection;
import java.sql.SQLException;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class JDBCSQLAppender extends SQLAppender {
	private static final Logger logger = LoggerFactory.initLogger(JDBCSQLAppender.class);
	private Connection conn = null;
	private String driverPath = null;
	private String driverClass = null; 
	private String jdbcURL = null;
	private String userName = null;
	private String password = null;
    private Long retryConnectionTime = null;
    private final long RETRY_CONNECTION_TIMEOUT_MINUTES = Long.getLong(this.getClass().getName() +".RETRY_CONNECTION_TIMEOUT_MINUTES", 5);
	private final static JDBCHelper.DriverCache driverCache = new JDBCHelper.DriverCache();

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

    
    private boolean canRetryConnection() {
    	return (retryConnectionTime == null || retryConnectionTime.longValue() <= System.currentTimeMillis());
    }
    
    public synchronized Connection getConnection() throws SQLException {
    	if (conn == null && canRetryConnection()) {
    		try {
        		conn = JDBCHelper.createJDBCConnection(driverCache, this.getDriverClass(), this.getDriverPath(), 
        				this.getJdbcURL(), this.getUserName(), this.getPassword());
        		retryConnectionTime = null;
    		} catch (SQLException ex) {
    			retryConnectionTime = Long.valueOf(System.currentTimeMillis() + (RETRY_CONNECTION_TIMEOUT_MINUTES * 60 * 1000));
    			String logMessage = "Unable to connect to JDBC URL: " + getJdbcURL() + " -- Will try again in " + RETRY_CONNECTION_TIMEOUT_MINUTES + " Minutes";
    			if (LoggerFactory.isDefaultDebugEnabled()) {
    				logger.logWarn(logMessage, ex);
    			} else {
        			logger.logWarn(logMessage);
    			}
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
