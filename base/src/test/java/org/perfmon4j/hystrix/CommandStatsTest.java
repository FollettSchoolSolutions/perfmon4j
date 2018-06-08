package org.perfmon4j.hystrix;

import org.perfmon4j.hystrix.CommandStats;

import junit.framework.TestCase;

public class CommandStatsTest extends TestCase {

	public CommandStatsTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testBuild() {
		CommandStats stats =
			CommandStats.builder()
				.setSuccessCount(1)
				.setFailureCount(2)
				.setSemaphoreRejectedCount(3)
				.setShortCircuitedCount(4)
				.setThreadPoolRejectedCount(5)
				.setTimeoutCount(6)
				.build();
		
		assertEquals(1, stats.getSuccessCount());
		assertEquals(2, stats.getFailureCount());
		assertEquals(3, stats.getSemaphoreRejectedCount());
		assertEquals(4, stats.getShortCircuitedCount());
		assertEquals(5, stats.getThreadPoolRejectedCount());
		assertEquals(6, stats.getTimeoutCount());
	}

	public void testAdd() {
		CommandStats stats =
				CommandStats.builder()
					.setSuccessCount(1)
					.setFailureCount(2)
					.setSemaphoreRejectedCount(3)
					.setShortCircuitedCount(4)
					.setThreadPoolRejectedCount(5)
					.setTimeoutCount(6)
					.build();
		
		CommandStats statsSum = stats.add(stats);
		
		assertEquals(2,statsSum.getSuccessCount());
		assertEquals(4, statsSum.getFailureCount());
		assertEquals(6, statsSum.getSemaphoreRejectedCount());
		assertEquals(8, statsSum.getShortCircuitedCount());
		assertEquals(10, statsSum.getThreadPoolRejectedCount());
		assertEquals(12, statsSum.getTimeoutCount());
	}


}
