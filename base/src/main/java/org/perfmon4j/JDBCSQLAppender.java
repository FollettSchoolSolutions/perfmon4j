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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.ThresholdCalculator;

public class JDBCSQLAppender extends SQLAppender {
	private static final Logger logger = LoggerFactory.initLogger(JDBCSQLAppender.class);
	private Connection conn = null;
	private String driverClass = null; 
	private String jdbcURL = null;
	private String userName = null;
	private String password = null;
	
	public JDBCSQLAppender(AppenderID id) {
		super(id);
	}
	
	public JDBCSQLAppender(long intervalMillis) {
		this(getAppenderID(intervalMillis));
	}
	
    public static AppenderID getAppenderID(long intervalMillis) {
        return Appender.getAppenderID(JDBCSQLAppender.class.getName(), intervalMillis);
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
    
    protected synchronized Connection getConnection() throws SQLException {
    	if (conn == null) {
    		try {
    			if (driverClass != null) {
    				Driver driver = (Driver)Class.forName(driverClass, true, PerfMon.getClassLoader()).newInstance();
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
				throw new SQLException("Unable to load driver class: " + driverClass, e);
			} catch (IllegalAccessException aec) {
				throw new SQLException("Unable to access driver class: " + driverClass, aec);
			} catch (InstantiationException e) {
				throw new SQLException("Unable to instantiate driver class: " + driverClass, e);
			}
    	}
    	return conn;
    }
    
	protected void resetConnection() {
		JDBCHelper.closeNoThrow(conn);
		conn = null;
	}
    
    protected void releaseConnection(Connection conn) {
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
	
	public static void main(String args[]) {
		JDBCSQLAppender appender = new JDBCSQLAppender(1000);
		appender.setDriverClass("net.sourceforge.jtds.jdbc.Driver");
		appender.setJdbcURL("jdbc:jtds:sqlserver:/localhost/perfmon4j");
		appender.setUserName("sa");
		appender.setPassword("stuffy");

		long now = System.currentTimeMillis();
		IntervalData d = new IntervalData(PerfMon.getMonitor("Xdave"), now, new MedianCalculator(),
				new ThresholdCalculator(new long[]{10, 100, 500}));
		d.start(0, now);
		d.stop(100, 10000, now + 500);
		d.setTimeStop(now + 1000);
		
		appender.outputData(d);
	}
}
