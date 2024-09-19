package org.perfmon4j.util.mbean;

import org.perfmon4j.util.mbean.MBeanQuery;
import org.perfmon4j.util.mbean.MBeanQueryBuilder;

import junit.framework.TestCase;

public class MBeanQueryBuilderTest extends TestCase {
	/*
	For reference here is an example of the parameters used to query MBeans  
	 
	<JMXSnapshot name="WildflyThreadPool" 
			jmxName="jboss:threads:type=thread-pool" 
			instanceName="name"  
			gauges="poolSize,activeCount,largestPoolSize,queueSize,largestQueueSize" 
			counters="completedTaskCount,rejectedTaskCount,submittedTaskCount,spinMissCount" >
	</JMXSnapShot>
	
	*/
	
	public void testSimpleBuild() {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		MBeanQuery query = builder.build();
		
		assertNotNull(query);
		assertEquals("jboss:threads:type=thread-pool", query.getBaseJMXName());
		assertNull("instanceName is optional", query.getInstancePropertyKey());
		assertEquals("gauges are optional", 0, query.getGauges().length);
		assertEquals("counters are optional", 0, query.getCounters().length);
	}

	public void testAddInstanceName() {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		
		MBeanQuery query = builder
			.setInstanceName("name")
			.build();
		assertEquals("name", query.getInstancePropertyKey());
	}
	
	public void testSetGauges() {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		
		MBeanQuery query = builder
			.setGauges("poolSize,activeCount,largestPoolSize,queueSize,largestQueueSize")
			.build();
		assertEquals("gauges length", 5, query.getGauges().length);
		assertEquals("gauges should be sorted", "activeCount", query.getGauges()[0]);
	}
	
	public void testSetCounters() {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		
		MBeanQuery query = builder
			.setCounters("rejectedTaskCount,submittedTaskCount,spinMissCount,completedTaskCount")
			.build();
		assertEquals("counters length", 4, query.getCounters().length);
		assertEquals("counters should be sorted", "completedTaskCount", query.getCounters()[0]);
	}
	
	public void testEquals() {
		MBeanQuery queryA = new MBeanQueryBuilder("jboss:threads:type=thread-pool")
			.setCounters("rejectedTaskCount,submittedTaskCount,spinMissCount,completedTaskCount")
			.setGauges("poolSize,activeCount,largestPoolSize,queueSize,largestQueueSize")
			.build();
		
		MBeanQuery queryB = new MBeanQueryBuilder("jboss:threads:type=thread-pool")
			.setCounters("rejectedTaskCount,submittedTaskCount,spinMissCount,completedTaskCount")
			.setGauges("poolSize,activeCount,largestPoolSize,queueSize,largestQueueSize")
			.build();
		
		assertEquals("hashCode", queryA.hashCode(), queryB.hashCode());
		assertEquals(queryA, queryB);
		
	}

}
