package org.perfmon4j.util.mbean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;
import org.perfmon4j.util.mbean.MBeanDatum.AttributeType;
import org.perfmon4j.util.mbean.MBeanDatum.OutputType;

import junit.framework.TestCase;

public class CompositeDataWrapperTest extends TestCase {
	private CompositeData compositeData;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
		ObjectName objectName = new ObjectName("org.perfmon4j.util.mbean:type=TestExample");
		mBeanServer.registerMBean(new TestExample(), objectName);
		compositeData = (CompositeData)mBeanServer.getAttribute(objectName, "CompositeData");
	}

	@Override
	protected void tearDown() throws Exception {
		compositeData = null;
		super.tearDown();
	}
	
	public void testGetDataDefinition_Long() throws Exception {
		CompositeDataWrapper wrapper = new CompositeDataWrapper(compositeData, "CompositeData");
		
		DatumDefinition dd = wrapper.getDataDefinition("completed", OutputType.GAUGE);
		assertEquals(AttributeType.LONG, dd.getAttributeType());
		assertEquals("Expected to include prefix of the CompositeData object name", "CompositeData.completed", dd.getName()); 
		assertEquals(OutputType.GAUGE, dd.getOutputType());
	}
	
	public void testForgiveFirstLetterCaseOfAttributeName() throws Exception {
		CompositeDataWrapper wrapper = new CompositeDataWrapper(compositeData, "CompositeData");
		
		DatumDefinition dd = wrapper.getDataDefinition("Completed", OutputType.GAUGE);
		assertNotNull("Should have found completed field even with first letter capitalized", dd);
		assertEquals("Should fully match case of attribute", "CompositeData.completed", dd.getName()); 
	}	
	
	public void testGetDataDefinition_Object() throws Exception {
		CompositeDataWrapper wrapper = new CompositeDataWrapper(compositeData, "CompositeData");
		
		DatumDefinition dd = wrapper.getDataDefinition("bigDecimal", OutputType.GAUGE);
		assertEquals("BigDecimal is an unsupported object type - will be returned as a String", 
			AttributeType.STRING, dd.getAttributeType());
		assertEquals("Expected to include prefix of the CompositeData object name", "CompositeData.bigDecimal", dd.getName()); 
		assertEquals(OutputType.GAUGE, dd.getOutputType());
	}
	
	public void testFindWithFullyQualifiedAttributeName() throws Exception {
		CompositeDataWrapper wrapper = new CompositeDataWrapper(compositeData, "CompositeData");
		
		DatumDefinition dd = wrapper.getDataDefinition("CompositeData.completed", OutputType.GAUGE);
		assertNotNull("Should be able to prefix object with baseName", dd);
		
		// Should also be forgiving if the case of the first letter is incorrect.
		dd = wrapper.getDataDefinition("compositeData.completed", OutputType.GAUGE);
		assertNotNull("Should be able to prefix object with baseName", dd);
	}
	
}
