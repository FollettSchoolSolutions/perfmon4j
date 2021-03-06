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

package org.perfmon4j.util;

import org.perfmon4j.PerfMon;

public class NumberFormatter {
	private static final Logger logger = LoggerFactory.initLogger(NumberFormatter.class);
	
	public String format(Number value) {
		if (value instanceof Float || value instanceof Double) {
			return String.format("%.3f", value);
		} else {
			return value.toString();
		}
	}
	
	public static NumberFormatter newInstance(String className) {
		NumberFormatter result;
		try {
			result = (NumberFormatter)Class.forName(className, true, PerfMon.getClassLoader()).newInstance();
		} catch (Exception e) {
			logger.logInfo("Failed to load formatter class \"" + className + "\" - Using default formatter");
			result = new NumberFormatter();
		}
		return result;
	}
}
