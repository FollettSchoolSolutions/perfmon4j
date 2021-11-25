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

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.perfmon4j.impl.exceptiontracker.MeasurementElement;

import junit.framework.TestCase;

public class ExceptionTrackerTest extends TestCase {
	private BootConfiguration.ExceptionTrackerConfig config = null;

	public void setUp() throws Exception {
		super.setUp();
		
		TestBridgeClass.consumer = null;
		config = new BootConfiguration.ExceptionTrackerConfig();
	}
	
	public void testRegister() throws Exception {
		ExceptionTracker.registerWithBridge(config, TestBridgeClass.class.getName());
		
		assertNotNull("Should have registered with our testBridge", TestBridgeClass.consumer);
		assertTrue("Should be enabled", ExceptionTracker.isEnabled());
	}
	
	public void testIncrementDoesNotDoubleIncrement() throws Exception {
		// This simulates a constructor chaining scenario, where one constructor
		// calls another constructor in the same class.
		
		BootConfiguration.ExceptionElement element = new BootConfiguration.ExceptionElement("java.lang.Exception", "exception");
		config.addElement(element);
		ExceptionTracker.registerWithBridge(config, TestBridgeClass.class.getName());
	
		Exception exception = new Exception();
		
		// This should be treated as a nested constructor, 2 counts from the same exception object
		TestBridgeClass.consumer.accept(newEntry(exception));
		TestBridgeClass.consumer.accept(newEntry(exception));
		
		assertEquals("Should not double count for the same object", 1, ExceptionTracker.getCount(Exception.class.getName()));
		
		// Now call with a different exception object, this should increment the count.
		TestBridgeClass.consumer.accept(newEntry(new Exception()));
		assertEquals("Different object", 2, ExceptionTracker.getCount(Exception.class.getName()));
	}
	
	public void testMultiThreadIncrement() throws Exception {
		// This simulates a exceptions count accumulation from multiple threads.
		BootConfiguration.ExceptionElement element = new BootConfiguration.ExceptionElement("java.lang.Exception", "exception");
		config.addElement(element);
		ExceptionTracker.registerWithBridge(config, TestBridgeClass.class.getName());
	
		TestBridgeClass.consumer.accept(newEntry(new NullPointerException()));
		
		CountDownLatch latch = new CountDownLatch(1);
		new Thread(new Runnable() {
			@Override
			public void run() {
				TestBridgeClass.consumer.accept(newEntry(new NullPointerException()));
				latch.countDown();
			}
		}).start();
		
		// Wait for thread to run
		latch.await();
		
		assertEquals("Expected count accross all threads", 2, 
			ExceptionTracker.getCount(NullPointerException.class.getName()));
		assertEquals("Expected count on just this thread", 1, 
			ExceptionTracker.getCountForCurrentThread(NullPointerException.class.getName()));
	}
	
	public void testGenerateDataMap_NoConfig() throws Exception {
		ExceptionTracker.registerWithBridge(null, TestBridgeClass.class.getName());
		
		Map<String, MeasurementElement> dataMap = ExceptionTracker.generateDataMap();
		assertEquals("No data expected in the dataMap", 0, dataMap.size()); 
	}

	public void testGenerateDataMap_NoElements() throws Exception {
		ExceptionTracker.registerWithBridge(config, TestBridgeClass.class.getName());
		
		Map<String, MeasurementElement> dataMap = ExceptionTracker.generateDataMap();
		assertEquals("No data expected in the dataMap", 0, dataMap.size()); 
	}
	
	public void testGenerateDataMap_NoSQL() throws Exception {
		final String EXCEPTION_CLASS_NAME = "testGenerateDataMap_NoSQL";
		BootConfiguration.ExceptionElement element = new BootConfiguration.ExceptionElement(EXCEPTION_CLASS_NAME, "exception");
		config.addElement(element);
		ExceptionTracker.registerWithBridge(config, TestBridgeClass.class.getName());
		
		Map<String, MeasurementElement> dataMap = ExceptionTracker.generateDataMap();
		assertEquals("Expected an exception count", 1, dataMap.size());
		MeasurementElement measurement = dataMap.get(EXCEPTION_CLASS_NAME);
		assertNotNull("Should have an Exception element", measurement);
		assertEquals("Expected measurement name", "exception", measurement.getFieldName());
		assertFalse("Should indicate not to display SQL count", measurement.isIncludeSQLCountInOutput());
	}

	public void testGenerateDataMap_WithSQL() throws Exception {
		final String EXCEPTION_CLASS_NAME = "testGenerateDataMap_WithSQL";
		BootConfiguration.ExceptionElement element = new BootConfiguration.ExceptionElement(EXCEPTION_CLASS_NAME, "exception", true);
		config.addElement(element);
		ExceptionTracker.registerWithBridge(config, TestBridgeClass.class.getName());
		
		Map<String, MeasurementElement> dataMap = ExceptionTracker.generateDataMap();
		MeasurementElement measurement = dataMap.get(EXCEPTION_CLASS_NAME);
		assertTrue("Should indicate SQL count should be displayed ", measurement.isIncludeSQLCountInOutput());
	}
	
	private Map.Entry<String, Object> newEntry(Throwable throwable) {
		return new AbstractMap.SimpleEntry<String, Object>(throwable.getClass().getName(), throwable);		
	}
	
	private static final class TestBridgeClass {
		public static Consumer<Map.Entry<?, ?>> consumer = null;
		
		@SuppressWarnings({ "unused", "unchecked" })
		public static void registerExceptionConsumer(Consumer<?> newConsumer) {
			consumer = (Consumer<Map.Entry<?, ?>>)newConsumer;
		}
	}
}
