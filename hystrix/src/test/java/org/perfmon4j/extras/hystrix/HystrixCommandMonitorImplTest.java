package org.perfmon4j.extras.hystrix;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;

import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixEventType;

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
			fail("Expected instance name to be required");
		} catch (GenerateSnapShotException ex) {
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
		HystrixCommandMetrics metrics = Mockito.mock(HystrixCommandMetrics.class);

		Mockito.when(metrics.getCumulativeCount(HystrixEventType.SUCCESS)).thenReturn(Long.valueOf(1));
		Mockito.when(metrics.getCumulativeCount(HystrixEventType.FAILURE)).thenReturn(Long.valueOf(2));
		Mockito.when(metrics.getCumulativeCount(HystrixEventType.TIMEOUT)).thenReturn(Long.valueOf(3));
		Mockito.when(metrics.getCumulativeCount(HystrixEventType.SHORT_CIRCUITED)).thenReturn(Long.valueOf(4));
		Mockito.when(metrics.getCumulativeCount(HystrixEventType.THREAD_POOL_REJECTED)).thenReturn(Long.valueOf(5));
		Mockito.when(metrics.getCumulativeCount(HystrixEventType.SEMAPHORE_REJECTED)).thenReturn(Long.valueOf(6));
		
		Mockito.when(impl.getOrCreateMetrics()).thenReturn(metrics);
		
		assertEquals("Success count", 1, impl.getSuccessCount());
		assertEquals("Failure count", 2, impl.getFailureCount());
		assertEquals("Timeout count", 3, impl.getTimeoutCount());
		assertEquals("Short Circuted count", 4, impl.getShortCircuitedCount());
		assertEquals("Thread Pool Rejected count", 5, impl.getThreadPoolRejectedCount());
		assertEquals("Semaphore Rejected count", 6, impl.getSemaphoreRejectedCount());
	}
}
