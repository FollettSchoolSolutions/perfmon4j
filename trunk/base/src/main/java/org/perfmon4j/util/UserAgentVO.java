/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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

public class UserAgentVO {
    final String browserName;
    final String browserVersion;
    final String osName;
    final String osVersion;
 
    final String outputString;
    
    public UserAgentVO(String browserName, String browserVersion, String osName, 
        String osVersion, String suffix) {
        this.browserName = browserName;
        this.browserVersion = browserVersion;
        this.osName = (osName == null) ? "Unknown" : osName;
        this.osVersion = osVersion;
    
        StringBuilder builder = new StringBuilder(browserName);
        if (browserVersion != null) {
            builder.append(" ")
                .append(browserVersion);
        }

        builder.append("; ").append(osName);
        if (osVersion != null) {
            builder.append(" ")
                .append(osVersion);
        }
        
        if (suffix != null) {
            builder.append("; ").append(suffix);
        }
        
        outputString = builder.toString();
    }
    
    public UserAgentVO(String userAgentString) {
        this.browserName = null;
        this.browserVersion = null;
        this.osName = null;
        this.osVersion = null;
    
        outputString = userAgentString;
    }

    public String getBrowserName() {
        return browserName;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public int hashCode() {
        return outputString.hashCode();
    }
    
    public boolean equals(Object compare) {
        boolean result = (this == compare);
        
        if (!result && compare instanceof UserAgentVO) {
            result =  outputString.equals(((UserAgentVO)compare).outputString);
        }
        return result;
    }
    
    public String toString() {
        return outputString;
    }
}
