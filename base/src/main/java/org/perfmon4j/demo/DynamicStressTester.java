/* 
 *	Copyright 2019 Follett Software Company  
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
 
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    	boolean inactive = true;
    	boolean additionalMonitoring = false;
    	boolean monitorEverything = false;
    	
        BasicConfigurator.configure(); 
        Logger.getRootLogger().setLevel(Level.INFO); 
        Logger.getLogger("org.perfmon4j").setLevel(Level.INFO); 
//        Logger logger = Logger.getLogger(DynamicStressTester.class); 
         
        if (!inactive) {
	        PerfMonConfiguration config = new PerfMonConfiguration(); 
	        config.defineAppender("FastAppender", TextAppender.class.getName(), "10 Seconds"); 
	        config.defineAppender("MediumAppender", TextAppender.class.getName(), "30 Seconds"); 
	        config.defineSnapShotMonitor("JVM Memory", JVMMemory.class.getName()); 
	        config.attachAppenderToSnapShotMonitor("JVM Memory", "MediumAppender"); 
	 
	        // Default Monitoring
	        config.defineMonitor("SimpleExample.outer.active"); 
	        config.attachAppenderToMonitor("SimpleExample.outer.active", "FastAppender", "."); 
	
	        // Additional
	        if (additionalMonitoring) {
		        config.defineMonitor("SimpleExample"); 
		        config.attachAppenderToMonitor("SimpleExample", "FastAppender", "/*"); 
	        }
	        
	        if (monitorEverything) {
	        	// If you turn on monitorEverything you will see messages like the one below
	        	// because the TextAppender will not be able to keep up and will throw away
	        	// logging events.  The textAppender is just not designed to keep up
	        	// with this kind of volume.  Example:
	        	//198652 [PerfMon.priorityTimer] WARN org.perfmon4j.Appender  - Unable to log PerfMonData(owner=PerfMon(monitorID=50084 name=SimpleExample.fixed.runner_25 parent=PerfMon(monitorID=50005 name=SimpleExample.fixed parent=PerfMon(monitorID=2 name=SimpleExample parent=PerfMon(monitorID=1 name=<ROOT> parent=null)))) timeStart=2019-03-23 00:15:28 timeStop=2019-03-23 00:15:41) to appender Appender(className=org.perfmon4j.TextAppender intervalMillis=10000)because event queue is full.
	        	
		        config.defineMonitor("SimpleExample"); 
		        config.attachAppenderToMonitor("SimpleExample", "FastAppender", "./**"); 
	        }
	         
	        PerfMon.configure(config);
        }
         
        final int oneSecond = 1000; 
         
 
        for (int i = 0; i < 50000; i++) { 
        	// fill up hashmap with monitors to simulate 
        	// a production system 
        	PerfMon.getMonitor("Monitor" + i); 
        } 
 
        NanoTimer dynamicTimer = new NanoTimer("Dynamic"); 
        NanoTimer staticTimer = new NanoTimer("Static"); 
        NanoTimer fixedTimer = new NanoTimer("Fixed"); 
         
        for (int i = 0; i < 100; i++) { 
        	sleep(1); 
        	RunnerImpl.startThread(RunnerImpl.MonitorType.DYNAMIC, i, dynamicTimer); 
        	RunnerImpl.startThread(RunnerImpl.MonitorType.STATIC, i, staticTimer); 
        	RunnerImpl.startThread(RunnerImpl.MonitorType.FIXED, i, fixedTimer); 
        } 
         
        AtomicBoolean stopper = new AtomicBoolean(false); 
         
        PerfMon.utilityTimer.schedule(new TestStarter(), 15 * oneSecond); 
        PerfMon.utilityTimer.schedule(new TestEnder(), 45 * oneSecond); 
        PerfMon.utilityTimer.schedule(new Finalizer(stopper), (45 * oneSecond) + (5 * oneSecond)); 
         
        while (!stopper.get()) { 
            PerfMonTimer timer = PerfMonTimer.start("SimpleExample.outer.active", true); 
            try { 
            	sleep(); 
            } finally { 
                PerfMonTimer.stop(timer); 
            } 
        } 
         
        System.out.println(fixedTimer); 
        System.out.println(staticTimer); 
        System.out.println(dynamicTimer); 
    } 
     
    public static class TestStarter extends TimerTask {  
		@Override 
		public void run() { 
			onRun(); 
			System.out.println("!*!*!*!*!*!*!*!*! " + getMessage() + " !*!*!*!*!*!*!*!*!"); 
			 
		} 
 
		protected String getMessage() { 
			return "START"; 
		} 
		 
		protected void onRun() { 
			NanoTimer.start(); 
		} 
    } 
 
    public static class TestEnder extends TestStarter {  
		protected String getMessage() { 
			return "END"; 
		} 
		 
		protected void onRun() { 
			NanoTimer.stop(); 
		} 
    } 
     
     
    public static class Finalizer extends TimerTask {  
    	private final AtomicBoolean stopper; 
    	 
    	Finalizer(AtomicBoolean stopper) { 
    		this.stopper = stopper; 
    	} 
 
		@Override 
		public void run() { 
			stopper.set(true); 
		} 
    } 
     
     
    private static class RunnerImpl implements Runnable { 
    	private static final int THREADS_PER_MONITOR = 20;
    	static enum MonitorType {
    		FIXED,
    		STATIC,
    		DYNAMIC
    	}
    	
    	private final String monitorName; 
    	private final boolean dynamic; 
    	private final PerfMon monitor; 
    	private final NanoTimer nanoTimer; 
    	 
    	static void startThread(MonitorType monitorType, int threadNum, NanoTimer nanoTimer) { 
    		String threadName = monitorType.name() + ".runner_" + threadNum;  
    		String monitorName =  "SimpleExample." + monitorType.name() + ".runner_" + (threadNum / THREADS_PER_MONITOR); 
    		Thread t = null;
    		if (!monitorType.equals(MonitorType.FIXED)) {
    			t = new Thread(null, new RunnerImpl(monitorName, monitorType.equals(MonitorType.DYNAMIC), nanoTimer), threadName); 
    		} else {
    			t = new Thread(null, new RunnerImpl(PerfMon.getMonitor(monitorName), nanoTimer), threadName); 
    		}
            t.setDaemon(true); 
            t.start(); 
    	} 
 
    	 
    	RunnerImpl(String monitorName, boolean dynamic, NanoTimer nanoTimer) { 
    		this.monitorName = monitorName; 
    		this.dynamic = dynamic; 
    		this.monitor = null; 
    		this.nanoTimer = nanoTimer; 
    	} 
 
    	RunnerImpl(PerfMon monitor, NanoTimer nanoTimer) { 
    		this.monitorName = null; 
    		this.dynamic = false; 
    		this.monitor = monitor; 
    		this.nanoTimer = nanoTimer; 
    	} 
    	 
    	 
        public void run() { 
            while (true) { 
                PerfMonTimer timer = null; 
                 
                long nano = 0l; 
                if (monitor != null) { 
                    nano = System.nanoTime(); 
                	timer = PerfMonTimer.start(monitor); 
                    nanoTimer.recordStart(System.nanoTime() - nano); 
                } else { 
                    nano = System.nanoTime(); 
                	timer = PerfMonTimer.start(monitorName, dynamic); 
                    nanoTimer.recordStart(System.nanoTime() - nano); 
                } 
                try { 
                    sleep(); 
                }finally { 
                    nano = System.nanoTime(); 
                    PerfMonTimer.stop(timer); 
                    nanoTimer.recordStop(System.nanoTime() - nano); 
                } 
            } 
        } 
    } 
     
     
    private static class NanoTimer { 
    	static private final AtomicBoolean active = new AtomicBoolean(false); 
    	private final AtomicLong startTotal = new AtomicLong(0); 
    	private final AtomicInteger numStarts = new AtomicInteger(0); 
    	private final AtomicLong stopTotal = new AtomicLong(0); 
    	private final AtomicInteger numStops = new AtomicInteger(0); 
    	private final String name; 
    	 
    	NanoTimer(String name) { 
    		this.name = name; 
    	} 
    	 
    	public void recordStart(long nanos) { 
    		if (active.get()) { 
	    		startTotal.addAndGet(nanos); 
	    		numStarts.incrementAndGet(); 
    		} 
    	} 
    	 
    	public void recordStop(long nanos) { 
    		if (active.get()) { 
	    		stopTotal.addAndGet(nanos); 
	    		numStops.incrementAndGet(); 
    		} 
    	} 
    	 
    	public static void start() { 
    		active.set(true); 
    	} 
 
    	public static void stop() { 
    		active.set(false); 
    	} 
    	 
    	@Override 
    	public String toString() { 
    		float nanosInMillis = 1000000f; 
    		 
    		return name + " start(" + ((startTotal.longValue()/numStarts.intValue())/nanosInMillis) + " millis) stop(" 
    				+ ((stopTotal.longValue()/numStops.intValue())/nanosInMillis) + " millis)"; 
    	} 
    } 
} 
