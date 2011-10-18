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
/**
 * IMPORTANT!!
 * DO NOT Import any classes other than production classes
 * from the package org.perfmon4j.remotemanagement.intf
 * 
 * These test classes are intended to simulate an external management application.
 * 
 * @author ddeucher
 *
 */



public class SimpleRunnable {

	public static class Test1 implements Runnable {
		
		public void run() {
			System.out.println("I am here");
		}
	}

	public static class TestSimpleConnect implements Runnable {
		public void run() {
			RemoteInterface r = null;
			try {
				r = RemoteManagement.getRemoteInterface(8571);
				String sessionID = r.connect(ManagementVersion.MAJOR_VERSION);
				System.out.println("Retrieved sessionID: " + sessionID);
				r.disconnect(sessionID);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


}
