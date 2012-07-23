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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.instrument.InstrumentationMonitor;
import org.perfmon4j.instrument.TransformerParams;
import org.perfmon4j.java.management.GarbageCollectorSnapShot;
import org.perfmon4j.java.management.JVMSnapShot;
import org.perfmon4j.java.management.MemoryPoolSnapShot;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.IncompatibleClientVersionException;
import org.perfmon4j.remotemanagement.intf.InvalidMonitorTypeException;
import org.perfmon4j.remotemanagement.intf.ManagementVersion;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.MonitorNotFoundException;
import org.perfmon4j.remotemanagement.intf.RemoteInterface;
import org.perfmon4j.remotemanagement.intf.RemoteInterfaceExt1;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.remotemanagement.intf.UnableToParseKeyException;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class RemoteImpl implements RemoteInterface {
	private static final Logger logger = LoggerFactory.initLogger(RemoteImpl.class);
	
	private static final long serialVersionUID = ManagementVersion.RMI_VERSION;
	private static Registry registry = null;
	private static Integer registeredPort = null;
	
	public static final int AUTO_RMI_PORT_RANGE_START=
		Integer.getInteger(RemoteImpl.class.getName() + ".AUTO_RMI_PORT_RANGE_START", 5400).intValue();
	public static final int AUTO_RMI_PORT_RANGE_END=
		Integer.getInteger(RemoteImpl.class.getName() + ".AUTO_RMI_PORT_RANGE_END", 5500).intValue();
	
	static final private RemoteImplExt1 singleton = new RemoteImplExt1(new RemoteImpl());
	
	
	private RemoteImpl() {
		ExternalAppender.registerSnapShotClass(InstrumentationMonitor.class.getName());
		ExternalAppender.registerSnapShotClass(JVMSnapShot.class.getName());
		ExternalAppender.registerSnapShotClass(GarbageCollectorSnapShot.class.getName());
		ExternalAppender.registerSnapShotClass(MemoryPoolSnapShot.class.getName());
		ExternalAppender.registerSnapShotClass("org.perfmon4j.extras.sunjava6.MemoryMonitorImpl");
		ExternalAppender.registerSnapShotClass("org.perfmon4j.extras.sunjava6.OperatingSystemMonitorImpl");
		
		ExternalAppender.registerSnapShotClass("org.perfmon4j.extras.tomcat55.GlobalRequestProcessorMonitorImpl");
		ExternalAppender.registerSnapShotClass("org.perfmon4j.extras.tomcat55.ThreadPoolMonitorImpl");
		
		ExternalAppender.registerSnapShotClass("org.perfmon4j.extras.tomcat7.GlobalRequestProcessorMonitorImpl");
		ExternalAppender.registerSnapShotClass("org.perfmon4j.extras.tomcat7.ThreadPoolMonitorImpl");
	}

	public static RemoteInterfaceExt1 getSingleton() {
		return singleton;
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
        	int portStart = port;
        	int portEnd = port;
        	if (port == TransformerParams.REMOTE_PORT_AUTO) {
        		portStart = RemoteImpl.AUTO_RMI_PORT_RANGE_START;
        		portEnd = RemoteImpl.AUTO_RMI_PORT_RANGE_END;
        	}
        	port = portStart;
        	while (true) {
	        	try {
	        		registry = LocateRegistry.createRegistry(port);
	        		break; // Found a port... break out of the loop....
	        	} catch (RemoteException ex) {
	        		if (++port > portEnd) {
	        			throw ex;
	        		}
	        	}
        	}
        	// For backwards compatibility register both the Base "RemoteInterface" object and the
        	// new RemoteInterfaceExt1 object.
        	
        	// Register the base object for backwards compatibility...
        	RemoteImpl implBase = (RemoteImpl)singleton.getDelegate();
			registry.bind(RemoteInterface.serviceName, UnicastRemoteObject.exportObject(implBase, 0));
			
			// Register the Ext1 interface.
			RemoteImplExt1 implExt1 = singleton;
			registry.bind(RemoteInterfaceExt1.serviceName, (RemoteInterfaceExt1)UnicastRemoteObject.exportObject(implExt1, 0));
			
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
				unregisterRMIListener();
				registry = null;
			}
		}
	}
	
	public static void unregisterRMIListener() {
		if (registry != null) {
			// Unregister ext1 implementation.
			try {registry.unbind(RemoteInterfaceExt1.serviceName);} catch (Exception ex){logger.logInfo("unregisterRMIListener", ex);}
			try {UnicastRemoteObject.unexportObject(singleton, true);} catch (Exception ex){logger.logInfo("unregisterRMIListener", ex);}

			// Unregister base implementation.
			try {registry.unbind(RemoteInterface.serviceName);} catch (Exception ex){logger.logInfo("unregisterRMIListener", ex);}
			try {UnicastRemoteObject.unexportObject(singleton.getDelegate(), true);} catch (Exception ex){logger.logInfo("unregisterRMIListener", ex);}
			
			try { UnicastRemoteObject.unexportObject(registry, true);} catch (Exception ex){logger.logInfo("unregisterRMIListener", ex);}
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

	private void copyToResultMap(Map<FieldKey, Object> from, Map<String, Object> to) {
		Iterator<Map.Entry<FieldKey, Object>> itr = from.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<FieldKey, Object> entry = itr.next();
			to.put(entry.getKey().toString(), entry.getValue());
		}
	}

	public Map<String, Object> getData(String sessionID) throws SessionNotFoundException, RemoteException {
		Map<String, Object> result = new HashMap<String, Object>();
		MonitorKeyWithFields monitors[] = ExternalAppender.getSubscribedMonitors(sessionID);
		
		Map<FieldKey, Object> from;
		for (int i = 0; i < monitors.length; i++) {
			try {
				from = ExternalAppender.takeSnapShot(sessionID, monitors[i]);
				copyToResultMap(from, result);
			} catch (MonitorNotFoundException e) {
				if (logger.isDebugEnabled()) {
					logger.logWarn("Monitor not found", e);
				} else {
					logger.logInfo("Monitor not found: " + monitors[i].getMonitorKeyOnly().toString());
				}
			}			
		}
		
		from = ExternalAppender.getThreadTraceData(sessionID);
		if (from != null) {
			copyToResultMap(from, result);
		}
		
		return result;
	}

	public String[] getFieldsForMonitor(String sessionID, String monitorKey)
			throws SessionNotFoundException, RemoteException {
		ExternalAppender.validateSession(sessionID);
		
		String[] result = new String[]{};
		try {
			MonitorKey key = MonitorKey.parse(monitorKey);
			if (MonitorKey.INTERVAL_TYPE.equals(key.getType())) {
				FieldKey fields[] = IntervalData.getFields(key).getFields();
				result = FieldKey.toStringArray(fields);
			} else if (MonitorKey.THREADTRACE_TYPE.equals(key.getType())) {
				FieldKey fields[] = new FieldKey[]{new FieldKey(key, "stack", FieldKey.STRING_TYPE)}; 
				result = FieldKey.toStringArray(fields);
			} else if (MonitorKey.SNAPSHOT_TYPE.equals(key.getType())) {
				FieldKey fields[] = ExternalAppender.getFieldsForSnapShotMonitor(key);
				result = FieldKey.toStringArray(fields);
			}
		} catch (UnableToParseKeyException e) {
			logger.logWarn("Unable to parse monitor key: " + monitorKey);
		}

		return result;
	}

	public String[] getMonitors(String sessionID) throws SessionNotFoundException, RemoteException {
		List<String> result = new ArrayList<String>();
		ExternalAppender.validateSession(sessionID);
		
		
		List<MonitorKey> intervalMonitors = PerfMon.getMonitorKeys();
		for (MonitorKey key : intervalMonitors) {
			result.add(key.toString());
		}
		
		MonitorKey keys[] = ExternalAppender.getSnapShotMonitorKeys();
		for (int i = 0; i < keys.length; i++) {
			result.add(keys[i].toString());
		}
		
		return result.toArray(new String[]{});	
	}

	public void subscribe(String sessionID, String[] fieldKeys) throws SessionNotFoundException, RemoteException {
		MonitorKeyWithFields[] monitorKey = MonitorKeyWithFields.groupFields(FieldKey.toFieldKeyArrayNoThrow(fieldKeys));
		
		List<MonitorKeyWithFields> alreadySubscribed = new ArrayList<MonitorKeyWithFields>(monitorKey.length);
		List<MonitorKeyWithFields> newSubscribed = Arrays.asList(monitorKey);
		MonitorKeyWithFields[] subscribed = ExternalAppender.getSubscribedMonitors(sessionID);
		
		for (int i = 0; i < subscribed.length; i++) {
			MonitorKeyWithFields key = subscribed[i];
			if (newSubscribed.contains(subscribed[i])) {
				// Already subscribed...  Dont need to add again.
				alreadySubscribed.add(key);
			} else {
				// No longer subscribed...  Unsubscribe.
				ExternalAppender.unSubscribe(sessionID, key);
			}
		}
	
		Iterator<MonitorKeyWithFields> itr = newSubscribed.iterator();
		while (itr.hasNext()) {
			MonitorKeyWithFields key = itr.next();
			if (!alreadySubscribed.contains(key)) {
				ExternalAppender.subscribe(sessionID, key);
			}
		}
	}

	public void scheduleThreadTrace(String sessionID, String fieldKey)
			throws SessionNotFoundException, RemoteException, InvalidMonitorTypeException {
		try {
			FieldKey key = FieldKey.parse(fieldKey);
			ExternalAppender.scheduleThreadTrace(sessionID, key);
		} catch (UnableToParseKeyException e) {
			throw new RemoteException("Unable to parse key: " + fieldKey);
		}
	}

	public void unScheduleThreadTrace(String sessionID, String fieldKey)
			throws SessionNotFoundException, RemoteException, InvalidMonitorTypeException {
		try {
			FieldKey key = FieldKey.parse(fieldKey);
			ExternalAppender.unScheduleThreadTrace(sessionID, key);
		} catch (UnableToParseKeyException e) {
			throw new RemoteException("Unable to parse key: " + fieldKey);
		}
	}
}
