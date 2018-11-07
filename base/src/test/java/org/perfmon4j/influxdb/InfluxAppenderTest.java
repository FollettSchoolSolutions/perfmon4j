package org.perfmon4j.influxdb;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;

public class InfluxAppenderTest extends TestCase {

	public InfluxAppenderTest(String name) {
		super(name);
	}

	InfluxAppender appender = null;
	PerfMonObservableData mockData = null;
	
	@SuppressWarnings("boxing")
	protected void setUp() throws Exception {
		super.setUp();
		appender = new InfluxAppender(AppenderID.getAppenderID(InfluxAppender.class.getName()));
		
		mockData = Mockito.mock(PerfMonObservableData.class);
		Mockito.when(mockData.getDataCategory()).thenReturn("MyCategory");
		Mockito.when(mockData.getTimestamp()).thenReturn(Long.valueOf(1000));
	
		Map<String, PerfMonObservableDatum<?>> map = new HashMap<String, PerfMonObservableDatum<?>>();
 		map.put("throughput",PerfMonObservableDatum.newDatum(25));
 		
 		Mockito.when(mockData.getObservations()).thenReturn(map);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testBuildPostURL() {
		appender.setBaseURL("http://localhost:8086");
		appender.setDatabase("perfmon4j");
		
		assertEquals("baseURL", "http://localhost:8086/write?db=perfmon4j&precision=s",
			appender.buildPostURL());
	}

	public void testBuildPostURLDoesNotAddExtraPathSep() {
		appender.setBaseURL("http://localhost:8086/");
		appender.setDatabase("perfmon4j");
		
		assertEquals("baseURL", "http://localhost:8086/write?db=perfmon4j&precision=s",
			appender.buildPostURL());
	}
	
	public void testBuildPostURLAddsUserNameAndPassword() {
		appender.setBaseURL("http://localhost:8086/");
		appender.setDatabase("perfmon4j");
		appender.setUsername("dave");
		appender.setPassword("fish");
		
		assertEquals("baseURL", 
			"http://localhost:8086/write?db=perfmon4j&precision=s&u=dave&p=fish",
			appender.buildPostURL());
	}

	public void testBuildPostURLAddsOptionalRetentionPolicy() {
		appender.setBaseURL("http://localhost:8086/");
		appender.setDatabase("perfmon4j");
		appender.setRetentionPolicy("3months");
		
		assertEquals("baseURL", 
			"http://localhost:8086/write?db=perfmon4j&precision=s&rp=3months",
			appender.buildPostURL());
	}
	
	public void testBuildDataLine() {
		appender.setSystemNameBody("MySystemName");
		appender.setGroups("MyGroup");
		
		assertEquals("MyCategory,system=MySystemName,group=MyGroup throughput=25 1",
			appender.buildPostDataLine(mockData));
	}
	
	public void testBuildDataLineNoGroup() {
		appender.setSystemNameBody("MySystemName");
		
		assertEquals("MyCategory,system=MySystemName throughput=25 1",
			appender.buildPostDataLine(mockData));
	}
}
