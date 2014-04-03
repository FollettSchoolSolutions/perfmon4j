/*
 *	Copyright 2008,2009 Follett Software Company 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j.instrument.jmx;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotProviderWrapper;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
import org.perfmon4j.instrument.jmx.JMXSnapShotProxyFactory.AttributeConfig;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.util.ByteFormatter;
import org.perfmon4j.util.NumberFormatter;

public class JMXSnapShotProxyFactoryTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    private MBeanServer mBeanServer = null;
    private static final String MBEAN_SERVER_DOMAIN = "JMXFactoryTest";
    private final ObjectName exampleObjectName;
    private Example exampleMBean;
    
/*----------------------------------------------------------------------------*/
    public JMXSnapShotProxyFactoryTest(String name) throws Exception {
        super(name);
        
        exampleObjectName = new ObjectName("JMXProxyFactoryTest:type=Example");
    }
    
/*----------------------------------------------------------------------------*/
    public void setUp() throws Exception {
    	super.setUp();
    	
    	mBeanServer = MBeanServerFactory.createMBeanServer(MBEAN_SERVER_DOMAIN);
    	exampleMBean = new Example();
    	mBeanServer.registerMBean(exampleMBean, exampleObjectName);
    }
  
/*----------------------------------------------------------------------------*/
    public void tearDown() throws Exception {
    	mBeanServer.unregisterMBean(exampleObjectName);
    	MBeanServerFactory.releaseMBeanServer(mBeanServer);

    	mBeanServer = null;
    	exampleMBean = null;
    	
    	super.tearDown();
    }
    
    public static interface ExampleMBean {
    	public long getLongValue();
    	public void setLongValue(long value);

    	public int getIntValue();
    	public void setIntValue(int value);
    	
    	public short getShortValue();
    	public void setShortValue(short value);
    	
    	public float getFloatValue();
    	public void setFloatValue(float value);
    	
    	public double getDoubleValue();
    	public void setDoubleValue(double value);
    	
    	public boolean isBooleanValue();
    	public void setBooleanValue(boolean value);

    	public Object getObjectValue();
    	public void setObjectValue(Object value);

    	public String getStringValue();
    	public void setStringValue(String value);
    	
    	public long getBytesWritten();
    	public void setBytesWritten(long value);
    	
		public long getTotalKeywordHits();
		public void setTotalKeywordHits(long totalKeywordHits);

		public long getCachedKeywordHits();
		public void setCachedKeywordHits(long cachedKeywordHits);
    }

    public static class Example implements ExampleMBean {
    	public static final String OBJECT_NAME = "JMXProxyFactoryTest:type=Example";
    	private long longValue = 0;
    	private double doubleValue = 0.0;
    	private float floatValue = 0.0f;
    	private int intValue = 0;
    	private short shortValue = 0;
    	private boolean booleanValue = false;
    	private Object objectValue = null;
    	private String stringValue = null;
    	
    	private long bytesWritten = 0;
    	private long totalKeywordHits = 0;
    	private long cachedKeywordHits = 0;
    	
		public long getLongValue() {
			return longValue;
		}

		public void setLongValue(long longValue) {
			this.longValue = longValue;
		}

		public double getDoubleValue() {
			return doubleValue;
		}

		public float getFloatValue() {
			return floatValue;
		}

		public int getIntValue() {
			return intValue;
		}

		public short getShortValue() {
			return shortValue;
		}

		public boolean isBooleanValue() {
			return booleanValue;
		}

		public void setBooleanValue(boolean value) {
			booleanValue = value;
		}

		public void setDoubleValue(double value) {
			doubleValue = value;
		}

		public void setFloatValue(float value) {
			floatValue = value;
		}

		public void setIntValue(int value) {
			intValue = value;
		}

		public void setShortValue(short value) {
			shortValue = value;
		}

		public Object getObjectValue() {
			return objectValue;
		}

		public String getStringValue() {
			return stringValue;
		}

		public void setObjectValue(Object value) {
			objectValue = value;
		}

		public void setStringValue(String value) {
			stringValue = value;
		}

		public long getBytesWritten() {
			return bytesWritten;
		}

		public void setBytesWritten(long bytesWritten) {
			this.bytesWritten = bytesWritten;
		}

		public long getTotalKeywordHits() {
			return totalKeywordHits;
		}

		public void setTotalKeywordHits(long totalKeywordHits) {
			this.totalKeywordHits = totalKeywordHits;
		}

		public long getCachedKeywordHits() {
			return cachedKeywordHits;
		}

		public void setCachedKeywordHits(long cachedKeywordHits) {
			this.cachedKeywordHits = cachedKeywordHits;
		}
    }
