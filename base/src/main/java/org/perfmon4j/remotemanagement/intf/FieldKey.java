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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldKey implements Comparable<FieldKey>{
	final public static String INTEGER_TYPE = "INTEGER";
	final public static String LONG_TYPE = "LONG";
	final public static String DOUBLE_TYPE = "DOUBLE";
	final public static String TIMESTAMP_TYPE = "TIMESTAMP";
	final public static String STRING_TYPE = "STRING";
	
	private static final Pattern pattern = Pattern.compile(".+?:FIELD\\(name=([^;]+);type=([^\\)]+)\\)");
	
	// Returned when session is subscribed to a ThreadTrace, and the data is not available.
	final public static String THREAD_TRACE_PENDING = "ThreadTracePending";
	
	final public static String THREAD_TRACE_MIN_DURATION_ARG = "MinDurationToCapture";
	final public static String THREAD_TRACE_MAX_DEPTH_ARG = "MaxDepth";
	
	
	public final MonitorKey monitorKey;
	public final String fieldName;
	public final String fieldType;

	public FieldKey(MonitorKey monitorKey, String fieldName, String fieldType) {
		this.monitorKey = monitorKey;
		this.fieldName = fieldName;
		this.fieldType = fieldType;
	}
	
	public MonitorKey getMonitorKey() {
		return monitorKey;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldType() {
		return fieldType;
	}

	public String toString() {
		return monitorKey + ":FIELD(name=" + fieldName + ";type=" + fieldType + ")";
	}
	
	/**
	 * Returns null if we could not parse field key
	 * @param key
	 * @return
	 */
	public static FieldKey parseNoThrow(String key) {
		FieldKey result = null;
		
		MonitorKey monitorKey = MonitorKey.parseNoThrow(key);
		if (monitorKey != null) {
			Matcher m = pattern.matcher(key);
			if (m.matches()) {
				result = new FieldKey(monitorKey, m.group(1), m.group(2));
			}
		}
		return result;
	}

	public static String buildDebugString(Map<FieldKey, Object> map) {
		StringBuilder result = new StringBuilder();
		
		Iterator<Map.Entry<FieldKey, Object>> itr = map.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<FieldKey, Object> e = itr.next();
			result.append(e.getKey() + "=" + e.getValue());
			if (itr.hasNext()) {
				result.append("\r\n");
			}
		}
		return result.toString();
	}
	
	public static FieldKey getFieldByName(FieldKey fields[], String fieldName) {
		FieldKey result = null;
		
		for (int i = 0; i < fields.length && result == null; i++) {
			if (fields[i].getFieldName().equals(fieldName)) {
				result = fields[i];
			}
		}
		return result;
	}
	
	public static FieldKey parse(String value) throws UnableToParseKeyException {
		FieldKey result = parseNoThrow(value);
		if (result == null) {
			throw new UnableToParseKeyException(value);
		}
		return result;
	}

	public static FieldKey buildThreadTraceKeyFromInterval(MonitorKey intervalKey) {
		return buildThreadTraceKeyFromInterval(intervalKey, null);
	}

	/**
	 * Returns null if the intervalKey parameter is NOT a interval or threadtrace type.
	 * @param intervalKey
	 * @return
	 */
	public static FieldKey buildThreadTraceKeyFromInterval(MonitorKey intervalKey, Map<String, String> extraParams) {
		FieldKey result = null;
		MonitorKey monitorKey = null;
		String instance = "";
		
		if (extraParams != null) {
			Iterator<Map.Entry<String, String>> itr = extraParams.entrySet().iterator();
			while(itr.hasNext()) {
				Map.Entry<String, String> entry = itr.next();
	 			instance += entry.getKey() + "=" + entry.getValue();
	 			if (itr.hasNext()) {
	 				instance+= ",";
	 			}
			}
		}
		if (intervalKey.getType().equals(MonitorKey.INTERVAL_TYPE)) {
			monitorKey = new MonitorKey(MonitorKey.THREADTRACE_TYPE, intervalKey.getName(), 
					"".equals(instance) ? null : instance);
			
		} else if (intervalKey.getType().equals(MonitorKey.THREADTRACE_TYPE)) {
			monitorKey = intervalKey;
		}

		if (monitorKey != null) {
			result = new FieldKey(monitorKey, "stack", FieldKey.STRING_TYPE);
		}
		return result;
	}	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result
				+ ((fieldType == null) ? 0 : fieldType.hashCode());
		result = prime * result
				+ ((monitorKey == null) ? 0 : monitorKey.hashCode());
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
		FieldKey other = (FieldKey) obj;
		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
			return false;
		if (fieldType == null) {
			if (other.fieldType != null)
				return false;
		} else if (!fieldType.equals(other.fieldType))
			return false;
		if (monitorKey == null) {
			if (other.monitorKey != null)
				return false;
		} else if (!monitorKey.equals(other.monitorKey))
			return false;
		return true;
	}

	public static Set<FieldKey> toSet(FieldKey[] fields) {
		Set<FieldKey> result = new HashSet<FieldKey>();
		
		result.addAll(Arrays.asList(fields));
		
		return result;
	}

	public static String[] toStringArray(FieldKey[] fields) {
		String[] result = new String[fields.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = fields[i].toString();
		}
		return result;
	}
	
	public static FieldKey[] toFieldKeyArrayNoThrow(String[] fields) {
		Set<FieldKey> result = new HashSet<FieldKey>(fields.length);
		
		for (int i = 0; i < fields.length; i++) {
			FieldKey key = FieldKey.parseNoThrow(fields[i]);
			if (key != null) {
				result.add(key);
			}
		}
		return result.toArray(new FieldKey[result.size()]);
	}

	public int compareTo(FieldKey o) {
		return this.toString().compareTo(o.toString());
	}
	
	public Object matchObjectToFieldType(Object obj) {
		Object result = obj;
		
		if (INTEGER_TYPE.equals(fieldType)) {
			if (!(obj instanceof Integer)) {
				result = new Integer(((Number)obj).intValue());
			}
		} else if (LONG_TYPE.equals(fieldType) || TIMESTAMP_TYPE.equals(fieldType)) {
			if (!(obj instanceof Long)) {
				result = new Long(((Number)obj).longValue());
			}
		} else if (DOUBLE_TYPE.equals(fieldType)) {
			if (!(obj instanceof Double)) {
				result = new Double(((Number)obj).doubleValue());
			}
		} else if (STRING_TYPE.equals(fieldType) ) {
			if (!(obj instanceof String)) {
				result = obj.toString();
			}
		} 
		
		return result;
	}
}
