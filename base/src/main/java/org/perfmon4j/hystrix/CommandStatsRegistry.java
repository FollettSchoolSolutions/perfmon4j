package org.perfmon4j.hystrix;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * You might wonder how Hystrix classes get registered with this CommandStatsRegistry.  
 * It is done by the Perfmon4J java agent when it discovers 
 * the class com.netflix.hystrix.HystrixCommandMetrics being loaded.
 * 
 * See org.perfmon4j.instrument.JavassistRuntimeTimerInjector.installHystrixCommandMetricsHook() 
 * for details on the class instrumentation.
 * 
 * @author perfmon
 */
public class CommandStatsRegistry {
	private static final CommandStatsRegistry singleton = new CommandStatsRegistry(Integer.getInteger(
			CommandStatsRegistry.class.getName() + "CACHE_DURATION_MILLIS", 2000).intValue());
	private final int cacheMilliseconds;
	private CommandStatsAccumulator cachedValue = null;
	private long cacheLastUpdated = 0;
	
	public static CommandStatsRegistry getRegistry() {
		return singleton;
	}

	private final Set<WeakReference<CommandStatsProvider>> providers = 
			Collections.synchronizedSet(new HashSet<WeakReference<CommandStatsProvider>>());
	
	/**
	 * Other than for unit testing you should use the singleton: 
	 * 		CommandStatsRegistry.getRegistry();
	 */
	/* package scope for unit testing */ CommandStatsRegistry() {
		this(0); // No caching for unit testing.
	}
	
	
	private CommandStatsRegistry(int cacheMilliseconds) {
		this.cacheMilliseconds = cacheMilliseconds;
	}
	
	
	/**
	 * Since within an ApplicationServer Hystrix instances can/come and go 
	 * (Classes being loaded/Unloaded) we must maintain a weak reference 
	 * to the providers.  This will allow Hystrix classes to be garbage collected
	 * when their classloader is removed.
	 * @param provider
	 */
	public void registerProvider(CommandStatsProvider provider) {
		providers.add(new WeakReference<CommandStatsProvider>(provider));
	}

	public int getNumActiveProviders() {
		int result = 0;

		for (WeakReference<CommandStatsProvider> r : providers) {
			if (r.get() != null) {
				result++;
			}
		}
		
		return result;
	}
	
	
	public CommandStatsAccumulator getStats() {
		if (cachedValue != null && System.currentTimeMillis() < (cacheLastUpdated + cacheMilliseconds)) {
			return cachedValue;
		}
		
		CommandStatsAccumulator result = new CommandStatsAccumulator();
		
		for (WeakReference<CommandStatsProvider> r : providers) {
			CommandStatsProvider provider = r.get();
			if (provider != null) {
				provider.collectStats(result);
			}
		}
		
		cachedValue = result;
		cacheLastUpdated = System.currentTimeMillis();
		
		return result;
	}

	
}
