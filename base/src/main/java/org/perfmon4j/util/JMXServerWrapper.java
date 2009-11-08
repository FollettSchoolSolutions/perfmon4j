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
package org.perfmon4j.util;

import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

public class JMXServerWrapper {
	private static final Logger logger = LoggerFactory.initLogger(JMXServerWrapper.class);
	private MBeanServer server;
	
	public JMXServerWrapper() throws JMException {
		List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		if (servers == null || servers.isEmpty()) {
			throw new JMException("Unable to find MBeanServer");
		}
		server = servers.get(0);
	}

	
	public Number getNumericAttribute(String jmxObject, String attributeName) throws JMException {
		ObjectName objectName = new ObjectName(jmxObject);
		return (Number)server.getAttribute(objectName, attributeName);
	}

	
	public Number getNumericAttribute(String jmxObject, String attributeName, Number defaultValue) {
		Number result = defaultValue;
		try {
			result = getNumericAttribute(jmxObject, attributeName);
		} catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.logDebug("Unable to retrieving numeric attribute - ObjectName: " + jmxObject 
						+ " Attribute: " + attributeName, ex);
			}
		}
		return result;
	}

	public Object getAttribute(String jmxObject, String attributeName, Class expectedType) throws JMException {
		ObjectName objectName = new ObjectName(jmxObject);
		Object result = server.getAttribute(objectName, attributeName);
		if (result != null && expectedType != null && !expectedType.isAssignableFrom(result.getClass())) {
			throw new JMException("Unexpected return type - Was: " + result.getClass().getName() + " Expected: " 
					+ expectedType.getName());
		}
		return result;
	}

	public Object getAttribute(String jmxObject, String attributeName, Class expectedType, Object defaultValue) {
		Object result = defaultValue;
		try {
			result = getAttribute(jmxObject, attributeName, expectedType);
		} catch (Exception ex) {
			logger.logDebug("Unable to retrieving attribute - ObjectName: " + jmxObject 
					+ " Attribute: " + attributeName, ex);
		}
		return result;
	}

}
