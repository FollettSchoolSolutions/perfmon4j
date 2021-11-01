package org.perfmon4j.impl.exceptiontracker;

import org.perfmon4j.PerfMon;

public class MeasurementElement extends Element {
	private final long count;
	private final long sqlCount;
	
	public MeasurementElement(String fieldName, long count) {
		this(fieldName, count, PerfMon.NOT_SET);
	}
	
	public MeasurementElement(String fieldName, Counter counter) {
		this(fieldName, counter.getCount(), counter.getSQLCount());
	}

	public MeasurementElement(String fieldName, long count, long sqlCount) {
		super(fieldName);
		this.count = count;
		this.sqlCount = sqlCount;
	}
	
	public long getCount() {
		return count;
	}

	public long getSqlCount() {
		return sqlCount;
	}
}
