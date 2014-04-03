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

package web.org.perfmon4j.extras.jbossweb7;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;


public abstract class JMXMonitorBase {
	final private MBeanServer mBeanServer;
	final private ObjectName queryObjectName;
	
	public JMXMonitorBase(String baseObjectName, String instanceNameKey, String instanceNameValue) {
		if (instanceNameValue == null || "".equals(instanceNameValue)) {
			instanceNameValue="*";
		}
		String instanceName = "," + instanceNameKey + "=" + instanceNameValue;

		mBeanServer = ManagementFactory.getPlatformMBeanServer();
		if (mBeanServer == null) {
			throw new RuntimeException("Unable to find mBeanServer");
		}
		try {
			queryObjectName = new ObjectName(baseObjectName + instanceName);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException("Unable to format object name", e);
		}
	}

	public MBeanServer getMBeanServer() {
		return mBeanServer;
	}

	public ObjectName getQueryObjectName() {
		return queryObjectName;
	}
}
