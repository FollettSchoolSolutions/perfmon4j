package org.perfmon4j.extras.hystrix;

import org.perfmon4j.instrument.snapshot.GeneratedData;

public interface HystrixThreadPoolData extends HystrixBaseData, GeneratedData {
	public long getExecutedThreadCount();
	public long getRejectedThreadCount();
	public long getCompletedTaskCount();
	public long getScheduledTaskCount();
	
	//Gauges (no deltas)
	public long getMaxActiveThreads();
	public long getCurrentQueueSize();
	public long getCurrentPoolSize();
}
