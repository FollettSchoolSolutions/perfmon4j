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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.TextAppender;

public class DynamicStressTester {
    
    private DynamicStressTester() {
    }

    private static void sleep() {
    	sleep(-1);
    }
    
    private static void sleep(int millis) {
        try {
        	millis = (millis > 0) ? millis : (10);
            Thread.sleep(millis);
        } catch (InterruptedException ie) { 
            // Nothing todo
        }
    }
    
    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("org.perfmon4j").setLevel(Level.INFO);
//        Logger logger = Logger.getLogger(DynamicStressTester.class);
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("SimpleExample.outer.active");
        config.defineAppender("10 Second Monitor", TextAppender.class.getName(), "10 Seconds");
        config.attachAppenderToMonitor("SimpleExample.outer.active", "10 Second Monitor", ".");

        config.defineAppender("60 Second Monitor", TextAppender.class.getName(), "60 Seconds");
        config.defineSnapShotMonitor("JVM Memory", JVMMemory.class.getName());
        config.attachAppenderToSnapShotMonitor("JVM Memory", "60 Second Monitor");
        
        PerfMon.configure(config);

        for (int i = 0; i < 50000; i++) {
        	// fill up hashmap with monitors to simulate
        	// a production system
        	PerfMon.getMonitor("Monitor" + i);
        }
        
        for (int i = 0; i < 100; i++) {
        	sleep(1);
        	RunnerImpl.startThread("dynamic", true, i);
        	RunnerImpl.startThread("static", false, i);
        	RunnerImpl.startThread(PerfMon.getMonitor("SimpleExample.fixed.runner_" + i));
        }
        
        AtomicBoolean stopper = new AtomicBoolean(false);
        Timer timerThread = new Timer(true);
        
        timerThread.schedule(new Announcement("START"), 1000 * 60);
        timerThread.schedule(new Announcement("END"), 1000 * 60 * 2);
        timerThread.schedule(new Stopper(stopper), (1000 * 60 * 2) + 10000);
        
        while (!stopper.get()) {
            PerfMonTimer timer = PerfMonTimer.start("SimpleExample.outer.active", true);
            try {
            	sleep();
            } finally {
                PerfMonTimer.stop(timer);
            }
        }
    }
    
    public static class Announcement extends TimerTask { 
    	private final String message;
    	
    	Announcement(String message) {
    		this.message = message;
    	}

		@Override
		public void run() {
			System.out.println("!*!*!*!*!*!*!*!*! " + message + " !*!*!*!*!*!*!*!*!");
		}
    }

    public static class Stopper extends TimerTask { 
    	private final AtomicBoolean stopper;
    	
    	Stopper(AtomicBoolean stopper) {
    		this.stopper = stopper;
    	}

		@Override
		public void run() {
			stopper.set(true);
		}
    }
    
    
    private static class RunnerImpl implements Runnable {
    	private final String monitorName;
    	private final boolean dynamic;
    	private final PerfMon monitor;
    	
    	static void startThread(String threadType, boolean dynamic, int threadNum) {
    		String threadName = threadType + ".runner_" + threadNum; 
    		String monitorName =  "SimpleExample." + threadName;
            new Thread(null, new RunnerImpl(monitorName, dynamic), threadName).start();
    	}

    	static void startThread(PerfMon mon) {
    		String threadName = mon.getName().replaceFirst("SimpleExample\\.", ""); 
            new Thread(null, new RunnerImpl(mon), threadName).start();
    	}
    	
    	RunnerImpl(String monitorName, boolean dynamic) {
    		this.monitorName = monitorName;
    		this.dynamic = dynamic;
    		this.monitor = null;
    	}

    	RunnerImpl(PerfMon monitor) {
    		this.monitorName = null;
    		this.dynamic = false;
    		this.monitor = monitor;
    	}
    	
    	
        public void run() {
            while (true) {
                PerfMonTimer timer = null;
                if (monitor != null) {
                	timer = PerfMonTimer.start(monitor);
                } else {
                	timer = PerfMonTimer.start(monitorName, dynamic);
                }
                try {
                    sleep();
                }finally {
                    PerfMonTimer.stop(timer);
                }
            }
        }
    }
    
}
