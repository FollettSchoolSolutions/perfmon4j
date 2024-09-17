package org.perfmon4j.util.mbean;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import junit.framework.TestCase;

public class MBeanQueryEngineTest extends TestCase {
	private MBeanServer mBeanServer = null;
	private MBeanQueryEngine engine;
	private static final String BASE_OBJECT_NAME = "org.perfmon4j.util.mbean:type=TestExample";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mBeanServer = MBeanServerFactory.createMBeanServer();
		engine = new MBeanQueryEngine(mBeanServer);
	}

	@Override
	protected void tearDown() throws Exception {
		engine = null;
		mBeanServer = null;
		
		super.tearDown();
	}

	public void testNotFound() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.build();
		MBeanQueryResult result = engine.doQuery(query);
		
		assertNotNull("Should never return null", result);
		assertEquals("Should just return empty results", 0, result.getInstances().length);
	}
	
	public void testExactMatchQuery() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("nextValue").build();
		
		MBeanQueryResult result = engine.doQuery(query);
		assertNotNull("Should never return null", result);
		assertEquals("Expected number of instances", 1, result.getInstances().length);
	}

	public static interface TestExampleMBean {
		public int getNextValue();
	}

	public static class TestExample implements TestExampleMBean {
		private static final AtomicInteger nextValue = new AtomicInteger(0);

		@Override
		public int getNextValue() {
			return nextValue.getAndIncrement();
		} 
	}
}
