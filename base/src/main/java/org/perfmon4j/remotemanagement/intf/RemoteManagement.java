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

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class RemoteManagement {
	
	private RemoteManagement() {
	}

	public static RemoteInterface getRemoteInterface(int port) throws RemoteException {
		try {
			return (RemoteInterface)Naming.lookup("rmi://localhost:" + port + "/" +  RemoteInterface.serviceName);
		} catch (MalformedURLException e) {
			throw new RemoteException("Bad URL", e);
		} catch (RemoteException e) {
			throw e;
		} catch (NotBoundException e) {
			throw new RemoteException("NotBound", e);
		}
	}
}
