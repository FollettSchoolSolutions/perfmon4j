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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
	
	public String getSessionID() {
		return sessionID;
	}
	
    public MonitorKey[] getMonitors() throws SessionNotFoundException, RemoteException {
    	String values[] = remoteInterface.getMonitors(sessionID);
    	List<MonitorKey> result = new ArrayList<MonitorKey>(values.length);
    	
    	for (int i = 0; i < values.length; i++) {
			MonitorKey key = MonitorKey.parseNoThrow(values[i]);
			if (key != null) {
				result.add(key);
			}
		}
    	return result.toArray(new MonitorKey[]{});
    }
    
    
    public FieldKey[] getFieldsForMonitor(MonitorKey monitorKey) throws SessionNotFoundException, RemoteException {
    	String values[] = remoteInterface.getFieldsForMonitor(sessionID, monitorKey.toString());
    	List<FieldKey> result = new ArrayList<FieldKey>(values.length);
    	
    	for (int i = 0; i < values.length; i++) {
			FieldKey key = FieldKey.parseNoThrow(values[i]);
			if (key != null) {
				result.add(key);
			}
		}
    	return result.toArray(new FieldKey[]{});
    }
    
    public void subscribe(FieldKey[] fieldKeys) throws SessionNotFoundException, RemoteException {
    	String[] fields = new String[fieldKeys.length];
    	for (int i = 0; i < fieldKeys.length; i++) {
    		fields[i] = fieldKeys[i].toString();
		}
    	
    	remoteInterface.subscribe(sessionID, fields);
    }
    
    
    public Map<FieldKey, Object> getData() throws SessionNotFoundException, RemoteException {
    	Map<FieldKey, Object> result = new HashMap<FieldKey, Object>();
    	
    	Iterator<Map.Entry<String, Object>> itr = remoteInterface.getData(sessionID).entrySet().iterator();
    	while (itr.hasNext()) {
    		Map.Entry<String, Object> v = itr.next();
    		FieldKey key = FieldKey.parseNoThrow(v.getKey());
    		if (key != null) {
    			result.put(key, v.getValue());
    		}
    	}
    	return result;
    }
    public void scheduleThreadTrace(FieldKey fieldKey) throws SessionNotFoundException, 
    	RemoteException, InvalidMonitorTypeException {
    	
    	remoteInterface.scheduleThreadTrace(sessionID, fieldKey.toString());
    }
    
    public void unScheduleThreadTrace(String sessionID, FieldKey fieldKey) throws SessionNotFoundException, 
		RemoteException, InvalidMonitorTypeException {
    	
    	remoteInterface.unScheduleThreadTrace(sessionID, fieldKey.toString());
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
