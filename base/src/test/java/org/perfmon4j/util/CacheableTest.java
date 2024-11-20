package org.perfmon4j.util;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

public class CacheableTest extends TestCase {

	public void testBuild() throws Exception {
		final AtomicInteger counter = new AtomicInteger(0);
		
		Cacheable<Integer> cacheable = new Cacheable<>(() -> Integer.valueOf(counter.incrementAndGet()), 50);   
		
		Integer callA = cacheable.get();
		Integer callB = cacheable.get();
		
		assertEquals("First call should have created object, second call should have returned cached object", callA, callB);
		
		Thread.sleep(100); // Sleep so cache expires
		Integer callC = cacheable.get();
		
		assertFalse("Cache has expired, we should now receive a new object", callA.equals(callC));
	}
	
	public void testInvalidate() throws Exception {
		final AtomicInteger counter = new AtomicInteger(0);
		
		Cacheable<Integer> cacheable = new Cacheable<>(() -> Integer.valueOf(counter.incrementAndGet()), 50);   
		
		Integer callA = cacheable.get();
		cacheable.invalidate();
		Integer callB = cacheable.get();
		
		assertFalse("Since we invalidated the cache we should recieve a newly created object", callA.equals(callB));
	}
	
}
