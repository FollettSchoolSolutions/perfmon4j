package org.perfmon4j.hystrix;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ThreadPoolStatsAccumulator {
	private final Map<String, ThreadPoolStats> statsMap = new HashMap<String, ThreadPoolStats>();
	private final Object lockToken = new Object();

	public ThreadPoolStats increment(String context, ThreadPoolStats stats) {
		ThreadPoolStats result = stats;
		synchronized(lockToken) {
			ThreadPoolStats current = statsMap.get(context);
			if (current != null) {
				result = current.add(stats);
			} 
			statsMap.put(context, result);
		}
		
		return result;
	}

	public Set<String> getContexts() {
		synchronized(lockToken) {
			return Collections.unmodifiableSet(statsMap.keySet()) ;
		}
	}
	
	public ThreadPoolStats getStats(String context) {
		synchronized(lockToken) {
			ThreadPoolStats result = statsMap.get(context);
			if (result == null) {
				result = ThreadPoolStats.builder().build();
			}
			return result;
		}
	}
}
