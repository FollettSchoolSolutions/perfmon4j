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

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
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

	@SnapShotInstanceDefinition
	static public String[] getInstanceNames() throws MalformedObjectNameException, NullPointerException {
		MBeanServer mBeanServer = MiscHelper.findMBeanServer(MiscHelper.isRunningInJBossAppServer() ? "jboss" : null);
		return MiscHelper.getAllObjectName(mBeanServer, new ObjectName(buildBaseObjectName()), "name", true);
	}
	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		return MiscHelper.getInstanceNames(getMBeanServer(), getQueryObjectName(), "name", true);
	}

	@SnapShotGauge
	public long getCurrentThreadsBusy() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadsBusy", true);
	}
	
	@SnapShotGauge
	public long getCurrentThreadCount() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadCount", true);
	}
	
	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
			throws SQLException {
			writeToSQL(conn, schema, (ThreadPoolMonitor)data, systemID);
		}
		
		public void writeToSQL(Connection conn, String schema, ThreadPoolMonitor data, long systemID)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JThreadPoolMonitor " +
				"(SystemID, ThreadPoolOwner, InstanceName, StartTime, EndTime, Duration,  " +
				"CurrentThreadsBusy, CurrentThreadCount) " +
				"VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(SQL);
				
				int index = 1;
				stmt.setLong(index++, systemID);
				stmt.setString(index++, "Apache/Tomcat");
				stmt.setString(index++, data.getInstanceName());
				stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
				stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
				stmt.setLong(index++, data.getDuration());
				stmt.setLong(index++, data.getCurrentThreadsBusy());
				stmt.setLong(index++, data.getCurrentThreadCount());
				
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
