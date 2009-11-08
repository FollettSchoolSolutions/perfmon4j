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

package org.perfmon4j.extras.tomcat55;

import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.ByteFormatter;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR,
		dataInterface=GlobalRequestProcessorMonitor.class)
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
}
