/*
 *	Copyright 2008-2017 Follett Software Company 
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyStringFilter {
    /**
     * @param args
     */
    final static String test = "dave${last.name}dave${first.name}dave";
    final static Pattern REQUEST_PATTERN =  Pattern.compile("\\$\\{\\s*([^\\$\\{\\}\\:]+)(?:\\s*:\\s*([^}]+))?\\s*\\}");
    
    
    private final ConfigurationProperties configurationProperties;
    
    public PropertyStringFilter() {
    	this(null, true);
    }

    public PropertyStringFilter(boolean includeEnvVariables) {
    	this(null, includeEnvVariables);
    }

    public PropertyStringFilter(ConfigurationProperties properties) {
    	this(properties, true);
    }
    
    public PropertyStringFilter(ConfigurationProperties properties, boolean includeEnvVariables) {
    	this.configurationProperties = properties != null ? properties : new ConfigurationProperties();
    	this.configurationProperties.setAutoEnvProperties(includeEnvVariables);
    }
    
    private String replaceAll(String source, String find, String replace) {
        String result = source;
        int index = result.indexOf(find);
        
        while (index >= 0) {
            result = result.replace(find, replace);
            index = result.indexOf(find, index + replace.length());
        }
        
        return result;
    }
    
    public String doFilter(String sourceString) {
        List<String> recursionPreventionList = new ArrayList<String>();
        return doFilter(sourceString, recursionPreventionList);
    }
    
    private String doFilter(String sourceString, List<String> recursionPreventionList) {
        String result = sourceString;
        if (sourceString != null) {
	        Matcher matcher = REQUEST_PATTERN.matcher(sourceString);
	        while (matcher.find()) {
	            final String key = matcher.group(1);
	            final String defaultValue = matcher.group(2);
	            String value = getProperty(key);
	            value = value != null ? value : defaultValue;
	            if (value != null) {
	                if (!recursionPreventionList.contains(key)) {
	                    try {
	                        recursionPreventionList.add(key);
	                        value = doFilter(value, recursionPreventionList);
	                        final String fullKey = defaultValue == null ?  key : key + ":" + defaultValue;  
	                        result = replaceAll(result, "${" + fullKey + "}", value);
	                    } finally {
	                        recursionPreventionList.remove(key);
	                    }
	                }
	            }
	        }
        }
        return result;
    }

    private String getProperty(String key) {
    	return configurationProperties.getProperty(key);
    }
    
    // Test only method
//    void setMockEnvVariables_TEST_ONLY(Map<String, String> mockEnvVariables) {
//    	envVariables = mockEnvVariables;
//    }    
    
//    @Deprecated
//    public static String filter(String sourceString) {
//    	return new PropertyStringFilter().doFilter(sourceString);
//    }
// 
//    @Deprecated
//    public static String filter(Properties props, String sourceString) {
//    	return new PropertyStringFilter(props).doFilter(sourceString);
//    }
}
