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

package org.perfmon4j.remotemanagement;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;

public class MonitorKeyWithFields extends MonitorKey {
	private final Set<FieldKey> fields;
	
	
	
	public MonitorKeyWithFields(MonitorKey key, Collection<FieldKey> fields) {
		super(key.getType(), key.getName(), key.getInstance());
		this.fields = new HashSet<FieldKey>(fields);
	}

	public FieldKey[] getFields() {
		return fields.toArray(new FieldKey[]{});
	}
	
	public MonitorKey getMonitorKeyOnly() {
		return new MonitorKey(getType(), getName(), getInstance());
	}
	
	public static MonitorKeyWithFields[] groupFields(FieldKey fields[]) {
		Map<MonitorKey, Set<FieldKey>> map = new HashMap<MonitorKey, Set<FieldKey>>(); 
		
		// First group into a map...
		for (int i = 0; i < fields.length; i++) {
			FieldKey field = fields[i];
			MonitorKey monitor = field.getMonitorKey();
			
			Set<FieldKey> set = map.get(monitor);
			if (set == null) {
				set = new HashSet<FieldKey>();
				map.put(monitor, set);
			}
			set.add(field);
		}
		
		// Then fill the array to return.
		MonitorKeyWithFields[] result = new MonitorKeyWithFields[map.size()];
		Iterator<Map.Entry<MonitorKey, Set<FieldKey>>> itr = map.entrySet().iterator();
		int index = 0;
		
		while (itr.hasNext()) {
			Map.Entry<MonitorKey, Set<FieldKey>> entry = itr.next();
			result[index++] = new MonitorKeyWithFields(entry.getKey(), entry.getValue());
		}
		
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonitorKeyWithFields other = (MonitorKeyWithFields) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}
}