/*----------------------------------------------------------------------------*/    
    public void testParseClassName() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest'>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	
    	assertNotNull(config);
    	assertEquals("serverDomain", MBEAN_SERVER_DOMAIN, config.getServerDomain());
    }
    
    
    /*----------------------------------------------------------------------------*/    
    public void testParseSnapShotRatios() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest' defaultObjectName='JMXProxyFactoryTest:type=Example'>\r\n" +
    		"    <snapShotRatio name='KeywordHitRatio'\r\n" +
    		"    	denominator='TotalKeywordHits'\r\n" +
    		"    	numerator='CachedKeywordHits'\r\n" +
    		"    	displayAsPercentage='true'/>\r\n" +
    		"    	<attribute name='TotalKeywordHits'>\r\n" +
    		"    		<snapShotCounter/>\r\n" +
    		"    	</attribute>\r\n" +
    		"    	<attribute name='CachedKeywordHits'>\r\n" +
    		"    		<snapShotCounter/>\r\n" +
    		"    	</attribute>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	SnapShotRatios ratios = config.getSnapShotRatios();
    	
    	assertEquals("Should have one snapShotRatio", 1, ratios.value().length);
    	SnapShotRatio ratio = ratios.value()[0];
    	
    	assertEquals("name", "KeywordHitRatio", ratio.name());
    	assertEquals("denominator", "TotalKeywordHits", ratio.denominator());
    	assertEquals("numerator", "CachedKeywordHits", ratio.numerator());
    	assertTrue(ratio.displayAsPercentage());

    	SnapShotProviderWrapper wrapper  = JMXSnapShotProxyFactory.getnerateSnapShotWrapper("test", XML);
    	assertNotNull(wrapper);

    	long now = System.currentTimeMillis();

    	exampleMBean.setTotalKeywordHits(100);	
    	exampleMBean.setCachedKeywordHits(0);	
    	
    	SnapShotData data = wrapper.initSnapShot(now - 60000);

    	exampleMBean.setTotalKeywordHits(100 + exampleMBean.getTotalKeywordHits());	
    	exampleMBean.setCachedKeywordHits(50 + exampleMBean.getCachedKeywordHits());	

    	data = wrapper.takeSnapShot(data, now);
    	String appenderString = data.toAppenderString();
    
