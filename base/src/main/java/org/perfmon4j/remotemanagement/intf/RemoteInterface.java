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
*/

package org.perfmon4j.remotemanagement.intf;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteInterface extends Serializable {
    public static final String serviceName = "P4JServiceName";
    public static final String P4J_LISTENER_PORT = "PERFMON4J_LISTENER_PORT";
    
	public String connect(int majorAPIVersion) throws RemoteException;
	public String connect(int majorAPIVersion, int keepMonitorsAliveSeconds) throws RemoteException;

    public void disconnect(String connectionID) throws RemoteException;
	
    public List<MonitorInstance> getMonitors(String connectionID) throws RemoteException;
    public void subscribe(String connectionID, List<String> monitorKeys) throws RemoteException;
    public List<MonitorInstance> getData(String connectionID) throws RemoteException;
}
