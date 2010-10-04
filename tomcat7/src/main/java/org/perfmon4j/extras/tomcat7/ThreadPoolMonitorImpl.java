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

package org.perfmon4j.extras.tomcat7;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR, 
		dataInterface=ThreadPoolMonitor.class,
		sqlWriter=ThreadPoolMonitorImpl.SQLWriter.class)
public class ThreadPoolMonitorImpl extends JMXMonitorBase {
	final private static Logger logger = LoggerFactory.initLogger(ThreadPoolMonitorImpl.class);
	
	private static String buildBaseObjectName() {
		String result = "Catalina:type=ThreadPool";
		if (MiscHelper.isRunningInJBossAppServer()) {
			result = "jboss.web:type=ThreadPool";
		}
		return result;
	}
	
	public ThreadPoolMonitorImpl() {
		super(buildBaseObjectName(), "name", null);
	}
	
	public ThreadPoolMonitorImpl(String instanceName) {
		super(buildBaseObjectName(), "name", instanceName);
	}
	
	@SnapShotString
	public String getInstanceName() {
		return MiscHelper.getInstanceNames(getMBeanServer(), getQueryObjectName(), "name");
	}

	@SnapShotGauge
	public long getCurrentThreadsBusy() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadsBusy");
	}
	
	@SnapShotGauge
	public long getCurrentThreadCount() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadCount");
	}
	
	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data)
			throws SQLException {
			writeToSQL(conn, schema, (ThreadPoolMonitor)data);
		}
		
		public void writeToSQL(Connection conn, String schema, ThreadPoolMonitor data)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JThreadPoolMonitor " +
				"(ThreadPoolOwner, InstanceName, StartTime, EndTime, Duration,  " +
				"CurrentThreadsBusy, CurrentThreadCount) " +
				"VALUES(?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(SQL);
				stmt.setString(1, "Apache/Tomcat");
				stmt.setString(2, data.getInstanceName());
				stmt.setTimestamp(3, new Timestamp(data.getStartTime()));
				stmt.setTimestamp(4, new Timestamp(data.getEndTime()));
				stmt.setLong(5, data.getDuration());
				stmt.setLong(6, data.getCurrentThreadsBusy());
				stmt.setLong(7, data.getCurrentThreadCount());
				
				int count = stmt.executeUpdate();
				if (count != 1) {
					throw new SQLException("ThreadPoolMonitor failed to insert row");
				}
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		}
	}
}
