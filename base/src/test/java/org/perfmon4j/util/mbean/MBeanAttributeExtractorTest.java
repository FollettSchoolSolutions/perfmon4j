package org.perfmon4j.util.mbean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;
import org.perfmon4j.util.mbean.MBeanDatum.Type;

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
	
	public void testBuildData_NoMatchingAttributes() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("THIS WILL NOT MATCH ANYTHING").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 0, dataDefinition.length);
	}
	
	public void testBuildData_ExactMatch() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("NextValue").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 1, dataDefinition.length);
		assertEquals("Should be the actual name of the MBean Attribute", "NextValue", dataDefinition[0].getName());
		assertEquals("Type of measurement", Type.COUNTER, dataDefinition[0].getType());
	}
	
	
	
	/**
	 * JMX Attribute names are commonly in camel case, however there seems to be an inconsistency
	 * regarding the first letter being capitalized -- sometimes it is, sometimes not.  
	 * We want to be forgiving an match a definition of "nextValue" to "NextValue" or "nextValue".
	 * @throws Exception
	 */
	public void testBuildData_IncorrectCapitalizationMatch() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("nextValue").build();  // Lower case first letter does not strictly match, but we want to be forgiving.
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 1, dataDefinition.length);
	}

	public void testBuildData_FindGauge() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("nextValue").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 1, dataDefinition.length);
		assertEquals("Should be the actual name of the MBean Attribute", "NextValue", dataDefinition[0].getName());
		assertEquals("Type of measurement", Type.GAUGE, dataDefinition[0].getType());
	}

	public void testExtractData() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("nextValue").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServer, objectName, query);
		MBeanDatum<?>[] data = extractor.extractAttributes();
		
		assertNotNull(data);
		assertEquals("Expected number of elements", 1, data.length);
		MBeanDatum<?> d = data[0];
	
		assertEquals("Expected name", "NextValue", d.getName());
		assertEquals("Expected type", Type.COUNTER, d.getType());
		assertEquals("Expected Value", Long.valueOf(0), d.getValue());
	}
	
}
