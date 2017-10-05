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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeAdjustmentValue {
	private static final Pattern PATTERN = Pattern.compile("\\s*\\~ADJ([\\-\\+]?\\d*)([HDWM]?)");
	
	public enum Period {
		NOADJUSTMENT,
		HOUR,
		DAY,
		WEEK,
		MONTH
	}

	private final int increment;
	private final Period period;
	
	public TimeAdjustmentValue(Period period, int increment) {
		this.increment = increment;
		this.period = period;
	}

	public int getIncrement() {
		return increment;
	}

	public Period getPeriod() {
		return period;
	}

	static public String stripTimeAdjustment(String input) {
		String result = input;
		
		if (input != null) {
			Matcher m = PATTERN.matcher(input);
			result = m.replaceFirst("");
		}
		
		return result;
	}
	
	static public TimeAdjustmentValue parse(String input) {
		TimeAdjustmentValue result = null;
		if (input != null) {
			Matcher m = PATTERN.matcher(input);
			if (m.find()) {
				if (m.groupCount() > 0 && !"".equals(m.group(1))) {
					Period p = Period.DAY; // Default value;
					int i = Integer.parseInt(m.group(1));
					String g2 = m.group(2);
					if ("H".equals(g2)) {
						p = Period.HOUR;
					} else if ("W".equals(g2)) {
						p = Period.WEEK;
					} else if ("M".equals(g2)) {
						p = Period.MONTH;
					}
					result = new TimeAdjustmentValue(p, i);
				} else {
					result = new TimeAdjustmentValue(Period.NOADJUSTMENT, 0);
				}
			}
		}
		return result;
	}
	

	/**
	 * 
	 * @param value
	 * @return - The input value modified to match the period and increment  
	 */
	public long adjustDateTime(long value) {
		long result = value;
		
		if (!Period.NOADJUSTMENT.equals(period)) {
			Calendar cal = new GregorianCalendar();
			cal.setTimeInMillis(value);
			
			if (Period.HOUR.equals(period)) {
				cal.add(Calendar.HOUR, increment);
			} else if (Period.DAY.equals(period)) {
				cal.add(Calendar.DATE, increment);
			} else if (Period.WEEK.equals(period)) {
				cal.add(Calendar.DATE, increment * 7);
			} else if (Period.MONTH.equals(period)) {
				cal.add(Calendar.MONTH, increment);
			}
			
			result = cal.getTimeInMillis();
		}
		
		return result;
	}
	
	
	@Override
	public String toString() {
		return "TimeAdjustmentValue [increment=" + increment + ", period="
				+ period + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + increment;
		result = prime * result + ((period == null) ? 0 : period.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimeAdjustmentValue other = (TimeAdjustmentValue) obj;
		if (increment != other.increment)
			return false;
		if (period != other.period)
			return false;
		return true;
	}
}
