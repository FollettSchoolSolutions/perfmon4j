/*
 *	Copyright 2008,2009 Follett Software Company 
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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.GeneratedData;
import org.perfmon4j.util.ByteFormatter;
import org.perfmon4j.util.JDBCHelper;

@SnapShotProvider(type = SnapShotProvider.Type.INSTANCE_PER_MONITOR, 
		dataInterface=JVMSnapShot.JVMData.class,
		sqlWriter=JVMSnapShot.SQLWriter.class
		)
@SnapShotRatios({
		@SnapShotRatio(name="heapMemUsedCommitted", denominator="heapMemCommitted", 
			numerator="heapMemUsed", displayAsPercentage=true),
		@SnapShotRatio(name="heapMemUsedMax", denominator="heapMemMax", 
			numerator="heapMemUsed", displayAsPercentage=true),
		@SnapShotRatio(name="nonHeapMemUsedCommitted", denominator="nonHeapMemCommitted", 
			numerator="nonHeapMemUsed", displayAsPercentage=true),
		@SnapShotRatio(name="nonHeapMemUsedMax", denominator="nonHeapMemMax", 
			numerator="nonHeapMemUsed", displayAsPercentage=true)
})
public class JVMSnapShot {
	public static interface JVMData extends GeneratedData {
		public int getClassesLoaded();
		public Delta getTotalLoadedClassCount();
		public Delta getUnloadedClassCount();
		public Delta getCompilationTime();
		public boolean getCompilationTimeActive();
		public long getHeapMemUsed();
		public long getHeapMemCommitted();
		public long getHeapMemMax();
		public long getNonHeapMemUsed();
		public long getNonHeapMemCommitted();
		public long getNonHeapMemMax();
		public long getPendingFinalization();
		public double getSystemLoadAverage();
		public int getThreadCount();
		public int getDaemonThreadCount();
		public Delta getThreadsStarted();
	}

	private final static int LOOKUP_VM_CACHE_MILLIS = 60000; // 60 Seconds 
	
	private long lastCacheFill = 0;
	private JVMManagementObjects cachedObject = null;

	private JVMManagementObjects getMonitoredBeans() {	
		if (System.currentTimeMillis() > (lastCacheFill + LOOKUP_VM_CACHE_MILLIS)) {
			cachedObject = new JVMManagementObjects(); 
		}
		return cachedObject;
	}
	
	public JVMSnapShot() {
	}
	
	/**
	 * Data from the ClassLoadingMXBean.getLoadedClassCount()
	 * The current number of classes loaded in the JVM
	 */
	@SnapShotGauge()
	public int getClassesLoaded() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.classLoadingMXBean.getLoadedClassCount();
	}
	
	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN) 
	public long getTotalLoadedClassCount() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.classLoadingMXBean.getTotalLoadedClassCount();
	}
	
	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN) 
	public long getUnloadedClassCount() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.classLoadingMXBean.getUnloadedClassCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN) 
	public long getCompilationTime() {
		JVMManagementObjects o = getMonitoredBeans();
		
		// JVM Implementation may not have a compilation bean.
		CompilationMXBean comp = o.compilationMXBean;
		if (comp == null) {
			return 0;
		} else {
			return comp.getTotalCompilationTime();
		}
	}

	@SnapShotGauge() 
	public boolean getCompilationTimeActive() {
		JVMManagementObjects o = getMonitoredBeans();

		// JVM Implementation may not have a compilation bean.
		CompilationMXBean comp = o.compilationMXBean;
		return (comp != null) && comp.isCompilationTimeMonitoringSupported();
	}
	
	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getHeapMemUsed() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.memoryMXBean.getHeapMemoryUsage().getUsed();
	}

	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getHeapMemCommitted() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.memoryMXBean.getHeapMemoryUsage().getCommitted();
	}

	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getHeapMemMax() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.memoryMXBean.getHeapMemoryUsage().getMax();
	}
	
	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getNonHeapMemUsed() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.memoryMXBean.getNonHeapMemoryUsage().getUsed();
	}
	
	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getNonHeapMemCommitted() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.memoryMXBean.getNonHeapMemoryUsage().getCommitted();
	}
	
	@SnapShotGauge(formatter=ByteFormatter.class) 
	public long getNonHeapMemMax() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.memoryMXBean.getNonHeapMemoryUsage().getMax();
	}
	
	@SnapShotGauge() 
	public long getPendingFinalization() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.memoryMXBean.getObjectPendingFinalizationCount();
	}
	
	@SnapShotGauge() 
	public double getSystemLoadAverage() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.operatingSystemMXBean.getSystemLoadAverage();
	}
	
	@SnapShotGauge() 
	public int getThreadCount() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.threadMXBean.getThreadCount();
	}
	
	@SnapShotGauge() 
	public int getDaemonThreadCount() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.threadMXBean.getDaemonThreadCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN) 
	public long getThreadsStarted() {
		JVMManagementObjects o = getMonitoredBeans();
		return o.threadMXBean.getTotalStartedThreadCount();
	}
	
	private static class JVMManagementObjects {
		private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean(); 
		private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean(); 
		private final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
		private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
		private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	}
	
	private static JVMManagementObjects getJVMManagementObjects() {
		return new JVMManagementObjects();
	}

	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
			throws SQLException {
			writeToSQL(conn, schema, (JVMData)data, systemID);
		}
		
		public void writeToSQL(Connection conn, String schema, JVMData data, long systemID)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JVMSnapShot " +
				"(SystemID, StartTime, EndTime, Duration, CurrentClassLoadCount, " +
		    	" ClassLoadCountInPeriod, ClassLoadCountPerMinute, ClassUnloadCountInPeriod, " +
		    	" ClassUnloadCountPerMinute, PendingClassFinalizationCount, " +  
		    	" CurrentThreadCount, CurrentDaemonThreadCount, " +
		    	" ThreadStartCountInPeriod, ThreadStartCountPerMinute, " +
		    	" HeapMemUsedMB,  HeapMemCommitedMB,  HeapMemMaxMB, " +
		    	" NonHeapMemUsedMB, NonHeapMemCommittedUsedMB, NonHeapMemMaxUsedMB, " +  
		    	" SystemLoadAverage, CompilationMillisInPeriod, CompilationMillisPerMinute) " +
				" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(SQL);
				
				int index = 1;
	        	stmt.setLong(index++, systemID);
	        	stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
	        	stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
	        	stmt.setLong(index++, data.getDuration());
	        	stmt.setInt(index++, data.getClassesLoaded());
	        	stmt.setLong(index++, data.getTotalLoadedClassCount().getDelta());
	        	stmt.setDouble(index++, data.getTotalLoadedClassCount().getDeltaPerMinute());
	        	stmt.setLong(index++, data.getUnloadedClassCount().getDelta());
	        	stmt.setDouble(index++, data.getUnloadedClassCount().getDeltaPerMinute());
	        	stmt.setLong(index++, data.getPendingFinalization());
	        	stmt.setLong(index++, data.getThreadCount());
	        	stmt.setLong(index++, data.getDaemonThreadCount());
	        	stmt.setLong(index++, data.getThreadsStarted().getDelta());
	        	stmt.setDouble(index++, data.getThreadsStarted().getDeltaPerMinute());
	        	stmt.setDouble(index++, data.getHeapMemUsed() / (double)1024);
	        	stmt.setDouble(index++, data.getHeapMemCommitted() / (double)1024);
	        	stmt.setDouble(index++, data.getHeapMemMax() / (double)1024);
	        	stmt.setDouble(index++, data.getNonHeapMemUsed() / (double)1024);
	        	stmt.setDouble(index++, data.getNonHeapMemCommitted() / (double)1024);
	        	stmt.setDouble(index++, data.getNonHeapMemMax() / (double)1024);
	        	
	        	if (data.getSystemLoadAverage() < 0) {
		        	stmt.setNull(index++, Types.DECIMAL);
	        	} else {
	        		stmt.setDouble(index++, data.getSystemLoadAverage());
	        	}
	        	
	        	if (data.getCompilationTimeActive()) {
		        	stmt.setLong(index++, data.getCompilationTime().getDelta());
		        	stmt.setDouble(index++, data.getCompilationTime().getDeltaPerMinute());
	        	} else {
		        	stmt.setNull(index++, Types.INTEGER);
		        	stmt.setNull(index++, Types.DECIMAL);
	        	}
				
				int count = stmt.executeUpdate();
				if (count != 1) {
					throw new SQLException("JVMSnapShot failed to insert row");
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
//    	
//        PerfMonConfiguration config = new PerfMonConfiguration();
//        config.defineAppender("SimpleAppender", TextAppender.class.getName(), "2 seconds");
//
//        config.defineSnapShotMonitor("JVM Monitor", org.perfmon4j.java.management.JVMSnapShot.class.getName());
//        config.attachAppenderToSnapShotMonitor("JVM Monitor", "SimpleAppender");
//        
//        
//        PerfMon.configure(config);
//        System.out.println("Sleeping for 5 seconds -- Will take a JVM SnapShot every 2 second");
//        Thread.sleep(5000);
//        System.out.println("DONE");
//    }
}
