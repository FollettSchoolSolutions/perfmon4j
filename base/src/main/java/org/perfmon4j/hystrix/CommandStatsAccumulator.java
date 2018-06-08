package org.perfmon4j.hystrix;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandStatsAccumulator {
	private final Map<String, CommandStats> statsMap = new HashMap<String, CommandStats>();
	private final Object lockToken = new Object();

	public CommandStats increment(String context, CommandStats stats) {
		CommandStats result = stats;
		synchronized(lockToken) {
			CommandStats current = statsMap.get(context);
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
	
	public CommandStats getStats(String context) {
		synchronized(lockToken) {
			CommandStats result = statsMap.get(context);
			if (result == null) {
				result = CommandStats.builder().build();
			}
			return result;
		}
	}
}
