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

package org.perfmon4j.demo.sample;

import java.util.concurrent.CountDownLatch;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.InvalidConfigException;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.TextAppender;
import org.perfmon4j.instrument.DeclarePerfMonTimer;


class TimeTheTimerExample {
    private static final int LOOP_COUNT = 2000;
    private static final int NUM_SAMPLES = 500;
    private static final int NUM_THREADS = 4;
    
    private static abstract class TimeTheTimerRunnable implements Runnable {
        int getTimersPerRun() {
            return 1;
        }
    }
   
    static class NoTimer extends TimeTheTimerRunnable {
        int getTimersPerRun() {
            return 0;
        }
        
        public void run() {
        }
    }
    
    static class BasicTimer extends TimeTheTimerRunnable {
        public void run() {
            PerfMonTimer timer = PerfMonTimer.start("TimeTheTimer.basicTimer");
            PerfMonTimer.stop(timer);
        }
    }

    static class NestedBasicTimer extends TimeTheTimerRunnable {
        int getTimersPerRun() {
            return 4;
        }
        
        public void run() {
            PerfMonTimer timer = PerfMonTimer.start("TimeTheTimer.basicTimer");

            PerfMonTimer timerA0 = PerfMonTimer.start("TimeTheTimer.basicTimer.array0");
            PerfMonTimer.stop(timerA0);
            
            PerfMonTimer timerA1 = PerfMonTimer.start("TimeTheTimer.basicTimer.array1");
            PerfMonTimer.stop(timerA1);

            PerfMonTimer timerA2 = PerfMonTimer.start("TimeTheTimer.basicTimer.array2");
            PerfMonTimer.stop(timerA2);
            
            PerfMonTimer.stop(timer);
        }
    }

    static class AnnotationTimer extends TimeTheTimerRunnable {
        @DeclarePerfMonTimer("TimeTheTimer.annotationTimer")
        public void run() {
        }
    }
    
    public static class ThreadRunner extends Thread {
        private final Runnable runner;
        private final CountDownLatch latch;
        long duration = 0;
        
        ThreadRunner(Runnable runner, CountDownLatch latch) {
            this.runner = runner;
            this.latch = latch;
        }
        
        public void run() {
            latch.countDown();
            long nanoStart = System.nanoTime();
            for (int i = 0; i < LOOP_COUNT; i++) {
                runner.run();
            }
            duration = System.nanoTime() - nanoStart;
         }
    }

    public static long runSample(Runnable runner) throws InterruptedException {
        long totalDuration = 0;
        long samples = 0;
        
        for (int sampleCount = 0; sampleCount < NUM_SAMPLES; sampleCount++) {
            CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        
            ThreadRunner threads[] = new ThreadRunner[NUM_THREADS];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new ThreadRunner(runner, latch);
                threads[i].start();
            }
    
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
                long time = threads[i].duration;
                totalDuration += time;
                samples++;
            }
        }
        return totalDuration/samples;
    }
    
    private static String formatDuration(long durationNanos, int numTimersPerLoop) {
        long avgNanos = durationNanos/(LOOP_COUNT * numTimersPerLoop);
        double avgMillis = (double)avgNanos/1000000;
        long timingsPerMS = (1000000)/(avgNanos == 0 ? 1 : avgNanos);
        
        return String.format("%,d total, %,.5f milliseconds, %,d nanoseconds, %,d timings/milli-second", 
            new Long(durationNanos), new Double(avgMillis), new Long(avgNanos), new Long(timingsPerMS));
    }
    
    static void outputTimer(String timerDesc, TimeTheTimerRunnable runnable, long baseline) throws InvalidConfigException, InterruptedException {
        long duration;
        PerfMonConfiguration config;
        String monitorName;
        
        // First run with perfmon not configured...
        PerfMon.deInit();
        duration = runSample(runnable);
        System.out.println(timerDesc + "(PerfMon disabled) " + formatDuration(duration-baseline, runnable.getTimersPerRun()));
       
        // Initialize perfom but timer is not enabled...
        config = new PerfMonConfiguration();
        config.defineAppender("Basic", TextAppender.class.getName(), "1 minute");
        
        monitorName = TimeTheTimerExample.class.getName();
        config.defineMonitor(monitorName);
        config.attachAppenderToMonitor(monitorName, "Basic", PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS);
        PerfMon.configure(config);
        
        duration = runSample(runnable);
        System.out.println(timerDesc + "(Timer disabled) " + formatDuration(duration-baseline, runnable.getTimersPerRun()));
        
        // Now enable the timer...
        monitorName = "TimeTheTimer";
        config.defineMonitor(monitorName);
        config.attachAppenderToMonitor(monitorName, "Basic", PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS);
        PerfMon.configure(config);
        
        duration = runSample(runnable);
        System.out.println(timerDesc + "(Timer enabled) " + formatDuration(duration-baseline, runnable.getTimersPerRun()));
    }
    
    public static void main(String args[]) throws Exception {
//    	PerfMonTimer.NO_OP = false;
//    	PerfMonTimer.DO_IS_ACTIVE_SHORTCUT = true;
//    	PerfMonTimer.DO_IS_ACTIVE_AND_THREADTRACE_SHORTCUT = true;
    	
    	// To run this demo the following parameters must be set on the JVM
        // -javaagent:./dist/perfmon4j.jar="a=java.lang.String e=org.perfmon4j.demo"
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        

        // Run through each a timer class to ensure all classes
        // are loaded... This will ensure that the timings are not skewed by
        // 1 time cost, such as class loading...
        runSample(new NoTimer());
        runSample(new BasicTimer());
        runSample(new NestedBasicTimer());
        runSample(new AnnotationTimer());

        long baseline = runSample(new NoTimer());
        System.out.println(String.format(NUM_THREADS + " threads - noTimer %,d", new Long(baseline)));
        
        outputTimer("basicTimer", new BasicTimer(), baseline);
        outputTimer("nestedBasicTimer", new NestedBasicTimer(), baseline);
        outputTimer("annotationTimer", new AnnotationTimer(), baseline);
        
        System.out.println("Done");
    }
}
