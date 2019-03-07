/*
 *	Copyright 2019 Follett Software Company 
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

package org.perfmon4j.util;


public class WildcardPatternHelper {
	static public PatternInfo massagePattern(String pattern) {
		String remainder = "";
		
		// Remove the prefix ("./" or "/") from the pattern.
		pattern = pattern.replaceFirst("(\\/|\\.\\/)", "");
	
		// Find the first section in case pattern 
		// represents multiple levels
		String[] split = pattern.split("\\.");
		if (split.length > 1) {
			remainder = pattern.substring(split[0].length() + 1, pattern.length());
			pattern = split[0];
		}
		
		pattern = pattern.replaceAll("#\\*", "\\\\w+");
		pattern = pattern.replaceAll("#", "\\\\w");
	   
		return new PatternInfo(pattern, remainder);
	}

	static public class PatternInfo {
		   private final String regEx;
		   private final String remainder;
		   
			public PatternInfo(String regEx, String remainder) {
				super();
				this.regEx = regEx;
				this.remainder = remainder;
			}

			public String getRegEx() {
				return regEx;
			}

			public String getRemainder() {
				return remainder;
			}
	}
}




