package org.perfmon4j.azure;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;

public class LogAnalyticsAppenderTest extends TestCase {
	private final String CUSTOMER_ID = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";

	public LogAnalyticsAppenderTest(String name) {
		super(name);
	}

	LogAnalyticsAppender appender = null;
	PerfMonObservableData mockData = null;
	
	protected void setUp() throws Exception {
		super.setUp();
		appender = new LogAnalyticsAppender(AppenderID.getAppenderID(LogAnalyticsAppender.class.getName()));
		
		mockData = Mockito.mock(PerfMonObservableData.class);
		Mockito.when(mockData.getDataCategory()).thenReturn("MyCategory");  //Comma, space should be escaped, but not the equals sign,
		Mockito.when(mockData.getTimestamp()).thenReturn(Long.valueOf(1000));
	
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
 		set.add(PerfMonObservableDatum.newDatum("throughput", 25));
 		
 		Mockito.when(mockData.getObservations()).thenReturn(set);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	public void testBuildPostURL() {
		appender.setCustomerID(CUSTOMER_ID);
		
		assertEquals("baseURL", "https://" + CUSTOMER_ID + ".ods.opinsights.azure.com/api/logs?api-version=2016-04-01",
			appender.buildPostURL());
	}

//	public void testBuildPostURLDoesNotAddExtraPathSep() {
//		appender.setBaseURL("http://localhost:8086/");
//		appender.setDatabase("perfmon4j");
//		
//		assertEquals("baseURL", "http://localhost:8086/write?db=perfmon4j&precision=s",
//			appender.buildPostURL());
//	}
//	
//	public void testBuildPostURLAddsUserNameAndPassword() {
//		appender.setBaseURL("http://localhost:8086/");
//		appender.setDatabase("perfmon4j");
//		appender.setUserName("dave");
//		appender.setPassword("fish");
//		
//		assertEquals("baseURL", 
//			"http://localhost:8086/write?db=perfmon4j&precision=s&u=dave&p=fish",
//			appender.buildPostURL());
//	}
//
//	public void testBuildPostURLAddsOptionalRetentionPolicy() {
//		appender.setBaseURL("http://localhost:8086/");
//		appender.setDatabase("perfmon4j");
//		appender.setRetentionPolicy("3months");
//		
//		assertEquals("baseURL", 
//			"http://localhost:8086/write?db=perfmon4j&precision=s&rp=3months",
//			appender.buildPostURL());
//	}
//	
//	public void testBuildDataLine() {
//		appender.setSystemNameBody("MySystemName");
//		appender.setGroups("My =\\Group"); //Comma (Perfmon4j does not allow comma in group name), space AND the equals sign should be escaped
//		
//		Mockito.when(mockData.getDataCategory()).thenReturn("My, \\=Category");  //Comma and space should be escaped, but not the equals sign,
//
//		assertEquals("My\\,\\ \\\\=Category,system=MySystemName,group=My\\ \\=\\\\Group throughput=25i 1",
//			appender.buildPostDataLine(mockData));
//	}
//
	public void testBuildDataLine_NoData() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("MyGroup");
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
 		Mockito.when(mockData.getObservations()).thenReturn(set);
		
		assertNull("There are no data elements to report so should return null", appender.buildJSONElement(mockData));
	}
	
	public void testBuildDataLine_IgnoresDataumWithNullInput() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("MyGroup");
		
		Number nullValue = null;
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
 		set.add(PerfMonObservableDatum.newDatum("this was null input and shouldn't be sent to influxDb", nullValue));
		
		Mockito.when(mockData.getObservations()).thenReturn(set);
 		
		
		assertNull("There are no data elements to report so should return null", appender.buildJSONElement(mockData));
	}

	public void testBuildDataLine_OnlyNonNumericData() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("MyGroup");
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
		set.add(PerfMonObservableDatum.newDatum("stringData", "This is a string value"));
 		Mockito.when(mockData.getObservations()).thenReturn(set);
		
 		assertNotNull("numericOnly is false so should return a data line", appender.buildJSONElement(mockData));
		
		// Now set numericOnly=true on the appender
		appender.setNumericOnly(true);
		assertNull("numericOnly is true should return null", appender.buildJSONElement(mockData));
	}
	
	public void testBuildDataLineNoGroup() {
		appender.setSystemNameBody("MySystemName");
		
		assertFalse("Should not contain group element",
			appender.buildJSONElement(mockData).contains("\"group\" :"));
		
		appender.setGroups("MyGroup");
		assertTrue("Should Contain Group Element",
				appender.buildJSONElement(mockData).contains("\"group\" :"));
	}

	public void testBooleanValueInJSON() {
		appender.setSystemNameBody("MySystemName");
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
		set.add(PerfMonObservableDatum.newDatum("trueValue", true));
		set.add(PerfMonObservableDatum.newDatum("falseValue", false));
 		Mockito.when(mockData.getObservations()).thenReturn(set);
 		
		final String JSONValue = appender.buildJSONElement(mockData);
		
		assertTrue("boolean values should not be quoted in JSON", JSONValue.contains("\"trueValue\" : true,"));
		assertTrue("boolean values should not be quoted in JSON", JSONValue.contains("\"falseValue\" : false,"));
	}

	public void testNumericValueInJSON() {
		appender.setSystemNameBody("MySystemName");
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
		set.add(PerfMonObservableDatum.newDatum("floatValue", 10.601f));
		set.add(PerfMonObservableDatum.newDatum("longValue", 9872));
 		Mockito.when(mockData.getObservations()).thenReturn(set);
 		
		final String JSONValue = appender.buildJSONElement(mockData);
		
		assertTrue("float values should not be quoted in JSON", JSONValue.contains("\"floatValue\" : 10.601,"));
		assertTrue("long values should not be quoted in JSON", JSONValue.contains("\"longValue\" : 9872,"));
	}
}
