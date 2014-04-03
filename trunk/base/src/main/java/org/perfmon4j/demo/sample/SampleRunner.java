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
*/

package org.perfmon4j.demo.sample;

import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


public abstract class SampleRunner implements Runnable {
    protected Logger logger = null;
    protected final Random random = new Random();
    protected int values[];
    
    private final int minSleepMillis;
    private final int maxSleepMillis;
    private boolean stop = false;
    
    protected SampleRunner() {
        this(50, 250);
    }
    
    protected SampleRunner(int minSleepMillis, int maxSleepMillis) {
        values = new int[10000];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt();
        }
        this.minSleepMillis = minSleepMillis;
        this.maxSleepMillis = maxSleepMillis;
    }

    public void run() {
        logger = LoggerFactory.initLogger(this.getClass());
        final String threadName = Thread.currentThread().getName();
        logger.logInfo("Starting thread " + threadName);
        while (!stop) {
            if (minSleepMillis > 0) {
                try {
                    Thread.sleep(random.nextInt(maxSleepMillis - minSleepMillis) + minSleepMillis);
                } catch (InterruptedException ex) {
                    // Ignored
                }
            }
            sampleMethod();
        }
        logger.logInfo("Stopping thread " + threadName);
    }
    
    
    public final void stop() {
        stop = true;
    }

    protected  int[] cloneArray() {
        int result[] = new int[values.length];
        System.arraycopy(values, 0, result, 0, values.length);
        return result;
    }

    public abstract void sampleMethod();
    
    public static void launchSamplers(Class clazz) throws RuntimeException {
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
        org.apache.log4j.Logger.getLogger("org.perfmon4j").setLevel(Level.INFO);
        
        int numThreads = 5;
        int secondsToRun = 300; // Run for 5 minutes by default
        
        try {
            for (int i = 0; i < numThreads; i++) {
                Thread thread = new Thread((Runnable)clazz.newInstance(), "SamplerThread - " + (i+1));
                thread.setDaemon(true);
                thread.start();
            } 
        } catch (Exception ex) {
            throw new RuntimeException("Unable to intialize threads", ex);
        }
        
        try {
            Thread.sleep(secondsToRun * 1000);
        } catch (InterruptedException ex) {
        }
    }
}