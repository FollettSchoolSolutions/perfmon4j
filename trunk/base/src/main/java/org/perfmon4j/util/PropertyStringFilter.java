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

import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyStringFilter {
    /**
     * @param args
     */
    
    final static String test = "dave${last.name}dave${first.name}dave";
    final static Pattern REQUEST_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    
    private final static String replaceAll(String source, String find, String replace) {
        String result = source;
        int index = result.indexOf(find);
        
        while (index >= 0) {
            result = result.replace(find, replace);
            index = result.indexOf(find, index + replace.length());
        }
        
        return result;
    }
    
    public static String filter(String sourceString) {
        return filter(System.getProperties(), sourceString);
    }
    
    public static String filter(Properties props, String sourceString) {
        List recursionPreventionList = new Vector();
        return filter(props, sourceString, recursionPreventionList);
    }

    private static String filter(Properties props, String sourceString, List recursionPreventionList) {
        String result = sourceString;
        Matcher matcher = REQUEST_PATTERN.matcher(sourceString);
       
        while (matcher.find()) {
            final String key = matcher.group(1);
            String value = props.getProperty(key);
            if (value != null) {
                if (!recursionPreventionList.contains(key)) {
                    try {
                        recursionPreventionList.add(key);
                        value = filter(props, value, recursionPreventionList);
                        result = replaceAll(result, "${" + key + "}", value);
                    } finally {
                        recursionPreventionList.remove(key);
                    }
                }
            }
        }
        
        return result;
    }
}
