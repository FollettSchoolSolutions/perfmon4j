/*
 *	Copyright 2008-2011 Follett Software Company 
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

package org.perfmon4j.demo;

import java.util.Properties;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.influxdb.InfluxAppender;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.java.management.JVMSnapShot;

/**
 * This is an example of a very simple SnapShotMonitor...  
 * It collects two attributes, the totalMemory and freeMemory
 * based on the JVM.
 * @author dave
 *
 */
@SnapShotProvider(type = SnapShotProvider.Type.STATIC)
public class JVMMemory {

	
	@SnapShotString
	public static String getUserName() {
		return System.getProperty("user.name");
	}
	
	@SnapShotGauge
	public static long getFreeMemory() {
		return Runtime.getRuntime().freeMemory();
	}
	
	@SnapShotGauge
	public static long getTotalMemory() {
		return Runtime.getRuntime().totalMemory();
	}
	
	private static class Runner implements Runnable {
		private final Random r = new Random();
		public void run() {
			while (true) {
				PerfMonTimer timer = PerfMonTimer.start("RandomRunner");
				try {
					Thread.sleep(r.nextInt(10) * 50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
				} finally {
					PerfMonTimer.stop(timer);
				}
				try {
					Thread.sleep(r.nextInt(10) * 100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
				}
			}
		}
		
	}
	
    
    public static void main(String args[]) throws Exception {
//    	System.setProperty("PERFMON_APPENDER_ASYNC_TIMER_MILLIS", "500");
    	
    	BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("org.perfmon4j").setLevel(Level.DEBUG);
    	
        PerfMonConfiguration config = new PerfMonConfiguration();
        Properties props = new Properties();
        props.setProperty("baseURL", "http://192.168.56.1:9099");
        props.setProperty("database", "perfmon4j");
        props.setProperty("groups", "\"my group\" other\\group=test");
//        props.setProperty("batchSeconds", "30");
        
        
//        props.setProperty("maxMeasurementsPerBatch", "2");
        
        config.defineAppender("SimpleAppender", InfluxAppender.class.getName(), "1 second", props);
        config.defineMonitor("RandomRunner");
        
        config.attachAppenderToMonitor("RandomRunner", "SimpleAppender");
        
        
        config.defineSnapShotMonitor("Daves=Cool Test", JVMMemory.class.getName());
        config.attachAppenderToSnapShotMonitor("Daves=Cool Test", "SimpleAppender");

        config.defineSnapShotMonitor("NewJVM", JVMSnapShot.class.getName());
        config.attachAppenderToSnapShotMonitor("NewJVM", "SimpleAppender");
        
        
        PerfMon.configure(config);
        System.out.println("Sleeping for 60 seconds -- Will take a JVM SnapShot every 1 second");
        
        for (int i = 0; i < 10; i++) {
        	Thread t = new Thread(new Runner());
        	t.setDaemon(true);
        	t.start();	
        }
        
        Thread.sleep(600000);
    }
}
