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

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.management.ObjectName;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;


public class MiscHelperTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";
    private static final double DELTA = .002;    

/*----------------------------------------------------------------------------*/
    public MiscHelperTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        PerfMon.configure();
    }
    
/*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
        PerfMon.deInit();
        super.tearDown();
    }
    
    private double getStdDev(int values[]) {
        int sampleCount = values.length;
        int sum = 0;
        long sumOfSquares = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
            sumOfSquares += (((long)values[i]) * values[i]);
        }
        
        return MiscHelper.calcStdDeviation(sampleCount, sum, sumOfSquares);
    }
    

	/*----------------------------------------------------------------------------*/
	public void testCalculateStandardDeviation() throws Exception {
	    assertEquals(7703731.909, getStdDev(new int[]{34432,44322,422,342342,233422,  4332,    342552,  23422345,    2342342}), DELTA);
	    assertEquals(6.534, getStdDev(new int[]{20,30,23,24,25,10,16,29,28}), DELTA);
	    assertEquals(672.126, getStdDev(new int[]{-2000,30,23,24,25,10,16,0,1}), DELTA);
	    assertEquals(625.083, getStdDev(new int[]{-2000,30,23,24,25,10,16,0,1,5,7,300,800}), DELTA);
	}
	
	
	public void testFormatTextAppenderDataLine() {
		assertEquals("test...... 5\r\n", MiscHelper.formatTextDataLine(10, "test", "5"));
		assertEquals("bogus..... 10\r\n", MiscHelper.formatTextDataLine(10, "bogus", "10"));
		assertEquals("truncatet. 700\r\n", MiscHelper.formatTextDataLine(10, "truncatethis", "700"));
	
		assertEquals("test...... 5.34%\r\n", MiscHelper.formatTextDataLine(10, "test", 5.3424f, "%"));
	}
    
    
/*----------------------------------------------------------------------------*/    
    public void testGetInteger() throws Exception {
        final String STR = "valueX=1 valueY=2 *Unfriendly?reg+ex[name=101";
        
        Integer valueX = MiscHelper.parseInteger("valueX", STR);
        assertEquals(1, valueX.intValue());
        
        Integer valueY = MiscHelper.parseInteger("valueY", STR);
        assertEquals(2, valueY.intValue());
        
        Integer valueZ = MiscHelper.parseInteger("valueZ", STR);
        assertNull(valueZ);
        
        // Make sure we handle names with unfriendly regex characters
        Integer unfriendly = MiscHelper.parseInteger("*Unfriendly?reg+ex[name", STR);
        assertEquals(101, unfriendly.intValue());
    }

    
    public void testNoExceptionForNullInput() throws Exception {
        try {
            assertNull(MiscHelper.parseInteger("valueZ", null));
        } catch (Exception ex) {
            fail("Should handle null input");
        }
    }
    
    
    final static long FIVE_SECONDS = 5 * 1000;
    final static long FIVE_MINUTES = 5 * 60 * 1000;
    final static long ONE_HOUR = 60 * 60 * 1000;
    final static long FIVE_HOURS = 5 * 60 * 1000 * 60;
    
    final static long DEFAULT_FOR_TEST = ONE_HOUR;

/*----------------------------------------------------------------------------*/
    public void testGetMillisDisplayable() throws Exception {
        final int second = 1000;
        final int minute = 60 * second;
        
        assertEquals("100 ms",  MiscHelper.getMillisDisplayable(100));
        assertEquals("1001 ms", MiscHelper.getMillisDisplayable(1001));
       
        assertEquals("1 second", MiscHelper.getMillisDisplayable(1000));
        assertEquals("2 seconds", MiscHelper.getMillisDisplayable(2000));
        
        assertEquals("1 minute", MiscHelper.getMillisDisplayable(minute));
        assertEquals("2 minutes", MiscHelper.getMillisDisplayable(minute * 2));
        assertEquals("121 seconds", MiscHelper.getMillisDisplayable((minute * 2) + second));
        assertEquals("120100 ms", MiscHelper.getMillisDisplayable((minute * 2) + 100));
        
        assertEquals("60 minutes", MiscHelper.getMillisDisplayable((minute * 60)));
    }


    /** TODO See if we can actually make this test work in linux....  For now
     * it is more trouble than it is worth.
     */
    public void IGNOREtestCurrentTimeWithMilliResolution() throws Exception {
        for (int i = 0; i < 1000; i++) {
            long jvm = System.currentTimeMillis();
            long milliRes = MiscHelper.currentTimeWithMilliResolution();
            
            long diff = Math.abs(jvm-milliRes);
            assertTrue("diff expected to be < 100 but was: " + diff, diff < 100);
        }
        
        // There is little margin for error here... 
        // The garbage collector or other system pauses could cause the
        // test to intermitently fail.  If we fail 100 times out of 1000 we 
        // probably have a problem
        
        long stop = MiscHelper.currentTimeWithMilliResolution() + 500;
        long last = MiscHelper.currentTimeWithMilliResolution(); 
        while (last < stop) {
            long now = MiscHelper.currentTimeWithMilliResolution();
            if (now - last > 2) {
                fail("Millis must have rounded... Two successive calls increased by: " + (now - last));
            }
            last = now;
        }
    }
