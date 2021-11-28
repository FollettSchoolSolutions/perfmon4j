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
 * 	ddeucher@follett.com 
 * 	David Deuchert 
 * 	Follett School Solutions, LLC 
 * 	1340 Ridgeview Dr 
 * 	McHenry, IL 60050 
*/ 

package org.perfmon4j;

import org.perfmon4j.jmx.Perfmon4j;
import org.perfmon4j.util.JMXServerWrapper;

import junit.framework.TestCase;

public class Perfmon4jTest extends TestCase {
	private JMXServerWrapper serverWrapper;
	private Perfmon4j managedObject;
	
	public void setUp() throws Exception {
		super.setUp();
		managedObject = Perfmon4j.getSingleton(true);
		serverWrapper = new JMXServerWrapper();
	}
	
	public void testInitialize() {
		assertTrue("Perfmon4j jmx Object Should be initialized", Perfmon4j.isInitialized());
	}
	
	public void testGetVersion() throws Exception {
		String versionThroughJMX = (String)serverWrapper.getAttribute(managedObject.getObjectName(), "Version", String.class);
		assertEquals("Expected version", managedObject.getVersion(), versionThroughJMX);
	}
}
