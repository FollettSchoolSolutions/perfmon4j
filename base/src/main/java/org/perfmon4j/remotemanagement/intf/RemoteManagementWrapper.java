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

package org.perfmon4j.remotemanagement.intf;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

public class RemoteManagementWrapper implements Closeable {
	private final RemoteInterface remoteInterface;
	private final String sessionID;
	
	private RemoteManagementWrapper(RemoteInterface remoteInterface, String sessionID) {
		this.remoteInterface = remoteInterface;
		this.sessionID = sessionID;
	}

	public static RemoteManagementWrapper open(String host, int port) throws RemoteException, IncompatibleClientVersionException {
		RemoteInterface remote = getRemoteInterface(host, port);
		String sessionID = remote.connect(ManagementVersion.VERSION);
		return new RemoteManagementWrapper(remote, sessionID);
	}

	public static RemoteManagementWrapper open(String host, int port, int timeoutSeconds) throws RemoteException, IncompatibleClientVersionException {
		RemoteInterface remote = getRemoteInterface(host, port);
		String sessionID = remote.connect(ManagementVersion.VERSION, timeoutSeconds);
		return new RemoteManagementWrapper(remote, sessionID);
	}
	
	public static void closeNoThrow(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void close() throws IOException {
		remoteInterface.disconnect(sessionID);
	}

	public List<MonitorInstance> getData()
			throws SessionNotFoundException, RemoteException {
		return remoteInterface.getData(sessionID);
	}

	public List<MonitorInstance> getMonitors()
			throws SessionNotFoundException, RemoteException {
		return remoteInterface.getMonitors(sessionID);
	}

	public void subscribe(String sessionID, List<String> monitorKeys)
		throws SessionNotFoundException, RemoteException {
		remoteInterface.subscribe(sessionID, monitorKeys);
	}
	
	public MonitorDefinition getMonitorDefinition(MonitorDefinition.Type monitorType) 
		throws SessionNotFoundException, RemoteException {
		return remoteInterface.getMonitorDefinition(sessionID, monitorType);
	}
	
	public String getSessionID() {
		return sessionID;
	}

	/**
	 * Package levelfor testing
	 * @param host
	 * @param port
	 * @return
	 * @throws RemoteException
	 */
	static RemoteInterface getRemoteInterface(String host, int port) throws RemoteException {
		try {
			return (RemoteInterface)Naming.lookup("rmi://" + host + ":" + port + "/" +  RemoteInterface.serviceName);
		} catch (MalformedURLException e) {
			throw new RemoteException("Bad URL", e);
		} catch (RemoteException e) {
			throw e;
		} catch (NotBoundException e) {
			throw new RemoteException("NotBound", e);
		}
	}

}
