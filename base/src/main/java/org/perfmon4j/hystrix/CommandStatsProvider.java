package org.perfmon4j.hystrix;

public interface CommandStatsProvider {
	public void collectStats(CommandStatsAccumulator accumulator);
}
