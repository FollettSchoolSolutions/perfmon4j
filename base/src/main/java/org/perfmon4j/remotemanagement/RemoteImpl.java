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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.remotemanagement.intf.FieldDefinition;
import org.perfmon4j.remotemanagement.intf.IncompatibleClientVersionException;
import org.perfmon4j.remotemanagement.intf.ManagementVersion;
import org.perfmon4j.remotemanagement.intf.MonitorDefinition;
import org.perfmon4j.remotemanagement.intf.MonitorInstance;
import org.perfmon4j.remotemanagement.intf.MonitorNotFoundException;
import org.perfmon4j.remotemanagement.intf.RemoteInterface;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class RemoteImpl implements RemoteInterface {
	private static final Logger logger = LoggerFactory.initLogger(RemoteImpl.class);
	
	private static final long serialVersionUID = ManagementVersion.RMI_VERSION;
	private static Registry registry = null;
	private static Integer registeredPort = null;
	
	static final RemoteImpl singleton = new RemoteImpl();
	
	private final MonitorDefinition intervalDefinition = buildIntervalDefinition();
	
	
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
	
	public String connect(String clientVersion) throws RemoteException, IncompatibleClientVersionException {
		return connect(clientVersion, ExternalAppender.DEFAULT_TIMEOUT_SECONDS);
	}

	public String connect(String clientVersion, int keepMonitorsAliveSeconds) throws RemoteException, IncompatibleClientVersionException {
		int clientMajorVersion = ManagementVersion.extractMajorVersion(clientVersion);
		if (clientMajorVersion != ManagementVersion.MAJOR_VERSION) {
			throw new IncompatibleClientVersionException(clientVersion, ManagementVersion.VERSION);
		}
		return ExternalAppender.connect(keepMonitorsAliveSeconds);
	}

	public void disconnect(String sessionID) throws RemoteException {
		ExternalAppender.disconnect(sessionID);
	}

	public List<MonitorInstance> getData(String sessionID ) throws SessionNotFoundException, RemoteException {
		List<MonitorInstance> result = new ArrayList<MonitorInstance>();
		
		String[] keys = ExternalAppender.getSubscribedMonitors(sessionID);
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			
			try {
				PerfMonData d = ExternalAppender.takeSnapShot(sessionID, key);
				result.add(new MonitorInstance(key, MonitorDefinition.INTERVAL_TYPE));
			} catch (MonitorNotFoundException mnf) {
				logger.logWarn("Monitor \"" + key + "\" not found for sessionID: " + sessionID, mnf);
			}
		}
		
		return result;
	}

	public List<MonitorInstance> getMonitors(String sessionID)
			throws RemoteException, SessionNotFoundException {
		List<MonitorInstance> result = new ArrayList<MonitorInstance>();
		
		ExternalAppender.validateSession(sessionID);
		
		Iterator<String> intervalMonitors = PerfMon.getMonitorNames().iterator();
		while (intervalMonitors.hasNext()) {
			String key = MonitorDefinition.buildIntervalMonitorKey(intervalMonitors.next());
			result.add(new MonitorInstance(key, intervalDefinition.getType())); 
		}
		return result;	
	}

	public void subscribe(String sessionID, String[] monitorKeys)
			throws RemoteException, SessionNotFoundException {
		
		List<String> newSubscribed = Arrays.asList(monitorKeys);
		List<String> alreadySubscribedList = new ArrayList<String>(newSubscribed.size());
		
		String[] subscribed = ExternalAppender.getSubscribedMonitors(sessionID);
		for (int i = 0; i < subscribed.length; i++) {
			String key = subscribed[i];
			if (newSubscribed.contains(subscribed[i])) {
				// Already subscribed...  Dont need to add again.
				alreadySubscribedList.add(key);
			} else {
				// No longer subscribed...  Unsubscribe.
				ExternalAppender.unSubscribe(sessionID, key);
			}
		}
		
		Iterator<String> itr = newSubscribed.iterator();
		while (itr.hasNext()) {
			String key = itr.next();
			if (!alreadySubscribedList.contains(key)) {
				ExternalAppender.subscribe(sessionID, key);
			}
		}
	}

	public MonitorDefinition getMonitorDefinition(String sessionID,
			MonitorDefinition.Type monitorType) throws SessionNotFoundException, RemoteException {

		ExternalAppender.validateSession(sessionID);
		
		if (monitorType.equals(MonitorDefinition.INTERVAL_TYPE)) {
			return intervalDefinition;
		} else {
			return null;
		}
	}
	
	private static MonitorDefinition buildIntervalDefinition() {
		final List<FieldDefinition> fields = new ArrayList<FieldDefinition>();
		final MonitorDefinition.Type type = MonitorDefinition.INTERVAL_TYPE;
	
		fields.add(new FieldDefinition(type, "MaxActiveThreadCount", FieldDefinition.INTEGER_TYPE));
		fields.add(new FieldDefinition(type, "MaxDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "MaxSQLDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "MinDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "MinSQLDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "TotalDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "TotalSQLDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "AverageDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "AverageSQLDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "SumOfSquares", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(type, "SumOfSQLSquares", FieldDefinition.LONG_TYPE));

		fields.add(new FieldDefinition(type, "TotalCompletions", FieldDefinition.INTEGER_TYPE));
		fields.add(new FieldDefinition(type, "TotalHits", FieldDefinition.INTEGER_TYPE));

		fields.add(new FieldDefinition(type, "TimeStart", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(type, "TimeStop", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(type, "TimeMaxActiveThreadCountSet", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(type, "TimeMaxDurationSet", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(type, "TimeMinDurationSet", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(type, "TimeMaxSQLDurationSet", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(type, "TimeMinSQLDurationSet", FieldDefinition.TIMESTAMP_TYPE));
		
		fields.add(new FieldDefinition(type, "ThroughputPerMinute", FieldDefinition.DOUBLE_TYPE));
		fields.add(new FieldDefinition(type, "StdDeviation", FieldDefinition.DOUBLE_TYPE));

		return new MonitorDefinition("INTERVAL", type, fields);	
	}
	
}
