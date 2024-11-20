package org.perfmon4j.util;

public class Cacheable<T> {
	private final Builder<T> builder;
	private final long cacheDurationMillis;
	private long lastBuildTime = -1;
	private T cachedObject = null;

	public Cacheable(Builder<T> builder, long cacheDurationMillis) {
		this.builder = builder;
		this.cacheDurationMillis = cacheDurationMillis;
	}
	
	public synchronized T get() throws Exception {
		long now = System.currentTimeMillis();
		
		if (cachedObject == null || (lastBuildTime + cacheDurationMillis) < now) {
			cachedObject = builder.build();
			lastBuildTime = now;
		}
		
		return cachedObject;
	}
	
	public synchronized void invalidate() {
		cachedObject = null;
	}
	
	@FunctionalInterface
	public static interface Builder<T> {
		T build() throws Exception;
	}
}
