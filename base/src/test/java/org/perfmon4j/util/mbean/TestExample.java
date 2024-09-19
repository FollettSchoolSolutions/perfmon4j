package org.perfmon4j.util.mbean;

import java.util.concurrent.atomic.AtomicInteger;

public class TestExample implements TestExampleMBean {
	private static final AtomicInteger nextValue = new AtomicInteger(0);

	@Override
	public int getNextValue() {
		return nextValue.getAndIncrement();
	} 
}
