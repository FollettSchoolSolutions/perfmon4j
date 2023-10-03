package org.perfmon4j.emitter;

import org.perfmon4j.emitter.EmitterDataImpl;
import org.perfmon4j.util.MiscHelper;

import junit.framework.TestCase;

public class EmitterDataTest extends TestCase {
	private final long now = System.currentTimeMillis();
	private final String nowString = MiscHelper.formatDateTimeAsString(now, true); 
	
	public void testToAppenderStringWithInstanceName() {
		EmitterDataImpl data = new EmitterDataImpl("MyCategory", "MyInstance", now);
	
		final String expected = "\r\n********************************************************************************\r\n" 
				+ "MyCategory\r\n"
				+ nowString + "\r\n"
				+ " instanceName............. MyInstance\r\n"
				+ "********************************************************************************";
		
		assertEquals("Expected appender output", expected, data.toAppenderString());
	}

	public void testToAppenderNullInstanceName() {
		EmitterDataImpl data = new EmitterDataImpl("MyCategory", null, now);
		data.addData("myFloat", 10.0 / 3.0);
	
		final String expected = "\r\n********************************************************************************\r\n" 
				+ "MyCategory\r\n"
				+ nowString + "\r\n"
				+ " myFloat.................. 3.3333333333333335\r\n"
				+ "********************************************************************************";
		
		assertEquals("Expected appender output", expected, data.toAppenderString());
	}
}
