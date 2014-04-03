/*
 *	Copyright 2008 Follett Software Company 
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

package org.perfmon4j.extras.sunjava6;

import java.lang.management.MemoryUsage;

import javax.management.JMException;
import javax.management.openmbean.CompositeData;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.JMXServerWrapper;

@SnapShotProvider(dataInterface=MemoryMonitor.class)
public class MemoryMonitorImpl {
	private final JMXServerWrapper wrapper;

	private static final Long DEFAULT_LONG = Long.valueOf(-1);
	private static final Integer DEFAULT_INT = Integer.valueOf(-1);
	
	public MemoryMonitorImpl() throws JMException {
		wrapper = new JMXServerWrapper();
	}
	
	@SnapShotCounter
	public long getCollectionCount() {
		return wrapper.getNumericAttribute("java.lang:type=GarbageCollector,name=MarkSweepCompact", 
				"CollectionCount", DEFAULT_LONG).longValue();
	}
	
	@SnapShotCounter
	public long getCollectionTimeMillis() {
		return wrapper.getNumericAttribute("java.lang:type=GarbageCollector,name=MarkSweepCompact", 
				"CollectionTime", DEFAULT_LONG).longValue();
	}
	
	@SnapShotGauge
	public int getPendingFinalizationCount() {
		return wrapper.getNumericAttribute("java.lang:type=Memory", 
				"ObjectPendingFinalizationCount", DEFAULT_INT).intValue();
	}
	
	@SnapShotString
	public MemoryUsage getHeapMemoryUsage() {
		return getMemoryUsage("java.lang:type=Memory", "HeapMemoryUsage");
	}

	@SnapShotString
	public MemoryUsage getNonHeapMemoryUsage() {
		return getMemoryUsage("java.lang:type=Memory", "NonHeapMemoryUsage");
	}
	
	@SnapShotString
	public MemoryUsage getCodeCacheUsage() {
		return getMemoryUsage("java.lang:type=MemoryPool,name=Code Cache", "Usage");
	}
	
	@SnapShotString
	public MemoryUsage getEdenSpaceUsage() {
		return getMemoryUsage("java.lang:type=MemoryPool,name=Eden Space", "Usage");
	}
	
	@SnapShotString
	public MemoryUsage getPermGenUsage() {
		return getMemoryUsage("java.lang:type=MemoryPool,name=Perm Gen", "Usage");
	}
	
	@SnapShotString
	public MemoryUsage getSurvivorSpaceUsage() {
		return getMemoryUsage("java.lang:type=MemoryPool,name=Survivor Space", "Usage");
	}
	
	@SnapShotString
	public MemoryUsage getTenuredGenUsage() {
		return getMemoryUsage("java.lang:type=MemoryPool,name=Tenured Gen", "Usage");
	}

	private MemoryUsage getMemoryUsage(String objectName, String attributeName) {
		MemoryUsage result = null;
		
		CompositeData data = (CompositeData)wrapper.getAttribute(objectName, attributeName, 
				CompositeData.class, null);
		if (data != null) {
			result = MemoryUsage.from(data);
		}
		
		return result;
	}
}
