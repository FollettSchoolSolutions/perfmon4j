package org.perfmon4j.hystrix;

public interface ThreadPoolStatsProvider {
	public void collectStats(ThreadPoolStatsAccumulator accumulator);
}
