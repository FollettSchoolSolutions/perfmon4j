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
package org.perfmon4j;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

import org.perfmon4j.util.BeanHelper;
import org.perfmon4j.util.FailSafeTimerTask;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.ThresholdCalculator;

public abstract class Appender {
    // Do not use Log4j here... Class may not be available
    private static final Logger logger = LoggerFactory.initLogger(Appender.class);
    
    public static final String PERFMON_APPENDER_QUEUE_SIZE = "PERFMON_APPENDER_QUEUE_SIZE";
    public static final String PERFMON_APPENDER_ASYNC_TIMER_MILLIS = "PERFMON_APPENDER_ASYNC_TIMER_MILLIS";
    public static final int DEFAULT_APPENDER_QUEUE_SIZE = 500;
    public static final int DEFUALT_APPENDER_ASYNC_TIMER_MILLIS = 5000;
    
    public static final long DEFAULT_INTERVAL_MILLIS = 5 * 60 * 1000; // 5 Minutes...
    private static final Object APPENDER_SINGLETON_TOKEN = new Object();
    private static final Map<AppenderID, Appender> appenderSingletonMap = new HashMap<AppenderID, Appender>();
   
    private final List<PerfMonData> eventQueue;
    private final int maxQueueSize;
    private final AsyncAppenderTimerTask timerTask;
    private final Object eventQueueLockToken = new Object();
    protected MedianCalculator medianCalculator = null;
    protected ThresholdCalculator thresholdCalculator = null;
    
    
    private final long intervalMillis;
    
    /**
     * Do not USE....  Use Appender.getOrCreateAppender(AppenderID id)
     * to ensure that we have singleton objects...
     */
    protected Appender(AppenderID id) {
        this.intervalMillis = id.intervalMillis;
        maxQueueSize = Integer.getInteger(PERFMON_APPENDER_QUEUE_SIZE, DEFAULT_APPENDER_QUEUE_SIZE).intValue();
        long timerMillis = Long.getLong(PERFMON_APPENDER_ASYNC_TIMER_MILLIS, DEFUALT_APPENDER_ASYNC_TIMER_MILLIS).longValue();
        if (maxQueueSize > 0 && timerMillis > 0) {
            eventQueue =  new ArrayList<PerfMonData>(maxQueueSize);
            timerTask = new AsyncAppenderTimerTask();
            PerfMon.getUtilityTimer().schedule(timerTask, timerMillis, timerMillis);
        } else {
            eventQueue = null;
            timerTask = null;
        }
    }
    
    public void finalize() {
        logger.logDebug("finalize done on appender - className: " +
            this.getClass().getName() + " interval " + intervalMillis);
    }
    
    public static AppenderID getAppenderID(String className) {
        return new AppenderID(className, DEFAULT_INTERVAL_MILLIS);
    }

    public static AppenderID getAppenderID(String className, long intervalMillis) {
        return new AppenderID(className, intervalMillis);
    }

    public static AppenderID getAppenderID(String className, long intervalMillis, Properties attributes) {
        return new AppenderID(className, intervalMillis, attributes);
    }
    
    public AppenderID getMyAppenderID() {
        return new AppenderID(this.getClass().getName(), intervalMillis);
    }
    
