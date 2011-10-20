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


import java.util.Iterator;

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
			RemoteManagementWrapper r = null;
			try {
				r = RemoteManagementWrapper.open("localhost", 8571);
				System.out.println("Retrieved sessionID: " + r.getSessionID());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RemoteManagementWrapper.closeNoThrow(r);
			}
		}
	}

	public static class TestMajorVersionMismatch implements Runnable {
		public void run() {
			RemoteInterface r = null;
			try {
				r = RemoteManagementWrapper.getRemoteInterface("localhost", 8571);
				try {
					r.connect("1" + ManagementVersion.VERSION);
				} catch (IncompatibleClientVersionException re) {
					System.out.println("IncompatibleClientVersionException thrown");
					re.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class TestGetMonitors implements Runnable {
		public void run() {
			RemoteManagementWrapper r = null;
			try {
				r = RemoteManagementWrapper.open("localhost", 8571);
				Iterator<MonitorInstance> itr =  r.getMonitors().iterator();
				while (itr.hasNext()) {
					MonitorInstance inst = itr.next();
					System.out.println("Monitor: " +  inst.getKey());
					MonitorDefinition def = r.getMonitorDefinition(inst.getMonitorType());
					
					Iterator<FieldDefinition> itrDef = def.getFieldItr();
					while (itrDef.hasNext()) {
						System.out.println("FieldDefinition: " +  itrDef.next());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RemoteManagementWrapper.closeNoThrow(r);
			}
		}
	}
}
