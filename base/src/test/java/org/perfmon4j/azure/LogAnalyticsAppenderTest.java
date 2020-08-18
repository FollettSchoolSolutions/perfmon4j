package org.perfmon4j.azure;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
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
		appender.setCustomerID(CUSTOMER_ID);
		appender.setSystemNameBody("MySystemName");
		appender.setSharedKey("MySharedKey");
		
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
		assertEquals("baseURL", "https://" + CUSTOMER_ID + ".ods.opinsights.azure.com/api/logs?api-version=2016-04-01",
			appender.buildPostURL());
	}

	public void testBuildDataLine_NoData() {
		appender.setGroups("MyGroup");
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
 		Mockito.when(mockData.getObservations()).thenReturn(set);
		
		assertNull("There are no data elements to report so should return null", appender.buildJSONElement(mockData));
	}
	
	public void testBuildDataLine_IgnoresDataumWithNullInput() {
		appender.setGroups("MyGroup");
		
		Number nullValue = null;
		
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
 		set.add(PerfMonObservableDatum.newDatum("this was null input and shouldn't be sent to influxDb", nullValue));
		
		Mockito.when(mockData.getObservations()).thenReturn(set);
 		
		
		assertNull("There are no data elements to report so should return null", appender.buildJSONElement(mockData));
	}

	public void testBuildDataLine_OnlyNonNumericData() {
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
		assertFalse("Should not contain group element",
			appender.buildJSONElement(mockData).contains("\"group\" :"));
		
		appender.setGroups("MyGroup");
		assertTrue("Should Contain Group Element",
				appender.buildJSONElement(mockData).contains("\"group\" :"));
	}

	public void testBooleanValueInJSON() {
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
		set.add(PerfMonObservableDatum.newDatum("trueValue", true));
		set.add(PerfMonObservableDatum.newDatum("falseValue", false));
 		Mockito.when(mockData.getObservations()).thenReturn(set);
 		
		final String JSONValue = appender.buildJSONElement(mockData);
		
		assertTrue("boolean values should not be quoted in JSON", JSONValue.contains("\"trueValue\" : true,"));
		assertTrue("boolean values should not be quoted in JSON", JSONValue.contains("\"falseValue\" : false,"));
	}
	
	public void testTimestampInISO8601Format() {
		final String JSONValue = appender.buildJSONElement(mockData);
		assertTrue("timestamp should be 1970-01-01T00:00:01Z", JSONValue.contains("\"timestamp\" : \"1970-01-01T00:00:01Z\""));
	}
	
	public void testNumericValueInJSON() {
		Set<PerfMonObservableDatum<?>> set = new HashSet<PerfMonObservableDatum<?>>();
		set.add(PerfMonObservableDatum.newDatum("floatValue", 10.601f));
		set.add(PerfMonObservableDatum.newDatum("longValue", 9872));
 		Mockito.when(mockData.getObservations()).thenReturn(set);
 		
		final String JSONValue = appender.buildJSONElement(mockData);
		
		assertTrue("float values should not be quoted in JSON", JSONValue.contains("\"floatValue\" : 10.601,"));
		assertTrue("long values should not be quoted in JSON", JSONValue.contains("\"longValue\" : 9872,"));
	}

	public void testAzureResourceIDInHeader()  throws Exception {
		Map<String, String> headers = appender.buildRequestHeaders(1024);
		assertNull("No azure resource ID specified, it should not exist in the map", headers.get("x-ms-AzureResourceId"));
		
		appender.setAzureResourceID("my.azure.reource.id");
		
		headers = appender.buildRequestHeaders(1024);
		assertEquals("Azure Resource ID", "my.azure.reource.id", headers.get("x-ms-AzureResourceId"));
	}

	public void testLogTypeInHeader()  throws Exception {
		Map<String, String> headers = appender.buildRequestHeaders(1024);
		assertEquals("LogType", "Perfmon4j", headers.get("Log-Type"));
	}

	public void testTimeGeneratedFieldInHeader()  throws Exception {
		Map<String, String> headers = appender.buildRequestHeaders(1024);
		assertEquals("time-generated-field", "timestamp", headers.get("time-generated-field"));
	}

	public void testDateTimeFieldInHeader()  throws Exception{
		DateFormat rfc1123Format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		
		Map<String, String> headers = appender.buildRequestHeaders(1024);
		String dateTime = headers.get("x-ms-date");
		
		assertNotNull("x-ms-date should be provided", dateTime);
		
		try {
			rfc1123Format.parse(dateTime);
		} catch (ParseException e) {
			fail("Should be in rfc 1123 format");
		}
	}

	public void testAuthorizationHeader() throws Exception {
		Map<String, String> headers = appender.buildRequestHeaders(1024);
		String authorizationHeader = headers.get("Authorization");
		
		assertNotNull("Authorization Header should be provided", authorizationHeader);
		final String authHeaderStartsWith = "SharedKey " + CUSTOMER_ID + ":";
			
		assertTrue("Authorization header should start with: " + authHeaderStartsWith,
			authorizationHeader.startsWith(authHeaderStartsWith));
	}

	
	public void testBuildStringToSign() {
		String stringToSign = appender.buildStringToSign(1024, "Mon, 04 Apr 2016 08:00:00 GMT");
		assertEquals("Signature string", "POST\n1024\napplication/json\nx-ms-date:Mon, 04 Apr 2016 08:00:00 GMT\n/api/logs",
			stringToSign);
	}
	
	public void testCreateSignatureNoSharedKey() {
		try {
			appender.setSharedKey(null);
			appender.createSignature("test");
			fail("Expected an exception if we don't have a sharedKey");
		} catch (Exception ex) {
			// Nothing todo.
		}
	}
	
	public void testCreateSignature() throws Exception {
		String result = appender.createSignature("POST\n1024\napplication/json\nx-ms-date:Mon, 04 Apr 2016 08:00:00 GMT\n/api/logs");
		assertEquals("Expected signature", "+TbcCCiIeRZ0wkd0sggjSXlwdgKLVzpM5f2Z7GaovbQ=", result);
	}

}
