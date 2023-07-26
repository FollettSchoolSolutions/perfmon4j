package org.perfmon4j;

import java.util.GregorianCalendar;

import junit.framework.Assert;

public class TestHelper {
    public final static long SECOND = 1000;
    public final static long MINUTE = SECOND * 60;
    public final static long HOUR = MINUTE * 60;

	/*
	 * Will return midnight January 1 2007 in current timezone
	 */
	public static long getTimeForTest() {
	    return (new GregorianCalendar(2007, 0, 1)).getTimeInMillis();
	}
	
	public static long addHours(long time, int numHours) {
		return time + (numHours * HOUR);
	}

	public static long addMinutes(long time, int numMinutes) {
		return time + (numMinutes * MINUTE);
	}
	
	public static long addSeconds(long time, int numSeconds) {
		return time + (numSeconds * SECOND);
	}

    public static String extractFailures(String output) {
    	StringBuilder failures = new StringBuilder();
    	
    	for (String line : output.split(System.lineSeparator())) {
    		if (line.contains("**FAIL:")) {
    			failures.append(System.lineSeparator())
    				.append(line);
    		}
    	}
    	return failures.toString();
    }

    public static void validateNoFailuresInOutput(String output) {
    	String failures = extractFailures(output);
		if (!failures.isEmpty()) {
			Assert.fail("One or more exceptions thrown: " + failures);
		}
    }
}
