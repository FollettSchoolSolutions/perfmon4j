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

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import org.perfmon4j.remotemanagement.intf.ManagementVersion;
import org.perfmon4j.remotemanagement.intf.MonitorInstance;
import org.perfmon4j.remotemanagement.intf.RemoteInterface;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class RemoteImpl implements RemoteInterface {
	private static final Logger logger = LoggerFactory.initLogger(RemoteImpl.class);
	
	private static final long serialVersionUID = ManagementVersion.RMI_VERSION;
	private static Registry registry = null;
	private static Integer registeredPort = null;
	private static final RemoteImpl singleton = new RemoteImpl();
	
	private RemoteImpl() {
	}

	public static Integer getRegisteredPort() {
		return registeredPort;
	}
	
	public static void registerRMIListener(int port) throws RemoteException {
		if (registry != null) {
			unregisterRMIListener();
		}
		boolean bound = false;
        try {
			registry = LocateRegistry.createRegistry(port);
	
			RemoteInterface stub = (RemoteInterface)UnicastRemoteObject.exportObject(singleton, 0);
			registry.bind(RemoteInterface.serviceName, stub);
			bound = true;
			registeredPort = Integer.valueOf(port);
	        System.setProperty(RemoteInterface.P4J_LISTENER_PORT, registeredPort.toString());
	        logger.logInfo("Perfmon4j management interface listening on port: " + port);
		} catch (RemoteException e) {
			throw e;
		} catch (AlreadyBoundException e) {
			logger.logError("Unexpected exception", e);
		} finally {
			if (!bound) {
				registry = null;
			}
		}
	}
	
	public static void unregisterRMIListener() {
		if (registry != null) {
		    try {
				registry.unbind(RemoteInterface.serviceName);
				UnicastRemoteObject.unexportObject(singleton, true);
				UnicastRemoteObject.unexportObject(registry, true);
			} catch (Exception e) {
				logger.logWarn("Unable to unbind", e);
			} 
		    registry = null;
		    registeredPort = null;
	        System.getProperties().remove(RemoteInterface.P4J_LISTENER_PORT);
	        logger.logInfo("Perfmon4j management interface is unbound");
		}
	}
	
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
