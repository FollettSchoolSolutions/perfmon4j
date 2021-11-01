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
