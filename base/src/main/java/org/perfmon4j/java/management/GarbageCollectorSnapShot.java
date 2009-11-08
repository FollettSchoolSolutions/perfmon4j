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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j.java.management;

import java.lang.management.ManagementFactory;
import java.util.List;

import java.lang.management.GarbageCollectorMXBean;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.snapshot.Delta;

@SnapShotProvider(type = SnapShotProvider.Type.FACTORY, 
		dataInterface=GarbageCollectorSnapShot.GarbageCollectorData.class)
public class GarbageCollectorSnapShot {
	public static interface GarbageCollectorData {
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
	
	@SnapShotString()
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
}
