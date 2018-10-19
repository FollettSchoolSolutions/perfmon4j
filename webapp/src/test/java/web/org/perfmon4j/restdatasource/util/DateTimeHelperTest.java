/*
 *	Copyright 2015 Follett School Solutions 
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
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package web.org.perfmon4j.restdatasource.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.ws.rs.BadRequestException;

import junit.framework.TestCase;

public class DateTimeHelperTest extends TestCase {
	private final DateTimeHelper helper = new TestDateTimeHelper();
	
	public DateTimeHelperTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetTimeForStart() throws Exception { 
		DateTimeValue value = helper.parseDateTime("now");
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(value.getTimeForStart());
		
		assertEquals(2015, cal.get(Calendar.YEAR));
		assertEquals(Calendar.APRIL, cal.get(Calendar.MONTH));
		assertEquals(26, cal.get(Calendar.DATE));
		assertEquals(18, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(15, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));
	}
	
	public void testGetTimeForEnd() throws Exception { 
		// Should set to the last millisecond of the minute.
		DateTimeValue value = helper.parseDateTime("now");
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(value.getTimeForEnd());
		
		assertEquals(2015, cal.get(Calendar.YEAR));
		assertEquals(Calendar.APRIL, cal.get(Calendar.MONTH));
		assertEquals(26, cal.get(Calendar.DATE));
		assertEquals(18, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(15, cal.get(Calendar.MINUTE));
		assertEquals(59, cal.get(Calendar.SECOND));
		assertEquals(999, cal.get(Calendar.MILLISECOND));
	}
	
	public void testGetNowTimeOnly() throws Exception {
		DateTimeValue value = null;
		
		value = helper.parseDateTime("08:30");
		assertEquals("2015-04-26T08:30", value.getFixedDateTime());
		assertTrue(value.isRelative());
	}

	public void testInvalidTimeOnly() throws Exception {
		try {
			helper.parseDateTime("00:00");
		} catch (BadRequestException pe) {
			fail("Valid hour and minutes");
		}

		try {
			helper.parseDateTime("23:59");
		} catch (BadRequestException pe) {
			fail("Valid hour and minutes");
		}
		
		try {
			helper.parseDateTime("24:00");
			fail("Hours out of bounds, should have thrown an exception");
		} catch (BadRequestException pe) {
			// expected.
		}

		try {
			helper.parseDateTime("00:60");
			fail("Minutes out of bounds, should have thrown an exception");
		} catch (BadRequestException pe) {
			// expected.
		}
	}
	

	public void testInvalidDateOnly() throws Exception {
		try {
			helper.parseDateTime("2014-02-28");
		} catch (BadRequestException pe) {
			fail("Valid date");
		}

		try {
			helper.parseDateTime("2014-00-28");
			fail("month out of bounds should have thrown exception");
		} catch (BadRequestException pe) {
			// expected.
		}
		
		try {
			helper.parseDateTime("2014-13-28");
			fail("month out of bounds should have thrown exception");
		} catch (BadRequestException pe) {
			// expected.
		}
		
		try {
			helper.parseDateTime("2014-02-29");
			fail("day out of bounds should have thrown exception");
		} catch (BadRequestException pe) {
			// expected.
		}
		
		try {
			helper.parseDateTime("2014-02-00");
			fail("day out of bounds should have thrown exception");
		} catch (BadRequestException pe) {
			// expected.
		}
	}
	
	public void testTimeOnlyFixedTime() throws Exception  {
		DateTimeValue value = null;
		
		value = helper.parseDateTime("now");
		assertEquals("2015-04-26T18:15", value.getFixedDateTime());
		assertTrue(value.isRelative());

		// Subtract minutes
		value = helper.parseDateTime("now-30");
		assertEquals("2015-04-26T17:45", value.getFixedDateTime());
		assertTrue(value.isRelative());

		// Subtract hours
		value = helper.parseDateTime("now-8H");
		assertEquals("2015-04-26T10:15", value.getFixedDateTime());
		assertTrue(value.isRelative());
	}
	
	public void testGetDateOnly() throws Exception {
		DateTimeValue value = null;
		
		value = helper.parseDateTime("2015-03-15");
		assertEquals("2015-03-15", value.getFixedDateTime());
		assertNull(value.getRelativeDateTime());
		assertFalse(value.isRelative());

		// Start time should be first millisecond of the day
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(value.getTimeForStart());
		
		assertEquals(2015, cal.get(Calendar.YEAR));
		assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
		assertEquals(15, cal.get(Calendar.DATE));
		assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(0, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));
		

		// End time should be last millisecond of the day
		cal = new GregorianCalendar();
		cal.setTimeInMillis(value.getTimeForEnd());
		
		assertEquals(2015, cal.get(Calendar.YEAR));
		assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
		assertEquals(15, cal.get(Calendar.DATE));
		assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, cal.get(Calendar.MINUTE));
		assertEquals(59, cal.get(Calendar.SECOND));
		assertEquals(999, cal.get(Calendar.MILLISECOND));
	}
	
	
	public void testGetRelativeTime() throws Exception  { 
		// now is considered a relative time.
		DateTimeValue value = helper.parseDateTime("now");
		assertEquals("now", value.getRelativeDateTime());
		
		// now minus minutes is considered a relative time.
		value = helper.parseDateTime("now-45");
		assertEquals("now-45", value.getRelativeDateTime());

		// now minus hours is considered a relative time.
		value = helper.parseDateTime("now-8H");
		assertEquals("now-8H", value.getRelativeDateTime());

		// A time without a date is considered a relative term.
		value = helper.parseDateTime("08:00");
		assertEquals("08:00", value.getRelativeDateTime());
		
		// Date only is NOT considered a relative term.
		value = helper.parseDateTime("2015-04-27");
		assertNull(value.getRelativeDateTime());
		
		// Date/Time is NOT considered a relative term.
		value = helper.parseDateTime("2015-04-27T08:00");
		assertNull(value.getRelativeDateTime());
	}
	
	
	public void testGetDateAndTime() throws Exception {
		DateTimeValue value = null;
		
		value = helper.parseDateTime("2015-03-15T08:30");
		assertEquals("2015-03-15T08:30", value.getFixedDateTime());
		assertNull(value.getRelativeDateTime());
		assertFalse(value.isRelative());

		// Start time should be first millisecond of the minute
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(value.getTimeForStart());
		
		assertEquals(2015, cal.get(Calendar.YEAR));
		assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
		assertEquals(15, cal.get(Calendar.DATE));
		assertEquals(8, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(30, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));
		

		// End time should be last millisecond of the minute
		cal = new GregorianCalendar();
		cal.setTimeInMillis(value.getTimeForEnd());
		
		assertEquals(2015, cal.get(Calendar.YEAR));
		assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
		assertEquals(15, cal.get(Calendar.DATE));
		assertEquals(8, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(30, cal.get(Calendar.MINUTE));
		assertEquals(59, cal.get(Calendar.SECOND));
		assertEquals(999, cal.get(Calendar.MILLISECOND));
	}

	
	
	public void testTruncateToMinute() throws Exception { 
		// Should set to the last millisecond of the minute.
		Calendar cal = new GregorianCalendar();
		cal.set(2015, 4, 7, 22, 45, 13);
		cal.set(Calendar.MILLISECOND, 345);
		
		
		long millis = helper.truncateToMinute(cal.getTimeInMillis());
		
		cal = new GregorianCalendar();
		cal.setTimeInMillis(millis);
		
		assertEquals(2015, cal.get(Calendar.YEAR));
		assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
		assertEquals(7, cal.get(Calendar.DATE));
		assertEquals(22, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(45, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));
	}
	

	public void testParseDateTimeIgnoresTimeAdjustment() throws Exception {
		try {
			helper.parseDateTime("now ~ADJ-1D");
		} catch (Exception e) {
			fail("Should have ignored the time adjustment...This is handled in Rest Implementation layer");
		}
	}
	
	
	
	/**
	 * Force our Test DateTimeHelper to always return the same value for the current system time.
	 */
	private static final class TestDateTimeHelper extends DateTimeHelper {
		@Override
		long getCurrentTime() {
		    TimeZone timezone = TimeZone.getDefault();
		    
			// 2015-04-26T18:15:14.998 adjusted for daylight savings and machine timezone
			return 1430072114998L - (timezone.getRawOffset() + timezone.getDSTSavings());
		}
	}

}
