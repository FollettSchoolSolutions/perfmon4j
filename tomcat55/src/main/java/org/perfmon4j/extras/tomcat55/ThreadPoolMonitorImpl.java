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

import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR, 
		dataInterface=ThreadPoolMonitor.class)
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
	
	@SnapShotString
	public String getInstanceName() {
		return MiscHelper.getInstanceNames(getMBeanServer(), getQueryObjectName(), "name");
	}

	@SnapShotGauge
	public long getCurrentThreadsBusy() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadsBusy");
	}
	
	@SnapShotGauge
	public long getCurrentThreadCount() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadCount");
	}
}
