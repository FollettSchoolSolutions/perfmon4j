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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonitorKey implements Comparable<MonitorKey> {
	public static final String INTERVAL_TYPE = "INTERVAL";
	public static final String SNAPSHOT_TYPE = "SNAPSHOT";
	public static final String THREADTRACE_TYPE = "THREADTRACE";

	private static final Pattern pattern_no_instance = Pattern.compile("^([^\\(]+)\\(name=([^\\)]+)\\).*");
	private static final Pattern pattern_with_instance = Pattern.compile("^([^\\(]+)\\(name=([^,]+);instance=([^\\)]+)\\).*");
	
	private final String type;
	private final String name;
	private final String instance;

	public MonitorKey(String type, String name) {
		this(type, name, null);
	}
	
	public MonitorKey(String type, String name, String instance) {
		this.type = type;
		this.name = name;
		this.instance = instance;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}
	
	public String getInstance() {
		return instance;
	}

	/**
	 * Returns the MonitorKey parsed out of the string
	 * or null if it could not be parsed.
	 */
	public static MonitorKey parseNoThrow(String value) {
		MonitorKey result = null; 
		
		Matcher m = pattern_with_instance.matcher(value);
		if  (m.matches()) {
			result = new MonitorKey(m.group(1), m.group(2), m.group(3));
		} else {
			m = pattern_no_instance.matcher(value);
			if (m.matches()) {
				result = new MonitorKey(m.group(1), m.group(2));
			}
		}
		return result;
	}
	
	public static MonitorKey parse(String value) throws UnableToParseKeyException {
		MonitorKey result = parseNoThrow(value);
		if (result == null) {
			throw new UnableToParseKeyException(value);
		}
		return result;
	}

	
	@Override
	public String toString() {
		String result =	type + "(name=" + name;	
		
		if (instance == null) {
			result +=  ")";	
		} else {
			return result += ";instance=" + instance + ")";	
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((instance == null) ? 0 : instance.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		MonitorKey other = (MonitorKey) obj;
		if (instance == null) {
			if (other.instance != null)
				return false;
		} else if (!instance.equals(other.instance))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	public int compareTo(MonitorKey o) {
		return this.toString().compareTo(o.toString());
	}
}