/*----------------------------------------------------------------------------*/
    public void testConvertStringToMillis() throws Exception {
        
        validateStringToMillisConversion("missing unit defaults to minutes", "5", FIVE_MINUTES);
        validateStringToMillisConversion("unknown unit defaults to minutes", "5 eons", FIVE_MINUTES);
        validateStringToMillisConversion("Garbage values should be default", "WHAT IS THIS", ONE_HOUR);
        validateStringToMillisConversion("Empty String should be set to the default", "", ONE_HOUR);
        validateStringToMillisConversion("null should be set to the default", null, ONE_HOUR);
        validateStringToMillisConversion("0 should be set to the default", "0", ONE_HOUR);
        validateStringToMillisConversion("negative values are negated", "-5", FIVE_MINUTES);
        validateStringToMillisConversion("negative values are negated", "-1 hour", ONE_HOUR);
        validateStringToMillisConversion("extra trailing units are ignored", "5 hours minutes seconds", FIVE_HOURS);
        
        // Test Milliseconds
        validateStringToMillisConversion("5 millis", 5);
        validateStringToMillisConversion("5 Millis", 5);
        validateStringToMillisConversion("5 milli", 5);
        validateStringToMillisConversion("5 Milli", 5);
        validateStringToMillisConversion("5 milliseconds", 5);
        validateStringToMillisConversion("5 Milliseconds", 5);
        validateStringToMillisConversion("5 millisecond", 5);
        validateStringToMillisConversion("5 Millisecond", 5);
        validateStringToMillisConversion("5 ms", 5);
        validateStringToMillisConversion("5 MS", 5);

        // Test trimming extra whitespace
        validateStringToMillisConversion(" 5 MS ", 5);

        
        // Test Seconds
        validateStringToMillisConversion("5 seconds", FIVE_SECONDS);
        validateStringToMillisConversion("5 Seconds", FIVE_SECONDS);
        validateStringToMillisConversion("5 second", FIVE_SECONDS);
        validateStringToMillisConversion("5 Second", FIVE_SECONDS);
        validateStringToMillisConversion("5 sec", FIVE_SECONDS);
        validateStringToMillisConversion("5 secs", FIVE_SECONDS);
        validateStringToMillisConversion("5 Secs", FIVE_SECONDS);
        validateStringToMillisConversion("5 Sec", FIVE_SECONDS);
        validateStringToMillisConversion("5 s", FIVE_SECONDS);
        validateStringToMillisConversion("5 S", FIVE_SECONDS);
        
        // Test Minutes
        validateStringToMillisConversion("5 minutes", FIVE_MINUTES);
        validateStringToMillisConversion("5 Minutes", FIVE_MINUTES);
        validateStringToMillisConversion("5 minute", FIVE_MINUTES);
        validateStringToMillisConversion("5 Minute", FIVE_MINUTES);
        validateStringToMillisConversion("5 min", FIVE_MINUTES);
        validateStringToMillisConversion("5 mins", FIVE_MINUTES);
        validateStringToMillisConversion("5 min", FIVE_MINUTES);
        validateStringToMillisConversion("5 Mins", FIVE_MINUTES);
        validateStringToMillisConversion("5 mn", FIVE_MINUTES);
        validateStringToMillisConversion("5 Mn", FIVE_MINUTES);
        validateStringToMillisConversion("5 m", FIVE_MINUTES);
        validateStringToMillisConversion("5 M", FIVE_MINUTES);

        // Test Hours
        validateStringToMillisConversion("5 hours", FIVE_HOURS);
        validateStringToMillisConversion("5 Hours", FIVE_HOURS);
        validateStringToMillisConversion("5 hour", FIVE_HOURS);
        validateStringToMillisConversion("5 Hour", FIVE_HOURS);
        validateStringToMillisConversion("5 hr", FIVE_HOURS);
        validateStringToMillisConversion("5 HR", FIVE_HOURS);
        validateStringToMillisConversion("5 h", FIVE_HOURS);
        validateStringToMillisConversion("5 H", FIVE_HOURS);
    }
    
    
    public void testGetMBeanServer() {
    	assertNotNull("MiscHelper.findMBeanServer", MiscHelper.findMBeanServer(null));
    }

/*----------------------------------------------------------------------------*/    
    private void validateStringToMillisConversion(String value, long expectedMillis) {
        validateStringToMillisConversion(null, value, expectedMillis);
    }
    

