/*
 *	Copyright 2022 Follett School Solutions, LLC 
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
 *  Follett School Solutions, LLC
 *  1340 Ridgeview Drive
 *  McHenry, IL 60050
 * 
*/

package org.perfmon4j.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SubCategorySplitter {
	private static final String autoRemoveStartingPattern = "(Interval\\.|Snapshot\\.)?";
	
	private static final Logger logger = LoggerFactory.initLogger(SubCategorySplitter.class);
	
	private static final Pattern leadingAndTrailingPeriods = Pattern.compile("^\\.*|\\.*$");
	private static final Pattern multiplePeriods = Pattern.compile("\\.{2,}");
	private final Pattern pattern;
	
	public SubCategorySplitter(String regEx) {
		Pattern tmp = null;
		try {
			tmp = Pattern.compile(autoRemoveStartingPattern + regEx);
		} catch (PatternSyntaxException ex) {
			logger.logWarn("Invalid Sub Category Splitter pattern found and will be ignored: " + regEx);
		}
		pattern = tmp;
	}
	
	public Split split(String category) {
		if (pattern != null) {
			Matcher m = pattern.matcher(category);
			
			if (m.find()) {
				final int groupWithMatch = m.groupCount();
				String subCategory = m.group(groupWithMatch);
				category = category.substring(0, m.start(groupWithMatch)) + "." + category.substring(m.end(groupWithMatch), category.length());
				return new Split(fixupPeriods(category), fixupPeriods(subCategory));
			} else {
				return new Split(category, null);
			}
		} else {
			return new Split(category, null);
		}
	}
	
	private String fixupPeriods(String category) {
		category = leadingAndTrailingPeriods.matcher(category).replaceAll("");
		return multiplePeriods.matcher(category).replaceAll(".");
	}
	
	public class Split {
		private final String category;
		private final String subCategory;
		
		private Split(String category, String subCategory) {
			this.category = category;
			this.subCategory = subCategory;
		}

		public String getCategory() {
			return category;
		}

		public String getSubCategory() {
			return subCategory;
		}
		
		public boolean hasSubCategory() {
			return subCategory != null;
		}
	}
}
