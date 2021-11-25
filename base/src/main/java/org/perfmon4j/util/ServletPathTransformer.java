/*
 *	Copyright 2021 Follett School Solutions, LLC 
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ServletPathTransformer {
	private static final Logger logger = LoggerFactory.initLogger(ServletPathTransformer.class);
	
	@Deprecated
	public static ServletPathTransformer newTransformer(String transformations) {
		return new ServletPathTransformer(transformations, "WebRequest");
	}
	
	public static ServletPathTransformer newTransformer(String transformations, String baseCategory) {
		return new ServletPathTransformer(transformations, baseCategory);
	}
	
	private final Transformation transforms[];
	private final String baseCategory;

	private ServletPathTransformer(String transformations, String baseCategory) {
		this.baseCategory = baseCategory;
		List<ServletPathTransformer.Transformation> tList = new ArrayList<ServletPathTransformer.Transformation>();
		
		if (transformations != null) {
			for (String tranDef : transformations.split(",")) {
				String [] tranDefParts = tranDef.split("\\=\\>");
				if (tranDefParts.length == 2) {
					String patternString = tranDefParts[0].trim();
					String replacement = tranDefParts[1].trim();
					if (!patternString.isBlank()) {
						Pattern pattern = compilePattern(patternString, replacement.startsWith("$"));
						if (pattern != null) {
							tList.add(new Transformation(pattern, replacement));
						}
					} else {
						logger.logWarn("Unable to parse servlet path. Pattern must not be blank: " + tranDef);
					}
				} else {
					logger.logWarn("Unable to parse servlet path transformation definition: " + tranDef);
				}
			}
		}
		
		transforms = tList.toArray(new ServletPathTransformer.Transformation[] {});
	}
	
	private final String ASTRISKS_REPLACEMENT =  "934234ASTRISKS08234986";
	private final String DOUBLE_ASTRISKS_REPLACEMENT =  "2374DOUBLEASTRISKS32386";
	private final String QUESTION_MARK_REPLACEMENT =  "0349945QMARKS878353";
	private final String BAR_REPLACEMENT =  "03045945BARS87382353";
	
	
	private Pattern compilePattern(String patternString, boolean makeStartsWith) {
		patternString = patternString.replaceAll("\\*{2}", DOUBLE_ASTRISKS_REPLACEMENT);
		patternString = patternString.replaceAll("\\*", ASTRISKS_REPLACEMENT);
		patternString = patternString.replaceAll("\\?", QUESTION_MARK_REPLACEMENT);
		patternString = patternString.replaceAll("\\|", BAR_REPLACEMENT);
		
		
		patternString = escapeRegEx(patternString);
		
		patternString  = patternString.replaceAll(DOUBLE_ASTRISKS_REPLACEMENT, ".*");
		patternString  = patternString.replaceAll(ASTRISKS_REPLACEMENT, "[^/]*");
		patternString  = patternString.replaceAll(QUESTION_MARK_REPLACEMENT, "[^/]");
		patternString  = patternString.replaceAll(BAR_REPLACEMENT, "|");
		
		if (makeStartsWith) {
			patternString = "^(" + patternString + ")";
		}
		
		try {
			return Pattern.compile(patternString);
		} catch (PatternSyntaxException pse) {
			logger.logWarn("Unable to compile servlet path transformation pattern:" + patternString);
		}
		return null;
	}
	
	/* package level getter for testing */
	int getNumTransforms() {
		return transforms.length;
	}

	private String escapeRegEx(String value) {
		// Found this unique way to safely escape, all special characters 
		// in a pattern string here: https://stackoverflow.com/questions/14134558/list-of-all-special-characters-that-need-to-be-escaped-in-a-regex
		return value.replaceAll("[\\W]", "\\\\$0");
	}
	
	@Deprecated
	public String transform(String servletPath) {
		for (Transformation t : transforms) {
			Matcher m = t.pattern.matcher(servletPath);
			servletPath = m.replaceAll(escapeRegEx(t.replacement));
		}
		if (!servletPath.startsWith("/") && !servletPath.startsWith("$")) {
			servletPath = "/" + servletPath;
		}
		
		return servletPath;
	}

	public String transformToCategory(String servletPath) {
		servletPath = transform(servletPath);
		
		if (servletPath.startsWith("$")) {
			servletPath = servletPath.replaceFirst("\\$", "");
		} else {
			servletPath = baseCategory + servletPath;
		}
		
		return servletPath.replaceAll("\\.", "_")
				.replaceAll("/", "\\.")
				.replaceAll("[\\.\\_]+$", "")  // Remove trailing periods or underscores
				.replaceAll("\\.{2,}", "."); // Remove duplicate periods
		}
	
	
	private static class Transformation {
		private final Pattern pattern;
		private final String replacement;
		
		Transformation(Pattern pattern, String replacement) {
			this.pattern = pattern;
			if (replacement.startsWith("$") && !replacement.endsWith("/")) {
				// If they are doing a Category substitution make sure it ends wit a slash.
				replacement += "/";
			}
			
			this.replacement = replacement;
		}
	}
}
