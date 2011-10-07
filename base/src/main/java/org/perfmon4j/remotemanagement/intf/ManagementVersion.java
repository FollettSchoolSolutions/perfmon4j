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
 * It is important that all classes in this this package 
 * (org.perfmon4j.remotemanagement.intf) remain stable accross
 * releases of perfmon4j.
 * 
 * These classes serve as the bridge between a perfmon4j monitoring
 * application (e.g. Perfmon4j plugin for VisualVM) and an application
 * running perfmon4j.
 * 
 *  If/when changes are made to this class that break the compatibility
 *  the MAJOR_VERSION must be changed.
 *  
 *  If implementation changes are made, however the remote interface 
 *  remains backwards/forwards compatible the minor version is changed.
 *  
 *  Any classes that are passed through the remote interface must
 *  have a serial version id that is consistent with 
 *  ManagementVersion.MAJOR_VERSION
 * 
 * @author ddeucher
 *
 */
public final class ManagementVersion {
	
	public static final int MAJOR_VERSION = 1;
	public static final int MINOR_VERSION = 0;
	public static final String VERSION;
	
	static {
		VERSION = String.format("%d.%03d", Integer.valueOf(MAJOR_VERSION),
				Integer.valueOf(MINOR_VERSION));
	}
	
	private ManagementVersion() {
	}
}
