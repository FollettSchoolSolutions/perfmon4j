package org.perfmon4j.util.mbean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.mockito.Mockito;

import junit.framework.TestCase;

public class MBeanQueryEngineTest extends TestCase {
	private MBeanServer mBeanServer = null;
	private MBeanServerFinder mBeanServerFinder;
	private MBeanQueryEngine engine;
	private static final String BASE_OBJECT_NAME = "org.perfmon4j.util.mbean:type=TestExample";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mBeanServer = MBeanServerFactory.createMBeanServer();
		mBeanServerFinder = Mockito.mock(MBeanServerFinder.class);
		Mockito.when(mBeanServerFinder.getMBeanServer()).thenReturn(mBeanServer);
		engine = new MBeanQueryEngine(mBeanServerFinder);
	}

	@Override
	protected void tearDown() throws Exception {
		engine = null;
		mBeanServerFinder = null;
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
	
	public void testMatchBasedOnQuery() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME + ",name=OldGen"));
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME + ",name=Eden"));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder
			.setInstanceKey("name")	
			.setCounters("nextValue")
			.build();
		
		MBeanQueryResult result = engine.doQuery(query);
		assertNotNull("Should never return null", result);
		assertEquals("Expected number of instances", 2, result.getInstances().length);
	}
	
	public void testNoMatchOnAdditionalProperties() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME + ",name=OldGen,size=small"));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder
			.setInstanceKey("name")	
			.setCounters("nextValue")
			.build();
		
		MBeanQueryResult result = engine.doQuery(query);
		assertNotNull("Should never return null", result);
		assertEquals("Since there is not a 'full` match on the object name we do not expect a match", 
			0, result.getInstances().length);
		
		// Now update object name in query so we have a 'full' match.
		
		builder = new MBeanQueryBuilder(BASE_OBJECT_NAME + ",size=small");
		query = builder
			.setInstanceKey("name")	
			.setCounters("nextValue")
			.build();
		
		result = engine.doQuery(query);
		assertEquals("Now with a 'full' match we should find it", 
				1, result.getInstances().length);
	}
}
