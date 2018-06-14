package org.perfmon4j.hystrix;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;

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
		} catch (GenerateSnapShotException ex) {
			fail("Instance name can be null.  You will get a composite monitor of all instances");
		}
		
		instanceName = "Test";
		try {
			gen.generateBundle(HystrixThreadPoolMonitorImpl.class, instanceName);
		} catch (GenerateSnapShotException ex) {
			fail("Should succeed with non-null instance name");
		}
	}
	
	public void testGettersFromHystrixMetrics() throws Exception {
		HystrixThreadPoolMonitorImpl impl = Mockito.spy(new HystrixThreadPoolMonitorImpl("Test"));
		ThreadPoolStats stats = ThreadPoolStats.builder()
			.setExecutedThreadCount(1)
			.setRejectedThreadCount(2)
			.setCompletedTaskCount(3)
			.setScheduledTaskCount(4)
			.setMaxActiveThreads(5)
			.setCurrentQueueSize(6)
			.setCurrentPoolSize(7)
			.build();
				
		Mockito.when(impl.getStats()).thenReturn(stats);
		
		assertEquals("Executed Thread count", 1, impl.getExecutedThreadCount());
		assertEquals("Rejected Thread count", 2, impl.getRejectedThreadCount());
		assertEquals("Completed Task count", 3, impl.getCompletedTaskCount());
		assertEquals("Scheduled Task count", 4, impl.getScheduledTaskCount());
		assertEquals("Max Active Threads", 5, impl.getMaxActiveThreads());
		assertEquals("Current Queue Size", 6, impl.getCurrentQueueSize());
		assertEquals("Current Pool Size", 7, impl.getCurrentPoolSize());
	}
}
