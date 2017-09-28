/*
 *	Copyright 2017 Follett School Solutions 
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

import junit.framework.TestCase;
import web.org.perfmon4j.restdatasource.util.TimeAdjustmentValue.Period;

public class TimeAdjustmentValueTest extends TestCase {
	
	public TimeAdjustmentValueTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testNothingToParse() throws Exception { 
		assertNull("There is nothing here", TimeAdjustmentValue.parse(null));
		assertNull("There is nothing here", TimeAdjustmentValue.parse(""));
		assertNull("There is nothing here", TimeAdjustmentValue.parse("asfasdf"));
		assertNull("There is nothing here", TimeAdjustmentValue.parse("{asfasdf}"));
		assertNull("There is nothing here", TimeAdjustmentValue.parse("{asfasdf"));
		assertNull("There is nothing here", TimeAdjustmentValue.parse("{asfasdf}"));
	}
	
	/**
	 * An empty "{}" pattern indicate that no adjustment should be applied to the specific date field.
	 * 
	 * @throws Exception
	 */
	public void testParseNoOpAdjustment() throws Exception { 
		TimeAdjustmentValue noAdjustment = new TimeAdjustmentValue(Period.NOADJUSTMENT, 0);
		
		assertEquals("No adjustment", noAdjustment, TimeAdjustmentValue.parse("{}"));
		assertEquals("Date/Time value before is OK", noAdjustment, TimeAdjustmentValue.parse("now{}"));
		assertEquals("Date/Time value before is OK with whitespace", noAdjustment, TimeAdjustmentValue.parse("now{}"));
		assertEquals("Date/Time value after is OK", noAdjustment, TimeAdjustmentValue.parse("{}now"));
		assertEquals("Date/Time value after is OK with whitespace", noAdjustment, TimeAdjustmentValue.parse("{} now"));
	}
	
	/**
	 * The default period will be Days.  The following values will result in adding 2 days to the 
	 * specified time:
	 * 	"2017-09-27T15:30 {2}"
	 * 	"2017-09-27T15:30 {2D}"
	 * 	"2017-09-27T15:30 {+2}"
	 * 	"2017-09-27T15:30 {+2D}"
	 * 
	 * The following values will result in subtracting 2 days from the specified time:
	 * 	"2017-09-27T15:30 {-2}"
	 * 	"2017-09-27T15:30 {-2D}"
	 * 
	 * @throws Exception
	 */
	public void testParseDays() throws Exception { 
		TimeAdjustmentValue positive2Days = new TimeAdjustmentValue(Period.DAY, 2);
		TimeAdjustmentValue negative2Days = new TimeAdjustmentValue(Period.DAY, -2);
		
		assertEquals("+sign allowed, period defaults to days", positive2Days, TimeAdjustmentValue.parse("{+2}"));
		assertEquals("period defaults to days", positive2Days, TimeAdjustmentValue.parse("{2}"));
		assertEquals("explicit period", positive2Days, TimeAdjustmentValue.parse("{2D}"));
		assertEquals("period defaults to days", negative2Days, TimeAdjustmentValue.parse("{-2}"));
		assertEquals("explicit period", negative2Days, TimeAdjustmentValue.parse("{-2D}"));
	}
	

	/**
	 * The following values will result in adding 2 hours to the 
	 * specified time:
	 * 	"2017-09-27T15:30 {2H}"
	 * 	"2017-09-27T15:30 {+2H}"
	 * 
	 * The following values will result in subtracting 2 hours from the specified time:
	 * 	"2017-09-27T15:30 {-2H}"
	 * 
	 * @throws Exception
	 */
	public void testParseHours() throws Exception { 
		TimeAdjustmentValue positive2 = new TimeAdjustmentValue(Period.HOUR, 2);
		TimeAdjustmentValue negative2 = new TimeAdjustmentValue(Period.HOUR, -2);
		
		assertEquals("+sign allowed", positive2, TimeAdjustmentValue.parse("{+2H}"));
		assertEquals("+sign not required", positive2, TimeAdjustmentValue.parse("{2H}"));
		assertEquals("negative value", negative2, TimeAdjustmentValue.parse("{-2H}"));
	}

	/**
	 * The following values will result in adding 2 weeks to the 
	 * specified time:
	 * 	"2017-09-27T15:30 {2W}"
	 * 	"2017-09-27T15:30 {+2W}"
	 * 
	 * The following values will result in subtracting 2 weeks from the specified time:
	 * 	"2017-09-27T15:30 {-2W}"
	 * 
	 * @throws Exception
	 */
	public void testParseWeeks() throws Exception { 
		TimeAdjustmentValue positive2 = new TimeAdjustmentValue(Period.WEEK, 2);
		TimeAdjustmentValue negative2 = new TimeAdjustmentValue(Period.WEEK, -2);
		
		assertEquals("+sign allowed", positive2, TimeAdjustmentValue.parse("{+2W}"));
		assertEquals("+sign not required", positive2, TimeAdjustmentValue.parse("{2W}"));
		assertEquals("negative value", negative2, TimeAdjustmentValue.parse("{-2W}"));
	}


	/**
	 * The following values will result in adding 2 months to the 
	 * specified time:
	 * 	"2017-09-27T15:30 {2M}"
	 * 	"2017-09-27T15:30 {+2M}"
	 * 
	 * The following values will result in subtracting 2 months from the specified time:
	 * 	"2017-09-27T15:30 {-2M}"
	 * 
	 * @throws Exception
	 */
	public void testParseMonths() throws Exception { 
		TimeAdjustmentValue positive2 = new TimeAdjustmentValue(Period.MONTH, 2);
		TimeAdjustmentValue negative2 = new TimeAdjustmentValue(Period.MONTH, -2);
		
		assertEquals("+sign allowed", positive2, TimeAdjustmentValue.parse("{+2M}"));
		assertEquals("+sign not required", positive2, TimeAdjustmentValue.parse("{2M}"));
		assertEquals("negative value", negative2, TimeAdjustmentValue.parse("{-2M}"));
	}

	
	/**
	 * 
	 * @throws Exception
	 */
	public void testStripTimeAdjustments() throws Exception { 
		String timeValue = "2017-09-27T15:30";
		
		assertEquals("Invalid adjustment \"{2X}\" should NOT be stripped", timeValue + " {2X}", TimeAdjustmentValue.stripTimeAdjustment(timeValue + " {2X}"));
		
		assertEquals("Valid adjustment should be stripped", timeValue, TimeAdjustmentValue.stripTimeAdjustment(timeValue + "{+2M}"));
		assertEquals("Valid adjustment should be stripped", timeValue, TimeAdjustmentValue.stripTimeAdjustment(timeValue + " {+2M}"));
		assertEquals("Valid adjustment should be stripped", timeValue, TimeAdjustmentValue.stripTimeAdjustment(timeValue + " {-2D}"));
		assertEquals("Valid adjustment should be stripped", timeValue, TimeAdjustmentValue.stripTimeAdjustment(timeValue + " {-2}"));
		assertEquals("Valid adjustment should be stripped", timeValue, TimeAdjustmentValue.stripTimeAdjustment(timeValue + " {+2}"));
		assertEquals("Valid adjustment should be stripped", timeValue, TimeAdjustmentValue.stripTimeAdjustment(timeValue + " {2}"));
		assertEquals("Valid adjustment should be stripped", timeValue, TimeAdjustmentValue.stripTimeAdjustment(timeValue + " {}"));
	}
	
	public void testAdjustNoAdjustment() throws Exception { 
		TimeAdjustmentValue adjustment = new TimeAdjustmentValue(Period.NOADJUSTMENT, 10);
		
		long now = System.currentTimeMillis();
		
		assertEquals("Should be no adjustment", now, adjustment.adjustDateTime(now));
	}

	public void testAdjustHours() throws Exception { 
		TimeAdjustmentValue adjustment = new TimeAdjustmentValue(Period.HOUR, -2);
		
		Calendar calStart = new GregorianCalendar(2017, 9, 10, 0, 0);
		Calendar calAdjust = new GregorianCalendar(2017, 9, 9, 22, 0);
		
		assertEquals("Should be moved back 2 hours", calAdjust.getTimeInMillis(), adjustment.adjustDateTime(calStart.getTimeInMillis()));
	}

	public void testAdjustDays() throws Exception { 
		TimeAdjustmentValue adjustment = new TimeAdjustmentValue(Period.DAY, -2);
		
		Calendar calStart = new GregorianCalendar(2017, 8, 1);
		Calendar calAdjust = new GregorianCalendar(2017, 7, 30);
		
		assertEquals("Should be moved back 2 days", calAdjust.getTimeInMillis(), adjustment.adjustDateTime(calStart.getTimeInMillis()));
	}

	public void testAdjustDaysRollsOverYear() throws Exception { 
		TimeAdjustmentValue adjustment = new TimeAdjustmentValue(Period.DAY, -2);
		
		Calendar calStart = new GregorianCalendar(2017, 0, 1);
		Calendar calAdjust = new GregorianCalendar(2016, 11, 30);
		
		assertEquals("Should be moved back 2 days", calAdjust.getTimeInMillis(), adjustment.adjustDateTime(calStart.getTimeInMillis()));
	}


	public void testAdjustWeeks() throws Exception { 
		TimeAdjustmentValue adjustment = new TimeAdjustmentValue(Period.WEEK, -2);
		
		Calendar calStart = new GregorianCalendar(2017, 8, 1);
		Calendar calAdjust = new GregorianCalendar(2017, 7, 18);
		
		assertEquals("Should be moved back 2 weeks", calAdjust.getTimeInMillis(), adjustment.adjustDateTime(calStart.getTimeInMillis()));
	}
	
	
	public void testAdjustMonth() throws Exception { 
		TimeAdjustmentValue adjustment = new TimeAdjustmentValue(Period.MONTH, -2);
		
		Calendar calStart = new GregorianCalendar(2017, 8, 1);
		Calendar calAdjust = new GregorianCalendar(2017, 6, 1);
		
		assertEquals("Should be moved back 2 months", calAdjust.getTimeInMillis(), adjustment.adjustDateTime(calStart.getTimeInMillis()));
	}

	public void testAdjustMonthAcrossYear() throws Exception { 
		TimeAdjustmentValue adjustment = new TimeAdjustmentValue(Period.MONTH, -2);
		
		Calendar calStart = new GregorianCalendar(2017, 1, 5);
		Calendar calAdjust = new GregorianCalendar(2016, 11, 5);
		
		assertEquals("Should be moved back 2 months", calAdjust.getTimeInMillis(), adjustment.adjustDateTime(calStart.getTimeInMillis()));
	}

	public void testAdjustMonthIntoShorterMonth() throws Exception { 
		TimeAdjustmentValue adjustment = new TimeAdjustmentValue(Period.MONTH, -1);
		
		Calendar calStart = new GregorianCalendar(2017, 2, 31);  // March 31
		Calendar calAdjust = new GregorianCalendar(2017, 1, 28); // February 28th
		
		assertEquals("Should be moved back 1 month", calAdjust.getTimeInMillis(), adjustment.adjustDateTime(calStart.getTimeInMillis()));
	}

}
