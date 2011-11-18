/*
 *	Copyright 2008,2009, 2010 Follett Software Company 
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
package org.perfmon4j.java.management;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.snapshot.GeneratedData;
import org.perfmon4j.instrument.snapshot.Ratio;
import org.perfmon4j.util.ByteFormatter;
import org.perfmon4j.util.JDBCHelper;

@SnapShotProvider(type = SnapShotProvider.Type.FACTORY, 
		dataInterface=MemoryPoolSnapShot.MemoryPoolData.class,
		sqlWriter=MemoryPoolSnapShot.SQLWriter.class)
@SnapShotRatios({
		@SnapShotRatio(name="usedCommittedRatio", denominator="Committed", 
			numerator="Used", displayAsPercentage=true),
		@SnapShotRatio(name="usedMaxRatio", denominator="Max", 
			numerator="Used", displayAsPercentage=true)
})
public class MemoryPoolSnapShot {
	public static interface MemoryPoolData extends GeneratedData {
		public String getInstanceName();
		public long getInit();
		public long getMax();
		public long getCommitted();
		public long getUsed();
		public String getType();
		
		public Ratio getUsedCommittedRatio();
		public Ratio getUsedMaxRatio();
	}
	
	public static MemoryPoolSnapShot getInstance() {
		return new MemoryPoolSnapShot();
	}
	
	public static MemoryPoolSnapShot getInstance(String instanceName) {
		return new MemoryPoolSnapShot(instanceName);
	}
	
	private final static int LOOKUP_MEMORY_POOL_CACHE_MILLIS = 60000; // 60 Seconds 
	
	private final String monitorName; // null = composite data containing all active monitors.
	private long lastCacheFill = 0;
	private MemoryPoolMXBean[] cachedBeans = null;

	private MemoryPoolMXBean[] getMonitoredBeans() {	
		if (System.currentTimeMillis() > (lastCacheFill + LOOKUP_MEMORY_POOL_CACHE_MILLIS)) {
			if (monitorName == null) {
				cachedBeans = getAllMemoryPools();
			} else {
				MemoryPoolMXBean bean = getMemoryPool(monitorName);
				if (bean != null) {
					cachedBeans = new MemoryPoolMXBean[]{bean};
				} else {
					cachedBeans = new MemoryPoolMXBean[]{};
				}
			}
			lastCacheFill = System.currentTimeMillis();
		}
		return cachedBeans;
	}
	
	public MemoryPoolSnapShot() {
		this(null);
	}
	
	public MemoryPoolSnapShot(String monitorName) {
		this.monitorName = monitorName;
	}
	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		String result = monitorName;
		
		MemoryPoolMXBean beans[] = getMonitoredBeans();
		if (beans.length == 0) {
			result += " (MemoryPool NOT registered)";
		} else if (monitorName == null) {
				result = "Composite(";
				for (int i = 0; i < beans.length; i++) {
					if (i > 0) {
						result += ", ";
					}
					result += "\"" + beans[i].getName() + "\"";
				}
				result += ")";
		}
		return result;
	}

	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getInit() {
		long result = 0;
		MemoryPoolMXBean beans[] = getMonitoredBeans();
		for (int i = 0; i < beans.length; i++) {
			result += beans[i].getUsage().getInit();
		}
		return result;
	}
	
	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getMax() {
		long result = 0;
		MemoryPoolMXBean beans[] = getMonitoredBeans();
		for (int i = 0; i < beans.length; i++) {
			result += beans[i].getUsage().getMax();
		}
		return result;
	}

	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getCommitted() {
		long result = 0;
		MemoryPoolMXBean beans[] = getMonitoredBeans();
		for (int i = 0; i < beans.length; i++) {
			result += beans[i].getUsage().getCommitted();
		}
		return result;
	}

	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getUsed() {
		long result = 0;
		MemoryPoolMXBean beans[] = getMonitoredBeans();
		for (int i = 0; i < beans.length; i++) {
			result += beans[i].getUsage().getUsed();
		}
		return result;
	}
	
	@SnapShotString 
	public String getType() {
		String result = "";
		MemoryPoolMXBean beans[] = getMonitoredBeans();
		if (beans.length > 1) {
			result = "(N/A)";
		} else if (beans.length > 0){
			result = beans[0].getType().toString();
		}
		
		return result;
	}
	
	@SnapShotInstanceDefinition
	static public String[] getInstanceNames() {
		List<String> result = new ArrayList<String>();

		MemoryPoolMXBean[] beans = getAllMemoryPools();
		for (int i = 0; i < beans.length; i++) {
			result.add(beans[i].getName());
		}
		
		return result.toArray(new String[result.size()]);
	}

	
	/** Package level for testing **/
	static MemoryPoolMXBean[] getAllMemoryPools() {
		ManagementFactory.getMemoryPoolMXBeans();
		
		List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
		return beans.toArray(new MemoryPoolMXBean[]{});
	}

	/** Package level for testing **/
	static MemoryPoolMXBean getMemoryPool(String name) {
		MemoryPoolMXBean result = null;
		
		MemoryPoolMXBean beans[] = getAllMemoryPools();
		for (int i = 0; i < beans.length; i++) {
			if (name.equals(beans[i].getName())) {
				result = beans[i];
			}
		}
		return result;
	}

	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data)
			throws SQLException {
			writeToSQL(conn, schema, (MemoryPoolData)data);
		}

		public void writeToSQL(Connection conn, String schema, MemoryPoolData data)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JMemoryPool " +
				"(InstanceName, StartTime, EndTime, Duration, InitialMB, " +
				"UsedMB, CommittedMB, MaxMB, MemoryType) " +
				"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(SQL);
				stmt.setString(1, data.getInstanceName());
				stmt.setTimestamp(2, new Timestamp(data.getStartTime()));
				stmt.setTimestamp(3, new Timestamp(data.getEndTime()));
				stmt.setLong(4, data.getDuration());
				stmt.setLong(5, data.getInit()/1024);
				stmt.setLong(6, data.getUsed()/1024);
				stmt.setDouble(7, data.getCommitted()/1024);
				stmt.setDouble(8, data.getMax()/1024);
				stmt.setString(9, data.getType());
				
				int count = stmt.executeUpdate();
				if (count != 1) {
					throw new SQLException("MemoryPoolSnapShot failed to insert row");
				}
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		}
	}
	
