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

package org.perfmon4j.extras.tomcat55;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.ByteFormatter;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR,
		dataInterface=GlobalRequestProcessorMonitor.class,
		sqlWriter=GlobalRequestProcessorMonitorImpl.SQLWriter.class)
public class GlobalRequestProcessorMonitorImpl extends JMXMonitorBase {
	final private static Logger logger = LoggerFactory.initLogger(GlobalRequestProcessorMonitorImpl.class);
	
	private static String buildBaseObjectName() {
		String result = "Catalina:type=GlobalRequestProcessor";
		if (MiscHelper.isRunningInJBossAppServer()) {
			result = "jboss.web:type=GlobalRequestProcessor";
		}
		return result;
	}
	
	public GlobalRequestProcessorMonitorImpl() {
		super(buildBaseObjectName(), "name", null);
	}
	
	public GlobalRequestProcessorMonitorImpl(String instanceName) {
		super(buildBaseObjectName(), "name", instanceName);
	}
	
	@SnapShotString
	public String getInstanceName() {
		return MiscHelper.getInstanceNames(getMBeanServer(), getQueryObjectName(), "name");
	}
	
	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getRequestCount() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "requestCount");
	}
	
	@SnapShotCounter(formatter=ByteFormatter.class, 
			preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getBytesSent() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "bytesSent");
	}
	
	@SnapShotCounter(formatter=ByteFormatter.class, 
			preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getBytesReceived() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "bytesReceived");
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getProcessingTimeMillis() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "processingTime");
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getErrorCount() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "errorCount");
	}
	
	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data)
			throws SQLException {
			writeToSQL(conn, schema, (GlobalRequestProcessorMonitor)data);
		}
		
		public void writeToSQL(Connection conn, String schema, GlobalRequestProcessorMonitor data)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JGlobalRequestProcessor " +
	    	" (InstanceName, StartTime, EndTime, Duration, " +
	    	" RequestCountInPeriod, RequestCountPerMinute, MBytesSentInPeriod, " +
	    	" MBytesSentPerMinute, MBytesReceivedInPeriod, MBytesReceivedPerMinute, " +
	    	" ProcessingMillisInPeriod, ProcessingMillisPerMinute, ErrorCountInPeriod, " +
	    	" ErrorCountPerMinute) " +				
			" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(SQL);
	        	stmt.setString(1, data.getInstanceName());
	        	stmt.setTimestamp(2, new Timestamp(data.getStartTime()));
	        	stmt.setTimestamp(3, new Timestamp(data.getEndTime()));
	        	stmt.setLong(4, data.getDuration());
	        	stmt.setLong(5, data.getRequestCount().getDelta());
	        	stmt.setDouble(6, data.getRequestCount().getDeltaPerMinute());
	        	stmt.setLong(7, (data.getBytesSent().getDelta() / 1024));
	        	stmt.setDouble(8, (data.getBytesSent().getDeltaPerMinute() / 1024));
	        	stmt.setLong(9, (data.getBytesReceived().getDelta() / 1024));
	        	stmt.setDouble(10, (data.getBytesReceived().getDeltaPerMinute() / 1024));
	        	stmt.setLong(11, data.getProcessingTimeMillis().getDelta());
	        	stmt.setDouble(12, data.getProcessingTimeMillis().getDeltaPerMinute());
	        	stmt.setLong(13, data.getErrorCount().getDelta());
	        	stmt.setDouble(14, data.getErrorCount().getDeltaPerMinute());
				
				int count = stmt.executeUpdate();
				if (count != 1) {
					throw new SQLException("GlobalRequestProcessor failed to insert row");
				}
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		}
	}
}