//    	System.err.println(appenderString);
    	assertTrue(appenderString.contains("KeywordHitRatio.......... 50.000%"));
    }

    /*----------------------------------------------------------------------------*/    
    public void testParseJMXAttribute() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest'>\r\n" +
    		"    <attribute objectName='dave:type=x' name='bytesWritten'/>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	
    	assertEquals("attributes", 1, config.getAttributeConfigs().size());
    	
    	AttributeConfig jmxAttribute = config.getAttributeConfigs().get(0);
    	
    	assertEquals("attribtue.objectName", "dave:type=x", jmxAttribute.getObjectName());
    	assertEquals("attribtue.name", "bytesWritten", jmxAttribute.getName());
    }

    public void testDefaultObjectName() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest' defaultObjectName='dave:type=x'>\r\n" +
    		"    <attribute name='cpuLoad'/>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	AttributeConfig attr = config.getAttributeConfigs().get(0);
    	
    	assertEquals("Should have used the default object name", "dave:type=x", attr.getObjectName());
    }
    
    public void testAddCounterWithDefault() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest'>\r\n" +
    		"    <attribute objectName='dave:type=x' name='bytesWritten'>\r\n" +
    		"    	<snapShotCounter/>\r\n" +
    		"    </attribute>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	AttributeConfig attr = config.getAttributeConfigs().get(0);
    	
    	assertNotNull(attr);
    	SnapShotCounter counter = attr.getSnapShotCounter();
    	
    	assertNotNull(counter);
    	assertEquals(SnapShotCounter.Display.DELTA, counter.preferredDisplay());
    	assertEquals("", counter.suffix());
    	assertEquals(NumberFormatter.class, counter.formatter());
    }
    
    public void testAddCounterWithCustomAttributes() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest'>\r\n" +
    		"    <attribute objectName='JMXProxyFactoryTest:type=Example' name='BytesWritten'>\r\n" +
    		"    	<snapShotCounter formatter='org.perfmon4j.util.ByteFormatter'\r\n" +
    		"   	  	display='DELTA_PER_MIN'\r\n" +
    		"   	  	suffix=' (Estimated)'/>\r\n" +
    		"    </attribute>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	AttributeConfig attr = config.getAttributeConfigs().get(0);
    	
    	assertNotNull(attr);
    	SnapShotCounter counter = attr.getSnapShotCounter();
    	
    	
    	assertNotNull(counter);
    	assertEquals(" (Estimated)", counter.suffix());
    	assertEquals(ByteFormatter.class, counter.formatter());
    	assertEquals(SnapShotCounter.Display.DELTA_PER_MIN, counter.preferredDisplay());

    	
    	SnapShotProviderWrapper wrapper  = JMXSnapShotProxyFactory.getnerateSnapShotWrapper("test", XML);
    	assertNotNull(wrapper);

    	long now = System.currentTimeMillis();
    	SnapShotData data = wrapper.initSnapShot(now - 60000);

    	exampleMBean.setBytesWritten(1024 * 100);	

    	data = wrapper.takeSnapShot(data, now);
    	String appenderString = data.toAppenderString();
    	
    	
    	assertTrue(appenderString.contains("bytesWritten"));
    	assertTrue(appenderString.contains("100.000 KB (Estimated)"));
    }
    
    public void testAddGaugeWithDefault() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest'>\r\n" +
    		"    <attribute objectName='dave:type=x' name='cpuLoad'>\r\n" +
    		"    	<snapShotGauge/>\r\n" +
    		"    </attribute>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	AttributeConfig attr = config.getAttributeConfigs().get(0);
    	
    	assertNotNull(attr);
    	SnapShotGauge gauge = attr.getSnapShotGauge();
    	
    	assertNotNull(gauge);
    	assertEquals(NumberFormatter.class, gauge.formatter());
    }
    
    public void testAddGaugeWithCustomAttributes() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest'>\r\n" +
    		"    <attribute objectName='dave:type=x' name='cpuLoad'>\r\n" +
    		"    	<snapShotGauge formatter='org.perfmon4j.util.ByteFormatter'/>\r\n" +
    		"    </attribute>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	AttributeConfig attr = config.getAttributeConfigs().get(0);
    	
    	assertNotNull(attr);
    	SnapShotGauge gauge = attr.getSnapShotGauge();
    	
    	assertNotNull(gauge);
    	assertEquals(ByteFormatter.class, gauge.formatter());
    }

    
    /**
     * The jmxName defaults to the attribute name.  This can be overriden by
     * setting the jmxName attribute.
     * @throws Exception
     */
    public void testOverrideJMXName() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest'>\r\n" +
    		"    <attribute objectName='dave:type=x' name='cpuLoad' jmxName='TotalCPU'/>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.Config config = JMXSnapShotProxyFactory.parseConfig(XML);
    	AttributeConfig attr = config.getAttributeConfigs().get(0);
    	
    	assertNotNull(attr);
    	assertEquals("JMXName", "TotalCPU", attr.getJMXName());
    }
    
    public void testSimpleBuildClass() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest'>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.JMXSnapShotImpl impl = JMXSnapShotProxyFactory.newSnapShotImpl(XML);
    	assertEquals("MBEAN_SERVER_DOMAIN", MBEAN_SERVER_DOMAIN, impl.getServerDomain());
    	assertEquals("mBeanServer", mBeanServer, impl.getMBeanServer());
    	assertNotNull("Should have the config imbedded", impl.getConfig());
    	assertEquals("Config should have serverDomain", MBEAN_SERVER_DOMAIN, impl.getConfig().getServerDomain());
    }

    private static boolean methodExists(Class clazz, String methodName, Class returnType) throws Exception {
    	boolean result = false;
    	try {
    		Method method = clazz.getMethod(methodName, new Class[]{});
    		result = method.getReturnType().equals(returnType);
    	} catch (NoSuchMethodException nms) {
    		// Nothing todo
    	}

    	return result;
    }

    private static Object getValue(Object impl, String methodName) throws Exception {
    	Object result = null;
    	
    	Class clazz = impl.getClass();
		Method method = clazz.getMethod(methodName, new Class[]{});
		result = method.invoke(impl, new Object[]{});

    	return result;
    }
    
    
    /**
     * If no annotation is specified for a numeric value...  Assume it is a gauge.
     */
    public void testBuildClassWithDefaultGauge() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest' defaultObjectName='JMXProxyFactoryTest:type=Example'>\r\n" +
    		"	<attribute name='LongValue'/>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.JMXSnapShotImpl impl = JMXSnapShotProxyFactory.newSnapShotImpl(XML);
    	JMXSnapShotProxyFactory.Config config = impl.getConfig();
    	
    	assertEquals(1, config.getAttributeConfigs().size());
    	JMXSnapShotProxyFactory.AttributeConfig attr = config.getAttributeConfigs().get(0);
    	
    	assertNotNull("Numeric attribute should default to a gaugeAnnotation", attr.getSnapShotGauge());
    	
    	assertTrue("Should have a getter returning a long", methodExists(impl.getClass(), "getLongValue",
    			long.class));
    	
    	exampleMBean.setLongValue(5);
    	assertEquals("value from mBean", 5,((Long)getValue(impl, "getLongValue")).longValue());

    	
    	SnapShotProviderWrapper wrapper  = JMXSnapShotProxyFactory.getnerateSnapShotWrapper("test", XML);
    	assertNotNull(wrapper);
    	
    	SnapShotData data = wrapper.initSnapShot(System.currentTimeMillis() - 1000);
    	data = wrapper.takeSnapShot(data, System.currentTimeMillis());
    	
    	assertTrue(data.toAppenderString().contains("longValue"));
    }
    
    public void testBuildAllSupportedNonStringTypes() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest' defaultObjectName='JMXProxyFactoryTest:type=Example'>\r\n" +
    		"	<attribute name='LongValue'/>\r\n" +
    		"	<attribute name='DoubleValue'/>\r\n" +
    		"	<attribute name='FloatValue'/>\r\n" +
    		"	<attribute name='IntValue'/>\r\n" +
    		"	<attribute name='ShortValue'/>\r\n" +
    		"	<attribute name='BooleanValue'/>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.JMXSnapShotImpl impl = JMXSnapShotProxyFactory.newSnapShotImpl(XML);
    	
    	exampleMBean.setLongValue(1);
    	exampleMBean.setDoubleValue(2.2);
    	exampleMBean.setFloatValue(3.3f);
    	exampleMBean.setIntValue(4);
    	exampleMBean.setShortValue((short)5);
    	exampleMBean.setBooleanValue(true);
    
    	
    	assertEquals("longValue", 1,((Long)getValue(impl, "getLongValue")).longValue());
    	assertEquals("doubleValue", 2, Math.round(((Double)getValue(impl, "getDoubleValue")).doubleValue()));
    	assertEquals("floatValue", 3, Math.round(((Float)getValue(impl, "getFloatValue")).floatValue()));
    	assertEquals("intValue", 4,((Integer)getValue(impl, "getIntValue")).intValue());
    	assertEquals("shortValue", ((short)5),((Short)getValue(impl, "getShortValue")).shortValue());
    	assertEquals("booleanValue", true,((Boolean)getValue(impl, "getBooleanValue")).booleanValue());
    }

    /**
     * If no annotation is specified for a String value...  Assume it is a gauge.
     */
    public void testBuildStringType() throws Exception {
    	final String XML = 
    		"<JMXWrapper serverDomain='JMXFactoryTest' defaultObjectName='JMXProxyFactoryTest:type=Example'>\r\n" +
    		"	<attribute name='StringValue'/>\r\n" +
    		"	<attribute name='ObjectValue'/>\r\n" +
    		"</JMXWrapper>";
    	
    	JMXSnapShotProxyFactory.JMXSnapShotImpl impl = JMXSnapShotProxyFactory.newSnapShotImpl(XML);
    	JMXSnapShotProxyFactory.Config config = impl.getConfig();
    	Iterator<JMXSnapShotProxyFactory.AttributeConfig> itr = config.getAttributeConfigs().iterator();
    	
    	// Each one should have a SnapShotString Annotation
    	while (itr.hasNext()) {
    		JMXSnapShotProxyFactory.AttributeConfig attr = itr.next();
    		assertNotNull("Should have a SnapShotString annotation", attr.getSnapShotString());
    	}
    	
    	Properties props = new Properties();
    	exampleMBean.setStringValue("str");
    	exampleMBean.setObjectValue(props);
    	
    	assertEquals("stringValue", "str", getValue(impl, "getStringValue"));
    	assertEquals("objectValue", props.toString(), getValue(impl, "getObjectValue"));
    }

    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(JMXSnapShotProxyFactoryTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {JMXSnapShotProxyFactoryTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() throws Exception {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new JMXSnapShotProxyFactoryTest("testParseSnapShotRatios"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(JMXSnapShotProxyFactoryTest.class);
        }
        return( newSuite);
    }
}
