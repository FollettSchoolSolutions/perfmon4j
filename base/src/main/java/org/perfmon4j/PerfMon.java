/*
 *	Copyright 2008,2009,2010,2011 Follett Software Company 
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

package org.perfmon4j;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.MonitorThreadTracker.Tracker;
import org.perfmon4j.PerfMonConfiguration.MonitorConfig;
import org.perfmon4j.remotemanagement.ExternalAppender;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.util.ActiveThreadMonitor;
import org.perfmon4j.util.EnhancedAppenderPatternHelper;
import org.perfmon4j.util.FailSafeTimerTask;
import org.perfmon4j.util.GlobalClassLoader;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.ThresholdCalculator;


public class PerfMon {
    // Dont use Log4j here... Class may not be loaded...
    private final static Logger logger = LoggerFactory.initLogger(PerfMon.class);
    
    public static final long NOT_SET = -1;

    static boolean configured = false;
    
    public static final int MAX_APPENDERS_PER_MONITOR = 
    	Integer.getInteger(PerfMon.class.getName() + ".MAX_APPENDERS_PER_MONITOR", 10).intValue();
    public static final int MAX_EXTERNAL_ELEMENTS_PER_MONITOR = 
    	Integer.getInteger(PerfMon.class.getName() + ".MAX_EXTERNAL_ELEMENTS_PER_MONITOR", 10).intValue();
    
    public static final int MAX_ALLOWED_INTERNAL_THREAD_TRACE_ELEMENTS = 
    	Integer.getInteger(PerfMon.class.getName() + ".MAX_ALLOWED_INTERNAL_THREAD_TRACE_ELEMENTS", 100000).intValue();
    
    // Value used to limit the max size of allowed thread trace elements  
    // captured for an external monitor (e.g. the VisualVM plugin)
    public static final int MAX_ALLOWED_EXTERNAL_THREAD_TRACE_ELEMENTS = 
    	Integer.getInteger(PerfMon.class.getName() + ".MAX_ALLOWED_EXTERNAL_THREAD_TRACE_ELEMENTS", 2500).intValue();
    
    
    public static final boolean USE_LEGACY_MONITOR_MAP_LOCK =
    	Boolean.getBoolean(PerfMon.class.getName() + ".USE_LEGACY_MONITOR_MAP_LOCK");
    
    public static final String ROOT_MONITOR_NAME;
    private final String name;

    public static final String APPENDER_PATTERN_NA = "";
    public static final String APPENDER_PATTERN_PARENT_ONLY = "./";
    public static final String APPENDER_PATTERN_PARENT_AND_CHILDREN_ONLY = "./*";
    public static final String APPENDER_PATTERN_CHILDREN_ONLY = "/*";
    public static final String APPENDER_PATTERN_ALL_DESCENDENTS = "/**";
    public static final String APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS = "./**";
    
    /** todo Make these timers lazy initialized.... If PerfMon is never instantiated
     * we dont want to start these thread... **/
    
    /**
     * priorityTimer should be used exclusivly to push PerfMonData onto
     * the appenders... All timerTasks should be fast...
     */
    private static final Timer priorityTimer = new Timer("PerfMon.priorityTimer", true);
    public static final Timer utilityTimer = new Timer("PerfMon.utilityTimer", true);
    
    private static long nextMonitorID;
    private static final PerfMon rootMonitor;
    private static ClassLoader classLoader = GlobalClassLoader.getClassLoader();

    // If this map is NON-Empty, we will create children of this
    // monitor even if the children are specified as a dynamic path.
    private final Map<Object, String> forceDynamicPathWeakMap = Collections.synchronizedMap(new WeakHashMap<Object, String>());
  
    // Meta system properties that are set regarding perfmon4j.
    public static final String PERFMON4J_VERSION = "Perfmon4j.version";
    public static final String PERFMON4J_COPYRIGHT = "Perfmon4j.copyright";
    public static final String PERFMON4J_CWD_HASH = "Perfmon4j.cwdHash";  // This is a unique hash within a single system.
    
    private final List<Appender> appenderList = Collections.synchronizedList(new ArrayList<Appender>());
    
    private final Lock dataArrayInsertLock = new ReentrantLock();
    
    private final PushAppenderDataTask dataArray[] = new PushAppenderDataTask[MAX_APPENDERS_PER_MONITOR];
    
    /**
     * External elements are monitors that will be polled by external (outside of the JVM) monitoring
     * tools.
     */
    private int	activeExternalElements = 0;
    private final Lock externalElementArrayLock = new ReentrantLock();
    private final IntervalData externalElementArray[] = new IntervalData[MAX_EXTERNAL_ELEMENTS_PER_MONITOR];
    
    
    /**
     * Make sure to synchronize on the mapMonitorLockToken when you access
     * the mapMonitors Map...
     */
    private static Map<String, PerfMon> mapMonitors = new HashMap<String, PerfMon>(10240);
    
    private static final Lock mapMonitorReadLock;
    private static final Lock mapMonitorWriteLock;

    static public final String version;
    static public final String copyright;
    
    static {
        // Order of these is important....
        nextMonitorID = 0;
        ROOT_MONITOR_NAME = "<ROOT>";
        rootMonitor = new PerfMon(null, ROOT_MONITOR_NAME);

        version = getImplementationVersion();
        copyright = "Copyright (c) 2015-2021 Follett School Solutions, LLC"; 
        
        System.setProperty(PERFMON4J_VERSION, version);
        System.setProperty(PERFMON4J_CWD_HASH, Integer.toString(MiscHelper.hashCodeForCWD));
        System.setProperty(PERFMON4J_COPYRIGHT, copyright);

    	if (USE_LEGACY_MONITOR_MAP_LOCK) {
    		System.out.println("***Perfmon4j Using Legacy Monitor Map Lock***");
    		
    		ReentrantLock lock = new ReentrantLock();
    		mapMonitorReadLock = mapMonitorWriteLock = lock;
    	} else {
    		ReadWriteLock lock = new ReentrantReadWriteLock();
    		mapMonitorReadLock = lock.readLock();
    		mapMonitorWriteLock = lock.writeLock();
    	}
    }
    
    private static final Set<String> monitorsWithThreadTraceConfigAttached = Collections.synchronizedSet(new HashSet<String>());
    
    private final Long monitorID;
    private Long startTime = null;
    private int totalHits = 0;
    private int totalCompletions = 0;
    
    /** Optional data...  May be null if SQL profiling is NOT enabled **/
    /** SQL/JDBC profiling START **/
    private long maxSQLDuration = 0;
    private long timeMaxSQLDurationSet = NOT_SET;
    
    private long minSQLDuration = NOT_SET;
    private long timeMinSQLDurationSet = NOT_SET;
    
    private long totalSQLDuration = 0;
    private long sumOfSQLSquares = 0;
    /** SQL/JDBC profiling END **/
    
    private long maxDuration = 0;
    private long timeMaxDurationSet = NOT_SET;
    
    long minDuration = NOT_SET; // Package level for testing..
    private long timeMinDurationSet = NOT_SET;
    
    private long totalDuration = 0;
    private long sumOfSquares = 0; // Used for standard deviation calculation
    private MaxThroughput maxThroughputPerMinute = null;
    
    private final PerfMon parent;
    
    private int maxActiveThreadCount = 0;
    private long timeMaxActiveThreadCountSet = NOT_SET;
    
    private PerfMonTimer cachedPerfMonTimer = null;
    private Set<PerfMon> childMonitors =  Collections.synchronizedSet(new HashSet<PerfMon>());
    private ThreadTraceConfig internalThreadTraceConfig = null;
    private final ExternalThreadTraceConfig.Queue externalThreadTraceQueue = new ExternalThreadTraceConfig.Queue();
    private final MonitorThreadTracker activeThreadList = new MonitorThreadTracker(this);