/*----------------------------------------------------------------------------*/    
    private void validateStringToMillisConversion(String comment, String value, long expectedMillis) {
        String expectedMsg = "Conversion of \"" + value + "\"";
        if (comment != null) {
            expectedMsg = expectedMsg + " (" + comment + ")";
        } 
        assertEquals(expectedMsg, expectedMillis,
            MiscHelper.convertIntervalStringToMillis(value, DEFAULT_FOR_TEST));
    }

    
	/*----------------------------------------------------------------------------*/
	public void testCreateJarManifest() throws Exception {
		File tmpJar = File.createTempFile("dave", ".jar");
		tmpJar.delete(); // Want to see if we create it... We only use the createTempFile to get a unique path name.
		try {
			Properties props = new Properties();
			props.setProperty("Premain-Class", "org.perfmon4j.instrument.PerfMonTimerTransformer");

			MiscHelper.createJarFile(tmpJar.getAbsolutePath(), props, new File[]{});
			assertTrue("File should have been created", tmpJar.exists());

			JarFile jar = new JarFile(tmpJar);
			Manifest manifest = jar.getManifest();
			
			assertNotNull("Should have a manifest", manifest);
			assertEquals("Should have manifest element", "org.perfmon4j.instrument.PerfMonTimerTransformer", manifest.getMainAttributes().getValue("Premain-Class"));
			jar.close();
		} finally {
			tmpJar.delete();
		}
	}
    
	
	/*----------------------------------------------------------------------------*/
	public void testCreateJar() throws Exception {
		File tmpJar = File.createTempFile("dave", ".jar");
		File fileFolder = File.createTempFile("bogus", "");
		fileFolder.delete();
		fileFolder.mkdir();
		File tmpFile = File.createTempFile("bogus", ".class", fileFolder);
		
		tmpJar.delete(); // Want to see if we create it... We only use the createTempFile to get a unique path name.
		try {
			Properties props = new Properties();

			MiscHelper.createJarFile(tmpJar.getAbsolutePath(), props, new File[]{fileFolder});
			assertTrue("File should have been created", tmpJar.exists());

			JarFile jar = new JarFile(tmpJar);
			JarEntry entry = jar.getJarEntry(tmpFile.getName());
			
			assertNotNull("Jar should contain our file", entry);
			jar.close();
		} finally {
			tmpJar.delete();
			tmpFile.delete();
			fileFolder.delete();
		}
	}
	
	
	public void testDateOnlyFromMillis() {
		long millis = MiscHelper.calcDateOnlyFromMillis(System.currentTimeMillis());
		
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(millis);
		
		assertEquals("Hour should be 0", 0, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals("Minute should be 0", 0, cal.get(Calendar.MINUTE));
		assertEquals("Second should be 0", 0, cal.get(Calendar.SECOND));
		assertEquals("MilliSecond should be 0", 0, cal.get(Calendar.MILLISECOND));
	}

	
	public void testStringTokenizer() {
		assertTrue(Arrays.equals(new String[]{"*"}, MiscHelper.tokenizeCSVString("*")));
		assertTrue(Arrays.equals(new String[]{"cat", "dog", "tv remote" }, MiscHelper.tokenizeCSVString("cat,   dog,  tv remote")));
		assertNull(MiscHelper.tokenizeCSVString(""));
		assertNull(MiscHelper.tokenizeCSVString(null));
	}
	

	public void testGenerateDefaultSystemName() {
		String systemName = MiscHelper.getDefaultSystemName();
		assertNotNull("systemName", systemName);
		// Make sure it does not append  the full CWD path.
		assertTrue("length <= 200 chars", systemName.length() <= 200);
	}

	public void testGenerateDefaultSystemNameWithoutCWDHash() {
		String systemNameWithCWDHash = MiscHelper.getDefaultSystemName(true);  
		String systemNameWithoutCWDHash = MiscHelper.getDefaultSystemName(false);  
		
		assertTrue("System name without the hash should be shorter", systemNameWithoutCWDHash.length()
				< systemNameWithCWDHash.length());
	}
	
	
	private void assertMatches(boolean shouldMatch, String strA, String strB) throws Exception {
		ObjectName a = new ObjectName(strA);
		ObjectName b = new ObjectName(strB);
		
		String debug = "Object names " + strA + " and " + strB + " ";
		if (shouldMatch) {
			assertTrue(debug + "should match", MiscHelper.objectNameAttributeKeysMatch(a, b));
		} else {
			assertFalse(debug + "should NOT match", MiscHelper.objectNameAttributeKeysMatch(a, b));
		}
	}
	

	public void testObjectNameAttributeKeysMatch() throws Exception {
		assertMatches(true, "myobject:a=b,c=d", "myobject:a=b,c=d");
		assertMatches(true, "myobject:a=Z,c=Y", "myobject:a=b,c=d");
		assertMatches(true, "myobject:a=*,c=*", "myobject:a=b,c=d");
		
		assertMatches(false, "myobject:a=*,c=*", "myobject:a=b,c=d,d=f");
	}

/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {MiscHelperTest.class.getName()};

        TestRunner.main(testCaseName);
    }
    

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new MiscHelperTest("testFormatTextAppenderDataLine"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(MiscHelperTest.class);
        }

        return( newSuite);
    }
}
