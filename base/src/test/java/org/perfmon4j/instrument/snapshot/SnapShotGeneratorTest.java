/*
 *	Copyright 2008 Follett Software Company 
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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j.instrument.snapshot;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.SQLWriteable;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.SnapShotStringFormatter;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.SnapShotLifecycle;
import org.perfmon4j.util.MiscHelper;

public class SnapShotGeneratorTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public SnapShotGeneratorTest(String name) {
        super(name);
    }


    public static class ArbitraryObject {
    	public String toString() {
    		return "This is an arbitrary object";
    	}
    }
    
    public interface ArbitraryData {
    	public ArbitraryObject getObj1();
    	public ArbitraryObject getObj2();
    }

    @SnapShotProvider(type=SnapShotProvider.Type.STATIC, dataInterface=ArbitraryData.class)
    public static class ArbitraryDataProvider {
    	public static ArbitraryObject obj1 = new ArbitraryObject();
    	public static ArbitraryObject obj2 = new ArbitraryObject();
    	
    	@SnapShotString
		public static ArbitraryObject getObj1() {
			return obj1;
		}
    	
    	@SnapShotString(formatter=CustomStringFormatter.class)
		public static ArbitraryObject getObj2() {
			return obj2;
		}
    }
    
    public void testSnapShotStringAllowsAnyObjectThatCanBeConvertedToAString() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(ArbitraryDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	validateMethodExists(clazz, "getObj1", ArbitraryObject.class);
	  	
	  	SnapShotGenerator.SnapShotLifecycle lc = (SnapShotGenerator.SnapShotLifecycle)clazz.newInstance();

	  	lc.init(null, 1000);
	  	lc.takeSnapShot(null, 2000);
	  	
	  	assertTrue("Should have implemented the interface", lc instanceof ArbitraryData);
	  	assertTrue("Objects should compare", ((ArbitraryData)lc).getObj1() == ArbitraryDataProvider.obj1);
   
	  	String appenderString = ((SnapShotData)lc).toAppenderString();
	  	assertTrue("the obj's to string should have been added to the appenders string", 
	  			appenderString.contains(" obj1..................... This is an arbitrary object"));
    }

    static public class CustomStringFormatter extends SnapShotStringFormatter {
		public String format(int preferredLabelLength, String label, Object data) {
			String v = (data == null) ? null : data.toString();
			return " " + MiscHelper.formatTextDataLine(preferredLabelLength, "CUSTOM(" + label + ")", v);
		}
    }
    
    public void testSnapShotStringUsesCustomFormatter() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(ArbitraryDataProvider.class);
	  	
	  	SnapShotGenerator.SnapShotLifecycle lc = (SnapShotGenerator.SnapShotLifecycle)clazz.newInstance();

	  	lc.init(null, 1000);
	  	lc.takeSnapShot(null, 2000);
	  	
	  	String appenderString = ((SnapShotData)lc).toAppenderString();
System.out.println(appenderString);	  	
	  	assertTrue("the obj's to string should have been added to the appenders string", 
				appenderString.contains(" CUSTOM(obj2)............. This is an arbitrary object"));
    }
    
    
    public interface SimpleData {
    	public long getTotalMemory();
    	public Delta getBytesWritten();
    }
    
    @SnapShotProvider(dataInterface=SimpleData.class)
    public static class SimpleDataProvider {
    	private long totalMemory = 0;
    	private long byteWritten = 0;
	
    	@SnapShotCounter
    	public long getBytesWritten() {
    		return byteWritten;
    	}
    	
    	@SnapShotGauge
    	public long getTotalMemory() {
    		return totalMemory;
    	}

    	public void setTotalMemory(long totalMemory) {
    		this.totalMemory = totalMemory;
    	}
    	
    	// Add all supported gauge types...
    	@SnapShotGauge
    	public boolean getBooleanValue() {
    		return false;
    	}

    	@SnapShotGauge
    	public long getLongValue() {
    		return 0;
    	}

    	@SnapShotGauge
    	public int getIntValue() {
    		return 0;
    	}

    	@SnapShotGauge
    	public short getShortValue() {
    		return 0;
    	}

    	@SnapShotGauge
    	public float getFloatValue() {
    		return 0.0f;
    	}

    	@SnapShotGauge
    	public double getDoubleValue() {
    		return 0.0;
    	}
    }
    
    public void testExtendsSnapShotData() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	Object obj = clazz.newInstance();
	  	
	  	assertTrue(obj instanceof SnapShotData);
	  	SnapShotData d = (SnapShotData)obj;
	  	
	  	// Verify we have name attribute declared in SnapShotData
	  	d.setName("Bogus");
	  	assertEquals("name", "Bogus", d.getName());
    }
    
    public static interface SimpleRatio {
    	public Ratio getKeywordCacheHitRatio();
    	public Ratio getAvailableEntityRatio();
    	public Delta getKeywordSearches();
    	public Delta getKeywordCacheHits();
    }
    
    @SnapShotProvider(type=SnapShotProvider.Type.STATIC,
    		dataInterface=SimpleRatio.class)
    @SnapShotRatios({
	    @SnapShotRatio(name="keywordCacheHitRatio", numerator="keywordCacheHits", 
	    		denominator="keywordSearches", displayAsPercentage=true),
	    @SnapShotRatio(name="availableEntityRatio", numerator="availableEntities", denominator="totalEntities")
	})
    public static class RatioDataProvider {
    	private static long keywordSearches = 0;
    	private static long keywordCacheHits = 0;
    	private static int availableEntities = 0;
    	private static int totalEntities = 0;
	
    	@SnapShotCounter
    	public static long getKeywordSearches() {
    		return keywordSearches;
    	}
    	
    	@SnapShotCounter
    	public static long getKeywordCacheHits() {
    		return keywordCacheHits;
    	}
    	
    	@SnapShotGauge
    	public static int getAvailableEntities() {
    		return availableEntities;
    	}
    	
    	@SnapShotGauge
    	public static int getTotalEntities() {
    		return totalEntities;
    	}
    }
    
    public void testRatio() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(RatioDataProvider.class);
	  	
	  	SnapShotGenerator.SnapShotLifecycle snapShotData = (SnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	SimpleRatio d = (SimpleRatio)snapShotData;
	  	
	  	RatioDataProvider.keywordCacheHits = 0;
	  	RatioDataProvider.keywordSearches = 0;
	  	snapShotData.init(null, 1000);
	  	
	  	RatioDataProvider.keywordCacheHits = 75;
	  	RatioDataProvider.keywordSearches = 101;
	  	RatioDataProvider.availableEntities = 999;
	  	RatioDataProvider.totalEntities = 1001;
	  	
	  	snapShotData.takeSnapShot(null, 2000);

	  	Ratio r = d.getKeywordCacheHitRatio();
	  	assertNotNull("ratio", r);
	  	assertEquals("r.getRatio()", 0.742574257f, r.getRatio());
	  	
	  	r = d.getAvailableEntityRatio();
	  	assertNotNull("ratio", r);
	  	assertEquals("r.getRatio()", 0.998001998f, r.getRatio());
	  	
	  	String appenderString = ((SnapShotData)d).toAppenderString();
//System.out.println(appenderString);

	  	assertTrue("keywordCacheHitRatio should be in appenderString", 
	  			appenderString.contains("keywordCacheHitRatio..... 74.257%"));
	  	assertTrue("availableEntityRatio should be in appenderString", 
	  			appenderString.contains("availableEntityRatio..... 0.998"));
    }
    
    public void testGetCounterData() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	
	  	SnapShotGenerator.SnapShotLifecycle snapShotData = (SnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	SimpleData d = (SimpleData)snapShotData;

	  	SimpleDataProvider p = new SimpleDataProvider();
	  	p.byteWritten = 500;
	  	
	  	assertNull("Should provide null before init", d.getBytesWritten());

	  	snapShotData.init(p, 1000);
	  	
	  	assertNull("Should provide null before takeSnapShot", d.getBytesWritten());

	  	p.byteWritten = 750;
	  	
	  	snapShotData.takeSnapShot(p, 2000);

	  	Delta delta = d.getBytesWritten();
	  	assertNotNull("Should have data after snapShot", delta);
	  	
	  	assertEquals("initialValue", 500, delta.getInitalValue());
	  	assertEquals("finalValue", 750, delta.getFinalValue());
	  	assertEquals("durationMillis", 1000.0, delta.getDurationMillis());
    }

    
    /*----------------------------------------------------------------------------*/    
    public void testGenerateSimpleCounter() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	validateMethodExists(clazz, "getBytesWritten", Delta.class);
    }
    
    public void testInitSetsStartTime() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	
	  	SnapShotGenerator.SnapShotLifecycle scl = (SnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	scl.init(new SimpleDataProvider(), 100);
	  	
	  	assertEquals("startTime", 100, scl.getStartTime());
    }
    
    public void testProvideDataStartEndTimeAndDuration() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	
	  	SnapShotGenerator.SnapShotLifecycle scl = (SnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	SimpleDataProvider p = new SimpleDataProvider();
	  	scl.init(p, 150);
	  	scl.takeSnapShot(p, 200);
	  	
	  	assertEquals("stopTime", 200, scl.getEndTime());
	  	assertEquals("duration", 50, scl.getDuration());
    }

    
    /*----------------------------------------------------------------------------*/    
    public void testGenerateSimpleGauge() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	validateMethodExists(clazz, "getTotalMemory", long.class);
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testImplementSpecifiedInterface() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	assertTrue(implementsInterface(clazz, SimpleData.class));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testImplementsSnapShotLifecycle() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	assertTrue(implementsInterface(clazz, SnapShotGenerator.SnapShotLifecycle.class));
	  	
	  	validateMethodExists(clazz, "getStartTime", long.class);
	  	validateMethodExists(clazz, "getEndTime", long.class);
	  	validateMethodExists(clazz, "getDuration", long.class);
	  	
	  	Class args[] = new Class[]{Object.class, long.class};
	  	validateMethodExists(clazz, "init", void.class, args);
	  	validateMethodExists(clazz, "takeSnapShot", void.class, args);
	  	
	  	SnapShotGenerator.SnapShotLifecycle scl = (SnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	assertEquals("initial value of getStartTime", PerfMon.NOT_SET, scl.getStartTime());
	  	assertEquals("initial value of getEndTime", PerfMon.NOT_SET, scl.getEndTime());
	  	assertEquals("initial value of getDuration", PerfMon.NOT_SET, scl.getDuration());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testDataProviderForGauge() throws Exception {
		Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
		
		SimpleDataProvider provider = new SimpleDataProvider();
		provider.setTotalMemory(10);
		
		SnapShotGenerator.SnapShotLifecycle lc = (SnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
		lc.takeSnapShot(provider, System.currentTimeMillis());
	
		SimpleData d = (SimpleData)lc;
		
		assertEquals("Should have memory timestamp updated", 10, d.getTotalMemory());
	}
  
	  /*----------------------------------------------------------------------------*/    
    public void testGetAllSupportedGaugeTypes() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	validateMethodExists(clazz, "getLongValue", long.class);
	  	validateMethodExists(clazz, "getIntValue", int.class);
	  	validateMethodExists(clazz, "getShortValue", short.class);
	  	validateMethodExists(clazz, "getFloatValue", float.class);
	  	validateMethodExists(clazz, "getDoubleValue", double.class);
	  	validateMethodExists(clazz, "getBooleanValue", boolean.class);
    }

    @SnapShotProvider
    public static class TestProvider {
    	private long totalMemory = 0;
    	private long bytesWritten = 0;
	
    	@SnapShotCounter
    	public long getBytesWritten() {
    		return bytesWritten;
    	}
    	
    	@SnapShotGauge
    	public long getTotalMemory() {
    		return totalMemory;
    	}

    	@SnapShotCounter
    	public long getLongValue() {
    		return 0;
    	}

    	@SnapShotCounter
    	public int getIntValue() {
    		return 0;
    	}

    	@SnapShotCounter
    	public short getShortValue() {
    		return 0;
    	}
    	
    	@SnapShotString
    	public String getStringValue() {
    		return "Anything";
    	}
    }
    
    public void testSnapShotString() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(TestProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	validateMethodExists(clazz, "getStringValue", String.class);
    	
    }
    
    
    public void testGetAllSupportedCounterTypes() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(TestProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	validateMethodExists(clazz, "getLongValue", Delta.class);
	  	validateMethodExists(clazz, "getIntValue", Delta.class);
	  	validateMethodExists(clazz, "getShortValue", Delta.class);
    }   
    
    private void validateStringContains(String value, String expectedContains) {
    	assertTrue("Expected String: \"" + value + "\" to contain: \"" + expectedContains + "\"",
    			value.contains(expectedContains));
    }
    
    
    public void testToAppenderString() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(TestProvider.class);
	  	
	  	SnapShotData d = (SnapShotData)clazz.newInstance();
	  	d.setName("MyData");

	  	SnapShotGenerator.SnapShotLifecycle lc = (SnapShotGenerator.SnapShotLifecycle)d;
	  	
	  	Calendar cal = GregorianCalendar.getInstance();
	  	cal.set(Calendar.HOUR_OF_DAY, 1);
	  	cal.set(Calendar.MINUTE, 5);
	  	cal.set(Calendar.SECOND, 15);
	  	cal.set(Calendar.MILLISECOND, 64);
	  	
	  	long startTime = cal.getTimeInMillis();
	  	long endTime = startTime + 60000;
	  	
	  	TestProvider p = new TestProvider();
	  	p.totalMemory = 512;
	  	p.bytesWritten = 9000;
	  	
	  	lc.init(p, startTime);
	  	
	  	p.bytesWritten = 20000;
	  	
	  	lc.takeSnapShot(p, endTime);

	  	String appenderString = d.toAppenderString();
	  	assertNotNull(appenderString);
//System.out.println(d.toAppenderString());
//        final String expected =
//                "\r\n********************************************************************************\r\n" +
//                "MyData\r\n" +
//                "01:05:15:064 -> 01:06:15:064\r\n" +   
//                " bytesWritten............. 11000/per duration\r\n" +
//                " totalMemory.............. 512\r\n" +
//                " longValue................ 0/per duration\r\n" +
//                " intValue................. 0/per duration\r\n" +
//                " shortValue............... 0/per duration\r\n" +
//                " stringValue.............. Anything\r\n" +
//                "********************************************************************************";
        validateStringContains(appenderString, "MyData\r\n" +
                "01:05:15:064 -> 01:06:15:064\r\n");
       
        validateStringContains(appenderString, " bytesWritten............. 11000/per duration\r\n");
        validateStringContains(appenderString, " totalMemory.............. 512\r\n");
        validateStringContains(appenderString, " longValue................ 0/per duration\r\n");
        validateStringContains(appenderString, " intValue................. 0/per duration\r\n");
        validateStringContains(appenderString, " shortValue............... 0/per duration\r\n");
        validateStringContains(appenderString, " stringValue.............. Anything\r\n");
    }

    
    @SnapShotProvider(type=SnapShotProvider.Type.STATIC, dataInterface=SimpleData.class)
    public static class TestStaticProvider {
    	private static long totalMemory = 0;
    	private static long bytesWritten = 0;
    	
    	@SnapShotCounter
    	public static long getBytesWritten() {
    		return bytesWritten;
    	}
    	
    	@SnapShotGauge
    	public static long getTotalMemory() { 
    		return totalMemory;
    	}

    	@SnapShotCounter
    	public long getBogusCounter() { // Because type is static instance methods will be ignored... Even if annotated.
    		return 0;
    	}
    	
    	@SnapShotGauge
    	public long getBogusGauge() {  // Because type is static instance methods will be ignored... Even if annotated.
    		return 0;
    	}
    }
    
    public void testGenerateSnapShotDataForStaticType() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(TestStaticProvider.class);
	  	SnapShotData d = (SnapShotData)clazz.newInstance();
	  	
	  	validateMethodExists(clazz, "getBytesWritten", Delta.class);
	  	validateMethodExists(clazz, "getTotalMemory", long.class);
	  	
	  	try {
		  	validateMethodExists(clazz, "getBogusCounter", Delta.class);
	  		fail("Did not expect to find instance method");
	  	} catch (AssertionFailedError ae) {
	  		// Expected
	  	}
	  	
	  	try {
		  	validateMethodExists(clazz, "getBogusGauge", long.class);
	  		fail("Did not expect to find instance method");
	  	} catch (AssertionFailedError ae) {
	  		// Expected
	  	}
    }

    public void testProvideDataForStaticType() throws Exception {
	  	Class clazz = SnapShotGenerator.generateSnapShotDataImpl(TestStaticProvider.class);
	  	SnapShotLifecycle lc = (SnapShotLifecycle)clazz.newInstance();

	  	lc.init(null, 1000);
	  	
	  	TestStaticProvider.bytesWritten = 500;
	  	TestStaticProvider.totalMemory = 1024;
	  	
	  	lc.takeSnapShot(null, 2000);
	  	
	  	SimpleData d = (SimpleData)lc;
	  	
	  	assertEquals("totalMemory", 1024, d.getTotalMemory());
	  	assertEquals("bytesWritten", 500, d.getBytesWritten().getDelta());
    }
    
    public void testGenerateBundleForStaticProvider() throws Exception {
	  	SnapShotGenerator.Bundle  bundle = SnapShotGenerator.generateBundle(TestStaticProvider.class);
	  	
	  	TestStaticProvider p = (TestStaticProvider)bundle.getProviderInstance();
	  	assertNull("providerInstance", p);
	  	SnapShotData d = bundle.newSnapShotData();
	  	
	  	assertNotNull("newSnapShotData", d);
    }
    
    @SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR)
    public static class TestInstancePerProvider {
    	private final String instanceName;
    	public TestInstancePerProvider() {
    		instanceName = null;
    	}
    	public TestInstancePerProvider(String instanceName) {
    		this.instanceName = instanceName;
    	}
    }
    
    public void testGenerateBundleForInstancePerMonitorProvider() throws Exception {
    	SnapShotGenerator.Bundle bundle = SnapShotGenerator.generateBundle(TestInstancePerProvider.class);
	  	
    	TestInstancePerProvider p = (TestInstancePerProvider)bundle.getProviderInstance();
	  	assertNotNull("providerInstance", p);
	  	SnapShotData d = bundle.newSnapShotData();
	  	
	  	assertNotNull("newSnapShotData", d);
	  	
	  	// Each call should get a new provider...
    	bundle = SnapShotGenerator.generateBundle(TestInstancePerProvider.class);
    	assertFalse("Each bundle should get a new provider instance", p == bundle.getProviderInstance());
    	
    	// Now pass an instance name.
    	bundle = SnapShotGenerator.generateBundle(TestInstancePerProvider.class, "bogus");
    	p = (TestInstancePerProvider)bundle.getProviderInstance();
    	
    	assertEquals("Should have called constructor with instance name", "bogus", p.instanceName);
    }
    
    @SnapShotProvider(type=SnapShotProvider.Type.FACTORY)
    public static class TestFactoryProvider {
    	private static TestFactoryProvider noInstanceName = new TestFactoryProvider();
    	private static TestFactoryProvider withInstanceName = new TestFactoryProvider();
    	
    	public static TestFactoryProvider getInstance() {
    		return noInstanceName;
    	}
    	
    	public static TestFactoryProvider getInstance(String instanceName) {
    		return withInstanceName;
    	}
    }
    
    public void testGenerateBundleFactoryProvider() throws Exception {
    	SnapShotGenerator.Bundle bundle = SnapShotGenerator.generateBundle(TestFactoryProvider.class);
	  	
    	TestFactoryProvider p = (TestFactoryProvider)bundle.getProviderInstance();
    	assertTrue("Should return single instance", p == TestFactoryProvider.noInstanceName);
    	
    	bundle = SnapShotGenerator.generateBundle(TestFactoryProvider.class, "bogus");
	  	
    	p = (TestFactoryProvider)bundle.getProviderInstance();
    	assertTrue("Should return single instance", p == TestFactoryProvider.withInstanceName);
    }

    @SnapShotProvider
    public static class DataProviderWithPrivateMethod {
    	@SnapShotCounter
    	public long getAccessibleCounter() {
    		return 0;
    	}
    	@SnapShotCounter
    	private long getInAccessibleCounter() {
    		return 0;
    	}
    	
    	@SnapShotGauge
    	public long getAccessibleGauge() {
    		return 0;
    	}
    	@SnapShotGauge
    	private long getInAccessibleGauge() {
    		return 0;
    	}
    	
    	@SnapShotString
    	public String getAccessibleString() {
    		return "";
    	}
    	
    	@SnapShotString
    	private String getInAccessibleString() {
    		return "";
    	}
    	
    }
    
    /**
     * If a snapshot provider contains an non-public methods we should just skip it
     * and log the error....
     * @throws Exception
     */
    public void testInaccessiblePrivateMethodDoesNotBlowUp() throws Exception {
    	SnapShotGenerator.Bundle bundle = SnapShotGenerator.generateBundle(DataProviderWithPrivateMethod.class);
    	
    	SnapShotData data = bundle.newSnapShotData();
    	validateMethodExists(data.getClass(), "getAccessibleCounter", Delta.class);
    	validateMethodExists(data.getClass(), "getAccessibleGauge", long.class);
    	validateMethodExists(data.getClass(), "getAccessibleString", String.class);
    }
    
    private static void validateMethodExists(Class clazz, String methodName, Class clazzReturnType) throws Exception {
    	validateMethodExists(clazz, methodName, clazzReturnType, new Class[]{});
    }
    
    private static void validateMethodExists(Class clazz, String methodName, Class clazzReturnType, Class args[]) throws Exception {
	  	try {
	  		Method method = clazz.getMethod(methodName, args);
	  		assertTrue("Not expected return type", clazzReturnType.equals(method.getReturnType()));
	  	} catch (NoSuchMethodException nse) {
	  		fail("Method " + methodName + " not found");
	  	}
    }
    
    public static boolean implementsInterface(Class clazz, Class interfaceClazz) {
    	Class interfaces[] = clazz.getInterfaces();
    	for (int i = 0; i < interfaces.length; i++) {
			if (interfaces[i].equals(interfaceClazz)) {
				return true;
			}
		}
    	return false;
    }


    public interface SQLCompatibleData {
    	public int getX();
    }
    
    public static class MySQLDataWriter implements SnapShotSQLWriter {
    	private static SnapShotData lastWritten = null;
    	
		public void writeToSQL(Connection conn, String schema, SnapShotData data)
				throws SQLException {
			lastWritten = data;
		}
    }

    @SnapShotProvider(type=SnapShotProvider.Type.STATIC, 
    		dataInterface=SQLCompatibleData.class,
    		sqlWriter=MySQLDataWriter.class)
    public static class SQLCompatibleDataProvider {
    	@SnapShotGauge
		public static int getX() {
			return 1;
		}
    }    

    public void testCreateSQLWritableData() throws Exception {
    	SnapShotGenerator.Bundle bundle = SnapShotGenerator.generateBundle(SQLCompatibleDataProvider.class);
    	SnapShotData data = bundle.newSnapShotData();
    	
    	assertTrue("Should implement the SQLWritable interface", data instanceof SQLWriteable);
    	
    	SQLWriteable w = (SQLWriteable)data;
    	w.writeToSQL(null, "dave");
    	assertTrue("Should have invoked our sql writer", w == MySQLDataWriter.lastWritten);
    	
    	// Should also implement the GeneratedData interface.
    	assertTrue("Should implement the GeneratedData interface", data instanceof GeneratedData);
    	
    	GeneratedData d = (GeneratedData)data;
 
    	// Just invoke the methods to make sure they really exists.
    	d.getDuration();
    	d.getEndTime();
    	d.getName();
    	d.getStartTime();
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
//    	System.setProperty("UNIT", "arg1");
        BasicConfigurator.configure();
        Logger.getLogger(SnapShotGeneratorTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {SnapShotGeneratorTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new SnapShotGeneratorTest("testInaccessiblePrivateMethodDoesNotBlowUp"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(SnapShotGeneratorTest.class);
        }

        return( newSuite);
    }
}
