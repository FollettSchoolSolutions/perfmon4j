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

import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.TextAppender;
import org.perfmon4j.java.management.JVMSnapShot;


/**
 * This example demonstrates how to programmatically define a timer and output the 
 * data to the serverlog.
 * 
 * Future examples will show more realistic methods for defining timers, including
 * via byte code injections and annotations.  
 */
public class Example1 extends Thread {
	private static final Logger logger = Logger.getLogger(Example1.class);
	private static final List<Example1> threads = new Vector<Example1>();
	private final Random rand = new Random();

	private Example1() {
		this.setDaemon(true);
		threads.add(this);
	}
	
	/**
	 * This is a brute force calculation designed to degrade as the number 
	 * of active threads are increased.   The intent is to mimic a production
	 * scenario where bottlenecks may appear in response to increasing load. 
	 */
	private int calcSlow(int value) {
		int result = 0;
		final int loadSimulator = Math.max(1, threads.size());
		while (true) {
			final int multiplier = result + 1;
			
			if (multiplier % (10000/loadSimulator) == 0) {
				// Slow down based on the number of threads... Simulate load...
				try {Thread.sleep(50);} catch  (InterruptedException ie) {}
			}
			if ((multiplier * multiplier) > value) {
				break;
			}
			result++;
		}
		
		return result;
	}
	
	private int calcSquareRoot(int value) {
		int result  = 0;
		
		// Start the timer...  Note the category name is completly arbitrary.
		PerfMonTimer timer = PerfMonTimer.start("calcSquareRoot");
		try {
			result = calcSlow(value);
		} finally {
			// Make sure timer start/stop's are paired in try-finally block
			PerfMonTimer.stop(timer);
		}
		
		return result;
	}
	
	private boolean keepRunning() {
		return threads.contains(this);
	}
	
	public void run() {
		while (keepRunning()) {
			int value = rand.nextInt(2000000);
			int sqrRoot = calcSquareRoot(value);
			logger.debug("SquareRoot of " + value + " is " + sqrRoot);
			try {Thread.sleep(50);} catch  (InterruptedException ie) {}
		}
	}
	
	static public void main(String args[]) throws Exception {
		// Configure log4j....   
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		
		System.out.println(System.getProperty("java.vm.version"));
		System.out.println(System.getProperty("java.vm.vendor"));
		System.out.println(System.getProperty("java.runtime.version"));
		
		
		PerfMonConfiguration config = new PerfMonConfiguration();
		final String MONITOR_NAME = "calcSquareRoot";
		final String APPENDER_NAME = "default";
		
		/**
		 * First indicate the monitor you would like to track.
		 * Monitor names are arbitrary strings.  
		 * For example "calcSquareRoot". Monitor names can also be grouped
		 * into a package hirearchy for instance "simpleexample.calcSquareRoot"
		*/
		config.defineMonitor(MONITOR_NAME); 
			
		// Define an appender to output monitor information.  This example uses the
		// TextAppender class which outputs monitor information to the standard output
		// log.  You also define the interval the monitor will be evaluated.
		config.defineAppender(APPENDER_NAME, TextAppender.class.getName() , "10 seconds");
		
		// Now attach your monitor to the appender..
		config.attachAppenderToMonitor(MONITOR_NAME, APPENDER_NAME);
		config.defineSnapShotMonitor("JVMSnapShot", JVMSnapShot.class.getName());
		config.attachAppenderToSnapShotMonitor("JVMSnapShot", APPENDER_NAME);
		
		// Initialize Perfmon4j with your configuration The perfered initializing method, using the 
		//XMLConfigurator will be displayed in subsequent examples.
		PerfMon.configure(config);
		
		// Ramp up (Add 1 thread per second).
		logger.info("Starting...");
		for (int i = 0; i < 50; i++) {
			new Example1().start();
			if ((i + 1) % 10 == 0) {
				logger.info("Ramping up... Now at " + threads.size() + " threads");
			}
			Thread.sleep(1000); // Sleep for 1 second
		}
		
		logger.info("Fully ramped up... Running at load for 10 seconds");
		Thread.sleep(10000);
		
		// Cool down (Remove 1 thread per second)
		for (int i = 0; i < 50; i++) {
			threads.remove(0);
			if ((i + 1) % 10 == 0) {
				logger.info("Cool down... Now at " + threads.size() + " threads");
			}
			Thread.sleep(1000); // Sleep for 1 second
		}
		
		logger.info("All threads have been told to stop... Waiting 25 seconds for threads to stop");
		Thread.sleep(25000);
		
		logger.info("Done");
	}
}
