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

import javax.management.JMException;

import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.util.ByteFormatter;
import org.perfmon4j.util.JMXServerWrapper;

@SnapShotProvider(dataInterface=OperatingSystemMonitor.class, 
		type = SnapShotProvider.Type.INSTANCE_PER_MONITOR) // MUST be instance per monitor! Member data required for CPU calculation.
public class OperatingSystemMonitorImpl {
	private final JMXServerWrapper wrapper;
	
	private final String JMX_OBJECT_NAME = "java.lang:type=OperatingSystem";

	private static final Long DEFAULT_LONG = Long.valueOf(-1);
	private static final Integer DEFAULT_INT = Integer.valueOf(-1);
	
	private Long lastCpuCheckMillis = null;
	private Long lastProcessCpuTime = null;
	
	public OperatingSystemMonitorImpl() throws JMException {
		wrapper = new JMXServerWrapper();
	}
	
	@SnapShotGauge(formatter=ByteFormatter.class)
	public long getCommittedVirtualMemorySize() {
		return wrapper.getNumericAttribute(JMX_OBJECT_NAME, 
				"CommittedVirtualMemorySize", DEFAULT_LONG).longValue();
	}
	
	@SnapShotGauge(formatter=ByteFormatter.class)
	public long getFreePhysicalMemorySize() {
		return wrapper.getNumericAttribute(JMX_OBJECT_NAME, 
				"FreePhysicalMemorySize", DEFAULT_LONG).longValue();
	}
	
	@SnapShotGauge(formatter=ByteFormatter.class)
	public long getFreeSwapSpaceSize() {
		return wrapper.getNumericAttribute(JMX_OBJECT_NAME, 
				"FreeSwapSpaceSize", DEFAULT_LONG).longValue();
	}

	@SnapShotGauge(formatter=ByteFormatter.class)
	public long getTotalPhysicalMemorySize() {
		return wrapper.getNumericAttribute(JMX_OBJECT_NAME, 
				"TotalPhysicalMemorySize", DEFAULT_LONG).longValue();
	}


	@SnapShotGauge(formatter=ByteFormatter.class)
	public long getTotalSwapSpaceSize() {
		return wrapper.getNumericAttribute(JMX_OBJECT_NAME, 
				"TotalSwapSpaceSize", DEFAULT_LONG).longValue();
	}

	@SnapShotGauge
	public float getPercentageCPU() {
		float result = 0F;
		long processCpuTime = getProcessCpuTime();
		long cpuCheckMillis = System.currentTimeMillis();
		int availableProcessors = getAvailableProcessors();
		
		long lastMillis = getLastCPUCheckMillis();
		long lastCpuTime = getLastProcessCpuTime();
		if (lastMillis >= 0 && lastCpuTime >= 0) {
			long duration = cpuCheckMillis - lastMillis;
			float processorDuration = (float)(processCpuTime - lastCpuTime);
			
			float denominator = duration * availableProcessors * 10000F;
			if (denominator > 0) {
				result = processorDuration / denominator;
				
				// result can be over 100 percent because the JMXOperating system does not
				// collect each element in the calculation at the exact same time.
				if (result > 100F) {
					result = 100F;
				}
			}
		}
		lastProcessCpuTime = Long.valueOf(processCpuTime);
		lastCpuCheckMillis = Long.valueOf(cpuCheckMillis);
		return result;
	}
	
	private int getAvailableProcessors() {
		return wrapper.getNumericAttribute(JMX_OBJECT_NAME, 
				"AvailableProcessors", DEFAULT_INT).intValue();
	}

	
	private long getProcessCpuTime() {
		return wrapper.getNumericAttribute(JMX_OBJECT_NAME, 
				"ProcessCpuTime", DEFAULT_LONG).longValue();
	}
	
	private long getLastCPUCheckMillis() {
		return lastCpuCheckMillis == null ? -1 : lastCpuCheckMillis.longValue();
	}
	
	private long getLastProcessCpuTime() {
		return lastProcessCpuTime == null ? -1 : lastProcessCpuTime.longValue();
	}
}
