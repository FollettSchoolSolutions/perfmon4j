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

package org.perfmon4j.restdatasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.TestCase;

import org.perfmon4j.dbupgrader.UpdateOrCreateDb;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.JDBCHelper.DriverCache;


public class BaseDatabaseTest extends TestCase {
//	public static final String SCHEMA = "TEST";
	public static final String JDBC_URL = "jdbc:derby:memory:mydb"; 
	public static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private Connection connection = null;
	
	private static final DriverCache driverCache = new DriverCache();
	
	public BaseDatabaseTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		connection = JDBCHelper.createJDBCConnection(driverCache, JDBC_DRIVER, null, JDBC_URL + ";create=true", null, null);
		connection.setAutoCommit(true);		
		
		// Start with an empty database...
		UpdateOrCreateDb.main(new String[]{"driverClass=org.apache.derby.jdbc.EmbeddedDriver",
				"jdbcURL=" + JDBC_URL,
				"driverJarFile=EMBEDDED"
		});
		
//		new dblook(new String[]{"-d", JDBC_URL, "-verbose"});
		
	}

	protected void tearDown() throws Exception {
		JDBCHelper.closeNoThrow(connection);
		
		try {
			JDBCHelper.createJDBCConnection(driverCache, JDBC_DRIVER, null, JDBC_URL + ";drop=true", null, null);
		} catch (SQLException sn) {
		}
		
		super.tearDown();
	}

	private long getID(Statement stmt) throws SQLException {
		ResultSet rs = null;
		try {
			rs = stmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new SQLException("Expected a generated key");
			} else {
				return rs.getLong(1);
			}
			
		} finally {
			JDBCHelper.closeNoThrow(rs);
		}
	}
	
	
	protected long addSystem(String systemName) throws SQLException {
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			stmt.executeUpdate("INSERT INTO P4JSystem (SystemName) VALUES('" + systemName + "')", Statement.RETURN_GENERATED_KEYS);
			return getID(stmt);
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
	}
	
	public Connection getConnection() {
		return connection;
	}
}
