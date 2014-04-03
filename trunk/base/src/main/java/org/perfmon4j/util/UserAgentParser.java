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

import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class UserAgentParser {
    private UserAgentParser() {
    }

   final static Pattern MSIE_PATTERN = Pattern.compile(".*?; MSIE ([\\d\\.]*).*");
   final static Pattern OPERA_PATTERN = Pattern.compile("(.*\\) Opera (\\d+\\.\\w*).*)|(Opera/(\\d+\\.\\w*).*)");
   final static Pattern NETSCAPE_PATTERN = Pattern.compile(".*Netscape\\d?((/)|(\\s))([\\d\\.]*).*");
   final static Pattern NAVIGATOR_PATTERN = Pattern.compile(".*Navigator/(.*)");
   final static Pattern MOZILLA_4x_NETSCAPE_PATTERN = Pattern.compile("Mozilla/(4\\.\\d++).*");
   final static Pattern MOZILLA_PATTERN = Pattern.compile("Mozilla/\\d\\.\\d.*; rv:(.*)\\).*");

   final static Pattern FIREFOX_PATTERN = Pattern.compile(".*Firefox/([\\d\\.]*).*");
   final static Pattern SAFARI_PATTERN = Pattern.compile(".*AppleWebKit/([\\d\\.]*).*Safari/([\\d\\.]*).*");
   final static Pattern SAFARI_PATTERN2 = Pattern.compile(".*Safari\\s([\\d\\.]*).*WebKit\\s([\\d\\.]*).*");
   final static Pattern CHROME_PATTERN = Pattern.compile(".*AppleWebKit/([\\d\\.]*).*Chrome/([\\d\\.]*).*");
   
   final static Pattern WINDOWS_PATTERN = Pattern.compile(".*; (Wind.*?)((\\d+\\.\\d+)|(\\d+)).*");
   final static Pattern WINDOWS_NETSCAPE_PATTERN = Pattern.compile(".*(Wind.*?)((\\d+\\.\\d+)|(\\d+)).*");
   final static Pattern LINUX_PATTERN = Pattern.compile(".*X11; .*?;(.*?);.*.*");
   final static Pattern MAC_PATTERN = Pattern.compile(".*;(.*Mac.*?);.*");
   final static Pattern FALLBACK_OS_PATTERN = Pattern.compile(".*((\\(X11;)|(\\())(.*); [A-Z]\\).*");

    public static UserAgentVO parseUserAgentString(String userAgent) {
        UserAgentVO result = null;

        String browser = null;
        String browserVersion = null;
        String osName = null;
        String osVersion = null;
        String suffix = null;

        boolean MSIE = false;

            // Look for browser strings...
        if (userAgent.indexOf("Opera") >= 0) {
            Matcher matcher = OPERA_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                browser = "Opera";
                browserVersion = matcher.group(2);
                if (browserVersion == null) {
                    browserVersion = matcher.group(4);
                }
            }
        } else if (userAgent.indexOf("compatible; MSIE") > 0) {
            Matcher matcher = MSIE_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                MSIE = true;
                browser = "MSIE";
                browserVersion = matcher.group(1);
            }
        } else if (userAgent.indexOf("Netscape") > 0) {
            Matcher matcher = NETSCAPE_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                browser = "Netscape";
                browserVersion = matcher.group(matcher.groupCount());
            }
         //Netscape as of 9.0 is now called Navigator
        } else if (userAgent.indexOf("Navigator") > 0) {
            Matcher matcher = NAVIGATOR_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                browser = "Netscape";
                browserVersion = matcher.group(matcher.groupCount());
            }
        } else if (userAgent.indexOf("Firefox") > 0) {
            Matcher matcher = FIREFOX_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                browser = "Firefox";
                browserVersion = matcher.group(1);
            }
        } else if (userAgent.indexOf("Chrome") > 0) {
            Matcher matcher = CHROME_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                suffix = "WebKit " + matcher.group(1);
                
                browser = "Chrome";
                browserVersion = matcher.group(2);
            }
        } else if (userAgent.contains("Safari")) {
            Matcher matcher = SAFARI_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                suffix = "WebKit " + matcher.group(1);

                browser = "Safari";
                browserVersion = matcher.group(2);
            } else {
                matcher = SAFARI_PATTERN2.matcher(userAgent);
                if (matcher.matches()) {
                    browser = "Safari";
                    browserVersion = matcher.group(1);

                    suffix = "WebKit " + matcher.group(2);
                }
            }
        } else if (userAgent.indexOf("Mozilla") == 0)  {
            Matcher matcher = MOZILLA_4x_NETSCAPE_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                browser = "Netscape";
                browserVersion = matcher.group(1);
            } else {
                matcher = MOZILLA_PATTERN.matcher(userAgent);
                if (matcher.matches()) {
                    browser = "Mozilla";
                    browserVersion = matcher.group(1);
                }
            }

        }


        boolean tryFallbackOS = false;
        // Look for OS Strings
//        if (userAgent.indexOf("; Windows CE") > 0) {
//            osName = "Windows CE";
//        }else if (userAgent.indexOf("; Windows ME") > 0) {
        if (userAgent.indexOf("; Windows ME") > 0) {
            osName = "Windows ME";
        } else if (userAgent.indexOf("; Windows XP") > 0) {
            osName = "Windows XP";
        } else if (userAgent.indexOf("; Wind") > 0) {
            Matcher matcher = WINDOWS_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                osName = matcher.group(1).trim();
                if (userAgent.indexOf(matcher.group(2) + "x") == -1) {
                    osVersion = matcher.group(2);
                }
            }
        } else if (userAgent.indexOf("Macintosh") > 0) {
            Matcher matcher = MAC_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                osName = matcher.group(1).trim();
            } else {
                osName = "Macintosh";
            }
        } else if (userAgent.indexOf("X11") > 0  || userAgent.indexOf("x11") > 0) {
            Matcher matcher = LINUX_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                osName = matcher.group(1).trim();
            } else {
                tryFallbackOS = true;
                osName = "X11";
            }
        } else {
            tryFallbackOS = true;
        }

        if (!MSIE && osName == null) {
            if (userAgent.indexOf("Win95") > 0) {
                osName = "Windows";
                osVersion = "95";
                tryFallbackOS = false;
            } else if (userAgent.indexOf("Win98") > 0) {
                osName = "Windows";
                osVersion = "98";
                tryFallbackOS = false;
            } else if (userAgent.indexOf("Win 9x") > 0) {
                osName = "Windows";
                osVersion = "9x";
                tryFallbackOS = false;
            } else if (userAgent.indexOf("WinNT") > 0) {
                osName = "WinNT";
                tryFallbackOS = false;
            } else if (userAgent.indexOf("Windows") > 0) {
                Matcher matcher = WINDOWS_NETSCAPE_PATTERN.matcher(userAgent);
                if (matcher.matches()) {
                    osName = matcher.group(1).trim();
                    osVersion = matcher.group(2);
                    tryFallbackOS = false;
                }
            }
        }

        if (tryFallbackOS) {
            Matcher matcher = FALLBACK_OS_PATTERN.matcher(userAgent);
            if (matcher.matches()) {
                osName = matcher.group(matcher.groupCount()).trim();
            }
        }
        if (browser != null) {
            result = new UserAgentVO(browser, browserVersion, osName, osVersion, suffix);
        } else {
            result = new UserAgentVO(userAgent);
        }

        return result;
    }
}
