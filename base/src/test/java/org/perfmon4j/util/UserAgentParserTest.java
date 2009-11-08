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

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class UserAgentParserTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public UserAgentParserTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

/*----------------------------------------------------------------------------*/
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    private UserAgentVO testParse(String expectedBrowser,
        String expectedBrowserVersion, String expectedOS, String expectedOSVersion, String userAgent) throws Exception {
        UserAgentVO vo = UserAgentParser.parseUserAgentString(userAgent);

        assertEquals("browser", expectedBrowser, vo.getBrowserName());
        assertEquals("browserVersion", expectedBrowserVersion, vo.getBrowserVersion());
        assertEquals("os", expectedOS, vo.getOsName());
        assertEquals("osVersion", expectedOSVersion, vo.getOsVersion());

        return vo;
    }


    /**
     * If we can't parse the useragent string, we just return the full value.
     */
    public void testNonsensense() throws Exception{
        final String bogusUserAgent = "This is a bogus user agent string";

        UserAgentVO vo = testParse(null, null, null, null, bogusUserAgent);
        assertEquals(bogusUserAgent, vo.toString());
    }

    public void testToString() {
        UserAgentVO vo = new UserAgentVO("MyBrowser", "1.3.4", "MyOS", "4.5.6", null);
        assertEquals("MyBrowser 1.3.4; MyOS 4.5.6", vo.toString());
    }


    public void testMSIE() throws Exception {
        // Windows 3.x
        testParse("MSIE", "3.0", "Windows", "3.1",
            "Mozilla/2.0 (compatible; MSIE 3.0; Windows 3.1)");

        // Windows95
        testParse("MSIE", "4.01", "Windows", "95",
            "Mozilla/4.0 (compatible; MSIE 4.01; Windows 95");
        testParse("MSIE", "5.1", "Windows", "95",
            "Mozilla/4.0 (compatible; MSIE 5.1; Windows 95; FREESERVE_IE4)");

        // WindowsCE
        testParse("MSIE", "4.01", "Windows CE; Smartphone;", null,
            "Mozilla/4.0 (compatible; MSIE 4.01; Windows CE; Smartphone; 176x220)");

        // Windows 98
        testParse("MSIE", "5.01", "Windows", "98",
            "Mozilla/4.0 (compatible; MSIE 5.01; Windows 98; .NET CLR 1.1.4322)");

        // Windows XP
        testParse("MSIE","5.5", "Windows XP", null,
            "Mozilla/5.0 (compatible; MSIE 5.5; Windows XP);");
        testParse("MSIE","6.0", "Windows NT", "5.1",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727; InfoPath.1)");


        // Windows 2000
        testParse("MSIE", "6.0", "Windows", "2000",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000; MCK);");

        // Windows NT
        testParse("MSIE", "3.0", "Windows NT", "5.0",
            "Mozilla/3.0 (compatible; MSIE 3.0; Windows NT 5.0)");
        testParse("MSIE", "6.0", "Windows NT", "5.2",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; .NET CLR 1.1.4322; .NET CLR 2.0.41115)");
        testParse("MSIE", "6.0", "Windows NT", "5.2",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; Win64; AMD64)");

        // Windows Vista
        testParse("MSIE", "7.0", "Windows NT", "6.0",
            "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)");
        
        // IE 8
        testParse("MSIE", "8.0", "Windows NT", "5.1", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 1.1.4322)");
        
        // Falcon / Panther devices
        testParse("MSIE", "6.0", "Windows CE - Follett Falcon", "1.0",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows CE - Follett Falcon 1.0.0)");
        testParse("MSIE", "6.0", "Windows CE - Follett Falcon", "2.0",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows CE - Follett Falcon 2.0.0)");
        testParse("MSIE", "6.0", "Windows CE - Follett Falcon", "3.0",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows CE - Follett Falcon 3.0.0)");
        testParse("MSIE", "6.0", "Windows CE - Follett Falcon", "4.0",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows CE - Follett Falcon 4.0.0)");
        
        //Well known user agent string issue with upgraded IE6 to IE7 browsers
        //*BOTH* versions exist in the user agent string. We should take the higher revision
        testParse("MSIE", "7.0", "Windows NT", "5.1",
            "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1) ; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
    }

    
    public void testChrome() throws Exception {
        UserAgentVO vo = testParse("Chrome", "0.2.149.29", "Windows NT", "5.2",
            "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.2.149.29 Safari/525.13");
        assertTrue("toString Should append the webkit", vo.toString().endsWith("; WebKit 525.13"));
    }

    
    
/*----------------------------------------------------------------------------*/
    public void testNetscape() throws Exception {
        // Windows 95
        testParse("Netscape", "6.2.3", "Windows", "95",
            "Mozilla/5.0 (Windows; U; Win95; en-US; rv:0.9.4.1) Gecko/20020508 Netscape6/6.2.3");

        // Mozilla 4.x assume netscape...
        testParse("Netscape", "4.04", "Windows", "95",
            "Mozilla/4.04 [en] (Win95; I)");
        testParse("Netscape", "4.06", "WinNT", null,
            "Mozilla/4.06 [en] (WinNT; I)");
        testParse("Netscape", "4.5", "Windows NT", "5.1",
            "Mozilla/4.5 [en] (Windows NT 5.1; U)");

        // Windows 98
        testParse("Netscape", "5.0", "Windows", "98",
            "Mozilla/5.25 Netscape/5.0 (Win98)");
        testParse("Netscape", "7.0", "Windows", "98",
            "Mozilla/5.0 (Windows; U; Win98; en-GB; rv:1.0.1) Gecko/20020823 Netscape/7.0");
        testParse("Netscape", "7.1", "Windows", "98",
            "Mozilla/5.0 (Windows; U; Win98; en-US; rv:0.9.2) Gecko/20010726 Netscape7/7.1");

        // Windows XP
        testParse("Netscape","7.4", "Windows XP", null,
            "Mozilla/5.0 (Windows; U; Windows XP; en-US; rv:1.7.5) Netscape 7.4");

        // *nix
        testParse("Netscape", "6.1", "HP-UX 9000/785", null,
            "Mozilla/5.0 (X11; U; HP-UX 9000/785; en-US; rv:1.6) Gecko/20020604 Netscape6/6.1");
        testParse("Netscape", "6.1", "Linux i686", null,
            "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:0.9.2) Gecko/20010726 Netscape6/6.1");
        testParse("Netscape", "6.1", "SunOS sun4u", null,
            "Mozilla/5.0 (X11; U; SunOS sun4u; en-US; rv:0.9.2) Gecko/20011002 Netscape6/6.1");

        testParse("Netscape", "9.0b3", "Windows NT", "5.2",
            "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.8.1.7pre) Gecko/20070815 Firefox/2.0.0.6 Navigator/9.0b3");
    }

    public void testFirefox() throws Exception {
        // Windows 98
        testParse("Firefox", "0.5.6", "Windows", "98",
            "Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.7) Gecko/20041122 Firefox/0.5.6+");

        // Windows XP
        testParse("Firefox", "2.0.0.5", "Windows NT", "5.1",
            "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.5) Gecko/20070713 Firefox/2.0.0.5");

        // Windows Vista
        testParse("Firefox", "1.5.0.7", "Windows NT", "6.0",
            "Mozilla/5.0 (Windows; U; Windows NT 6.0; pt-BR; rv:1.8.0.7) Gecko/20060909 Firefox/1.5.0.7");

        // *nix
        testParse("Firefox", "1.0.1", "Linux i686", null,
            "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.7.6) Gecko/20050225 Firefox/1.0.1");
        testParse("Firefox", "1.0.2", "Linux x86_64", null,
            "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.7.4) Gecko/20050308 Firefox/1.0.2");
        testParse("Firefox", "1.0", "FreeBSD i386", null,
            "Mozilla/5.0 (X11; U; FreeBSD i386; en-US; rv:1.7.5) Gecko/20050205 Firefox/1.0");

        testParse("Firefox", "1.0", "OpenBSD i386", null,
            "Mozilla/5.0 (X11; U; OpenBSD i386; en-US; rv:1.7.5) Gecko/20050101 Firefox/1.0");
        testParse("Firefox", "1.0", "Slackware", null,
            "Mozilla/5.0 (X11; U; Slackware; Linux i686; pl-PL; rv:1.7.5) Gecko/20041108 Firefox/1.0");
        testParse("Firefox", "1.0", "SunOS i86pc", null,
            "Mozilla/5.0 (X11; U; SunOS i86pc; en-US; rv:1.7.5) Gecko/20050101 Firefox/1.0");
        testParse("Firefox", "1.0", "SunOS sun4u", null,
            "Mozilla/5.0 (X11; U; SunOS sun4u; en-US; rv:1.7.5) Gecko/20041109 Firefox/1.0");

        // MAC
        testParse("Firefox","1.0.1", "PPC Mac OS X Mach-O", null,
            "Mozilla/5.0 (Macintosh; U; PPC Mac OS X Mach-O; en-US; rv:1.7.6) Gecko/20050223 Firefox/1.0.1");
    }


    public void testMozilla() throws Exception {
        testParse("Mozilla", "1.7.1", "Windows", "98", "Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.7.1) Gecko/20040707");
        testParse("Mozilla", "1.3.1", "Macintosh", null,
            "Mozilla/5.0 (Macintosh; U; PPC; en-US; rv:1.3.1) Gecko/20030721");
        testParse("Mozilla", "1.4", "Linux i686", null,
            "Mozilla/5.0 (X11; U; Linux i686; da-DK; rv:1.4) Gecko/20030630");
        testParse("Mozilla", "1.6b", "Windows", "9x",
            "Mozilla/5.0 (Windows; U; Win 9x 4.90; en-US; rv:1.6b) Gecko/20031208");
        testParse("Mozilla", "1.7.1", "Windows NT", "5.2",
            "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.7.1) Gecko/20040707");
        testParse("Mozilla", "1.8a5", "Linux i686", null,
            "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8a5) Gecko/20041123");
    }


    public void testOpera() throws Exception {
        testParse("Opera", "3.62", "Windows", "3.10", "Mozilla/4.71 (Windows 3.10;US) Opera 3.62  [en]");
        testParse("Opera", "5.12", "Windows", "98", "Mozilla/4.0 (compatible; MSIE 5.0; Windows 98) Opera 5.12  [en]");
        testParse("Opera", "5.12", "Windows", "2000", "Opera/5.12 (Windows 2000; U)  [en]");
        testParse("Opera", "6.04", "Windows XP", null, "Mozilla/4.0 (compatible; MSIE 5.0; Windows XP) Opera 6.04  [en]");
        testParse("Opera", "6.04", "Windows", "98", "Mozilla/4.0 (compatible; MSIE 5.0; Windows 98) Opera 6.04  [en-GB]");
        testParse("Opera", "6.04", "Windows ME", null, "Mozilla/4.0 (compatible; MSIE 5.0; Windows ME) Opera 6.04 [en]");
        testParse("Opera", "7.11", "Windows NT", "5.1", "Opera/7.11 (Windows NT 5.1; U)  [es]");
        testParse("Opera", "7.54u1", "Windows NT", "5.0", "Opera/7.54u1 (Windows NT 5.0; U)  [en]");
        testParse("Opera", "8.0", "PPC Mac OS X", null, "Opera/8.0 (Macintosh; PPC Mac OS X; U; en)");
        testParse("Opera", "8.00", "Windows NT", "5.1", "Opera/8.00 (Windows NT 5.1; U; Mockingbird)");
        testParse("Opera", "7.11", "Linux 2.4.18-xfs i686", null, "Opera/7.11 (Linux 2.4.18-xfs i686; U) [en]");
        testParse("Opera", "7.54", "Unix", null, "Opera/7.54 (Unix; U) [en]");
        testParse("Opera", "7.54", "FreeBSD i386", null, "Opera/7.54 (X11; FreeBSD i386; U)  [en]");
        testParse("Opera", "7.54", "SunOS sun4u", null, "Opera/7.54 (X11; SunOS sun4u; U) [en]");

        // This is not optimal... Since we dont not what *nix flavor we have, but we will live with it.
        testParse("Opera", "7.23", "X11", null, "Opera/7.23 (compatible; MSIE 6.0; X11; Linux i586) [en]");
    }


    public void testSafari() throws Exception {
        UserAgentVO vo = testParse("Safari","85.8.1", "PPC Mac OS X", null,
            "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; sa) AppleWebKit/85.8.5 (KHTML, like Gecko) Safari/85.8.1");
        assertTrue("toString Should append the webkit", vo.toString().endsWith("; WebKit 85.8.5"));

        vo = testParse("Safari","125.12", "PPC Mac OS X", null,
            "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/125.5.6 (KHTML, like Gecko) Safari/125.12");
        assertTrue("toString Should append the webkit", vo.toString().endsWith("; WebKit 125.5.6"));

        vo = testParse("Safari","522.12.2", "Windows NT", "5.1",
            "Mozilla/5.0 (Windows; U; Windows NT 5.1; en) AppleWebKit/522.12.1 (KHTML, like Gecko) Version/3.0.1 Safari/522.12.2");
        assertTrue("toString Should append the webkit", vo.toString().endsWith("; WebKit 522.12.1"));

        vo = testParse("Safari","419.3", "Intel Mac OS X", null,
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-us) AppleWebKit/419.3  (KHTML, like Gecko) Safari/419.3");
        assertEquals("toString Should append the webkit", "Safari 419.3; Intel Mac OS X; WebKit 419.3", vo.toString());
        

        //Test some Safari User Agent String's that did not parse properly
        //That's because it did not contain the Mozilla/5.0 we were expecting
        //Also had WebKit instead of AppleWebKit
        vo = testParse("Safari", "525.22", "Unknown", null,
            "Safari 525.22; PPC Mac OS X 10_4_11; WebKit 525.18");
        assertTrue("toString Should append the webkit", vo.toString().endsWith("; WebKit 525.18"));
        
        vo = testParse("Safari", "316.6", "Unknown", null,
            "Safari 316.6; PPC Mac OS X; WebKit 312.9");
        assertTrue("toString Should append the webkit", vo.toString().endsWith("; WebKit 312.9"));

        vo = testParse("Safari", "525.27.1", "Unknown", null,
            "Safari 525.27.1; Intel Mac OS X 10_5_6; WebKit 525.27.1");
        assertTrue("toString Should append the webkit", vo.toString().endsWith("; WebKit 525.27.1"));
        
        vo = testParse("Safari", "525.26.12", "Unknown", null,
            "Safari 525.26.12; PPC Mac OS X 10_4_11; WebKit 525.27.1");
        assertTrue("toString Should append the webkit", vo.toString().endsWith("; WebKit 525.27.1"));
    }


    public void testPerformance() throws Exception {
        // Should be able to parse 15000 per second...
        final String userAgentStrings[] = {
            "Mozilla/5.0 (compatible; MSIE 5.5; Windows XP)",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727; InfoPath.1)",
            "Mozilla/5.0 (Windows; U; Windows NT 6.0; pt-BR; rv:1.8.0.7) Gecko/20060909 Firefox/1.5.0.7",
            "Mozilla/5.25 Netscape/5.0 (Win98)",
            "Mozilla/5.0 (Windows; U; Win98; en-GB; rv:1.0.1) Gecko/20020823 Netscape/7.0",
            "Mozilla/5.0 (Windows; U; Win98; en-US; rv:0.9.2) Gecko/20010726 Netscape7/7.1",
            "Mozilla/5.0 (Windows; U; Windows XP; en-US; rv:1.7.5) Netscape 7.4",
            "Mozilla/5.0 (X11; U; HP-UX 9000/785; en-US; rv:1.6) Gecko/20020604 Netscape6/6.1" };

        int count = 0;
        System.gc();
        long start = System.currentTimeMillis();
        while (count++ < 20000) {
            int offset = count % userAgentStrings.length;
            UserAgentVO vo = UserAgentParser.parseUserAgentString(userAgentStrings[offset]);
            assertNotNull(vo);
            assertNotNull(vo.browserName);
        }
        long duration = System.currentTimeMillis() - start;
        // This is far from a perfect test but we should be able to parse 20000 per second
        assertTrue("Expected to do " + (--count) + " in less than 1 second - Actual duration: " + duration,
            duration < 1000);

    }

/*----------------------------------------------------------------------------*/
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {UserAgentParserTest.class.getName()};

        TestRunner.main(testCaseName);
    }


/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new UserAgentParserTest("testMSIE"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(UserAgentParserTest.class);
        }

        return( newSuite);
    }
}
