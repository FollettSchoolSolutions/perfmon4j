/*
 *	Copyright 2011 Follett Software Company 
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
import java.util.List;

import org.perfmon4j.remotemanagement.intf.ManagementVersion;
import org.perfmon4j.remotemanagement.intf.MonitorInstance;
import org.perfmon4j.remotemanagement.intf.RemoteInterface;

public class RemoteImpl implements RemoteInterface {
	private static final long serialVersionUID = ManagementVersion.RMI_VERSION;

	public String connect(int majorAPIVersion) throws RemoteException {
		return ExternalAppender.connect();
	}

	public String connect(int majorAPIVersion, int keepMonitorsAliveSeconds) throws RemoteException {
		return ExternalAppender.connect(keepMonitorsAliveSeconds);
	}

	public void disconnect(String connectionID) throws RemoteException {
		ExternalAppender.disconnect(connectionID);
	}

	public List<MonitorInstance> getData(String connectionID)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<MonitorInstance> getMonitors(String connectionID)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public void subscribe(String connectionID, List<String> monitorKeys)
			throws RemoteException {
		// TODO Auto-generated method stub
	}
}
