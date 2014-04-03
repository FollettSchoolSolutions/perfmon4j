/*
 *	Copyright 2008-2011 Follett Software Company 
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
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j.instrument.snapshot;


public class Delta {
	final private long initialValue;
	final private long finalValue;
	final private long delta;
	final private long durationMillis;
	final private double deltaPerSecond;
	final private double deltaPerMinute;

	public Delta(long initialValue, long finalValue, long durationMillis) {
		this.initialValue = initialValue;
		this.finalValue = finalValue;
		this.delta = finalValue - initialValue;
		this.durationMillis = durationMillis;
		deltaPerSecond = calcThrougput(delta, durationMillis, 1000);
		deltaPerMinute = calcThrougput(delta, durationMillis, 60 * 1000);
	}
	
	private static double calcThrougput(long delta, long duration, long timeUnitMillis) {
		if (duration > 0) {
			return (((double)delta / duration)) * timeUnitMillis;	
		} else {
			return 0;
		}
	}
	
	public long getInitalValue() {
		return initialValue;
	}
	
	public long getFinalValue() {
		return finalValue;
	}
	
	public long getDelta() {
		return finalValue - initialValue;
	}
	
	public double getDeltaPerMinute() {
		return deltaPerMinute;
	}

	public double getDeltaPerSecond() {
		return deltaPerSecond;
	}
	
	public double getDurationMillis() {
		return durationMillis;
	}
	
	public String toString() {
		return Long.toString(getDelta());
	}
	
	public Long getInitalValue_Object() {
		return new Long(initialValue);
	}
	
	public Long getFinalValue_object() {
		return new Long(finalValue);
	}
	
	public Long getDelta_object() {
		return new Long(finalValue - initialValue);
	}
	
	public Double getDeltaPerMinute_object() {
		return new Double(deltaPerMinute);
	}

	public Double getDeltaPerSecond_object() {
		return new Double(deltaPerSecond);
	}
	
	public Double getDurationMillis_object() {
		return new Double(durationMillis);
	}
}
