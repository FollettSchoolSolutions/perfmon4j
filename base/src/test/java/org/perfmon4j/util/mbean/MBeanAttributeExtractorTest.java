package org.perfmon4j.util.mbean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import junit.framework.TestCase;

public class MBeanAttributeExtractorTest extends TestCase {
	private MBeanServer mBeanServer = null;
	private MBeanQueryEngine engine;
	private static final String BASE_OBJECT_NAME = "org.perfmon4j.util.mbean:type=TestExample";
	private ObjectName objectName;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mBeanServer = MBeanServerFactory.createMBeanServer();
		engine = new MBeanQueryEngine(mBeanServer);
		objectName = new ObjectName(BASE_OBJECT_NAME);
	}

	@Override
	protected void tearDown() throws Exception {
		objectName = null;
		engine = null;
		mBeanServer = null;
		
		super.tearDown();
	}
	
	public void testExtractCounter() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("nextValue").build();
		
		
		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServer, objectName, query);
		MBeanDatum<?> data[] = extractor.extractAttributes();
		
//		assertNotNull("Should never return null", data);
//		assertEquals("Expected element size", 1, data.length);
//		
//		MBeanDatum<?> datum = data[0];
//		assertEquals("Expected datum name", "nextValue", datum.getName());
//		assertEquals("Expected datum Type", MBeanDatum.Type.COUNTER, datum.getType());
//		assertEquals("Expected datum value", Long.valueOf(1), datum.getValue());
	}
}