//    private ReferenceCount activeHead = null;
//    private ReferenceCount activeTail = null;
    
    private static ThreadLocal<Map<Long, ReferenceCount>> activeMonitors = new ThreadLocal<Map<Long, ReferenceCount>>() {
         protected synchronized Map<Long, ReferenceCount> initialValue() {
             return new HashMap<Long, ReferenceCount>();
         }
    };

    private final Lock startStopWriteLock;
    private final Lock startStopReadLock;
    
    private ThresholdCalculator thresholdCalculator = null;
    private ActiveThreadMonitor activeThreadMonitor = null;
    
/*----------------------------------------------------------------------------*/    
    private PerfMon(PerfMon parent, String name) {
//    	ReadWriteLock startStopLock = new ReentrantReadWriteLock();
//  	Using Read/Write locks actually degraded performance.
//        startStopReadLock = startStopLock.readLock(); 
//        startStopWriteLock = startStopLock.writeLock();
    	startStopReadLock = startStopWriteLock = new ReentrantLock();
        
    	this.name = parent == null ? ROOT_MONITOR_NAME : name;
        monitorID = new Long(++nextMonitorID);
        
        this.parent = parent;
        if (parent != null) {
            parent.childMonitors.add(this);
            this.thresholdCalculator = parent.getThresholdCalculator();
            this.activeThreadMonitor = parent.getActiveThreadMonitor();
        }

//        DO NOT LOG HERE!  You can create an infinite loop when debug is enabled!
//        if (logger.isDebugEnabled()) {
//            logger.logDebug("Constructing monitor: " + this.getName());
//        }
        
        
        // Assign any appenders that have been attached to this monitor.
        if (parent != null) {
        	this.resetAppenders();
        }
    }

/*----------------------------------------------------------------------------*/    
    static Timer getUtilityTimer() {
        return utilityTimer;
    }

