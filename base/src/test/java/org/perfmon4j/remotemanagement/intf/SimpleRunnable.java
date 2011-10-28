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

import java.util.Map;




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
				MonitorKey[] monitors = r.getMonitors();
				for (int i = 0; i < monitors.length; i++) {
					MonitorKey key = monitors[i];
					System.out.println("Monitor: " +  key);
					
					FieldKey[] fields = r.getFieldsForMonitor(key);
					for (int j = 0; j < fields.length; j++) {
						FieldKey field = fields[j];
						System.out.println("FieldDefinition: " +  field);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RemoteManagementWrapper.closeNoThrow(r);
			}
		}
	}
	
	
	/**
	 * This class can serve as a demonstration implementation of
	 * how to attach to a remote VM and remotely monitor Perfmon4j
	 * attributes...
	 * 
	 * @author ddeucher
	 *
	 */
	public static class TestFullRemoteMonitorLifecycle implements Runnable {
		public void run() {
			RemoteManagementWrapper r = null;
			try {
				// Step 1....
				// You need to know the host and port number that the monitor is
				// attached to.
				// You then open up a connection through the RemoteManagementWrapper 
				// class....
				r = RemoteManagementWrapper.open("localhost", 8571);
				
				// Step 2....
				// Get a list of all of the monitors currently available.
				MonitorKey[] activeMonitors = r.getMonitors();
	
				
				// Step 4...
				// Build a list of all of the current active monitors available.
				// Have the user select monitors from this list to subscribe to.
				for (int i = 0; i < activeMonitors.length; i++) {
					MonitorKey monitor = activeMonitors[i];
					
					System.out.println("Found monitor: " + monitor);
				}
				
				// Step 5...
				// Select one or more fields, from one or more monitors to subscribe 
				// to.
				FieldKey[] fields = r.getFieldsForMonitor(activeMonitors[0]);
				for (int i = 0; i < fields.length; i++) {
					System.out.println("Found field: " + fields[i]);
				}
				// For this example we will just subscribe to all the fields
				// for the first monitor.
				r.subscribe(fields);
				
				// Step 6...
				// Set up a polling process to retrieve the data at a regular interval...
				// Here we will use a simple loop with a 1 second delay..
				for(int i = 0; i < 15; i++) {
					Thread.sleep(1000);
					System.out.println("Taking snapshot....");
					
					Map<FieldKey, Object> snapShot =  r.getData();
					System.out.println(FieldKey.buildDebugString(snapShot));
				}

				// Step 7...  When done monitoring close the connection.
				r.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				RemoteManagementWrapper.closeNoThrow(r);
			}
		}
	}
}
