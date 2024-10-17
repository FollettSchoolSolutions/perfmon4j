package org.perfmon4j.util.mbean;

import junit.framework.TestCase;

public class MBeanQueryBuilderTest extends TestCase {
	/*
	For reference here is an example of the parameters used to query MBeans  
	 
	<JMXSnapshot name="WildflyThreadPool" 
			jmxName="jboss:threads:type=thread-pool" 
			instanceKey="name"  
			gauges="poolSize,activeCount,largestPoolSize,queueSize,largestQueueSize" 
			counters="completedTaskCount,rejectedTaskCount,submittedTaskCount,spinMissCount" 
			ratios="completedPercent=completedTaskCount/submittedTaskCount">
			
	</JMXSnapShot>
	*/
	
	public void testSimpleBuild() throws Exception {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		MBeanQuery query = builder.build();
		
		assertNotNull(query);
		assertEquals("jboss:threads:type=thread-pool", query.getBaseJMXName());
		assertNull("instanceKey is optional", query.getInstanceKey());
		assertEquals("gauges are optional", 0, query.getGauges().length);
		assertEquals("counters are optional", 0, query.getCounters().length);
	}

	public void testAddInstanceKey() throws Exception {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		
		MBeanQuery query = builder
			.setInstanceKey("name")
			.build();
		assertEquals("name", query.getInstanceKey());
	}
	
	public void testSetGauges() throws Exception {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		
		MBeanQuery query = builder
			.setGauges("poolSize,activeCount,largestPoolSize,queueSize,largestQueueSize")
			.build();
		assertEquals("gauges length", 5, query.getGauges().length);
		assertEquals("gauges should be sorted", "activeCount", query.getGauges()[0]);
	}
	
	public void testSetCounters() throws Exception {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		
		MBeanQuery query = builder
			.setCounters("rejectedTaskCount,submittedTaskCount,spinMissCount,completedTaskCount")
			.build();
		assertEquals("counters length", 4, query.getCounters().length);
		assertEquals("counters should be sorted", "completedTaskCount", query.getCounters()[0]);
	}
	
	public void testEquals() throws Exception {
		MBeanQuery queryA = new MBeanQueryBuilder("jboss:threads:type=thread-pool")
			.setCounters("rejectedTaskCount,submittedTaskCount,spinMissCount,completedTaskCount")
			.setGauges("poolSize,activeCount,largestPoolSize,queueSize,largestQueueSize")
			.build();
		
		MBeanQuery queryB = new MBeanQueryBuilder("jboss:threads:type=thread-pool")
			.setCounters("completedTaskCount,rejectedTaskCount,submittedTaskCount,spinMissCount")  // Note order of Counter should not be significant.
			.setGauges("largestQueueSize,poolSize,activeCount,largestPoolSize,queueSize")// Note order of Gauges should not be significant.
			.build();
		
		assertEquals("signatures match", queryA.getSignature(), queryB.getSignature());
		assertEquals("hashCode", queryA.hashCode(), queryB.hashCode());
		assertEquals(queryA, queryB);
	}
	
	public void testIncludeRatio() throws Exception {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		MBeanQuery query = builder.setCounters("rejectedTaskCount,submittedTaskCount,spinMissCount,completedTaskCount")
		.setGauges("poolSize,activeCount,largestPoolSize,queueSize,largestQueueSize")
		.setRatios("rejectedPercent=rejectedTaskCount/submittedTaskCount,inUsePercent=activeCount/queueSize")
		.build();
		
		assertNotNull(query);
		
		SnapShotRatio ratios[] = query.getRatios();
		assertNotNull("Should always return ratios", ratios);
		assertEquals("ratios.length", 2, ratios.length);

		// Ratios should return in Alpha order.
		assertEquals("ratio.name", "inUsePercent", ratios[0].getName());
		assertEquals("ratio.name", "rejectedPercent", ratios[1].getName());
	}

	public void testRatioHasVaildName() throws Exception {
		MBeanQueryBuilder builder = new MBeanQueryBuilder("jboss:threads:type=thread-pool");
		MBeanQuery query = builder.setCounters("rejectedTaskCount,submittedTaskCount,spinMissCount,completedTaskCount")
		.setRatios("  =rejectedTaskCount/submittedTaskCount") // missing name.
		.build();
		
		assertEquals("Ratio name must be provided and must contain non white-space characters", 0, query.getRatios().length);
	}


}
