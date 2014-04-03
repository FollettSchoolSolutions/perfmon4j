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


/**
 * This will format a numeric value that represents bytes as B, KB, MB or GB
 */
public class ByteFormatter extends NumberFormatter {
	private final long KB = 1024;
	private final long BYTE_LIMIT = KB * 10;
	private final long MB = KB * 1024;
	private final long KB_LIMIT = MB * 100;
	private final long GB = MB * 1024;
	private final long MB_LIMIT = GB * 100;
	
	public String format(Number value) {
		String result;
		long v = value.longValue();
		
		if (v < BYTE_LIMIT) {
			result = v + " bytes";
		} else if (v < KB_LIMIT) {
			value = new Double(((double)v)/KB);
			result = String.format("%.3f KB", value);
		} else if (v < MB_LIMIT) {
			value = new Double(((double)v)/MB);
			result = String.format("%.3f MB", value);
		} else {
			value = new Double(((double)v)/GB);
			result = String.format("%.3f GB", value);
		}
		
		return result;
	}
}