    public void deInit() {
        synchronized (APPENDER_SINGLETON_TOKEN) {
            appenderSingletonMap.remove(getMyAppenderID());
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
    }

    public static Appender getAppender(AppenderID id) {
        Appender result = null;
        synchronized (APPENDER_SINGLETON_TOKEN) {
            result = appenderSingletonMap.get(id);
        }
        return result;
    }

    public static AppenderID[] getAllAppenders() { 
        synchronized (APPENDER_SINGLETON_TOKEN) {
            return appenderSingletonMap.keySet().toArray(new Appender.AppenderID[]{});    
        }
    }
    
    public static Appender getOrCreateAppender(AppenderID id) throws InvalidConfigException {
        Appender result = null;
        if (id == null) {
            throw new InvalidConfigException("Null appenderID passed to Appender.getOrCreateAppender");
        }
        
        synchronized (APPENDER_SINGLETON_TOKEN) {
            try {
                result = appenderSingletonMap.get(id);
                
                if (result == null) {
                    Class clazz = null;
                    if (PerfMon.getClassLoader() == null){
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(id.getClassName());
                    } else {
                        clazz = PerfMon.getClassLoader().loadClass(id.getClassName());
                    }
                    Constructor constructor = clazz.getConstructor(new Class[] {AppenderID.class});
                    result = (Appender)constructor.newInstance(new Object[]{id});
                    
                    // Map any attributes into the appender...
                    Map attributes = id.getAttributes();
                    if (attributes != null) {
                        Iterator<Map.Entry> itr = attributes.entrySet().iterator();
                        while (itr.hasNext()) {
                            Map.Entry entry = itr.next();
                            try {
                                BeanHelper.setValue(result, (String)entry.getKey(), (String)entry.getValue());
                            } catch (BeanHelper.UnableToSetAttributeException ex) {
                                logger.logError(ex.getMessage(), ex);
                            }
                        }
                    }
                    
                    appenderSingletonMap.put(id, result);
                }
            } catch (Exception ex) {
                logger.logDebug("Unable to instatiate class: " + id.getClassName(), ex);
                throw new InvalidConfigException("Unable to instatiate class: " + id.getClassName());
            }
        }
        return result;
    }
    
    public abstract void outputData(PerfMonData data);
    
    /**
     * Package level.
     * This is how each monitor pushes their interval data on to the recorder...
     */
    final void appendData(PerfMonData data) {
        if (logger.isDebugEnabled()) {
            String dest = "";
            if (eventQueue != null) {
                dest = "to eventQueue ";
            }
            logger.logDebug("appendData " + dest + data);
        }
        if (eventQueue != null) {
            synchronized (eventQueueLockToken) {
                if (eventQueue.size() < maxQueueSize) {
                    eventQueue.add(data);
                } else {
                    logger.logWarn("Unable to log " + data + " to appender " + 
                        this + "because event queue is full.");
                }
            }
        } else {
            outputData(data);
        }
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }
    
    
    public void flush() {
        List<PerfMonData> outputList = null;
        synchronized (eventQueueLockToken) {
            if (!eventQueue.isEmpty()) {
                outputList = new ArrayList<PerfMonData>(eventQueue.size());
                outputList.addAll(eventQueue);
                eventQueue.clear();
            }
        }
        if (outputList != null) {
            Iterator<PerfMonData> itr = outputList.iterator();
            while (itr.hasNext()) {
                try {
                    outputData(itr.next());
                } catch (Exception ex) {
                    logger.logError("Unable to output data to appender", ex);
                }
            }
        }
    }
    
    /**
     * Simply returns true if the Appender is of the same
     * class as this appender...  We could make this more 
     * also take into account duration if we want to allow
     * multiple instances of the same appender on a class...
     */
    public boolean matchesAppenderType(Appender appender) {
        return appender != null && this.getClass().equals(appender.getClass());
    }
    
    public String toString() {
        return "Appender(className=" + this.getClass().getName() +
                " intervalMillis=" + this.intervalMillis +
                ")";
    }
    
    public IntervalData newIntervalData(PerfMon owner, long timeStart) {
        return new IntervalData(owner, timeStart, 
            medianCalculator != null ? medianCalculator.clone() : null,
            thresholdCalculator != null ? thresholdCalculator.clone() : null);
    }
    
    public final static class AppenderID extends ObjectID {
        private final long intervalMillis;
        
        protected AppenderID(String className) {
            this(className, DEFAULT_INTERVAL_MILLIS);
        }

        protected AppenderID(String className, long intervalMillis) {
            this(className, intervalMillis, null);
        }
        
        protected AppenderID(String className, long intervalMillis, Properties attributes) {
            super(className, attributes);
            this.intervalMillis = intervalMillis;
        }
        
        protected String buildAttributeString() {
            return super.buildAttributeString() + "-Interval=" + intervalMillis;
        }
        
        public long getIntervalMillis() {
            return intervalMillis;
        }
    }
   
    
    private class AsyncAppenderTimerTask extends FailSafeTimerTask {
        public void failSafeRun() {
            flush();
        }
    }

    public void setMedianCalculator(MedianCalculator medianCalculator) {
        this.medianCalculator = medianCalculator;
    }

    public MedianCalculator getMedianCalculator() {
        return medianCalculator;
    }
    
    public void setThresholdCalculator(ThresholdCalculator thresholdCalculator) {
        this.thresholdCalculator = thresholdCalculator;
    }

    public ThresholdCalculator getThresholdCalculator() {
        return thresholdCalculator;
    }
    
    public static void purgeUnusedAppenders(PerfMonConfiguration config) {
        AppenderID allAppendersInConfig[] = config.getAllDefinedAppenders();
        synchronized (APPENDER_SINGLETON_TOKEN) {
            AppenderID existingAppenderIDs[] = getAllAppenders();
            for (int i = 0; i < existingAppenderIDs.length; i++) {
                AppenderID current = existingAppenderIDs[i];
                if (!MiscHelper.objectExists(current, allAppendersInConfig)) {
                    appenderSingletonMap.remove(current);
                }
            }
        }
    }

/*----------------------------------------------------------------------------*/    
    public static void flushAllAppenders() {
        AppenderID ids[] = Appender.getAllAppenders();
        for (int i = 0; i < ids.length; i++) {
            Appender appender = Appender.getAppender(ids[i]);
            if (appender != null) {
                appender.flush();
            }
        }
    }
}
