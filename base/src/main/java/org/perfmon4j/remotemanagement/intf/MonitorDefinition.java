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

import java.io.Serializable;

public class MonitorDefinition implements Serializable {
	private static final long serialVersionUID = ManagementVersion.MAJOR_VERSION;
	
	final public static String INTERVAL_MONITOR = "INTERVAL";
	final public static String SNAPSHOT_MONITOR = "SNAPSHOT";
	final public static String THREADTRACE_MONITOR = "THREAD_TRACE";
	final public static String UNKNOWN_MONITOR = "UKNOWN";
	
	// Sub-types are only relevant for Interval monitors...
	final public static String SUBTYPE_NA = "NA";
	final public static String SUBTYPE_PACKAGE = "PACKAGE";
	final public static String SUBTYPE_CLASS = "CLASS";
	final public static String SUBTYPE_METHOD = "METHOD";
	final public static String SUBTYPE_USERDEFINED = "USERDEFINED";
	
	final private String name;
	final private String type;
	final private String subType;
	final private String toStringValue;

	public MonitorDefinition(String name, String type) {
		this(name, type, SUBTYPE_NA);
	}

	public MonitorDefinition(String name, String type, String subType) {
		this.name = name;
		this.type = type;
		this.subType = subType;
		this.toStringValue = this.getClass().getSimpleName() 
			+ "(name=" + name
			+ " ,type=" + type
			+ " ,subType=" + subType
			+ ")";
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime * toStringValue.hashCode();  
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonitorDefinition other = (MonitorDefinition) obj;
		return other.toStringValue == toStringValue;
	}
	
	@Override
	public String toString() {
		return toStringValue; 
	}
	
}
