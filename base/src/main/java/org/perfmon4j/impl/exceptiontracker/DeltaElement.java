/*
 *	Copyright 2021 Follett School Solutions, LLC 
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
 * 	ddeucher@follett.com
 * 	David Deuchert
 * 	Follett School Solutions, LLC
 * 	1340 Ridgeview Dr
 * 	McHenry, IL 60050
*/

package org.perfmon4j.impl.exceptiontracker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.PerfMon;
import org.perfmon4j.instrument.snapshot.Delta;

public class DeltaElement extends Element {
	private final Delta count;
	private final Delta sqlCount;
	
	public DeltaElement(MeasurementElement start, MeasurementElement end, long duration) {
		super(start.getFieldName());
		count = new Delta(start.getCount(), end.getCount(), duration);
		
		long startSQLCount = start.getSqlCount();
		long endSQLCount = end.getSqlCount();
		if (startSQLCount != PerfMon.NOT_SET && endSQLCount != PerfMon.NOT_SET) {
			sqlCount = new Delta(startSQLCount, endSQLCount, duration);
		} else {
			sqlCount = null;
		}
	}

	public Delta getCount() {
		return count;
	}

	public Delta getSqlCount() {
		return sqlCount;
	}
	
	public static Set<DeltaElement> createDeltaSet(Map<String, MeasurementElement> startMap, Map<String, MeasurementElement> endMap, long duration) {
		Set<DeltaElement> result = new HashSet<DeltaElement>();

		for (Map.Entry<String, MeasurementElement> entry : startMap.entrySet()) {
			MeasurementElement end = endMap.get(entry.getKey());
			if (end != null) {
				result.add(new DeltaElement(entry.getValue(), end, duration));
			}
		}
		
		return result;
	}
}
