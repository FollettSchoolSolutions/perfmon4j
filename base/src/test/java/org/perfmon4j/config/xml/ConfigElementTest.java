/*
 *	Copyright 2014 Follett Software Company 
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

package org.perfmon4j.config.xml;

import org.perfmon4j.PerfMonTestCase;

public class ConfigElementTest extends PerfMonTestCase {
	private ConfigElement a;
	private ConfigElement b;

	protected void setUp() throws Exception {
		super.setUp();
		a = new ConfigElement();
		b = new ConfigElement();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testMergeDifferentAppenders() {
		a.getAppenders().add(buildAppender("appA"));
		b.getAppenders().add(buildAppender("appB"));
		
		ConfigElement m = ConfigElement.mergeConfigs(a, b);
		
		// Both appenders should exists...
		assertNotNull(m.getAppender("appA"));
		assertNotNull(m.getAppender("appB"));
	}

	public void testMergeSameAppender() {
		a.getAppenders().add(buildAppender("appA", "this-one-should-be-replaced", ""));
		b.getAppenders().add(buildAppender("appA", "This-one-should-survive", ""));
		
		ConfigElement m = ConfigElement.mergeConfigs(a, b);
		
		AppenderConfigElement element = m.getAppender("appA"); 
		assertNotNull(element);
		assertEquals("Should not have duplicated appenders", 1, m.getAppenders().size());
		assertEquals("This-one-should-survive", element.getClassName());
	}
	
	public void testMergeSameMonitors() {
		a.getMonitors().add(buildMonitor("monA", false));
		b.getMonitors().add(buildMonitor("monA", true));
		
		ConfigElement m = ConfigElement.mergeConfigs(a, b);
		
		MonitorConfigElement element = m.getMonitor("monA"); 
		assertNotNull(element);
		assertEquals("Should not have duplicated monitor", 1, m.getMonitors().size());
		assertTrue("This-one-should-survive", element.isEnabled());
	}
	
	public void testMergeSameSnapShots() {
		a.getSnapShots().add(buildSnapShot("ssA", "this-one-should-be-replaced"));
		b.getSnapShots().add(buildSnapShot("ssA", "This-one-should-survive"));
		
		ConfigElement m = ConfigElement.mergeConfigs(a, b);
		
		SnapShotConfigElement element = m.getSnapShot("ssA"); 
		assertNotNull(element);
		assertEquals("Should not have duplicated appenders", 1, m.getSnapShots().size());
		assertEquals("This-one-should-survive", element.getClassName());
	}

	public void testMergeThreadTrace() {
		a.getThreadTraces().add(buildThreadTrace("ssA", "this-one-should-be-replaced"));
		b.getThreadTraces().add(buildThreadTrace("ssA", "This-one-should-survive"));
		
		ConfigElement m = ConfigElement.mergeConfigs(a, b);
		
		ThreadTraceConfigElement element = m.getThreadTrace("ssA"); 
		assertNotNull(element);
		assertEquals("Should not have duplicated appenders", 1, m.getThreadTraces().size());
		assertEquals("This-one-should-survive", element.getMaxDepth());
	}
	
	
	private ThreadTraceConfigElement buildThreadTrace(String name, String maxDepth) {
		ThreadTraceConfigElement result = new ThreadTraceConfigElement();

		result.setMonitorName(name);
		result.setMaxDepth(maxDepth);
		
		return result;
	}
	
	private SnapShotConfigElement buildSnapShot(String name, String className) {
		SnapShotConfigElement result = new SnapShotConfigElement();

		result.setName(name);
		result.setClassName(className);
		
		
		return result;
	}
	
	private MonitorConfigElement buildMonitor(String name, boolean enabled) {
		MonitorConfigElement result = new MonitorConfigElement();
		
		result.setName(name);
		result.setEnabled(enabled);
		
		return result;
	}
	
	private AppenderConfigElement buildAppender(String name) {
		return buildAppender(name, "className", "interval");
	}
	
	private AppenderConfigElement buildAppender(String name, String className, String interval) {
		AppenderConfigElement result = new AppenderConfigElement();
		result.setName(name);
		result.setClassName(className);
		result.setInterval(interval);
		
		return result;
	}
}
