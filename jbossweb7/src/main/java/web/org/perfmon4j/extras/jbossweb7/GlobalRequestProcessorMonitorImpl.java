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

package web.org.perfmon4j.extras.jbossweb7;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
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
		return "jboss.as:subsystem=web";
	}
	
	public GlobalRequestProcessorMonitorImpl() {
		super(buildBaseObjectName(), "connector", null);
	}
	
	public GlobalRequestProcessorMonitorImpl(String instanceName) {
		super(buildBaseObjectName(), "connector", instanceName);
	}
	
	@SnapShotInstanceDefinition
	static public String[] getInstanceNames() throws MalformedObjectNameException, NullPointerException {
		MBeanServer mBeanServer = MiscHelper.findMBeanServer(MiscHelper.isRunningInJBossAppServer() ? "jboss" : null);
		return MiscHelper.getAllObjectName(mBeanServer, new ObjectName(buildBaseObjectName() + ",connector=*"), "connector", true);
	}
	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		return MiscHelper.getInstanceNames(getMBeanServer(), getQueryObjectName(), "connector", true);
	}
	
	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getRequestCount() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "requestCount", true);
	}
	
	@SnapShotCounter(formatter=ByteFormatter.class, 
			preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getBytesSent() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "bytesSent", true);
	}
	
	@SnapShotCounter(formatter=ByteFormatter.class, 
			preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getBytesReceived() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "bytesReceived", true);
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getProcessingTimeMillis() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "processingTime", true);
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getErrorCount() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "errorCount", true);
	}
	
	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
			throws SQLException {
			writeToSQL(conn, schema, (GlobalRequestProcessorMonitor)data, systemID);
		}
		
		public void writeToSQL(Connection conn, String schema, GlobalRequestProcessorMonitor data, long systemID)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JGlobalRequestProcessor " +
	    	" (SystemID, InstanceName, StartTime, EndTime, Duration, " +
	    	" RequestCountInPeriod, RequestCountPerMinute, KBytesSentInPeriod, " +
	    	" KBytesSentPerMinute, KBytesReceivedInPeriod, KBytesReceivedPerMinute, " +
	    	" ProcessingMillisInPeriod, ProcessingMillisPerMinute, ErrorCountInPeriod, " +
	    	" ErrorCountPerMinute) " +				
			" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(SQL);
				
				int index = 1;
				stmt.setLong(index++, systemID);	        	
				stmt.setString(index++, data.getInstanceName());
	        	stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
	        	stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
	        	stmt.setLong(index++, data.getDuration());
	        	stmt.setLong(index++, data.getRequestCount().getDelta());
	        	stmt.setDouble(index++, data.getRequestCount().getDeltaPerMinute());
	        	stmt.setLong(index++, (data.getBytesSent().getDelta() / 1024));
	        	stmt.setDouble(index++, (data.getBytesSent().getDeltaPerMinute() / 1024));
	        	stmt.setLong(index++, (data.getBytesReceived().getDelta() / 1024));
	        	stmt.setDouble(index++, (data.getBytesReceived().getDeltaPerMinute() / 1024));
	        	stmt.setLong(index++, data.getProcessingTimeMillis().getDelta());
	        	stmt.setDouble(index++, data.getProcessingTimeMillis().getDeltaPerMinute());
	        	stmt.setLong(index++, data.getErrorCount().getDelta());
	        	stmt.setDouble(index++, data.getErrorCount().getDeltaPerMinute());
				
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
