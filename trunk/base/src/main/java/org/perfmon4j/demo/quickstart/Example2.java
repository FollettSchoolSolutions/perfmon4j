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

package org.perfmon4j.demo.quickstart;

import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.TextAppender;

/**
 * This example demonstrates how to put a simple timer in place.
 *
 */
public class Example2 implements Runnable {
	private static final Logger logger = Logger.getLogger(Example2.class);
	private final Random rand = new Random();
	private static boolean useSlowCalculation = true;
	
	
	private boolean isLargeNumber(int value) {
		// Remember... this is a toy example and not really valu
		return value > 999999;
	}
	
	private int calcSlow(int value) {
		int result = 0;
		/**
		 * For purpose of demo calculate square root using brute force.
		 */
		while (true) {
			int multiplier = result + 1;
			if (multiplier % 1000 == 0) {
				// Slow down a little more... Just to make timing interesting....
				try {Thread.sleep(100);} catch  (InterruptedException ie) {}
			}
			
			if ((multiplier * multiplier) > value) {
				break;
			}
			result++;
		}
		
		return result;
	}
	
	private int calcFast(int value) {
		return (int)Math.sqrt(value);
	}
	
	
	public int calcSquareRoot(int value) {
		int result  = 0;
		String category = "calcSquareRoot." + (isLargeNumber(value) ? "large" : "small");
		PerfMonTimer timer = PerfMonTimer.start(category);
		try {
			if (useSlowCalculation) {
				result = calcSlow(value);
			} else {
				result = calcFast(value);
			}
		} finally {
			PerfMonTimer.stop(timer);
		}
		
		return result;
	}
	
	public void run() {
		while (true) {
			int value = rand.nextInt(2000000);
			int sqrRoot = calcSquareRoot(value);
			logger.debug("SquareRoot of " + value + " is " + sqrRoot);
			try {Thread.sleep(100);} catch  (InterruptedException ie) {}
		}
	}
	
	
	static public void main(String args[]) throws Exception {
		// This example configures Perfmon4j programmatically
		// Using the XML configurator, which allows dynamic updates 
		// at runtime is described in lesson 2 and is the preferred approac
		
		// Configure log4j....   
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		
		
		PerfMonConfiguration config = new PerfMonConfiguration();
		final String MONITOR_NAME = "calcSquareRoot";
		final String APPENDER_NAME = "default";
		
		config.defineMonitor(MONITOR_NAME);
		config.defineAppender(APPENDER_NAME, TextAppender.class.getName() , "10 seconds");
		config.attachAppenderToMonitor(MONITOR_NAME, APPENDER_NAME, "./**" );
		PerfMon.configure(config);
		
		
		final int NUM_THREADS = 20;
		for (int i = 0; i < NUM_THREADS; i++) {
			Thread t = new Thread(new Example2());
			t.setDaemon(true);
			t.start();
		}
		
		logger.info("Running for 1 minute using slow calculations...");
		Thread.sleep(60000); // Sleep for 1 minute
		
		useSlowCalculation = false;
		logger.info("Switching to fast calculations.... Will run for 1 more minute");
		Thread.sleep(60000);
		
		logger.info("Done");
	}



}
