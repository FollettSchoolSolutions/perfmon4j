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

public class DateTimeValue {
	private final String relativeDateTime;
	private final String fixedDateTime;
	private final long timeForStart;
	private final long timeForEnd;
	
	private DateTimeValue(String relativeDateTime, String fixedDateTime,
			long timeForStart, long timeForEnd) {
		super();
		this.relativeDateTime = relativeDateTime;
		this.fixedDateTime = fixedDateTime;
		this.timeForStart = timeForStart;
		this.timeForEnd = timeForEnd;
	}
	
	static DateTimeValue forRelativeTime(String relativeDateTime, Calendar dateTime) {
		dateTime.setLenient(false);
		
		String fixedDateTime = DateTimeHelper.formatDate(dateTime);
		long timeForStart = DateTimeHelper.toStartOfMinute(dateTime).getTimeInMillis();
		long timeForEnd = DateTimeHelper.toEndOfMinute(dateTime).getTimeInMillis();
		
		return new DateTimeValue(relativeDateTime, fixedDateTime, timeForStart, timeForEnd);
	}

	static DateTimeValue forFixedDateOnly(Calendar dateTime) {
		dateTime.setLenient(false);

		String fixedDateTime = DateTimeHelper.formatDateWithoutTime(dateTime);
		
		dateTime.set(Calendar.HOUR_OF_DAY, 0);
		dateTime.set(Calendar.MINUTE, 0);
		long timeForStart = DateTimeHelper.toStartOfMinute(dateTime).getTimeInMillis();
		
		dateTime.set(Calendar.HOUR_OF_DAY, 23);
		dateTime.set(Calendar.MINUTE, 59);
		long timeForEnd = DateTimeHelper.toEndOfMinute(dateTime).getTimeInMillis();
		
		return new DateTimeValue(null, fixedDateTime, timeForStart, timeForEnd);
	}
	

	static DateTimeValue forFixedDateTime(Calendar dateTime) {
		dateTime.setLenient(false);

		String fixedDateTime = DateTimeHelper.formatDate(dateTime);
		long timeForStart = DateTimeHelper.toStartOfMinute(dateTime).getTimeInMillis();
		long timeForEnd = DateTimeHelper.toEndOfMinute(dateTime).getTimeInMillis();
		
		return new DateTimeValue(null, fixedDateTime, timeForStart, timeForEnd);
	}
	
	
	/**
	 * This method will return the relativeDateTime.
	 * Types of values that are considered relative include "now", "now-10", "now-8H", "09:00"
	 * 
	 * When this field is null it indicates that date/time was passed in as an fixed or absolute
	 * value.
	 * @return
	 */
	public String getRelativeDateTime() {
		return relativeDateTime;
	}
	
	
	/**
	 * When a date is parsed using a relative term (i.e. "now", "now-30", "08:00") the fixed date
	 * will be calculated based on the current system time.
	 * 
	 * The fixed date is the absolute date/time referenced by this value.  It will either be a full date time
	 * (i.e. 2014-03-15T08:30) or a date only (i.e. 2014-03-15).
	 * 
	 * In the case of a date only the time will be:
	 * 		* The first minute of the day for a start time (i.e. getTimeForStart)
	 * 		* The last minute of the day for an end time (i.e. getTimeForEnd) 
	 */
	public String getFixedDateTime() {
		return fixedDateTime;
	}

	/**
	 *  
	 * 
	 */
	public long getTimeForStart() {
		return timeForStart;
	}

	
	/**
	 * 
	 * @return The time specified by the fixedDateTime in milliseconds
	 */
	public long getTimeForEnd() {
		return timeForEnd;
	}
	
	/**
	 * 
	 * @return true if this value indicates a relative date/time.
	 */
	public boolean isRelative() {
		return relativeDateTime != null;
	}
}
