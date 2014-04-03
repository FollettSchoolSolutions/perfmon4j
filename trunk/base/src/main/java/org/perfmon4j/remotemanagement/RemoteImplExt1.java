/*
 *	Copyright 2012 Follett Software Company 
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
package org.perfmon4j.remotemanagement;

import java.rmi.RemoteException;
import java.util.Map;

import org.perfmon4j.remotemanagement.intf.IncompatibleClientVersionException;
import org.perfmon4j.remotemanagement.intf.InvalidMonitorTypeException;
import org.perfmon4j.remotemanagement.intf.ManagementVersion;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.RemoteInterface;
import org.perfmon4j.remotemanagement.intf.RemoteInterfaceExt1;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.remotemanagement.intf.UnableToParseKeyException;

public class RemoteImplExt1 implements RemoteInterfaceExt1{
	private static final long serialVersionUID = ManagementVersion.MAJOR_VERSION;
	final private RemoteInterface delegate;
	
	public RemoteImplExt1(RemoteInterface delegate) {
		this.delegate = delegate;
	}

	public void unForceDynamicChildCreation(String sessionID,
			String monitorKey) throws SessionNotFoundException,
			RemoteException {
		try {
			MonitorKey key = MonitorKey.parse(monitorKey);
			ExternalAppender.unForceDynamicChildCreation(sessionID, key);
		} catch (UnableToParseKeyException e) {
			throw new RemoteException("Unable to parse key: " + monitorKey);
		}
	}

	public void forceDynamicChildCreation(String sessionID,
			String monitorKey) throws SessionNotFoundException,
			RemoteException {
		try {
			MonitorKey key = MonitorKey.parse(monitorKey);
			ExternalAppender.forceDynamicChildCreation(sessionID, key);
		} catch (UnableToParseKeyException e) {
			throw new RemoteException("Unable to parse key: " + monitorKey);
		}
	}

	public String getServerManagementVersion() {
		return ManagementVersion.VERSION;
	}
	
	public String connect(String clientVersion)
			throws IncompatibleClientVersionException, RemoteException {
		return delegate.connect(clientVersion);
	}

	public String connect(String clientVersion, int keepMonitorsAliveSeconds)
			throws IncompatibleClientVersionException, RemoteException {
		return delegate.connect(clientVersion, keepMonitorsAliveSeconds);
	}

	public void disconnect(String sessionID) throws RemoteException {
		delegate.disconnect(sessionID);
	}

	public Map<String, Object> getData(String sessionID)
			throws SessionNotFoundException, RemoteException {
		return delegate.getData(sessionID);
	}

	public String[] getFieldsForMonitor(String sessionID, String monitorKey)
			throws SessionNotFoundException, RemoteException {
		return delegate.getFieldsForMonitor(sessionID, monitorKey);
	}

	public String[] getMonitors(String sessionID)
			throws SessionNotFoundException, RemoteException {
		return delegate.getMonitors(sessionID);
	}

	public void scheduleThreadTrace(String sessionID, String fieldKeys)
			throws SessionNotFoundException, RemoteException,
			InvalidMonitorTypeException {
		delegate.scheduleThreadTrace(sessionID, fieldKeys);
	}

	public void subscribe(String sessionID, String[] fieldKeys)
			throws SessionNotFoundException, RemoteException {
		delegate.subscribe(sessionID, fieldKeys);		
	}

	public void unScheduleThreadTrace(String sessionID, String fieldKeys)
			throws SessionNotFoundException, RemoteException,
			InvalidMonitorTypeException {
		delegate.unScheduleThreadTrace(sessionID, fieldKeys);
	}
	
	RemoteInterface getDelegate() {
		return delegate;
	}
}
