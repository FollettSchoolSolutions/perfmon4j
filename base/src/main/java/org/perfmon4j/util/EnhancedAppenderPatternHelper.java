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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.perfmon4j.PerfMon;


public class EnhancedAppenderPatternHelper {
	private static final Pattern validPattern = Pattern.compile("^(\\./|/)(\\S+)");
	private static final Pattern packageSplit = Pattern.compile("(\\.|/)"); // Can separate with a '.' or a '/'
	
	
	@Deprecated
	static public PatternInfo massagePattern(String pattern) {
		String remainder = "";
		
		// Remove the prefix ("./" or "/") from the pattern.
		pattern = pattern.replaceFirst("(\\/|\\.\\/)", "");
	
		// Find the first section in case pattern 
		// represents multiple levels
		String[] split = packageSplit.split(pattern);
		if (split.length > 1) {
			remainder = pattern.substring(split[0].length() + 1, pattern.length());
			pattern = split[0];
		}
		
		pattern = pattern.replaceAll("#\\*", "\\\\w+");
		pattern = pattern.replaceAll("#", "\\\\w");
	   
		return new PatternInfo(pattern, remainder);
	}
	
	static public boolean isTraditionalPattern(String pattern) {
		return PerfMon.APPENDER_PATTERN_PARENT_ONLY.equals(pattern)
			|| PerfMon.APPENDER_PATTERN_NA.equals(pattern)
			|| PerfMon.APPENDER_PATTERN_PARENT_ONLY.equals(pattern)
			|| PerfMon.APPENDER_PATTERN_PARENT_AND_CHILDREN_ONLY.equals(pattern)
			|| PerfMon.APPENDER_PATTERN_CHILDREN_ONLY.equals(pattern)
			|| PerfMon.APPENDER_PATTERN_ALL_DESCENDENTS.equals(pattern)
			|| PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS.equals(pattern)
			|| ".".equals(pattern);
	}
	
	@Deprecated
	static public boolean validateAppenderPattern(String pattern) {
		boolean result = false;
		
		Matcher m = validPattern.matcher(pattern);
		if (m.matches()) {
			// Remove the prefix
			pattern = m.group(2);
			
			// Don't allow empty packages
			String[] packages = packageSplit.split(pattern);
			if (packages.length > 0) { // Must specify at least one package
				result = true;
				for (String p : packages) {
					if (p.isEmpty()) { // Empty packages are not allowed
						result = false;
						break;
					}
				}
			}
		}
		
		
		return result;
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
			
			public boolean couldApplyToCurrent() {
				return !regEx.isEmpty() && remainder.isEmpty();
			}
			
			public boolean couldApplyToDescendants() {
				return !remainder.isEmpty();
			}
	}

	public static String buildPattern(String monitorName, String pattern) {
		if (!isTraditionalPattern(pattern) && (pattern.startsWith("/") || pattern.startsWith("./"))) {
			String patternSuffix = null;
			String patternPrefix = "\\.";
			
			// Check to see if we have a trailing child (/*) pattern
			if (pattern.endsWith(PerfMon.APPENDER_PATTERN_CHILDREN_ONLY)) {
				pattern = pattern.replaceAll("/\\*" + "$", "(\\.[^.]+)??");
			} else if (pattern.endsWith(PerfMon.APPENDER_PATTERN_ALL_DESCENDENTS)) {
				pattern = pattern.replaceAll("/\\*{2}" + "$", "(\\.[^.]+)*?");
			} else if (pattern.startsWith(PerfMon.APPENDER_PATTERN_CHILDREN_ONLY + "/")) {
				pattern = pattern.replaceFirst("/\\*/", "[^\\.]+(\\$|(\\.");
				patternSuffix = "))";
			} else if (pattern.startsWith(PerfMon.APPENDER_PATTERN_PARENT_AND_CHILDREN_ONLY + "/")) {
				pattern = pattern.replaceFirst("\\./\\*/", "[^\\.]+((\\$|\\.");
				patternSuffix = "))))";
				patternPrefix = "($|\\.(";	
			} else if (pattern.startsWith(PerfMon.APPENDER_PATTERN_PARENT_ONLY )) {
				pattern = pattern.replaceFirst("\\./", "(\\$|\\.");
				patternSuffix = ")";
				patternPrefix = "";	
			} 

			
			if (patternSuffix == null) {
				// Remove the prefix ("./" or "/") from the pattern.
				pattern = pattern.replaceFirst("(\\/|\\.\\/)", "");
			}	
			pattern = pattern.replaceAll("\\.", "\\\\.");
			pattern = pattern.replaceAll("/", "\\\\.");
			pattern = pattern.replaceAll("#\\*", "\\\\w+");
			pattern = pattern.replaceAll("#", "\\\\w");
			
			if (patternSuffix != null) {
				pattern += patternSuffix;
			}
		   
			pattern = Pattern.quote(monitorName) + patternPrefix + pattern; 
			
			return pattern;
		} else {
			return null;
		}
	}
}




