package org.perfmon4j.extras.hystrix;

import org.perfmon4j.instrument.snapshot.GeneratedData;


public interface HystrixCommandMonitor extends GeneratedData {
	public long getSuccessCount();
	public long getFailureCount();
	public long getTimeoutCount();
	public long getShortCircuitedCount();
	public long getThreadPoolRejectedCount();
	public long getSemaphoreRejectedCount();
	public String getInstanceName();
}
