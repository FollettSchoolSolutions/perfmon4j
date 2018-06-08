package org.perfmon4j.hystrix;

import org.perfmon4j.instrument.snapshot.GeneratedData;


public interface HystrixCommandData extends HystrixBaseData, GeneratedData {
	public long getSuccessCount();
	public long getFailureCount();
	public long getTimeoutCount();
	public long getShortCircuitedCount();
	public long getThreadPoolRejectedCount();
	public long getSemaphoreRejectedCount();
}
