/*
 *	Copyright 2011 Follett Software Company
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

package org.perfmon4j;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.perfmon4j.ThreadTraceConfig.HTTPCookieTrigger;
import org.perfmon4j.ThreadTraceConfig.HTTPRequestTrigger;
import org.perfmon4j.ThreadTraceConfig.HTTPSessionTrigger;
import org.perfmon4j.ThreadTraceConfig.Trigger;
import org.perfmon4j.ThreadTraceConfig.TriggerType;
import org.perfmon4j.remotemanagement.intf.FieldKey;


public class ExternalThreadTraceConfigTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public ExternalThreadTraceConfigTest(String name) {
        super(name);
    }

    public void testTriggerTypeFromPrefix() {
    	assertEquals(TriggerType.HTTP_REQUEST_PARAM, TriggerType.fromPrefix("HTTP"));
    	assertEquals(TriggerType.HTTP_SESSION_PARAM, TriggerType.fromPrefix("HTTP_SESSION"));
    	assertEquals(TriggerType.HTTP_COOKIE_PARAM, TriggerType.fromPrefix("HTTP_COOKIE"));
    	assertEquals(TriggerType.THREAD_NAME, TriggerType.fromPrefix("THREAD_NAME"));
    	assertEquals(TriggerType.THREAD_PROPERTY, TriggerType.fromPrefix("THREAD_PROPERTY"));
    	assertNull("Unrecognized prefix should return null", TriggerType.fromPrefix("BOGUS"));
    }

    public void testSetTriggerHTTPRequestParam() {
    	ExternalThreadTraceConfig config = new ExternalThreadTraceConfig();
    	config.setTrigger(FieldKey.encodeTriggerArg("HTTP", "reqParam", "value1"));

    	Trigger[] triggers = config.getTriggers();
    	assertNotNull(triggers);
    	assertEquals(1, triggers.length);
    	assertTrue(triggers[0] instanceof HTTPRequestTrigger);
    	assertEquals("HTTP:reqParam=value1", triggers[0].getTriggerString());
    }

    public void testSetTriggerHTTPSessionParam() {
    	ExternalThreadTraceConfig config = new ExternalThreadTraceConfig();
    	config.setTrigger(FieldKey.encodeTriggerArg("HTTP_SESSION", "sessAttr", "value2"));

    	Trigger[] triggers = config.getTriggers();
    	assertNotNull(triggers);
    	assertEquals(1, triggers.length);
    	assertTrue(triggers[0] instanceof HTTPSessionTrigger);
    	assertEquals("HTTP_SESSION:sessAttr=value2", triggers[0].getTriggerString());
    }

    public void testSetTriggerHTTPCookieParam() {
    	ExternalThreadTraceConfig config = new ExternalThreadTraceConfig();
    	// Name/value round-trips a comma and an equals sign -- proves the Base64
    	// URL-safe encoding, not the underlying value, is what's on the wire.
    	config.setTrigger(FieldKey.encodeTriggerArg("HTTP_COOKIE", "cookieName", "a,b=c"));

    	Trigger[] triggers = config.getTriggers();
    	assertNotNull(triggers);
    	assertEquals(1, triggers.length);
    	assertTrue(triggers[0] instanceof HTTPCookieTrigger);
    	assertEquals("HTTP_COOKIE:cookieName=a,b=c", triggers[0].getTriggerString());
    }

    public void testSetTriggerRejectsThreadNameAndThreadProperty() {
    	ExternalThreadTraceConfig config = new ExternalThreadTraceConfig();
    	try {
    		config.setTrigger(FieldKey.encodeTriggerArg("THREAD_NAME", "n", "v"));
    		fail("THREAD_NAME should not be supported for on-demand scheduling");
    	} catch (IllegalArgumentException ex) {
    		// Expected.
    	}

    	try {
    		config.setTrigger(FieldKey.encodeTriggerArg("THREAD_PROPERTY", "n", "v"));
    		fail("THREAD_PROPERTY should not be supported for on-demand scheduling");
    	} catch (IllegalArgumentException ex) {
    		// Expected.
    	}
    }

    public void testSetTriggerRejectsUnrecognizedPrefix() {
    	ExternalThreadTraceConfig config = new ExternalThreadTraceConfig();
    	try {
    		config.setTrigger(FieldKey.encodeTriggerArg("BOGUS", "n", "v"));
    		fail("Unrecognized prefix should throw");
    	} catch (IllegalArgumentException ex) {
    		// Expected.
    	}
    }

/*----------------------------------------------------------------------------*/
    public static void main(String[] args) {
        BasicConfigurator.configure();
        String[] testCaseName = {ExternalThreadTraceConfigTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(ExternalThreadTraceConfigTest.class);
        }

        return( newSuite);
    }
}
