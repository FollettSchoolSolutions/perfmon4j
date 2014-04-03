/*
 *	Copyright 2008 Follett Software Company 
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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j.instrument.snapshot;

public class Ratio {
	final private double numerator;
	final private double denominator;
	
	public Ratio(long numerator, long denominator) {
		this.numerator = numerator;
		this.denominator = denominator;
	}
	public Ratio(double numerator, double denominator) {
		this.numerator = numerator;
		this.denominator = denominator;
	}
	
	public float getRatio() {
		float result = 0f;
		
		if (denominator != 0) {
			result = (float)(numerator/denominator);
		}
		
		return result;
	}

	public static Ratio generateRatio(Delta numerator, Delta denominator) {
		Ratio result = null;
		if (numerator != null && denominator != null) {
			result = new Ratio(numerator.getDelta(), denominator.getDelta());
		}
		return result;
	}

	public static Ratio generateRatio(long numerator, long denominator) {
		return new Ratio(numerator, denominator);
	}

	public static Ratio generateRatio(double numerator, double denominator) {
		return new Ratio(numerator, denominator);
	}

	public static Ratio generateRatio(float numerator, float denominator) {
		return new Ratio(numerator, denominator);
	}

	public static Ratio generateRatio(short numerator, short denominator) {
		return new Ratio(numerator, denominator);
	}

	public static Ratio generateRatio(int numerator, int denominator) {
		return new Ratio(numerator, denominator);
	}

}
