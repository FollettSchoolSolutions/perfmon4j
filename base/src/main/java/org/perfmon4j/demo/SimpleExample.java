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
package org.perfmon4j.demo;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.MonitorThreadTracker;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.TextAppender;

public class SimpleExample {
    
    private SimpleExample() {
    }
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("org.perfmon4j").setLevel(Level.DEBUG);
        Logger logger = Logger.getLogger(SimpleExample.class);
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("SimpleExample");
        config.defineAppender("10 Second Monitor", TextAppender.class.getName(), "10 Seconds");
        config.attachAppenderToMonitor("SimpleExample", "10 Second Monitor", "/*");
//        config.defineSnapShotMonitor("JVM Memory", JVMMemory.class.getName());
//        config.attachAppenderToSnapShotMonitor("JVM Memory", "10 Second Monitor");
        
      config.defineSnapShotMonitor("MonitorThreadTracker", MonitorThreadTracker.class.getName());
      config.attachAppenderToSnapShotMonitor("MonitorThreadTracker", "10 Second Monitor");

        
        PerfMon.configure(config);
       
        for (int i = 0; i < 10; i++) {
            new Thread(new RunnerImpl()).start();
        }
        
        while (true) {
            PerfMonTimer timer = PerfMonTimer.start("SimpleExample.outer");
            try {
                logger.info(".");
                Thread.sleep(1000);
            } finally {
                PerfMonTimer.stop(timer);
            }
        }
    }
    
   
    
    private static class RunnerImpl implements Runnable {
    	private static final AtomicLong counter = new AtomicLong(0);
        private void sleep() {
            try {
            	// Make every 100th thread take longer
            	long outlierThreadMultipler = (counter.incrementAndGet() % 100 == 0) ? 10L : 1L;
                Thread.sleep((long)(Math.random() * 10000 * outlierThreadMultipler) );
            } catch (InterruptedException ie) { 
                // Nothing todo
            }
        }
        
        public void run() {
            while (true) {
                sleep();
                PerfMonTimer timer = PerfMonTimer.start("SimpleExample.inner");
                try {
                    sleep();
                }finally {
                    PerfMonTimer.stop(timer);
                }
            }
        }
    }
    
}
