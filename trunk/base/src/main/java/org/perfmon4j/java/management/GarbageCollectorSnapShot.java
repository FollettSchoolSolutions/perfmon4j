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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.GeneratedData;
import org.perfmon4j.util.JDBCHelper;

@SnapShotProvider(type = SnapShotProvider.Type.FACTORY, 
		dataInterface=GarbageCollectorSnapShot.GarbageCollectorData.class,
		sqlWriter=GarbageCollectorSnapShot.SQLWriter.class)
public class GarbageCollectorSnapShot {
	
	public static interface GarbageCollectorData extends GeneratedData {
		public String getInstanceName();
		public Delta getCollectionCount();
		public Delta getCollectionTime();
	}
	
	public static GarbageCollectorSnapShot getInstance() {
		return new GarbageCollectorSnapShot();
	}
	
	public static GarbageCollectorSnapShot getInstance(String instanceName) {
		return new GarbageCollectorSnapShot(instanceName);
	}
	
	private final static int LOOKUP_GARBAGE_COLLECTOR_CACHE_MILLIS = 60000; // 60 Seconds 
	
	private final String monitorName; // null = composite data containing all active monitors.
	private long lastCacheFill = 0;
	private GarbageCollectorMXBean[] cachedBeans = null;

	private GarbageCollectorMXBean[] getMonitoredBeans() {	
		if (System.currentTimeMillis() > (lastCacheFill + LOOKUP_GARBAGE_COLLECTOR_CACHE_MILLIS)) {
			if (monitorName == null) {
				cachedBeans = getAllGarbageCollectors();
			} else {
				GarbageCollectorMXBean bean = getGarbageCollector(monitorName);
				if (bean != null) {
					cachedBeans = new GarbageCollectorMXBean[]{bean};
				} else {
					cachedBeans = new GarbageCollectorMXBean[]{};
				}
			}
			lastCacheFill = System.currentTimeMillis();
		}
		return cachedBeans;
	}
	
	public GarbageCollectorSnapShot() {
		this(null);
	}
	
	public GarbageCollectorSnapShot(String monitorName) {
		this.monitorName = monitorName;
	}
	
	
	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		String result = monitorName;
		
		GarbageCollectorMXBean beans[] = getMonitoredBeans();
		if (beans.length == 0) {
			result += " (Garbage Collector NOT registered)";
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

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN) 
	public long getCollectionCount() {
		long result = 0;
		GarbageCollectorMXBean beans[] = getMonitoredBeans();
		for (int i = 0; i < beans.length; i++) {
			result += beans[i].getCollectionCount();
		}
		return result;
	}
	
	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN) 
	public long getCollectionTime() {
		long result = 0;
		GarbageCollectorMXBean beans[] = getMonitoredBeans();
		for (int i = 0; i < beans.length; i++) {
			result += beans[i].getCollectionTime();
		}
		return result;
	}

	@SnapShotInstanceDefinition
	static public String[] getInstanceNames() {
		List<String> result = new ArrayList<String>();

		GarbageCollectorMXBean[] beans = getAllGarbageCollectors();
		for (int i = 0; i < beans.length; i++) {
			result.add(beans[i].getName());
		}
		
		return result.toArray(new String[result.size()]);
	}
	
	/** Package level for testing **/
	static GarbageCollectorMXBean[] getAllGarbageCollectors() {
		List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
		return beans.toArray(new GarbageCollectorMXBean[]{});
	}

	/** Package level for testing **/
	static GarbageCollectorMXBean getGarbageCollector(String name) {
		GarbageCollectorMXBean result = null;
		
		GarbageCollectorMXBean beans[] = getAllGarbageCollectors();
		for (int i = 0; i < beans.length; i++) {
			if (name.equals(beans[i].getName())) {
				result = beans[i];
			}
		}
		return result;
	}
	
	
	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
			throws SQLException {
			writeToSQL(conn, schema, (GarbageCollectorData)data, systemID);
		}
		
		public void writeToSQL(Connection conn, String schema, GarbageCollectorData data, long systemID)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JGarbageCollection " +
				"(SystemID, InstanceName, StartTime, EndTime, Duration, NumCollections, " +
				"CollectionMillis, NumCollectionsPerMinute, CollectionMillisPerMinute) " +
				"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(SQL);
				int index = 1;
				
				stmt.setLong(index++, systemID);
				stmt.setString(index++, data.getInstanceName());
				stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
				stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
				stmt.setLong(index++, data.getDuration());
				stmt.setLong(index++, data.getCollectionCount().getDelta());
				stmt.setLong(index++, data.getCollectionTime().getDelta());
				stmt.setDouble(index++, data.getCollectionCount().getDeltaPerMinute());
				stmt.setDouble(index++, data.getCollectionTime().getDeltaPerMinute());
				
				int count = stmt.executeUpdate();
				if (count != 1) {
					throw new SQLException("GarbageCollectorSnapShot failed to insert row");
				}
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		}
	}
}
