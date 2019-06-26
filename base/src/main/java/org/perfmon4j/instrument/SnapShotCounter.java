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

package org.perfmon4j.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.perfmon4j.util.NumberFormatter;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SnapShotCounter {
	public enum Display {
		DELTA("/per duration", "getDelta_object"),
		DELTA_PER_MIN("/per minute", "getDeltaPerMinute_object"),
		DELTA_PER_SECOND("/per second", "getDeltaPerSecond_object"),
		INITIAL_VALUE("(InitialValue)", "getInitalValue_object"),
		FINAL_VALUE("(FinalValue)", "getFinalValue_object");
		
		private final String suffix;
		private final String getter;
 
		private Display(String suffix, String getter) {
			this.suffix = suffix;
			this.getter = getter;
		}
		
		public String getSuffix() {
			return suffix;
		}
		
		public String getGetter() {
			return getter;
		}
	}
	
	
	public Display preferredDisplay() default Display.DELTA;
	public Class<? extends NumberFormatter> formatter() default NumberFormatter.class;
	public String suffix() default "";
}
