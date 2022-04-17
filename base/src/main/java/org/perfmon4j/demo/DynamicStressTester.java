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
 
import java.util.ArrayList;
import java.util.List;
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
import org.perfmon4j.util.MedianCalculator;
 
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

    private static enum RunMode {
    	INACTIVE("Perfmon4j timers will be started/stopped, but perfmon4j" + System.lineSeparator()
    			+ "will not be collecting any information. Perfmon4j in a" + System.lineSeparator()
    			+ "disabled state."),
    	WEB_REQUEST_ONLY("Perfmon4j will only be collecting data in the" + System.lineSeparator()
    			+ "main application thread. This would be close to the" + System.lineSeparator()
    			+ "overhead seen when using servlet valve/web request monitoring"),
    	ADDITIONAL_MONITORING("This model adds additional monitors per thread." + System.lineSeparator()
    			+ "This more closely approximates WebRequest + typical code monitoring"),
    	MONITOR_EVERYTHING("This model creates a lot of individual monitor categories and" + System.lineSeparator()
    			+ "tracks each of them.  This is probably something in excess of what would" + System.lineSeparator()
    			+ "typically be seen in production use."); 
    	
    	final String description;
    	
    	RunMode(String description) {
    		this.description = description;
    	}

		public String getDescription() {
			return description;
		}
    }
    
    
    public static void main(String[] args) throws Exception {
    	final int rampupSeconds = 15;  // 15
    	final int runSeconds = 30;		// 30
    	final int shutdownSeconds = 5;	// 5
    	final int numThreads = 500;  // 100
    	final boolean useMedian = true;
    	
        BasicConfigurator.configure(); 
        Logger.getRootLogger().setLevel(Level.INFO); 
        Logger.getLogger("org.perfmon4j").setLevel(Level.INFO); 
        
        for (int i = 0; i < 50000; i++) { 
        	// fill up hashmap with monitors to simulate 
        	// a production system 
        	PerfMon.getMonitor("Monitor" + i); 
        } 

        List<RunResultVO> results = new ArrayList<RunResultVO>();
       
        for (RunMode runMode : RunMode.values()) {
//        	RunMode runMode = RunMode.MONITOR_EVERYTHING;
        
        	results.add(doRun(runMode, rampupSeconds, runSeconds, shutdownSeconds, numThreads, useMedian));
        }
        
        System.out.println("**************"); 
    	System.out.println("numThreads=" + numThreads);
    	System.out.println("rampupSeconds=" + rampupSeconds);
    	System.out.println("runSeconds=" + runSeconds);
    	System.out.println("shutdownSeconds=" + shutdownSeconds);
    	System.out.println("Average Calculation=" + (useMedian ? "Median" : "Mean"));
    	
    	for (RunResultVO vo : results) {
    		System.out.println("***" + vo.getRunMode());
    		System.out.println("***" + vo.getRunMode().getDescription());
    		System.out.println("***");
    		System.out.println(vo);
    	}
    } 
    

    public static RunResultVO doRun(RunMode runMode, int rampupSeconds, int runSeconds, int shutdownSeconds, int numThreads, boolean useMedian) throws Exception {
        if (!RunMode.INACTIVE.equals(runMode)) {
	        PerfMonConfiguration config = new PerfMonConfiguration(); 
	        config.defineAppender("FastAppender", TextAppender.class.getName(), "10 Seconds"); 
	        config.defineAppender("MediumAppender", TextAppender.class.getName(), "30 Seconds"); 
	        config.defineSnapShotMonitor("JVM Memory", JVMMemory.class.getName()); 
	        config.attachAppenderToSnapShotMonitor("JVM Memory", "MediumAppender"); 
	 
        	config.defineMonitor("WebRequest"); 
        	config.attachAppenderToMonitor("WebRequest", "FastAppender", ".");
	        	
	        if (RunMode.ADDITIONAL_MONITORING.equals(runMode)) {
		        config.defineMonitor("SimpleExample"); 
		        config.attachAppenderToMonitor("SimpleExample", "FastAppender", "/*"); 
	        } else if (RunMode.MONITOR_EVERYTHING.equals(runMode)) {
		        config.defineMonitor("SimpleExample"); 
		        config.attachAppenderToMonitor("SimpleExample", "FastAppender", "./**"); 
	        }
	        PerfMon.configure(config);
        } else {
        	PerfMon.deInit();
        }
        AtomicBoolean stopper = new AtomicBoolean(false); 
        NanoTimer dynamicTimer = null; 
        NanoTimer staticTimer = null; 
        NanoTimer fixedTimer = null; 
        
        if (useMedian) {
	        dynamicTimer = new MedianNanoTimer("Dynamic"); 
	        staticTimer = new MedianNanoTimer("Static"); 
	        fixedTimer = new MedianNanoTimer("Fixed");
        } else {
	        dynamicTimer = new MeanNanoTimer("Dynamic"); 
	        staticTimer = new MeanNanoTimer("Static"); 
	        fixedTimer = new MeanNanoTimer("Fixed");
        }
         
        for (int i = 0; i < numThreads; i++) { 
        	sleep(1); 
        	RunnerImpl.startThread(stopper, RunnerImpl.MonitorType.DYNAMIC, i, dynamicTimer); 
        	RunnerImpl.startThread(stopper, RunnerImpl.MonitorType.STATIC, i, staticTimer); 
        	RunnerImpl.startThread(stopper, RunnerImpl.MonitorType.FIXED, i, fixedTimer); 
        } 
         
        final int oneSecond = 1000; 
        PerfMon.utilityTimer.schedule(new TestStarter(), rampupSeconds * oneSecond); 
        PerfMon.utilityTimer.schedule(new TestEnder(), (rampupSeconds + runSeconds) * oneSecond); 
        PerfMon.utilityTimer.schedule(new Finalizer(stopper), (rampupSeconds + runSeconds + shutdownSeconds) * oneSecond); 
         
        while (!stopper.get()) { 
            PerfMonTimer timer = PerfMonTimer.start("WebRequest.something_do", true); 
            try { 
            	sleep(); 
            } finally { 
                PerfMonTimer.stop(timer); 
            } 
        } 

        return new RunResultVO(runMode, fixedTimer, staticTimer, dynamicTimer);
    } 
    
    
    public static class RunResultVO {
    	private final RunMode runMode;
    	private final NanoTimer fixedTimer;
    	private final NanoTimer staticTimer;
    	private final NanoTimer dynamicTimer;
    	
		public RunResultVO(RunMode runMode, NanoTimer fixedTimer, NanoTimer staticTimer, NanoTimer dynamicTimer) {
			super();
			this.runMode = runMode;
			this.fixedTimer = fixedTimer;
			this.staticTimer = staticTimer;
			this.dynamicTimer = dynamicTimer;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			
			result.append(fixedTimer).append(System.lineSeparator());
			result.append(staticTimer).append(System.lineSeparator());
			result.append(dynamicTimer).append(System.lineSeparator());
			
			return result.toString();
		}

		public RunMode getRunMode() {
			return runMode;
		}
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
    	private static final int MAX_MONITOR_GROUPS = 5;
    	static enum MonitorType {
    		FIXED,
    		STATIC,
    		DYNAMIC
    	}
    	
    	private final String monitorName; 
    	private final boolean dynamic; 
    	private final PerfMon monitor; 
    	private final NanoTimer nanoTimer; 
    	private final AtomicBoolean stopper;
    	 
    	static void startThread(AtomicBoolean stopper, MonitorType monitorType, int threadNum, NanoTimer nanoTimer) {
    		String threadName = monitorType.name() + ".runner_" + threadNum;  
    		String monitorName =  "SimpleExample." + monitorType.name() + ".runner_" + (threadNum % MAX_MONITOR_GROUPS); 
    		Thread t = null;
    		if (!monitorType.equals(MonitorType.FIXED)) {
    			t = new Thread(null, new RunnerImpl(stopper, monitorName, monitorType.equals(MonitorType.DYNAMIC), nanoTimer), threadName); 
    		} else {
    			t = new Thread(null, new RunnerImpl(stopper, PerfMon.getMonitor(monitorName), nanoTimer), threadName); 
    		}
            t.setDaemon(true); 
            t.start(); 
    	} 
 
    	 
    	RunnerImpl(AtomicBoolean stopper, String monitorName, boolean dynamic, NanoTimer nanoTimer) { 
    		this.monitorName = monitorName; 
    		this.dynamic = dynamic; 
    		this.monitor = null; 
    		this.nanoTimer = nanoTimer;
    		this.stopper = stopper;
    	} 
 
    	RunnerImpl(AtomicBoolean stopper, PerfMon monitor, NanoTimer nanoTimer) { 
    		this.monitorName = null; 
    		this.dynamic = false; 
    		this.monitor = monitor; 
    		this.nanoTimer = nanoTimer;
    		this.stopper = stopper;
    	} 
    	 
        public void run() { 
            while (!stopper.get()) { 
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

    private static abstract class NanoTimer {
    	static protected final AtomicBoolean active = new AtomicBoolean(false); 
    	protected final String name;

    	NanoTimer(String name) { 
    		this.name = name; 
    	} 
    	
    	public abstract void recordStart(long nanos);
    	 
    	public abstract void recordStop(long nanos); 
    	 
    	public static void start() { 
    		active.set(true); 
    	} 
 
    	public static void stop() { 
    		active.set(false); 
    	} 
    }

    
     
    private static class MeanNanoTimer extends NanoTimer { 
    	private final AtomicLong startTotal = new AtomicLong(0); 
    	private final AtomicInteger numStarts = new AtomicInteger(0); 
    	private final AtomicLong stopTotal = new AtomicLong(0); 
    	private final AtomicInteger numStops = new AtomicInteger(0); 
    	 
    	MeanNanoTimer(String name) { 
    		super(name);
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
    	 
    	@Override 
    	public String toString() { 
    		float nanosInMilli = 1000000f; 
    		
    		long startAvgNanos = startTotal.longValue()/numStarts.longValue();
    		long stopAvgNanos = stopTotal.longValue()/numStops.longValue();
    		
    		return name + " start(" + startAvgNanos * 1000 / nanosInMilli + " millis) stop(" 
				+ stopAvgNanos * 1000 / nanosInMilli + " millis) (Per 1000 operations)"; 
    	} 
    } 

    private static class MedianNanoTimer extends NanoTimer { 
    	private final MedianCalculator startMedian = new MedianCalculator(100000, 10);
    	private final MedianCalculator stopMedian = new MedianCalculator(100000, 10);
    	 
    	MedianNanoTimer(String name) {
    		super(name);
    	} 
    	 
    	public void recordStart(long nanos) { 
    		if (active.get()) { 
    			startMedian.putValue(nanos);
    		} 
    	} 
    	 
    	public void recordStop(long nanos) { 
    		if (active.get()) { 
    			stopMedian.putValue(nanos);
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
    		float nanosInMilli = 1000000f; 
    		
    		return name + " start(" + startMedian.getMedian().getResult().doubleValue() * 1000 / nanosInMilli + " millis) stop(" 
			+ stopMedian.getMedian().getResult().doubleValue() * 1000 / nanosInMilli + " millis) (per 1000 operations)"; 
    		
//    		return name + " start(" + (startMedian.getMedian().getResult().doubleValue()/(nanosInMillis/1000)) + " millis) stop(" 
//    				+ (stopMedian.getMedian().getResult().doubleValue()/(nanosInMillis/1000)) + " millis) - (per 1000 operations)"; 
    	} 
    } 


} 
