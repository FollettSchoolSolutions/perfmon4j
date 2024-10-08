/*
 *	Copyright 2008-2015 Follett School Solutions 
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
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.PerfMonObservableDatumTest;
import org.perfmon4j.PerfMonTestCase;
import org.perfmon4j.SQLWriteable;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.InstrumentationMonitor;
import org.perfmon4j.instrument.PerfMonTimerTransformer;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotCounter.Display;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotPOJO;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.SnapShotStringFormatter;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.SnapShotLifecycle;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.SnapShotPOJOLifecycle;
import org.perfmon4j.java.management.JVMSnapShot;
import org.perfmon4j.remotemanagement.MonitorKeyWithFields;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.util.MiscHelper;

import junit.framework.AssertionFailedError;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class SnapShotGeneratorTest extends PerfMonTestCase {
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
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(ArbitraryDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	validateMethodExists(clazz, "getObj1", ArbitraryObject.class);
	  	
	  	JavassistSnapShotGenerator.SnapShotLifecycle lc = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();

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
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(ArbitraryDataProvider.class);
	  	
	  	JavassistSnapShotGenerator.SnapShotLifecycle lc = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();

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
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
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
	  	Class<?> clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(RatioDataProvider.class);
	  	
	  	JavassistSnapShotGenerator.SnapShotLifecycle snapShotData = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
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

	  	// Run through one more cycle...
	  	snapShotData = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	d = (SimpleRatio)snapShotData;
	  	
	  	snapShotData.init(null, 2000);

	  	RatioDataProvider.keywordCacheHits += 50;   
	  	RatioDataProvider.keywordSearches += 100;
	  	
	  	snapShotData.takeSnapShot(null, 3000);
    
	  	appenderString = ((SnapShotData)d).toAppenderString();
	  	
	  	assertTrue("keywordCacheHitRatio should be in appenderString", 
	  			appenderString.contains("keywordCacheHitRatio..... 50.000%"));
    }

    
    @SnapShotProvider(type=SnapShotProvider.Type.STATIC)
    @SnapShotRatios({
	    @SnapShotRatio(name="keywordSearchAverage", numerator="keywordSearchTime", denominator="keywordSearchCount", displayAsDuration=true),
	})
    public static class AverageProvider {
    	private static long keywordSearchTime = 0;
    	private static long keywordSearchCount = 0;
	
    	@SnapShotCounter
    	public static long getKeywordSearchTime() {
    		return keywordSearchTime;
    	}
    	
    	@SnapShotCounter
    	public static long getKeywordSearchCount() {
    		return keywordSearchCount;
    	}
    }
    
    public void testAverage() throws Exception {
	  	Class<?> clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(AverageProvider.class);
	  	
	  	JavassistSnapShotGenerator.SnapShotLifecycle snapShotData = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	
	  	AverageProvider.keywordSearchCount = 100;
	  	AverageProvider.keywordSearchTime = 1000;
	  	
	  	snapShotData.init(null, 1000);
	  	AverageProvider.keywordSearchCount += 5;
	  	AverageProvider.keywordSearchTime += 5901;
	  	snapShotData.takeSnapShot(null, 2000);

	  	String appenderString = ((SnapShotData)snapShotData).toAppenderString();

	  	System.out.println(appenderString);	  	
	  	assertTrue("keywordCacheHitRatio should be in appenderString", 
	  			appenderString.contains("keywordSearchAverage..... 1180 ms"));
	  	
    }
    
    
    public void testGetCounterData() throws Exception {
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	
	  	JavassistSnapShotGenerator.SnapShotLifecycle snapShotData = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
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
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	validateMethodExists(clazz, "getBytesWritten", Delta.class);
    }
    
    public void testInitSetsStartTime() throws Exception {
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	
	  	JavassistSnapShotGenerator.SnapShotLifecycle scl = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	scl.init(new SimpleDataProvider(), 100);
	  	
	  	assertEquals("startTime", 100, scl.getStartTime());
    }
    
    public void testProvideDataStartEndTimeAndDuration() throws Exception {
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	
	  	JavassistSnapShotGenerator.SnapShotLifecycle scl = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	SimpleDataProvider p = new SimpleDataProvider();
	  	scl.init(p, 150);
	  	scl.takeSnapShot(p, 200);
	  	
	  	assertEquals("stopTime", 200, scl.getEndTime());
	  	assertEquals("duration", 50, scl.getDuration());
    }

    
    /*----------------------------------------------------------------------------*/    
    public void testGenerateSimpleGauge() throws Exception {
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	validateMethodExists(clazz, "getTotalMemory", long.class);
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testImplementSpecifiedInterface() throws Exception {
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	assertTrue(implementsInterface(clazz, SimpleData.class));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testImplementsSnapShotLifecycle() throws Exception {
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	assertTrue(implementsInterface(clazz, JavassistSnapShotGenerator.SnapShotLifecycle.class));
	  	
	  	validateMethodExists(clazz, "getStartTime", long.class);
	  	validateMethodExists(clazz, "getEndTime", long.class);
	  	validateMethodExists(clazz, "getDuration", long.class);
	  	
	  	Class args[] = new Class[]{Object.class, long.class};
	  	validateMethodExists(clazz, "init", void.class, args);
	  	validateMethodExists(clazz, "takeSnapShot", void.class, args);
	  	
	  	JavassistSnapShotGenerator.SnapShotLifecycle scl = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
	  	assertEquals("initial value of getStartTime", PerfMon.NOT_SET, scl.getStartTime());
	  	assertEquals("initial value of getEndTime", PerfMon.NOT_SET, scl.getEndTime());
	  	assertEquals("initial value of getDuration", PerfMon.NOT_SET, scl.getDuration());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testDataProviderForGauge() throws Exception {
		Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
		
		SimpleDataProvider provider = new SimpleDataProvider();
		provider.setTotalMemory(10);
		
		JavassistSnapShotGenerator.SnapShotLifecycle lc = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
		lc.takeSnapShot(provider, System.currentTimeMillis());
	
		SimpleData d = (SimpleData)lc;
		
		assertEquals("Should have memory timestamp updated", 10, d.getTotalMemory());
	}
  
	  /*----------------------------------------------------------------------------*/    
    public void testGetAllSupportedGaugeTypes() throws Exception {
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleDataProvider.class);
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
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(TestProvider.class);
	  	assertNotNull("clazz", clazz);
	  	
	  	validateMethodExists(clazz, "getStringValue", String.class);
    	
    }
    
    
    public void testGetAllSupportedCounterTypes() throws Exception {
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(TestProvider.class);
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
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(TestProvider.class);
	  	
	  	SnapShotData d = (SnapShotData)clazz.newInstance();
	  	d.setName("MyData");

	  	JavassistSnapShotGenerator.SnapShotLifecycle lc = (JavassistSnapShotGenerator.SnapShotLifecycle)d;
	  	
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
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(TestStaticProvider.class);
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
	  	Class clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(TestStaticProvider.class);
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
	  	JavassistSnapShotGenerator.Bundle  bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(TestStaticProvider.class);
	  	
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
    	JavassistSnapShotGenerator.Bundle bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(TestInstancePerProvider.class);
	  	
    	TestInstancePerProvider p = (TestInstancePerProvider)bundle.getProviderInstance();
	  	assertNotNull("providerInstance", p);
	  	SnapShotData d = bundle.newSnapShotData();
	  	
	  	assertNotNull("newSnapShotData", d);
	  	
	  	// Each call should get a new provider...
    	bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(TestInstancePerProvider.class);
    	assertFalse("Each bundle should get a new provider instance", p == bundle.getProviderInstance());
    	
    	// Now pass an instance name.
    	bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(TestInstancePerProvider.class, "bogus");
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
    	JavassistSnapShotGenerator.Bundle bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(TestFactoryProvider.class);
	  	
    	TestFactoryProvider p = (TestFactoryProvider)bundle.getProviderInstance();
    	assertTrue("Should return single instance", p == TestFactoryProvider.noInstanceName);
    	
    	bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(TestFactoryProvider.class, "bogus");
	  	
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
    	JavassistSnapShotGenerator.Bundle bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(DataProviderWithPrivateMethod.class);
    	
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
    	
		public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
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
    	JavassistSnapShotGenerator.Bundle bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(SQLCompatibleDataProvider.class);
    	SnapShotData data = bundle.newSnapShotData();
    	
    	assertTrue("Should implement the SQLWritable interface", data instanceof SQLWriteable);
    	
    	SQLWriteable w = (SQLWriteable)data;
    	w.writeToSQL(null, "dave", 1);
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
    
    
    public void validateContainsField(MonitorKeyWithFields key, String fieldName, String fieldType) throws Exception {
    	boolean found = false;
    	FieldKey fields[] = key.getFields();
    	
    	for (int i = 0; (i < fields.length) && !found; i++) {
			FieldKey f = fields[i];
			found = fieldName.equals(f.getFieldName()) && fieldType.equals(f.getFieldType());
		}
    	if (!found) {
    		fail("Expected to find field key with fieldName=\"" + fieldName + "\" fieldType=\"" 
    			+ fieldType + "\"");
    	}
    }
    

    public void testSimpleGetMonitorKeyWithFields() throws Exception {
    	MonitorKeyWithFields keys[] = PerfMonTimerTransformer.snapShotGenerator.generateExternalMonitorKeys(JVMSnapShot.class);
    	
    	assertNotNull("keys[]", keys);
    	assertEquals("keys.length", 1, keys.length);
    	
    	assertEquals("keys[0].getType()", MonitorKey.SNAPSHOT_TYPE, keys[0].getType());
    	assertEquals("keys[0].getName()", JVMSnapShot.class.getName(), keys[0].getName());
    	
    	assertNull("keys[0].getInstance()", keys[0].getInstance());
    	
    	// Validate a ratio field.
    	// Ratios should always add "Percent" to the attribute name
    	validateContainsField(keys[0], "heapMemUsedCommitted", FieldKey.DOUBLE_TYPE);
    	
    	// Validate a counter (Counters contain ever increasing values)
    	// Counters should always add "PerSecond" to the attribute name
    	validateContainsField(keys[0], "unloadedClassCountPerSecond", FieldKey.DOUBLE_TYPE);

    	// Validate a Gauge (Gauge values can increase/decrease)
    	validateContainsField(keys[0], "classesLoaded", FieldKey.INTEGER_TYPE);
    }

    public void testSimpleGetMonitorKeyWithStringField() throws Exception {
    	MonitorKeyWithFields keys[] = PerfMonTimerTransformer.snapShotGenerator.generateExternalMonitorKeys(InstrumentationMonitor.class);
    	
    	assertNotNull("keys[]", keys);
    	assertEquals("keys.length", 1, keys.length);
    	
    	assertEquals("keys[0].getType()", MonitorKey.SNAPSHOT_TYPE, keys[0].getType());
    	assertEquals("keys[0].getName()", InstrumentationMonitor.class.getName(), keys[0].getName());
    	
    	assertNull("keys[0].getInstance()", keys[0].getInstance());
    	
    	// Validate a String field
    	// Ratios should always add "Percent" to the attribute name
    	validateContainsField(keys[0], "loggingFramework", FieldKey.STRING_TYPE);
    }

    @SnapShotProvider(type=SnapShotProvider.Type.STATIC)
    public static class StaticMonitor {
    	private static int counter = 0;
    	
    	public static void incCounter(int value) {
    		counter += value;	
    	}
    	
    	@SnapShotCounter
    	public static int getCounter() {
    		return counter;
    	}
    }
    

    public void testSimpleGetMonitorKeyWithStaticMonitor() throws Exception {
    	MonitorKeyWithFields keys[] = PerfMonTimerTransformer.snapShotGenerator.generateExternalMonitorKeys(StaticMonitor.class);
    	
    	assertNotNull("keys[]", keys);
    	assertEquals("keys.length", 1, keys.length);
    	
    	assertEquals("keys[0].getType()", MonitorKey.SNAPSHOT_TYPE, keys[0].getType());
    	assertEquals("keys[0].getName()", StaticMonitor.class.getName(), keys[0].getName());
    	
    	assertNull("keys[0].getInstance()", keys[0].getInstance());
    	
    	// Validate a counter (Counters cotain ever increasing values)
    	// Counters should always add "PerSecond" to the attribute name
    	validateContainsField(keys[0], "counterPerSecond", FieldKey.DOUBLE_TYPE);
    	
//    	SnapShotGenerator.Bundle bundle = SnapShotGenerator.generateBundle(StaticMonitor.class);
//    	SnapShotMonitor w = new SnapShotProviderWrapper("", bundle);
    	
//    	long now = System.currentTimeMillis();
//    	
//    	SnapShotData d = w.initSnapShot(now);
//    	
//    	StaticMonitor.incCounter(100);
//    	
//    	SnapShotData d2 = w.initSnapShot(now + 1000);
//    	StaticMonitor.incCounter(50);
//    	w.takeSnapShot(d2, now + 2000);
//    	
//    	System.out.println(d2.toAppenderString());
//    	
//    	w.takeSnapShot(d, now + 3000);
//    	
//    	System.out.println(d.toAppenderString());
    }
    
    
    @SnapShotProvider
    public static class MonitorWithInstance {
    	private final String instanceName;
    	
    	public MonitorWithInstance() {
    		instanceName = null;
    	}
    	
    	public MonitorWithInstance(String instanceName) {
    		this.instanceName = instanceName;
    	}
    	
    	@SnapShotInstanceDefinition
    	public static String[] getInstances() {
    		return new String[]{"A", "B", "C"};
    	}
    	
    	@SnapShotGauge
    	public int getValue() {
    		return 1;
    	}
    	
    	@SnapShotString(isInstanceName=true)
    	public String getInstanceName() {
    		return instanceName;
    	}
    }
    
    /*----------------------------------------------------------------------------*/
    public void testSimpleGetMonitorKeyWithFieldsFromMonitorWithInstances() throws Exception {
    	MonitorKeyWithFields keys[] = PerfMonTimerTransformer.snapShotGenerator.generateExternalMonitorKeys(MonitorWithInstance.class);
    	
    	assertNotNull("keys[]", keys);
    	assertEquals("keys.length", 3, keys.length);
    	
    	MonitorKeyWithFields instanceA = keys[0];
    	assertEquals("instance", "C", instanceA.getInstance());

    	validateContainsField(keys[0], "value", FieldKey.INTEGER_TYPE);
    }
  
    @SnapShotProvider
	@SnapShotRatio(name="cacheHitRatio", denominator="totalSearches", numerator="searchCacheHits")
	public static class SimpleObservationProvider {
    	private static final StringBuffer STRING_BUFFER = new StringBuffer("This is a StringBuffer");
    	private int counter = 5;
    	private int searchCacheHits = 0;
    	private int totalSearches = 0;
    	
    	public void incrementCounters() {
    		counter++;
    		searchCacheHits += 7;
    		totalSearches += 10;
    	}

    	@SnapShotString
    	public String getStringValue() {
    		return "This is a string";
    	}

    	@SnapShotString
    	public StringBuffer getStringBuffer() {
    		return STRING_BUFFER;
    	}
    	
    	@SnapShotCounter
    	public int getSearchCacheHits() {
    		return searchCacheHits;
    	}
    	
    	@SnapShotCounter
    	public int getTotalSearches() {
    		return totalSearches;
    	}
    	
    	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA)
    	public int getCounter() {
    		return counter;
    	}
    	
    	// Add all supported gauge types...
    	@SnapShotGauge
    	public boolean getBooleanValue() {
    		return true;
    	}

    	@SnapShotGauge
    	public long getLongValue() {
    		return 2;
    	}

    	@SnapShotGauge
    	public int getIntValue() {
    		return 3;
    	}

    	@SnapShotGauge
    	public short getShortValue() {
    		return 4;
    	}

    	@SnapShotGauge
    	public float getFloatValue() {
    		return 5.0f;
    	}

    	@SnapShotGauge
    	public double getDoubleValue() {
    		return 6.0;
    	}
    }
    
    public void testImplementsPerfMonObservableData() throws Exception {
	  	Class<?> clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(SimpleObservationProvider.class);
	  	Object obj = clazz.newInstance();
	  	
	  	SimpleObservationProvider provider = new SimpleObservationProvider();

	  	// Run our data object through it's lifecycle.. 
	  	((SnapShotLifecycle)obj).init(provider, 1000);
	  	provider.incrementCounters(); // Increment our counter
	  	((SnapShotLifecycle)obj).takeSnapShot(provider, 2000);
	  	
	  	assertTrue("Shold have implemented the PerfMonObservableData interface", 
	  			obj instanceof PerfMonObservableData);
	  	PerfMonObservableData data = (PerfMonObservableData)obj;
	  	
	  	((SnapShotData)data).setName("Main");
	  	assertEquals("categoryName", "Snapshot.Main", data.getDataCategory());
	  	assertEquals("timestamp", 2000, data.getTimestamp());
	  	assertEquals("durationMillis", 1000, data.getDurationMillis());
	  	
	  	Set<PerfMonObservableDatum<?>> observations = data.getObservations();
	  	assertNotNull("Should have returned observations", observations);
	  	assertTrue("Should have some observations in the map", !observations.isEmpty());
	  	
	  	// Validate all of the gauge observations...
	  	validateObservation(observations, "booleanValue", "1");
	  	validateObservation(observations, "longValue", "2");
	  	validateObservation(observations, "intValue", "3");
	  	validateObservation(observations, "shortValue", "4");
	  	validateObservation(observations, "floatValue", "5.000");
	  	validateObservation(observations, "doubleValue", "6.000");
	  	
	  	// Validate a delta
	  	validateObservation(observations, "counter", "1");

	  	
	  	
	  	@SuppressWarnings("unchecked")
		PerfMonObservableDatum<Delta> delta = (PerfMonObservableDatum<Delta>)PerfMonObservableDatum.findObservationByFieldName("counter", observations);
	  	assertEquals("delta initial value", 5, delta.getComplexObject().getInitalValue());
	  	assertEquals("delta final value", 6, delta.getComplexObject().getFinalValue());
	  	assertEquals("The defalt observation value from a delta should be deltaPerSecond", Double.valueOf(1.0), 
	  			delta.getComplexObject().getDeltaPerSecond_object());
	  	
	  	// Validate a ratio
	  	validateObservation(observations, "cacheHitRatio", "0.700");
	  	@SuppressWarnings("unchecked")
		PerfMonObservableDatum<Ratio> ratio = (PerfMonObservableDatum<Ratio>)PerfMonObservableDatum.findObservationByFieldName("cacheHitRatio", observations);
	  	assertEquals("ratio", Float.valueOf(0.7f), Float.valueOf(ratio.getComplexObject().getRatio()));
	  	
	  	// Validate the String
	  	validateObservation(observations, "stringValue", "This is a string");
		@SuppressWarnings("unchecked")
		PerfMonObservableDatum<String> stringDatum = (PerfMonObservableDatum<String>)PerfMonObservableDatum.findObservationByFieldName("stringValue", observations);
	  	assertFalse("String is not a numeric datum", stringDatum.isNumeric());

	  	// Validate the StringBuffer
	  	validateObservation(observations, "stringBuffer", "This is a StringBuffer");
		@SuppressWarnings("unchecked")
		PerfMonObservableDatum<StringBuffer> stringBufferDatum = (PerfMonObservableDatum<StringBuffer>)PerfMonObservableDatum.findObservationByFieldName("stringBuffer", observations);
	  	assertFalse("StringBuffer is not a numeric datum", stringBufferDatum.isNumeric());
	  	assertEquals("The StringBuffer should be the complexObject", SimpleObservationProvider.STRING_BUFFER, stringBufferDatum.getComplexObject());
    }

    @SnapShotProvider
	public static class CounterObservableOptons {
    	private int count = 0;
    	
    	private void incCount(int value) {
    		count += value;
    	}
    	
    	// Should return datum named "countAPerSec"
    	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
    	public int getCountA() {
    		return count;
    	}
    	
    	// Should return datum named "countBPerSec"
    	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_SECOND)
    	public int getCountB() {
    		return count;
    	}
    	
    	// Should return datum named "countC"
    	@SnapShotCounter()
    	public int getCountC() {
    		return count;
    	}
    }
    
    public void testPerfMonObservableCounterOptions() throws Exception {
	  	Class<?> clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(CounterObservableOptons.class);
	  	Object obj = clazz.newInstance();
	  	
	  	CounterObservableOptons provider = new CounterObservableOptons();

	  	// Run our data object through it's lifecycle.. 
	  	((SnapShotLifecycle)obj).init(provider, 1000);
	  	provider.incCount(5); // Increment our counter
	  	((SnapShotLifecycle)obj).takeSnapShot(provider, 6000); // 5 second observation window
	  	
	  	assertTrue("Shold have implemented the PerfMonObservableData interface", 
	  			obj instanceof PerfMonObservableData);
	  	PerfMonObservableData data = (PerfMonObservableData)obj;
	  	
	  	Set<PerfMonObservableDatum<?>> observations = data.getObservations();
	  	assertNotNull("Should have returned observations", observations);
	  	assertTrue("Should have some observations in the map", !observations.isEmpty());
	  	
		PerfMonObservableDatum<?> countA = PerfMonObservableDatum.findObservationByDefaultDisplayName("countAPerSec", observations);
		assertNotNull("Display type of perMin should be returned with a 'PerSec' suffix", countA);
		validateObservation(observations, "countA", "1.000");
		
		PerfMonObservableDatum<?> countB = PerfMonObservableDatum.findObservationByDefaultDisplayName("countBPerSec", observations);
		assertNotNull("Display type of perSec should be returned with a 'PerSec' suffix", countB);
		validateObservation(observations, "countB", "1.000");
		
		PerfMonObservableDatum<?> countC = PerfMonObservableDatum.findObservationByDefaultDisplayName("countC", observations);
		assertNotNull("Display type of default should not have a suffix", countC);
		// Should just return the delta between start and end.
		validateObservation(observations, "countC", "5");
    }

    @SnapShotPOJO
    public static final class MyPOJO {
    	private long x = 0;
    	
    	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
    	public long getCounter() {
    		return x++;
    	}
    	
    }

    @Deprecated
    private SnapShotData initSnapShot(Bundle bundle, Object pojo, long currentTimeMillis) {
    	SnapShotData result =  bundle.newSnapShotData();
    	((SnapShotGenerator.SnapShotLifecycle)result).init(pojo, currentTimeMillis);
    	
    	return result;
    }

    @Deprecated
     public SnapShotData takeSnapShot(Object pojo, SnapShotData data, long currentTimeMillis) {
    	((SnapShotGenerator.SnapShotLifecycle)data).takeSnapShot(pojo, currentTimeMillis);
    	return data;
    }
    
    public void testGenerateBundlePOJO() throws Exception {
    	MyPOJO pojo = new MyPOJO();
    	Bundle bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundleForPOJO(MyPOJO.class);
    	
    	SnapShotData data = initSnapShot(bundle, pojo, 0);
    	data = takeSnapShot(pojo, data, 1000 * 60);
    	
    	assertTrue("Should implement SnapShotPOJOLifecyle", data instanceof SnapShotPOJOLifecycle);
    	assertTrue("Should implement PerfMonObservableData", data instanceof PerfMonObservableData);
    	
    	String appenderStringWithoutInstanceName = data.toAppenderString();
    	assertFalse("Since we didn't set instanceName it should not appear in list of data elements", appenderStringWithoutInstanceName.contains("instanceName"));
  
    	Set<PerfMonObservableDatum<?>> observations = ((PerfMonObservableData)data).getObservations();
    	assertNull("Should not include an instanceName element since we didn't set it", PerfMonObservableDatum.findObservationByFieldName("instanceName", observations));
    	
    	((SnapShotPOJOLifecycle)data).setInstanceName("myInstanceName");
    	String appenderStringWithInstanceName = data.toAppenderString();
    	assertTrue("Since we set instanceName it should be in list of data elements", appenderStringWithInstanceName.contains("instanceName"));
    	
    	
    	observations = ((PerfMonObservableData)data).getObservations();
    	assertNotNull("We Should include an instanceName element since we set it", PerfMonObservableDatum.findObservationByFieldName("instanceName", observations));
    	validateObservation(observations, "instanceName", "myInstanceName");
    }
    
 	void validateObservation(Set<PerfMonObservableDatum<?>> observations, String label, String expectedValue) {
 		PerfMonObservableDatumTest.validateObservation(observations, label, expectedValue);
     }
 	
 	
    @SnapShotProvider
    public static class FlexibleOptionsForGauge {
    	boolean boolValue = false;
	
    	@SnapShotGauge
    	public boolean isBoolValue() {
    		return boolValue;
    	}
    }
 	
    /**
     * We should be more forgiving for gauge types. To build a gauge we simply
     * need a method that takes no parameters and returns a numeric or boolean 
     * value.  However we were strictly requiring a method that followed the 
     * "getter" patter.  We would didn't allow an "isser" for boolean values.
     * 
     * We are now going to support "isser" methods.
     * 
     * @throws Exception
     */
    public void testFlexibleOptionsForGauge() throws Exception {
    	JavassistSnapShotGenerator.SnapShotLifecycle lc = null;
    	try {
    		Class<?> clazz = PerfMonTimerTransformer.snapShotGenerator.generateSnapShotDataImpl(FlexibleOptionsForGauge.class);
    		lc = (JavassistSnapShotGenerator.SnapShotLifecycle)clazz.newInstance();
    	} catch (Exception ex) {
//ex.printStackTrace();
    		fail("Expected to be able to generate SnapShotLifeCyleClass");
    	}
    	
		FlexibleOptionsForGauge provider = new FlexibleOptionsForGauge();
		lc.init(provider, 0);
		
		lc.takeSnapShot(provider, 0);
		String appenderString = ((SnapShotData)lc).toAppenderString();

//System.out.println(appenderString);
		assertTrue(appenderString.contains("boolValue................ false"));
		
		provider.boolValue = true;
		lc.takeSnapShot(provider, 0);
		appenderString = ((SnapShotData)lc).toAppenderString();

//System.out.println(appenderString);
		assertTrue(appenderString.contains("boolValue................ true"));
		
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
//        newSuite.addTest(new SnapShotGeneratorTest("testSimpleGetMonitorKeyWithStaticMonitor"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(SnapShotGeneratorTest.class);
        }

        return( newSuite);
    }
}