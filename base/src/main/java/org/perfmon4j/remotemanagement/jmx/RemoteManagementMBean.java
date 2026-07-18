/*
 *	Copyright 2026 Follett Software Company
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
*/

package org.perfmon4j.remotemanagement.jmx;

import java.util.Map;

import org.perfmon4j.remotemanagement.intf.IncompatibleClientVersionException;
import org.perfmon4j.remotemanagement.intf.InvalidMonitorTypeException;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;

/**
 * Standard MBean interface for perfmon4j's remote-management MBean
 * (org.perfmon4j:type=RemoteManagement). Must be named exactly
 * "RemoteManagementMBean", in the same package as the implementing class
 * "RemoteManagement", to satisfy the JMX Standard MBean naming convention.
 * <p>
 * This is a JMX/Jolokia-reachable re-exposure of the same monitor-browsing
 * and thread-trace functionality the legacy RMI interface
 * (org.perfmon4j.remotemanagement.intf.RemoteInterface) provides, backed by
 * the same session-based subscription model in
 * org.perfmon4j.remotemanagement.ExternalAppender - a session created here is
 * visible to, and shares state with, a session created over RMI, and vice
 * versa.
 * <p>
 * This covers the monitor-tree-browsing surface (connect, disconnect,
 * getMonitors, getFieldsForMonitor, subscribe, getData) plus thread-trace
 * scheduling (scheduleThreadTrace, unScheduleThreadTrace) and the
 * RemoteInterfaceExt1 dynamic-child-creation operations
 * (forceDynamicChildCreation, unForceDynamicChildCreation,
 * getServerManagementVersion).
 */
public interface RemoteManagementMBean {
	String connect(String clientVersion) throws IncompatibleClientVersionException;

	void disconnect(String sessionID) throws SessionNotFoundException;

	String[] getMonitors(String sessionID) throws SessionNotFoundException;

	String[] getFieldsForMonitor(String sessionID, String monitorKey) throws SessionNotFoundException;

	void subscribe(String sessionID, String[] fieldKeys) throws SessionNotFoundException;

	Map<String, Object> getData(String sessionID) throws SessionNotFoundException;

	void scheduleThreadTrace(String sessionID, String fieldKey)
			throws SessionNotFoundException, InvalidMonitorTypeException;

	void unScheduleThreadTrace(String sessionID, String fieldKey)
			throws SessionNotFoundException, InvalidMonitorTypeException;

	String getServerManagementVersion();

	void forceDynamicChildCreation(String sessionID, String monitorKey) throws SessionNotFoundException;

	void unForceDynamicChildCreation(String sessionID, String monitorKey) throws SessionNotFoundException;
}
