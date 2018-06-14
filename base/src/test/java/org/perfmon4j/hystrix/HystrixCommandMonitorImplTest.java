package org.perfmon4j.hystrix;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;

public class HystrixCommandMonitorImplTest extends TestCase {

	public HystrixCommandMonitorImplTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGenerationRequriesInstanceName() throws Exception {
		JavassistSnapShotGenerator gen = new JavassistSnapShotGenerator();
		String instanceName = null;
		try {
			gen.generateBundle(HystrixCommandMonitorImpl.class, instanceName);
		} catch (GenerateSnapShotException ex) {
			fail("Instance name is not required");
		}
		
		instanceName = "Test";
		try {
			gen.generateBundle(HystrixCommandMonitorImpl.class, instanceName);
		} catch (GenerateSnapShotException ex) {
			fail("Should succeed with non-nul instance name");
		}
	}
	
	public void testGettersFromHystrixMetrics() throws Exception {
		HystrixCommandMonitorImpl impl = Mockito.spy(new HystrixCommandMonitorImpl("Test"));
		CommandStats stats = Mockito.mock(CommandStats.class);
		
		Mockito.when(stats.getSuccessCount()).thenReturn(Long.valueOf(1));
		Mockito.when(stats.getFailureCount()).thenReturn(Long.valueOf(2));
		Mockito.when(stats.getTimeoutCount()).thenReturn(Long.valueOf(3));
		Mockito.when(stats.getShortCircuitedCount()).thenReturn(Long.valueOf(4));
		Mockito.when(stats.getThreadPoolRejectedCount()).thenReturn(Long.valueOf(5));
		Mockito.when(stats.getSemaphoreRejectedCount()).thenReturn(Long.valueOf(6));

		Mockito.when(impl.getStats()).thenReturn(stats);

		assertEquals("Success count", 1, impl.getSuccessCount());
		assertEquals("Failure count", 2, impl.getFailureCount());
		assertEquals("Timeout count", 3, impl.getTimeoutCount());
		assertEquals("Short Circuted count", 4, impl.getShortCircuitedCount());
		assertEquals("Thread Pool Rejected count", 5, impl.getThreadPoolRejectedCount());
		assertEquals("Semaphore Rejected count", 6, impl.getSemaphoreRejectedCount());
	}
}
