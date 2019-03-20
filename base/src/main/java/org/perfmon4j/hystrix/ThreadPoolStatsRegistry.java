package org.perfmon4j.hystrix;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * You might wonder how Hystrix classes get registered with the ThreadPoolStatsRegistry.  
 * It is done by the Perfmon4J java agent when it discovers 
 * the class com.netflix.hystrix.HystrixThreadPoolMetrics being loaded.
 * 
 * See org.perfmon4j.instrument.JavassistRuntimeTimerInjector.installHystrixThreadPoolMetricsHook() 
 * for details on the class instrumentation.
 * 
 * @author David Deuchert
 */
public class ThreadPoolStatsRegistry {
	private static final ThreadPoolStatsRegistry singleton = new ThreadPoolStatsRegistry(Integer.getInteger(
			ThreadPoolStatsRegistry.class.getName() + ".CACHE_DURATION_MILLIS", 500).intValue());
	private final int cacheMilliseconds;
	private ThreadPoolStatsAccumulator cachedValue = null;
	private long cacheLastUpdated = 0;
	
	public static ThreadPoolStatsRegistry getRegistry() {
		return singleton;
	}

	private final Set<WeakReference<ThreadPoolStatsProvider>> providers = 
			Collections.synchronizedSet(new HashSet<WeakReference<ThreadPoolStatsProvider>>());
	
	/**
	 * Other than for unit testing you should use the singleton: 
	 * 		ThreadPoolStatsRegistry.getRegistry();
	 */
	/* package scope for unit testing */ ThreadPoolStatsRegistry() {
		this(0); // No caching for unit testing.
	}
	
	
	private ThreadPoolStatsRegistry(int cacheMilliseconds) {
		this.cacheMilliseconds = cacheMilliseconds;
	}
	
	
	/**
	 * Since within an ApplicationServer Hystrix instances can/come and go 
	 * (Classes being loaded/Unloaded) we must maintain a weak reference 
	 * to the providers.  This will allow Hystrix classes to be garbage collected
	 * when their classloader is removed.
	 * @param provider
	 */
	public void registerProvider(ThreadPoolStatsProvider provider) {
		providers.add(new WeakReference<ThreadPoolStatsProvider>(provider));
	}

	public int getNumActiveProviders() {
		int result = 0;

		for (WeakReference<ThreadPoolStatsProvider> r : providers) {
			if (r.get() != null) {
				result++;
			}
		}
		
		return result;
	}
	
	
	public ThreadPoolStatsAccumulator getStats() {
		if (cachedValue != null && System.currentTimeMillis() < (cacheLastUpdated + cacheMilliseconds)) {
			return cachedValue;
		}
		
		ThreadPoolStatsAccumulator result = new ThreadPoolStatsAccumulator();
		
		for (WeakReference<ThreadPoolStatsProvider> r : providers) {
			ThreadPoolStatsProvider provider = r.get();
			if (provider != null) {
				provider.collectStats(result);
			}
		}
		
		cachedValue = result;
		cacheLastUpdated = System.currentTimeMillis();
		
		return result;
	}
}
