package org.perfmon4j.hystrix;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.GeneratedData;


public interface HystrixCommandData extends HystrixBaseData, GeneratedData {
	public Delta getSuccessCount();
	public Delta getFailureCount();
	public Delta getTimeoutCount();
	public Delta getShortCircuitedCount();
	public Delta getThreadPoolRejectedCount();
	public Delta getSemaphoreRejectedCount();
}
