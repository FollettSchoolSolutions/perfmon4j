package org.perfmon4j.hystrix;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.GeneratedData;

public interface HystrixThreadPoolData extends HystrixBaseData, GeneratedData {
	public Delta getExecutedThreadCount();
	public Delta getRejectedThreadCount();
	public Delta getCompletedTaskCount();
	public Delta getScheduledTaskCount();
	
	//Gauges (no deltas)
	public long getMaxActiveThreads();
	public long getCurrentQueueSize();
	public long getCurrentPoolSize();
}
