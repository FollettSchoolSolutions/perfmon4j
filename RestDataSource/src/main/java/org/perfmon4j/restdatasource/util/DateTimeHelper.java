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

package org.perfmon4j.restdatasource.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.BadRequestException;

public class DateTimeHelper {
	private static final Pattern NOW_MINUS_MINUTES = Pattern.compile("NOW\\-(\\d+)");
	private static final Pattern NOW_MINUS_HOURS = Pattern.compile("NOW\\-(\\d+)H");
	private static final Pattern TIME_ONLY = Pattern.compile("(\\d{2}):(\\d{2})");
	private static final Pattern DATE_ONLY = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
	private static final Pattern DATE_TIME = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2})");
	
	public DateTimeValue parseDateTime(String dateTime) {
		DateTimeValue result = null;
		
		// Ignore case on time matching...
		String upper = dateTime.toUpperCase();
		
		if ("NOW".equals(upper)) {
			Calendar cal = new GregorianCalendar();
			cal.setTimeInMillis(getCurrentTime());
			result = DateTimeValue.forRelativeTime(dateTime, cal);
		} 
		
		// Look for now minus minutes or hours (i.e. now-60, now-8H
		if (result == null) {
			Integer minutesToSubtract = getMinutes(upper);
			if (minutesToSubtract != null) {
				Calendar cal = new GregorianCalendar();
				cal.setTimeInMillis(getCurrentTime());
				cal.add(Calendar.MINUTE, -1 * minutesToSubtract.intValue());
				result = DateTimeValue.forRelativeTime(dateTime, cal);
			}
		}
		
		// Look for time only!
		if (result == null) {
			Matcher m = TIME_ONLY.matcher(upper);
			if (m.matches()) {
				int hours = Integer.parseInt(m.group(1));
				int minutes = Integer.parseInt(m.group(2));
				Calendar cal = new GregorianCalendar();
				cal.setTimeInMillis(getCurrentTime());
				cal.set(Calendar.HOUR_OF_DAY, hours);
				cal.set(Calendar.MINUTE, minutes);
				try {
					result = DateTimeValue.forRelativeTime(dateTime, cal);
				} catch (IllegalArgumentException ie) {
					new BadRequestException("Unable to parse date/time from input: " + dateTime);
				}
			 }
		}

		// Look for date only!
		if (result == null) {
			Matcher m = DATE_ONLY.matcher(upper);
			if (m.matches()) {
				int year = Integer.parseInt(m.group(1));
				int month = Integer.parseInt(m.group(2));
				int day = Integer.parseInt(m.group(3));
				Calendar cal = new GregorianCalendar();
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month - 1);
				cal.set(Calendar.DAY_OF_MONTH, day);

				try {
					result = DateTimeValue.forFixedDateOnly(cal);
				} catch (IllegalArgumentException ie) {
					new BadRequestException("Unable to parse date/time from input: " + dateTime);
				}
			}
		}
		
		
		// Look for date only!
		if (result == null) {
			Matcher m = DATE_TIME.matcher(upper);
			if (m.matches()) {
				int year = Integer.parseInt(m.group(1));
				int month = Integer.parseInt(m.group(2));
				int day = Integer.parseInt(m.group(3));
				int hour = Integer.parseInt(m.group(4));
				int minute = Integer.parseInt(m.group(5));
				
				Calendar cal = new GregorianCalendar();
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month - 1);
				cal.set(Calendar.DAY_OF_MONTH, day);
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);

				try {
					result = DateTimeValue.forFixedDateTime(cal);
				} catch (IllegalArgumentException ie) {
					new BadRequestException("Unable to parse date/time from input: " + dateTime);
				}
			}
		}

		
		if (result == null) {
			throw new BadRequestException("Unable to parse date/time from input: " + dateTime);
		}
		
		return result;
	}
	
	long getCurrentTime() {
		return System.currentTimeMillis();
	}
	
	private Integer getMinutes(String dateTime) {
		Integer result = null;
		Matcher m = NOW_MINUS_MINUTES.matcher(dateTime);
		
		if (m.matches()) {
			result = Integer.valueOf(m.group(1));
		} else {
			m = NOW_MINUS_HOURS.matcher(dateTime);
			if (m.matches()) {
				result = Integer.valueOf(Integer.parseInt(m.group(1)) * 60);
			}
		}
		
		return result;
	}
	
	static Calendar toStartOfMinute(Calendar cal) {
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
	
	static Calendar toEndOfMinute(Calendar cal) {
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return cal;
	}
	
	static String formatDate(Calendar cal) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		return format.format(cal.getTime());
	}
	
	static String formatDateWithoutTime(Calendar cal) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		return format.format(cal.getTime());
	}

	public String formatDate(long time) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		return format.format(time);
	}
	
	
	public long truncateToMinute(long timeInMillis) {
		final long ONE_MINUTE = 60000;
		
		return (timeInMillis/ONE_MINUTE)*ONE_MINUTE;
	}
}
