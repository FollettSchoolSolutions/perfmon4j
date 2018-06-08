package org.perfmon4j.hystrix;

import org.perfmon4j.hystrix.CommandStats;
import org.perfmon4j.hystrix.CommandStatsAccumulator;
import org.perfmon4j.hystrix.CommandStatsProvider;
import org.perfmon4j.hystrix.CommandStatsRegistry;

import junit.framework.TestCase;

public class CommandStatsRegistryTest extends TestCase {

	public CommandStatsRegistryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testWeakProviderReference() {
		CommandStatsRegistry registry = new CommandStatsRegistry();
		
		ProviderImpl a = new ProviderImpl();
		ProviderImpl b = new ProviderImpl();
		
		registry.registerProvider(a);
		registry.registerProvider(b);
		
		CommandStats stats = registry.getStats().getStats("default");
		assertEquals("Should have stats from provider a and b", 2, stats.getSuccessCount());
		
		// Make one of the providers available for garbage collection.
		a = null;
		
		// Advise the GarbageCollector to collect non-referenced objects;
		System.gc();
		
		stats = registry.getStats().getStats("default");
		assertEquals("Provider a should no longer be included", 1, stats.getSuccessCount());
	}
	
	private static class ProviderImpl implements CommandStatsProvider {

		public void collectStats(CommandStatsAccumulator accumulator) {
			CommandStats stats = CommandStats
				.builder()
				.setSuccessCount(1)
				.build();
			
			accumulator.increment("default", stats);
		}
	}
	
	
	

}
