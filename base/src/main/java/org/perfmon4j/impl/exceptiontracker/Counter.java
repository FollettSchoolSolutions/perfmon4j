package org.perfmon4j.impl.exceptiontracker;

import java.util.concurrent.atomic.AtomicLong;

public class Counter {
	private final AtomicLong count = new AtomicLong();
	private final AtomicLong sqlCount = new AtomicLong();
	
	void incrementCount() {
		count.incrementAndGet();
	}
	
	void incrementSQLCount() {
		sqlCount.incrementAndGet();
	}
	
	public long getCount() {
		return count.get();
	}
	
	public long getSQLCount() {
		return sqlCount.get();
	}
}