/*----------------------------------------------------------------------------*/    
    static Timer getPriorityTimer() {
        return priorityTimer;
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static int getNumMonitors() {
    	int result = 0;

    	mapMonitorReadLock.lock();
        try {
        	result = mapMonitors.size();
        } finally {
        	mapMonitorReadLock.unlock();
        }
        return result;
    }
    

    /*----------------------------------------------------------------------------*/    
    public static List<String> getMonitorNames() {
        List<String> result = new ArrayList<String>(getNumMonitors());
        
        mapMonitorReadLock.lock();
        try {
        	result.addAll(mapMonitors.keySet());
        } finally {
        	mapMonitorReadLock.unlock();
        }
        
        return result;
    }
    
    /*----------------------------------------------------------------------------*/    
    public static List<MonitorKey> getMonitorKeys() {
        List<MonitorKey> result = new ArrayList<MonitorKey>(getNumMonitors());
 
        List<PerfMon> monitors = new ArrayList<PerfMon>(getNumMonitors());
        mapMonitorReadLock.lock();
        try {
        	monitors.addAll(mapMonitors.values());
        } finally {
        	mapMonitorReadLock.unlock();
        }
        for (PerfMon mon : monitors) {
        	result.add(MonitorKey.newIntervalKey(mon.getName()));
        }
        return result;
    }

    /*----------------------------------------------------------------------------*/    
    private static List<PerfMon> getMonitors() {
        List<PerfMon> monitors = new ArrayList<PerfMon>(getNumMonitors());
        mapMonitorReadLock.lock();
        try {
        	monitors.addAll(mapMonitors.values());
        } finally {
        	mapMonitorReadLock.unlock();
        }
        return monitors;
    }
    
/*----------------------------------------------------------------------------*/    
    /**
     * Package level for testing....
     */
    boolean hasAppenderWithTask(String className, long interval) {
        boolean result = false;
        Appender.AppenderID id = new Appender.AppenderID(className, interval);
        
        for (int i = 0; (i < dataArray.length && !result); i++) {
            PushAppenderDataTask task = dataArray[i];
            if (task != null) {
                result = task.appender.getMyAppenderID().equals(id);
            }
        }
    
        return result;
    }
    
/*----------------------------------------------------------------------------*/    
    private PerfMon[] getChildMonitors() {
        return childMonitors.toArray(new PerfMon[]{});
    }
    
/*----------------------------------------------------------------------------*/    
    public static PerfMon getRootMonitor() {
        return rootMonitor;
    }
    
/*----------------------------------------------------------------------------*/
    public static PerfMon getMonitorNoCreate_PERFMON_USE_ONLY(String key) {
    	mapMonitorReadLock.lock();
    	try {
			return mapMonitors.get(key);
		} finally {
			mapMonitorReadLock.unlock();
		}
    }

    public static PerfMon getMonitor(String key) {
    	return getMonitor(key, false);
    }
    
    /**
     * The isDynamicPath is used to limit the number of monitors
     * that are created and maintained in memory.
     * 
     * For most purposed you want to call this with a value of false
     * however if you are calling with a "dynamically" generated key
     * value you would only want the monitor created an appender exists.
     */
    public static PerfMon getMonitor(String key, boolean isDynamicPath) {
        PerfMon result = null;
        if (ROOT_MONITOR_NAME.equals(key)) {
            result = rootMonitor;
        } else {
        	try {
                mapMonitorReadLock.lock();
                result = mapMonitors.get(key);
        	} finally {
        		mapMonitorReadLock.unlock();
        	}
        }
        if (result == null) {
            PerfMon parent = null;
            String[] hierarchy = parseMonitorHirearchy(key);
            if (hierarchy.length > 1) {
                parent = getMonitor(hierarchy[hierarchy.length-2], isDynamicPath);
            } else {
                parent = rootMonitor;
            }
            if (!isDynamicPath || parent.shouldChildBeDynamicallyCreated(key)) {
            	if (isDynamicPath) {
                		// Since the child is being dynamically created we need
                		// to fill in it's ancestors.
            		result = getMonitor(key, false);
            	} else {
            		try {
            			mapMonitorWriteLock.lock();
                    	result = mapMonitors.get(key);
                    	if (result == null) {
    	                	result = new PerfMon(parent, key);
    	                	mapMonitors.put(key, result);
                    	}
            		} finally {
            			mapMonitorWriteLock.unlock();
            		}
            	}
            } else {
            	result = parent;
            }
        }
        return result;
    }
    
/*----------------------------------------------------------------------------*/ 
    /**
     * Package level...
     * Applications should use PerfMonTimer.start() to start a timer
     * using this monitor!
     */
    void start(long systemTime) {
        ReferenceCount count = getThreadLocalReferenceCount();
        if (count.inc(systemTime) == 1) {
        	if (externalThreadTraceQueue.hasPendingElements()) {
        		ExternalThreadTraceConfig externalConfig = externalThreadTraceQueue.assignToThread();
	        	if (externalConfig != null) {
	        		count.hasExternalThreadTrace = true;
	                ThreadTraceMonitor.ThreadTracesOnStack tOnStack = ThreadTraceMonitor.getExternalThreadTracesOnStack();
	                tOnStack.start(getName(), externalConfig.getMaxDepth(), externalConfig.getMinDurationToCapture(), systemTime);
	                tOnStack.setExternalConfig(externalConfig);
	        	}
        	}
        	
            ThreadTraceConfig internalConfig = internalThreadTraceConfig;
            if (internalConfig != null && internalConfig.shouldTrace()) {
                count.hasInternalThreadTrace = true;
                ThreadTraceMonitor.ThreadTracesOnStack tOnStack = ThreadTraceMonitor.getInternalThreadTracesOnStack();
                tOnStack.start(getName(), internalConfig.getMaxDepth(), internalConfig.getMinDurationToCapture(), systemTime);
            }
            
            startStopWriteLock.lock();
            try {
                int activeThreadCount = activeThreadList.addTracker(count);
                
                if (isActive()) {
                    totalHits++;
                    if (activeThreadCount >= maxActiveThreadCount) {
                        maxActiveThreadCount = activeThreadCount;
                        timeMaxActiveThreadCountSet = systemTime;
                    }
                    for (int i = 0; i < dataArray.length; i++) {
                        PushAppenderDataTask data = dataArray[i];
                        if (data != null) {
                            data.perfMonData.start(activeThreadCount, systemTime);
                        }
                    }
                    if (hasExternalElement()) {
                        for (int i = 0; i < externalElementArray.length; i++) {
                            IntervalData data = externalElementArray[i];
                            if (data != null) {
                                data.start(activeThreadCount, systemTime);
                            }
                        }
                    }
                }
            } finally {
            	startStopWriteLock.unlock();
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    void stop(long systemTime, boolean abort) {
        ReferenceCount count = getThreadLocalReferenceCount();
        if (count.dec() == 0) {
            if (count.hasExternalThreadTrace) {
                ThreadTraceMonitor.ThreadTracesOnStack tOnStack = ThreadTraceMonitor.getExternalThreadTracesOnStack();
                ThreadTraceData data = tOnStack.stop(getName());
                count.hasExternalThreadTrace = false;
                ExternalThreadTraceConfig externalConfig = tOnStack.popExternalConfig();
                if (data != null && externalConfig != null) {
                	externalConfig.outputData(data);
                	if (!externalThreadTraceQueue.hasPendingElements()) {
                		this.clearCachedPerfMonTimer();
                	}
                }
            }
            if (count.hasInternalThreadTrace) {
                ThreadTraceMonitor.ThreadTracesOnStack tOnStack = ThreadTraceMonitor.getInternalThreadTracesOnStack();
                ThreadTraceData data = tOnStack.stop(getName());
                count.hasInternalThreadTrace = false;
                if (data != null && internalThreadTraceConfig != null) {
                    AppenderID appenders[] = internalThreadTraceConfig.getAppenders();
                    for (int i = 0; i < appenders.length; i++) {
                        Appender appender = Appender.getAppender(appenders[i]);
                        if (appender != null) {
                        	appender.appendData(data);
                        }
                    }
                }
            }
            
            startStopWriteLock.lock();
            try {
                long eventStartTime = count.getStartTime();
                
                activeThreadList.removeTracker(count);
                
                final boolean active = isActive() && (startTime.longValue() <= eventStartTime);
                final boolean externalElement = hasExternalElement();
                final boolean monitorEvent =
                	(active || externalElement) && 
                	!abort;
                final boolean sqlTimeEnabled = monitorEvent && SQLTime.isEnabled();
                	
                if (monitorEvent) {
                	long sqlDuration = 0;
                	long sqlDurationSquared = 0;
                    long duration = systemTime - eventStartTime;
                    if (duration < 0) {
                    	/**
                    	 * VMWare and other virtual machines may actually, under rare conditions
                    	 * have a call to System.currentTimeMillis() return 
                    	 * an earlier timestamp than an prior call.  Since we don't want to return
                    	 * a negative duration we set the duration to 0.
                    	 */
                    	if (logger.isInfoEnabled()) {
                    		logger.logInfo("System currentime millis returned a negative time increment: " + duration);
                    	}
                    	duration = 0;
                    }
                    long durationSquared = (duration * duration);
                	if (sqlTimeEnabled) {
                    	/** We have SQL logging enabled... Monitor the SQLDurations. **/
                    	sqlDuration = SQLTime.getSQLTime() - count.getSQLStartMillis();
                    	if (sqlDuration < 0) {
                    		sqlDuration = 0;
                    	}
                    	sqlDurationSquared = (sqlDuration * sqlDuration);
                    }
                    if (active) {
                    	if (sqlTimeEnabled) {
                        	this.totalSQLDuration += sqlDuration;
                        	this.sumOfSQLSquares += sqlDurationSquared;
                        	
                            if (sqlDuration >= this.maxSQLDuration) {
                                this.timeMaxSQLDurationSet = systemTime;
                                this.maxSQLDuration = sqlDuration;
                            }
                            if ((sqlDuration <= this.minSQLDuration) || (this.minSQLDuration == NOT_SET)) {
                                this.minSQLDuration = sqlDuration;
                                this.timeMinSQLDurationSet = systemTime;
                            }                    	
                        	/** We have SQL logging enabled... Monitor the SQLDurations. **/
                        }
                    	
                    	totalCompletions++;
                        
                        totalDuration += duration;
                        sumOfSquares += durationSquared;
                        if (duration >= maxDuration) {
                            timeMaxDurationSet = systemTime;
                            maxDuration = duration;
                        }
                        if ((duration <= minDuration) || (minDuration == NOT_SET)) {
                            minDuration = duration;
                            timeMinDurationSet = systemTime;
                        }
                        for (int i = 0; i < dataArray.length; i++) {
                            PushAppenderDataTask data = dataArray[i];
                            if (data != null) {
                                data.perfMonData.stop(duration, durationSquared, systemTime, sqlDuration, sqlDurationSquared);
                            }
                        }
                    	if (externalElement) {
                            for (int i = 0; i < externalElementArray.length; i++) {
                                IntervalData data = externalElementArray[i];
                                if (data != null) {
                                    data.stop(duration, durationSquared, systemTime, sqlDuration, sqlDurationSquared);
                                }
                            }
                    	}
                    }
                }
            } finally {
            	startStopWriteLock.unlock();
            }
        }
    }

/*----------------------------------------------------------------------------*/    
    public int getTotalHits() {
        return totalHits;
    }

/*----------------------------------------------------------------------------*/    
    public int getActiveThreadCount() {
        return activeThreadList.getLength();
    }
    
/*----------------------------------------------------------------------------*/    
    public String getName() {
        return name;
    }

/*----------------------------------------------------------------------------*/    
    public String getSimpleName() {
    	String[] split = name.split("\\.");
        return split[split.length-1];
    }
    
/*----------------------------------------------------------------------------*/    
	private ReferenceCount getThreadLocalReferenceCount() {
        Map<Long, ReferenceCount> map = activeMonitors.get();
        // No need to synchronize here since this is a thread local object...
        ReferenceCount count = map.get(monitorID);
        if (count == null) {
            count = new ReferenceCount(Thread.currentThread());
            map.put(monitorID, count);
        }
        return count;
    }
    
/*----------------------------------------------------------------------------*/    
    private static String removeTrailingPeriods(String val) {
        while (val.endsWith(".") && val.length() > 1) {
            val = val.substring(0, val.length() - 1);
        }
        
        return val;
    }
    
    // protected to expose for test...
/*----------------------------------------------------------------------------*/    
    protected static String[] parseMonitorHirearchy(String monitor) {
        String[] result = new String[]{};
        if (monitor != null && !monitor.equals("")) {
            monitor = removeTrailingPeriods(monitor);
            List<String> x = new ArrayList<String>();
            int offset = 0;
            while (true) {
                offset = monitor.indexOf('.', offset);
                if (offset > 0) {
                    x.add(monitor.substring(0, offset));
                    offset++;
                } else {
                    break;
                }
            }
            x.add(monitor);
            result = x.toArray(result);
        }
        return result;
    }
    
/*----------------------------------------------------------------------------*/    
    public boolean isRootMonitor() {
        return parent == null;
    }
    
    
/*----------------------------------------------------------------------------*/    
    MonitorThreadTracker getActiveThreadList() {
		return activeThreadList;
	}
    
/*----------------------------------------------------------------------------*/    
    private static class ReferenceCount implements MonitorThreadTracker.Tracker {
    	/** Store as a weak reference in case owningThread is aborted and Garbage Collected **/
    	private final WeakReference<Thread> owningThread;
    	
        private int refCount = 0;
        private long startTime;
        private long sqlStartMillis = 0;
        boolean hasInternalThreadTrace = false;
        boolean hasExternalThreadTrace = false;
		private ReferenceCount previous = null;
		private ReferenceCount next = null;
        
        ReferenceCount(Thread owningThread) {
        	this.owningThread = new WeakReference<Thread>(owningThread);
        }
        
        /**
         * @return The updated (incremented value of refCount)
         */
        private int inc(long startTime) {
            if (refCount == 0) {
                this.startTime = startTime;
                this.sqlStartMillis = SQLTime.getSQLTime(); // Will always be 0 of SQLtime is NOT enabled.
            }
            return ++refCount;
        }
        
        /**
         * @return The updated (decremented value of refCount)
         */
        private int dec() {
            return --refCount;
        }
        
       private long getSQLStartMillis() {
    	   return sqlStartMillis;
       }

       @Override
		/**
		 * The owningThread is stored as a weakReference to allow
		 * garbage collection of the thread if needed.  Caller must
		 * be prepared for this method to return null.
		 */
		public Thread getThread() {
			return owningThread.get();
		}
		
		@Override
		public void setPrevious(Tracker previous) {
			this.previous = (ReferenceCount)previous;
		}
		
		@Override
		public Tracker getPrevious() {
			return previous;
		}
		
		@Override
		public void setNext(Tracker next) {
			this.next = (ReferenceCount)next;
		}
		
		@Override
		public Tracker getNext() {
			return next;
		}
		
		@Override
		public long getStartTime() {
			return startTime;
		}
       
    }

/*----------------------------------------------------------------------------*/    
    public int getTotalCompletions() {
        return totalCompletions;
    }

/*----------------------------------------------------------------------------*/    
    public long getAverageDuration() {
    	startStopReadLock.lock();
        try {
            return totalCompletions > 0 ?
                totalDuration / totalCompletions : 0;
        } finally {
        	startStopReadLock.unlock();
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public double getStdDeviation() {
    	startStopReadLock.lock();
        try {
            return MiscHelper.calcStdDeviation(totalCompletions, totalDuration, sumOfSquares);
        } finally {
        	startStopReadLock.unlock();
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public long getMaxDuration() {
        return maxDuration;
    }

/*----------------------------------------------------------------------------*/    
    public long getMinDuration() {
        return Math.max(minDuration, 0); //Mask NOT_SET
    }
    
    long getMinDuration_NO_FIXUP() {
        return minDuration;
    }
    
/*----------------------------------------------------------------------------*/    
    public long getTotalDuration() {
        return totalDuration;
    }
    
/*----------------------------------------------------------------------------*/    
    public int getMaxActiveThreadCount() {
        return maxActiveThreadCount;
    }
    
    
	/*----------------------------------------------------------------------------*/    
    int getNumAppenders() {
        return appenderList.size();
    }
    
    /**
     * Package level for test only...
     */
/*----------------------------------------------------------------------------*/    
    int getNumPerfMonTasks() {
        int count = 0;
        for (int i = 0; i < dataArray.length; i++) {
            if (dataArray[i] != null) {
                count++;
            }
        }
        return count;
    }
    
    
    /*----------------------------------------------------------------------------*/    
    private int getNumExternalAppenderTasks() {
        int count = 0;
        for (int i = 0; i < externalElementArray.length; i++) {
            if (externalElementArray[i] != null) {
                count++;
            }
        }
        return count;
    }
    
/*----------------------------------------------------------------------------*/    
    private void addAppender(Appender.AppenderID id) throws InvalidConfigException {
        addAppender(Appender.getOrCreateAppender(id));
    }


/*----------------------------------------------------------------------------*/    
    private boolean shouldChildBeDynamicallyCreated(String key) {
    	return mapper.hasAppendersForMonitor(key) || 
    			(ExternalAppender.isActive() && !forceDynamicPathWeakMap.isEmpty());
    }
    
    
/*----------------------------------------------------------------------------*/    
    private void addAppender(Appender appender)  {
        boolean hasAppender = appenderList.contains(appender);
        
        if (!hasAppender) {
            if (logger.isDebugEnabled()) {
                logger.logDebug("Adding appender " + appender + " to monitor ");
            }
            appenderList.add(appender);
            
            
            int index = -1;
            
            if (!isRootMonitor() ) {
            	dataArrayInsertLock.lock();
                try {
                    for (int i = 0; i < dataArray.length; i++) {
                        if (dataArray[i] == null) {
                            index = i;
                            break;
                        }
                    }
                    if (index > -1) {
                        makeActive();
                        
                        FailSafeTimerTask task = new PushAppenderDataTask(this, appender, index);
                        if (logger.isDebugEnabled()) {
                            logger.logDebug("Scheduling task: " + task);
                        }
                        priorityTimer.schedule(task, 
                        		roundInterval(MiscHelper.currentTimeWithMilliResolution(), 
                        				appender.getIntervalMillis()));
                    } else {
                        logger.logError("Unable to add appender to monitor: " + this +
                                " - Max appenders exceeded");
                    }
                } finally {
                	dataArrayInsertLock.unlock();
                }
            }
        }
        clearCachedPerfMonTimer();    
    }
    
/*----------------------------------------------------------------------------*/    
    private void removeAppender(Appender appender)  {
        
        if (appenderList.contains(appender)) {
            if (logger.isDebugEnabled()) {
                logger.logDebug("Removing appender " + appender + " from monitor " 
                    + this);
            }
            dataArrayInsertLock.lock();
            try {
                appenderList.remove(appender);
                
                for (int i = 0; i < dataArray.length; i++) {
                    PushAppenderDataTask task = dataArray[i];
                    if (task != null && task.appender.equals(appender)) {
                        dataArray[i] = null;
                        if (!isRootMonitor()) {
                            makeInactiveIfNoAppenders();
                            
                            if (logger.isDebugEnabled()) {
                                logger.logDebug("Canceling appender task: " + task);
                            }
                            task.cancel();
                        }
                    }
                }
            } finally {
            	dataArrayInsertLock.unlock();
            }
        }
        clearCachedPerfMonTimer();
    }

    
    /**
     * 
     * @param data
     * @return true - if there was an open position to add the data element.
     * 		   false - if there the element could not be added. 
     */
    public boolean addExternalElement(IntervalData data) {
    	boolean added = false;
    	externalElementArrayLock.lock();
    	try {
    		for (int i = 0; i < externalElementArray.length && !added; i++) {
    			if (externalElementArray[i] == null) {
    				externalElementArray[i] = data;
    				added = true;
    				activeExternalElements++;
    			}
			}
    		if (added) {
    			if (!isActive()) {
    				makeActive();	
    			}
    			clearCachedPerfMonTimer();
    		}
    	} finally {
    		externalElementArrayLock.unlock();
    	}
    	
    	
    	return added;
    }
    
    
    /**
     * 
     * @param data
     * @return true - if the external element was found in the array and removed
     * 			false - if the element could not be found.
     */
    public boolean removeExternalElement(IntervalData data) {
    	boolean removed = false;
    	
    	externalElementArrayLock.lock();
    	try {
    		for (int i = 0; i < externalElementArray.length && !removed; i++) {
    			if (externalElementArray[i] == data) {
    				externalElementArray[i] = null;
    				removed = true;
    				activeExternalElements--;
    			}
			}
    		
    		if (removed) {
    			makeInactiveIfNoAppenders();
    			clearCachedPerfMonTimer();
    		}
    	} finally {
    		externalElementArrayLock.unlock();
    	}
    	return removed;
    }

    public IntervalData replaceExternalElement(IntervalData current, IntervalData replacement) {
    	IntervalData result = null;
    	
    	externalElementArrayLock.lock();
    	try {
    		for (int i = 0; i < externalElementArray.length && result == null; i++) {
    			if (externalElementArray[i] == current) {
    				externalElementArray[i] = replacement;
    				result = replacement;
    			}
			}
    	} finally {
    		externalElementArrayLock.unlock();
    	}
    	return result;
    }
    
    
    // Returns true if this monitor has one or more externally manaaged interval data
    // elements
    boolean hasExternalElement() {
    	return activeExternalElements > 0;
    }
    
/*----------------------------------------------------------------------------*/    
    private class PushAppenderDataTask extends FailSafeTimerTask {
        final PerfMon owner;
        final Appender appender;
        final int offset;
        final IntervalData perfMonData;
        
        PushAppenderDataTask(PerfMon owner, Appender appender, int offset) {
            this.owner = owner;
            this.appender = appender;
            this.offset = offset;
            
            startStopReadLock.lock();
            try {
                perfMonData = appender.newIntervalData(owner, MiscHelper.currentTimeWithMilliResolution());
            } finally {
            	startStopReadLock.unlock();
            }
            dataArray[offset] = this;
        }
        
        public void failSafeRun() {
        	long now = MiscHelper.currentTimeWithMilliResolution();
            priorityTimer.schedule(new PushAppenderDataTask(owner, this.appender, offset), 
            		roundInterval(now, appender.getIntervalMillis()));
            try {
                perfMonData.setTimeStop(now);
                maxThroughputPerMinute = perfMonData.refreshMonitorsMaxThroughputPerMinute(maxThroughputPerMinute);
                appender.appendData(perfMonData);
            } catch (Exception ex) {
                logger.logError("Error running " + this.getClass().getSimpleName() + " task", ex);
            }
        }
        
        
        public String toString() {
            return "PushAppenderDataTask(" +
                    "owner=" + owner + " appender=" + appender + ")";
        }
    }

    /**
     * This method has an arbitrary requirement that it will 
     * not round any appenderInterval that not evenly divisible by 1 second.
     * This requirement ensures that unit tests that use very small intervals
     * are not broken. 
     * 
     * @param nowInMillis
     * @param appenderIntervalInMillis
     * @return
     */
    static long roundInterval(long nowInMillis, long appenderIntervalInMillis) {
    	if (appenderIntervalInMillis > 0 && ((appenderIntervalInMillis % 1000) == 0)) {
	    	long delta = ((nowInMillis/appenderIntervalInMillis)*appenderIntervalInMillis) - nowInMillis;
	    	
	    	if (delta != 0) {
	    		long newInterval = appenderIntervalInMillis + delta; 
				if (newInterval <=  (appenderIntervalInMillis / 2)) {
					newInterval += appenderIntervalInMillis;
				} 
				appenderIntervalInMillis = newInterval;
	    	}
    	}
    	
    	return appenderIntervalInMillis;
    }
    
    private void resetAppenders() {
    	if (mapper == null) {
    		logger.logWarn("AppenderToMonitorMapper is null in resetAppenders for monitor: " + getName() + " -- Skipping");
    	} else {
	    	// Add all the appenders (or at least those that are not already on the monitor)
	    	Set<AppenderID> activeAppenders =  new HashSet<AppenderID>(Arrays.asList(mapper.getAppendersForMonitor(this.getName())));
	    	
	    	for (AppenderID appenderID : activeAppenders) {
	    		try {
					addAppender(appenderID);
				} catch (InvalidConfigException e) {
					logger.logDebug("Invalid appender", e);
				}
	    	}
	    	
	    	// Remove any appenders that are no longer configured...
	    	for (Appender currentAppender :  appenderList.toArray(new Appender[]{})) {
	    		if (!activeAppenders.contains(currentAppender.getMyAppenderID())) {
	    			removeAppender(currentAppender);
	    		}
	    	}
    	}
    }

    /**
     * Package level!
     * Applications should call PerfMonTimer.start() to start a timer...
     */
    PerfMonTimer getPerfMonTimer() {
        if (cachedPerfMonTimer == null) {
            if (isRootMonitor()) {
                cachedPerfMonTimer = PerfMonTimer.getNullTimer();
            } else if (isActive() || hasExternalElement() ||  internalThreadTraceConfig != null
            	|| externalThreadTraceQueue.hasPendingElements()) {
                cachedPerfMonTimer = new PerfMonTimer(this, parent.getPerfMonTimer());
            } else {
                cachedPerfMonTimer = parent.getPerfMonTimer();
            }
        }
        return cachedPerfMonTimer;
    }
    
/*----------------------------------------------------------------------------*/    
    public static boolean isConfigured() {
        return configured;
    }
    
/*----------------------------------------------------------------------------*/    
    public static void deInit() {
        configure(); // Will reset/turn everything off;
        configured = false;
        
        SnapShotManager.deInit();
    }
    

    /**
     * TESTONLY Dont Call this outside of TEST... Could have
     * Bad side effects!
     */
    public static void deInitAndCleanMonitors_TESTONLY() {
    	deInit();
    	mapMonitorWriteLock.lock();
    	try {
    		mapMonitors.clear();
		} finally {
			mapMonitorWriteLock.unlock();
		}
    }
    
/*----------------------------------------------------------------------------*/ 
    protected void clearCachedPerfMonTimer() {
        cachedPerfMonTimer = null;
        
        PerfMon children[] = getChildMonitors();
        for (int i = 0; i < children.length; i++) {
            children[i].clearCachedPerfMonTimer();
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public static void configure() {
        try {
            PerfMonConfiguration config = new PerfMonConfiguration();
            config.defineMonitor(PerfMon.ROOT_MONITOR_NAME);
            configure(config);
        } catch (InvalidConfigException ex) {
            logger.logWarn("Default configuration did not work", ex);
        }
    }
    
    // A count of the number of thread traces that have 
    // Triggers assigned to them.  This is used to inform the 
    // HttpRequestFilter if it should push the HttpRequest parameter
    // onto the request processing thread.
    private static int requestBasedTriggerCount = 0;
    
    public static boolean  hasHttpRequestBasedThreadTraceTriggers() {
    	return configured && (requestBasedTriggerCount > 0);
    }

    // A count of the number of thread traces that have 
    // Triggers assigned to them.  This is used to inform the 
    // HttpRequestFilter if it should push the HttpSessionValidator
    // onto the request processing thread.
    private static int sessionBasedTriggerCount = 0;
    
    public static boolean  hasHttpSessionBasedThreadTraceTriggers() {
    	return configured && (sessionBasedTriggerCount > 0);
    }
    
    // A count of the number of thread traces that have 
    // Triggers assigned to them.  This is used to inform the 
    // HttpRequestFilter if it should push the HttpCookieValidator
    // onto the request processing thread.
    private static int cookieBasedTriggerCount = 0;
    
    public static boolean  hasHttpCookieBasedThreadTraceTriggers() {
    	return configured && (cookieBasedTriggerCount > 0);
    }
    
    private static AppenderToMonitorMapper mapper = (new AppenderToMonitorMapper.Builder()).build();
    
/*----------------------------------------------------------------------------*/    
    public static void configure(PerfMonConfiguration config) throws InvalidConfigException {
        // First flush all active appenders..
        if (PerfMon.isConfigured()) {
            Appender.flushAllAppenders();
        }
        
        configured = true;
        
        RegisteredDatabaseConnections.config(config);
        
        MonitorConfig monitorConfigs[] = config.getMonitorConfigArray();
        Map<String, ThresholdCalculator> thresholdMap = new HashMap<String, ThresholdCalculator>(); 
        Map<String, ActiveThreadMonitor> activeThreadMonMap = new HashMap<String, ActiveThreadMonitor>(); 
        
        AppenderToMonitorMapper.Builder builder = new AppenderToMonitorMapper.Builder();
        for (MonitorConfig monitorConfig : monitorConfigs) {
        	// Explicitly create each monitor that was explicitly defined in the 
        	// configuration.
        	PerfMon.getMonitor(monitorConfig.getMonitorName()); 

        	// Look to see if a ThresholdCalculator is defined for the Monitor
        	String thresholdValues = monitorConfig.getProperty("thresholdCalculator");
        	if (thresholdValues != null) {
        		thresholdMap.put(monitorConfig.getMonitorName(), new ThresholdCalculator(thresholdValues));
        	}

        	// Look to see if a ActiveThreadMonitor is defined for the Monitor
        	String activeThreadValues = monitorConfig.getProperty("activeThreadMonitor");
        	if (activeThreadValues != null) {
        		activeThreadMonMap.put(monitorConfig.getMonitorName(), new ActiveThreadMonitor(activeThreadValues));
        	}
        	
            for (PerfMonConfiguration.AppenderAndPattern appender : config.getAppendersForMonitor(monitorConfig.getMonitorName(), config)) {
            	builder.add(monitorConfig.getMonitorName(), appender.getAppenderPattern(), appender.getAppenderID());
            }
        }
        mapper = builder.build();

        // After we initialized the MonitorToAppenderMapper.  Go through and
        // explicitly create all of the monitors.  This is a bit of a legacy/compatibility
        // process.  Prior to the enhanced appender pattern implementation,
        // each monitor defined in the perfmonconfig.xml file was created
        // automatically.
        for (MonitorConfig monitorConfig : monitorConfigs) {
        	// Explicitly create each monitor that was explicitly defined in the 
        	// configuration.
        	PerfMon.getMonitor(monitorConfig.getMonitorName()); 
        }
        
        
        // Walk through all the monitors
        for (PerfMon mon : getMonitors()) {
        	String[] monitorHirearchy = PerfMon.parseMonitorHirearchy(mon.getName());
        	// Update the ThresholdCalculators;
        	ThresholdCalculator calc = null;
        	for (int i = monitorHirearchy.length - 1; (i >= 0) && (calc == null); i--) {
        		calc = thresholdMap.get(monitorHirearchy[i]);
        	}
        	mon.setThresholdCalculator(calc);
        	
        	// Update the ActiveThreadMonitors
        	ActiveThreadMonitor activeThreadMon = null;
        	for (int i = monitorHirearchy.length - 1; (i >= 0) && (activeThreadMon == null); i--) {
        		activeThreadMon = activeThreadMonMap.get(monitorHirearchy[i]);
        	}
        	mon.setActiveThreadMonitor(activeThreadMon);
        	
        	mon.resetAppenders();
        }
        
        SnapShotManager.applyConfig(config);

        
        Appender.purgeUnusedAppenders(config);
        
        int numHttpRequestTriggers = 0;
        int numHttpSessionTriggers = 0;
        int numHttpCookieTriggers = 0;
        
        // Apply any threadTrace configurations...
        Map<String, ThreadTraceConfig> threadTraceMap = config.getThreadTraceConfigMap();
        Iterator<Map.Entry<String, ThreadTraceConfig>> threadTraceItr = threadTraceMap.entrySet().iterator();
        while (threadTraceItr.hasNext()) {
            Map.Entry<String, ThreadTraceConfig> current = threadTraceItr.next();
            ThreadTraceConfig traceConfig = current.getValue();
            ThreadTraceConfig.Trigger triggers[] = traceConfig.getTriggers();
            if (triggers != null) {
            	for (int i = 0; i < triggers.length; i++) {
					if (triggers[i] instanceof ThreadTraceConfig.HTTPRequestTrigger) {
						numHttpRequestTriggers++;
					} else if (triggers[i] instanceof ThreadTraceConfig.HTTPSessionTrigger) {
						numHttpSessionTriggers++;
					} else if (triggers[i] instanceof ThreadTraceConfig.HTTPCookieTrigger) {
						numHttpCookieTriggers++;
					}
				}
            }
            PerfMon.getMonitor(current.getKey()).setInternalThreadTraceConfig(traceConfig);
        }
        requestBasedTriggerCount = numHttpRequestTriggers;
        sessionBasedTriggerCount = numHttpSessionTriggers;
        cookieBasedTriggerCount = numHttpCookieTriggers;
        
        // Remove any ThreadTraceConfigs that are no longer active...
        String threadTraceMonitors[] = PerfMon.getMonitorNamesWithThreadTraceConfigAttached();
        for (int i = 0; i < threadTraceMonitors.length; i++) {
            String monitorName = threadTraceMonitors[i];
            if (!threadTraceMap.containsKey(monitorName)) {
                PerfMon.getMonitor(monitorName).setInternalThreadTraceConfig(null);
            }
        }
    }

    
/*----------------------------------------------------------------------------*/    
    /**
     * Package level to allow unit test
     */
    static String parentToChildConversion(String pattern, PerfMon parent, PerfMon child) {
        String result = APPENDER_PATTERN_NA;
        
        if (APPENDER_PATTERN_ALL_DESCENDENTS.equals(pattern) || 
            APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS.equals(pattern)) {
            result = APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS;
        } else if (APPENDER_PATTERN_CHILDREN_ONLY.equals(pattern)  || APPENDER_PATTERN_PARENT_AND_CHILDREN_ONLY.equals(pattern)) {
            result = APPENDER_PATTERN_PARENT_ONLY;
        } else if (!APPENDER_PATTERN_NA.equals(pattern)){
        	String childName = child.getName();
        	String parentName = parent.getName();

        	if (childName.length() > parentName.length()) {
	        	String simpleName = childName.substring(parentName.length() + 1, childName.length());
	        	
	        	EnhancedAppenderPatternHelper.PatternInfo info = EnhancedAppenderPatternHelper.massagePattern(pattern);
	        	if (simpleName.matches(info.getRegEx())) {
	        		String remainder = info.getRemainder();
	        		if (remainder.isEmpty()) {
	        			result = APPENDER_PATTERN_PARENT_ONLY;
	        		} else {
	        			result = "/" + remainder;
	        		}
	        	}
        	}
        }
        
        return result;
    }
    
    
    static private PerfMonConfiguration.AppenderAndPattern[] parentToChildConversion (
        PerfMonConfiguration.AppenderAndPattern[] appenders, PerfMon parent, PerfMon child) throws InvalidConfigException {
        
        List<PerfMonConfiguration.AppenderAndPattern> result = 
            new ArrayList<PerfMonConfiguration.AppenderAndPattern>();
        
        for (int i = 0; i < appenders.length; i++) {
            PerfMonConfiguration.AppenderAndPattern appender = appenders[i];
            String childPattern = parentToChildConversion(appender.getAppenderPattern(), parent, child);
            if (!APPENDER_PATTERN_NA.equals(childPattern)) {
                result.add(new PerfMonConfiguration.AppenderAndPattern(appender.getAppender().getMyAppenderID(),
                    childPattern));
            }
        }
        
        return result.toArray(new PerfMonConfiguration.AppenderAndPattern[]{});
    }
    
    public boolean isActive() {
        return startTime != null;
    }
    
    private void makeInactiveIfNoAppenders() {
    	if ((getNumPerfMonTasks() + getNumExternalAppenderTasks()) < 1) {
	    	startStopWriteLock.lock();
	        try {
	            startTime = null;
	            totalHits = 0;
	            totalCompletions = 0;
	            maxDuration = 0;
	            minDuration = NOT_SET;
	            totalDuration = 0;
	            sumOfSquares = 0;
	            maxActiveThreadCount = 0;
	            maxThroughputPerMinute = null;
	        } finally {
	        	startStopWriteLock.unlock();
	        }
    	}
    }
    
    private void makeActive() {
    	if (!isActive()) {
            if (logger.isDebugEnabled()) {
                logger.logDebug("Activating monitor " + this);
            }
	    	startStopWriteLock.lock();
	        try {
	            startTime = new Long(MiscHelper.currentTimeWithMilliResolution());
	        } finally {
	        	startStopWriteLock.unlock();
	        }
    	}
    }
    
    public long getStartTime() {
        return startTime != null ? startTime.longValue() : 0;
    }


    public long getTimeMaxActiveThreadCountSet() {
        return timeMaxActiveThreadCountSet;
    }


    public long getTimeMaxDurationSet() {
        return timeMaxDurationSet;
    }


    public long getTimeMinDurationSet() {
        return timeMinDurationSet;
    }

    public long getSumOfSquares() {
        return sumOfSquares;
    }
    
    public String toString() {
        return "PerfMon(monitorID=" + monitorID +
            " name=" + getName() +
            " parent=" + parent + ")";
    }

    public void scheduleExternalThreadTrace(ExternalThreadTraceConfig config) {
        externalThreadTraceQueue.schedule(config);
        clearCachedPerfMonTimer();
    }
    
    public void unScheduleExternalThreadTrace(ExternalThreadTraceConfig config) {
        externalThreadTraceQueue.unSchedule(config);
        clearCachedPerfMonTimer();
    }
    
    public void setInternalThreadTraceConfig(ThreadTraceConfig config) throws InvalidConfigException {
        clearCachedPerfMonTimer();
        boolean removeAppender = true;
        if (config != null) {
            AppenderID appenderIDs[] = config.getAppenders();
            boolean hasAppender = false;
            for (int i = 0; i < appenderIDs.length; i++) {
            	AppenderID id = appenderIDs[i];
            	if (id != null) {
            		Appender.getOrCreateAppender(appenderIDs[i]);
            		hasAppender = true;
            	}
			}
            if (hasAppender) {
            	removeAppender = false;
            	monitorsWithThreadTraceConfigAttached.add(this.getName());
            } else {
            	logger.logWarn("ThreadTraceMonitor on " + this.getName() + 
            			" will be ignored.  No valid appender attached");
            }
        } 
        if (removeAppender) {
            monitorsWithThreadTraceConfigAttached.remove(this.getName());
        }
        this.internalThreadTraceConfig = config;
    }
    
    static String[] getMonitorNamesWithThreadTraceConfigAttached() {
        return monitorsWithThreadTraceConfigAttached.toArray(new String[]{});
    }
    
    public ThreadTraceConfig getInternalThreadTraceConfig() {
        return internalThreadTraceConfig;
    }

    public void forceDynamicChildCreation(Object externalMonitorInstance) {
		forceDynamicPathWeakMap.put(externalMonitorInstance, "");
    }
    
    public void unForceDynamicChildCreation(Object externalMonitorInstance) {
		forceDynamicPathWeakMap.remove(externalMonitorInstance);
    }

    public ThresholdCalculator getThresholdCalculator() {
		return thresholdCalculator;
	}

	void setThresholdCalculator(ThresholdCalculator thresholdCalculator) {
		this.thresholdCalculator = thresholdCalculator;
	}
	
	public ActiveThreadMonitor getActiveThreadMonitor() {
		return activeThreadMonitor;
	}

	void setActiveThreadMonitor(ActiveThreadMonitor activeThreadMonitor) {
		this.activeThreadMonitor = activeThreadMonitor;
	}

	public String toHTMLString() {
        String result = "<STRONG>" + "PerfMon(" + monitorID + "-" + 
             name + ")</STRONG><CR>\r\n";
        result += "active=" + isActive() + "<CR>\r\n";
        if (isActive()) {
            result += "&nbsp;&nbsp;startTime=" + MiscHelper.formatDateTimeAsString(startTime) + "<CR>\r\n";
            result += "&nbsp;&nbsp;totalHits=" + totalHits + "<CR>\r\n";
            result += "&nbsp;&nbsp;totalCompletions=" + totalCompletions + "<CR>\r\n";
            result += "&nbsp;&nbsp;activeThreadCount=" + getActiveThreadCount() + "<CR>\r\n";
            result += "&nbsp;&nbsp;maxDuration=" + maxDuration + " " + MiscHelper.formatDateTimeAsString(timeMaxDurationSet, false, true) + "<CR>\r\n";
            result += "&nbsp;&nbsp;minDuration=" + minDuration + " " + MiscHelper.formatDateTimeAsString(timeMinDurationSet, false, true) + "<CR>\r\n";
            result += "&nbsp;&nbsp;maxActiveThreadcount=" + maxActiveThreadCount + " " + MiscHelper.formatDateTimeAsString(timeMaxActiveThreadCountSet, false, true) + "<CR>\r\n";
            result += "&nbsp;&nbsp;maxThroughputPerMinute=" + (maxThroughputPerMinute != null ? maxThroughputPerMinute : "") + "<CR>\r\n";
        }
        return result;
    }
    
    public static String buildHTMLString(PerfMon monitor) {
        String result = monitor.toHTMLString();
        
        PerfMon children[] = monitor.getChildMonitors();
        for (int i = 0; i < children.length; i++) {
            result += buildHTMLString(children[i]);
        }
        
        return result;
    }
    
    public static String buildHTMLString() {
        return buildHTMLString(PerfMon.getRootMonitor());
    }


    public static ClassLoader getClassLoader() {
        return classLoader;
    }


    public static void setClassLoader(ClassLoader classLoader) {
        PerfMon.classLoader = classLoader;
    }

	public long getMaxSQLDuration() {
		return maxSQLDuration;
	}

	public long getTimeMaxSQLDurationSet() {
		return timeMaxSQLDurationSet;
	}

    public long getMinSQLDuration() {
        return Math.max(minSQLDuration, 0); //Mask NOT_SET
    }
    
    long getMinSQLDuration_NO_FIXUP() {
        return minSQLDuration;
    }

	public long getTimeMinSQLDurationSet() {
		return timeMinSQLDurationSet;
	}

	public long getTotalSQLDuration() {
		return totalSQLDuration;
	}

	public long getSumOfSQLSquares() {
		return sumOfSQLSquares;
	}
	
	
	/**
	 * When running within the armstrong client getting the implementation version with:
	 * "PerfMon.class.getPackage().getImplementationVersion()" would throw a null 
	 * pointer exception.  This method was added to ensure every reference
	 * was checked for null
	 *  
	 * @return
	 */
	private static String getImplementationVersion() {
		String version = null;
		
		Class<?> clazz = PerfMon.class;
		if (clazz != null) {
			Package p = clazz.getPackage();
			if (p != null) {
				version = p.getImplementationVersion();
			}
		}

		if (version == null) {
			version = "NA(Running in test)";
		}
		
		return version;
	}
}
