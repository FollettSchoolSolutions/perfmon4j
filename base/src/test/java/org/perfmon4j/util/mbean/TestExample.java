package org.perfmon4j.util.mbean;

import java.util.concurrent.atomic.AtomicLong;

public class TestExample implements TestExampleMBean {
	private static final AtomicLong nextValue = new AtomicLong(0);

	@Override
	public long getNextValue() {
		return nextValue.getAndIncrement();
	} 
}
