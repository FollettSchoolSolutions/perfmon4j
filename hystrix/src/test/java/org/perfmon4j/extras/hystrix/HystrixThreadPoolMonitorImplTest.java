package org.perfmon4j.extras.hystrix;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;

import com.netflix.hystrix.HystrixThreadPoolMetrics;

public class HystrixThreadPoolMonitorImplTest extends TestCase {

	public HystrixThreadPoolMonitorImplTest(String name) {
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
			gen.generateBundle(HystrixThreadPoolMonitorImpl.class, instanceName);
			fail("Expected instance name to be required");
		} catch (GenerateSnapShotException ex) {
		}
		
		instanceName = "Test";
		try {
			gen.generateBundle(HystrixThreadPoolMonitorImpl.class, instanceName);
		} catch (GenerateSnapShotException ex) {
			fail("Should succeed with non-nul instance name");
		}
	}
	
	public void testGettersFromHystrixMetrics() throws Exception {
		HystrixThreadPoolMonitorImpl impl = Mockito.spy(new HystrixThreadPoolMonitorImpl("Test"));
		HystrixThreadPoolMetrics metrics = Mockito.mock(HystrixThreadPoolMetrics.class);

		Mockito.when(metrics.getCumulativeCountThreadsExecuted()).thenReturn(Long.valueOf(1));
		Mockito.when(metrics.getCumulativeCountThreadsRejected()).thenReturn(Long.valueOf(2));
		Mockito.when(metrics.getCurrentCompletedTaskCount()).thenReturn(Long.valueOf(3));
		Mockito.when(metrics.getCurrentTaskCount()).thenReturn(Long.valueOf(4));
		Mockito.when(metrics.getRollingMaxActiveThreads()).thenReturn(Long.valueOf(5));
		Mockito.when(metrics.getCurrentQueueSize()).thenReturn(Long.valueOf(6));
		Mockito.when(metrics.getCurrentPoolSize()).thenReturn(Long.valueOf(7));
		
		Mockito.when(impl.getOrCreateMetrics()).thenReturn(metrics);
		
		assertEquals("Executed Thread count", 1, impl.getExecutedThreadCount());
		assertEquals("Rejected Thread count", 2, impl.getRejectedThreadCount());
		assertEquals("Completed Task count", 3, impl.getCompletedTaskCount());
		assertEquals("Scheduled Task count", 4, impl.getScheduledTaskCount());
		assertEquals("Max Active Threads", 5, impl.getMaxActiveThreads());
		assertEquals("Current Queue Size", 6, impl.getCurrentQueueSize());
		assertEquals("Current Pool Size", 7, impl.getCurrentPoolSize());
	}
}
