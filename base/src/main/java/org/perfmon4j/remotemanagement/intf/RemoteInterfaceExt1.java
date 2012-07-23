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
*/

package org.perfmon4j.remotemanagement.intf;

import java.rmi.RemoteException;

public interface RemoteInterfaceExt1 extends RemoteInterface {
    public static final String serviceName = RemoteInterface.serviceName + "Ext1";

	public String getServerManagementVersion() throws RemoteException;
	public void forceDynamicChildCreation(String sessionID, String dynamicPathMonitorKey) throws SessionNotFoundException, RemoteException;
    public void unForceDynamicChildCreation(String sessionID, String dynamicPathMonitorKey) throws SessionNotFoundException, RemoteException;
}