//    public static void main(String args[]) throws Exception {
//    	System.setProperty("PERFMON_APPENDER_ASYNC_TIMER_MILLIS", "500");
//    	
//    	BasicConfigurator.configure();
//        Logger.getRootLogger().setLevel(Level.INFO);
//        Logger.getLogger("org.perfmon4j").setLevel(Level.DEBUG);
//    	
//        PerfMonConfiguration config = new PerfMonConfiguration();
//        config.defineAppender("SimpleAppender", TextAppender.class.getName(), "2 seconds");
//
//        Properties propsEden = new Properties();
//        propsEden.setProperty("instanceName", "PS Eden Space");
//        
//        config.defineSnapShotMonitor("MemoryPool Monitor (Eden)", 
//        		org.perfmon4j.java.management.MemoryPoolSnapShot.class.getName(),
//        		propsEden);
//        config.attachAppenderToSnapShotMonitor("MemoryPool Monitor (Eden)", "SimpleAppender");
//        
//        config.defineSnapShotMonitor("MemoryPool Monitor (ALL)", 
//        		org.perfmon4j.java.management.MemoryPoolSnapShot.class.getName());
//        config.attachAppenderToSnapShotMonitor("MemoryPool Monitor (ALL)", "SimpleAppender");
//   
//        
//        Properties propsCodeCache = new Properties();
//        propsCodeCache.setProperty("instanceName", "Code Cache");
//        config.defineSnapShotMonitor("MemoryPool Monitor (CodeCache)", 
//        		org.perfmon4j.java.management.MemoryPoolSnapShot.class.getName(),
//        		propsCodeCache);
//        config.attachAppenderToSnapShotMonitor("MemoryPool Monitor (CodeCache)", "SimpleAppender");
//        
//       
//        PerfMon.configure(config);
//        System.out.println("Sleeping for 5 seconds -- Will take a MemorySnapShot every 2 second");
//        Thread.sleep(5000);
//        System.out.println("DONE");
//    }
}


