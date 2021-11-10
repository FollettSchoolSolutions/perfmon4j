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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.perfmon4j.BootConfiguration.ExceptionElement;
import org.perfmon4j.impl.exceptiontracker.Counter;
import org.perfmon4j.impl.exceptiontracker.ExceptionPerThreadTracker;
import org.perfmon4j.impl.exceptiontracker.ExceptionTrackerData;
import org.perfmon4j.impl.exceptiontracker.MeasurementElement;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class ExceptionTracker extends SnapShotMonitor {
	private static final Logger logger = LoggerFactory.initLogger(ExceptionTracker.class);
	public static final String BRIDGE_CLASS_NAME = "generated.perfmon4j.ExceptionBridge";
	private static boolean enabled = false;
	private static BootConfiguration.ExceptionTrackerConfig config = null;
	
	public ExceptionTracker(String name) {
		super(name);
	}
	
	public ExceptionTracker(String name, boolean usePriorityTimer) {
		super(name, usePriorityTimer);
	}
	
	/**
	 * Package level for Unit Testing.
	 * @return
	 */
	static Map<String, MeasurementElement> generateDataMap() {
		Map<String, MeasurementElement> result = new HashMap<String, MeasurementElement>();
		
		if (config != null) {
			for (ExceptionElement element : config.getElements()) {
				result.put(element.getClassName(), new MeasurementElement(element.getDisplayName(), 
						ExceptionPerThreadTracker.getGlobalCount(element.getClassName()), element.isIncludeSQL()));
			}
		}
		return result;
	}
	
	@Override
	public SnapShotData initSnapShot(long currentTimeMillis) {
		return new ExceptionTrackerData(generateDataMap(), currentTimeMillis);
	}

	@Override
	public SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis) {
		return ((ExceptionTrackerData)data).stop(generateDataMap(), currentTimeMillis);
	}

	public static boolean isEnabled() {
		return enabled;
	}
	
	
	/**
	 * PerfMonTimerTransformer will use this method to connect the
	 * ExceptionTracker when the Exception class is successfully 
	 * instrumented.
	 * 
	 * If this connection is made the Tracker will be enabled.
	 *   
	 * @throws Exception
	 */
	public static boolean registerWithBridge(BootConfiguration.ExceptionTrackerConfig config) throws Exception {
		return registerWithBridge(config, BRIDGE_CLASS_NAME);
	}
	
	/**
	 * Package level for testing.  Should only be called from UnitTests.
	 * @param config
	 * @param bridgeClassName
	 * @return
	 * @throws Exception
	 */
	
	static boolean registerWithBridge(BootConfiguration.ExceptionTrackerConfig config, String bridgeClassName) throws Exception {
		ClassLoader loader = ExceptionTracker.class.getClassLoader();
		if (loader == null) {
			logger.logDebug("ExceptionTracker.class.getClassLoader() returned null, trying systemClassLoader");
			loader = ClassLoader.getSystemClassLoader();
		}
		if (loader != null) {
			Class<?> clazz = loader.loadClass(bridgeClassName);
			Method method = clazz.getDeclaredMethod("registerExceptionConsumer" , new Class[] {Consumer.class});
			method.invoke(null, new Object[] {new BridgeExceptionConsumer()});
			enabled = true;
			ExceptionTracker.config = config;
		} else {
			logger.logError("Unable to find classloader to load ExceptionTracker Bridge Class");
		}
	
		return enabled;
	}
	
	@SuppressWarnings("rawtypes")
	public static final class BridgeExceptionConsumer implements Consumer {
		@Override
		public void accept(Object mapEntry) {
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>)mapEntry;
			String className = (String)entry.getKey();
			Object exception = entry.getValue();

			notifyInConstructor(className, exception);
		}
	}

	@SnapShotString
	public static String getEnabled() {
		return enabled ? "enabled" : "disabled";
	}
	
	public static Counter getCounter(String className) {
		if (enabled) {
			return ExceptionPerThreadTracker.getGlobalCount(className);
		}
		return ExceptionPerThreadTracker.NO_VALUE_COUNTER;
	}
	
	public static long getCount(String className) {
		return getCounter(className).getCount();
	}
	
	public static long getSQLCount(String className) {
		return getCounter(className).getSQLCount();
	}

	public static Counter getCounterForCurrentThread(String className) {
		if (enabled) {
			return ExceptionPerThreadTracker.threadExceptionTracker.get().getThreadCount(className);
		}
		return ExceptionPerThreadTracker.NO_VALUE_COUNTER;
	}
	
	public static long getCountForCurrentThread(String className) {
		return getCounterForCurrentThread(className).getCount();
	}

	public static long getSQLCountForCurrentThread(String className) {
		return getCounterForCurrentThread(className).getSQLCount();
	}
	
	/**
	 * This should ONLY be called from the instrumented java.lang.Exception
	 * constructor.
	 * @param exception
	 */
	public static void notifyInConstructor(String className, Object exception) {
		if (enabled) {
			ExceptionPerThreadTracker.threadExceptionTracker.get().increment(className, exception);		
		}
	}
}
