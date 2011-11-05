/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


class ThreadTraceMonitor {
    // Dont use log4j here... The class may not have been loaded
    private static final Logger logger = LoggerFactory.initLogger(ThreadTraceMonitor.class);    
    
    private static ThreadLocal<ThreadTracesOnStack> internalMonitorsOnThread = new ThreadLocal<ThreadTracesOnStack>() {
         protected synchronized ThreadTracesOnStack initialValue() {
             return new ThreadTracesOnStack();
         }
     };

     private static ThreadLocal<ThreadTracesOnStack> externalMonitorsOnThread = new ThreadLocal<ThreadTracesOnStack>() {
         protected synchronized ThreadTracesOnStack initialValue() {
             return new ThreadTracesOnStack();
         }
     };
     
    private ThreadTraceMonitor() {
    }

    static ThreadTracesOnStack getInternalThreadTracesOnStack() {
    	return internalMonitorsOnThread.get();
    }

    static ThreadTracesOnStack getExternalThreadTracesOnStack() {
    	return externalMonitorsOnThread.get();
    }
    
    private static class PointerToHead {
        /**
         * The outermost level of the thread stack...
         */
        final ThreadTraceData rootElement;
        final private Set<String> uniqueThreadTraceTimerKeys = new HashSet<String>();
        
        /**
         * The current level of the thread stack... 
         */
        ThreadTraceData topOfTheStackElement;
        
        final int maxDepth;
        final int minDuratinToCapture;
        
        int checkpointsIgnoredDueToMaxDepth = 0; 
        
        PointerToHead(ThreadTraceData rootElement, int maxDepth, int minDurationToCapture) {
            this.rootElement = rootElement;
            topOfTheStackElement = rootElement;
            this.maxDepth = maxDepth;
            this.minDuratinToCapture = minDurationToCapture;
        }

        public boolean isTopOfTheStackOnRootElement() {
            // Yes we do want to do a reference compare here!
            return rootElement == topOfTheStackElement;
        }
    }
    
    /**
     * No need for synchronization of this class since it is bound to a single
     * thread....
     */
    static class ThreadTracesOnStack {
        private Map<String, PointerToHead> threadDataMap = new HashMap<String, PointerToHead>();
        private ExternalThreadTraceConfig externalConfig = null;
        boolean active = false;
        
        
        /**
         * Prevent recursion...  This can happen if multiple instrumentation
         * agents are active...
         */
         
      
        private boolean insideThreadTracesOnStack = false;
        
        
        
        boolean isActive() {
        	return active;
        }
        
        void start(String monitorName, int maxDepth, int minDurationToCapture, long startTime) {
            ThreadTraceData rootElement = new ThreadTraceData(monitorName, startTime);
            threadDataMap.put(monitorName, new PointerToHead(rootElement, maxDepth, minDurationToCapture));
            active = true;
        }
        
        ThreadTraceData stop(String monitorName) {
            ThreadTraceData result = null;
            PointerToHead head = threadDataMap.get(monitorName);
            if (head != null) {
                if (!head.isTopOfTheStackOnRootElement()) {
                    logger.logWarn("Not on the root element when stop called for ThreadTraceData " +
                    		"(This Indicates out of balance perfmonitoring) monitorName=" + 
                    		monitorName); 
                }
                threadDataMap.remove(monitorName);
                head.rootElement.stop();
                result = head.rootElement;
            }
            active = !threadDataMap.isEmpty();
            
            return result;
        }
        
        UniqueThreadTraceTimerKey enterCheckpoint(String timerMonitorName, long startTime) {
        	UniqueThreadTraceTimerKey result = null;
        	if (!insideThreadTracesOnStack) {
        		insideThreadTracesOnStack = true;
	        	// Make sure we DO NOT RECURSE.... This can happen if one of the
        		// classes below causes our thread to try to start another checkpoint.
        		// Since perfmon4j classes are never instrumented, this can only
        		// Happen if one of the native classes are instrumented OR an additional
        		// java agent is active.
        		try {
	                if (!threadDataMap.isEmpty()) {
	                    result = new UniqueThreadTraceTimerKey(timerMonitorName);
	                    Iterator<PointerToHead> itr = threadDataMap.values().iterator();
	                    while (itr.hasNext()) {
	                        PointerToHead head = itr.next();
	                        head.uniqueThreadTraceTimerKeys.add(result.toHashString());
	                        if (head.maxDepth > 0 && head.topOfTheStackElement.getDepth() >= head.maxDepth) {
	                            head.checkpointsIgnoredDueToMaxDepth++;
	                        } else {
	                            head.topOfTheStackElement = new ThreadTraceData(timerMonitorName, head.topOfTheStackElement, startTime);
	                        }
	                    }
	                }
	        	} finally {
	        		insideThreadTracesOnStack = false;
	        	}
        	}
            return result;
        }

        void exitCheckpoint(UniqueThreadTraceTimerKey timerKey) {
        	if (!insideThreadTracesOnStack) {
        		insideThreadTracesOnStack = true;
	        	// Make sure we DO NOT RECURSE.... This can happen if one of the
        		// classes below causes our thread to try to start another checkpoint.
        		// Since perfmon4j classes are never instrumented, this can only
        		// Happen if one of the native classes are instrumented OR an additional
        		// java agent is active.
        		try {
                	Iterator<PointerToHead> itr = threadDataMap.values().iterator();
                    while (itr.hasNext()) {
                        PointerToHead head = itr.next();
                        if (head.uniqueThreadTraceTimerKeys.remove(timerKey.toHashString())) {
                            if (head.checkpointsIgnoredDueToMaxDepth > 0) {
                                head.checkpointsIgnoredDueToMaxDepth--;
                            } else {
                                // Make sure we never get to the root element...
                                // That is removed via stop...  If for some reason we became 
                                // unbalanced and we are at the root element DO NOTHING.
                                if (!head.isTopOfTheStackOnRootElement()) {
                                    ThreadTraceData exitTraceData = head.topOfTheStackElement;
                                    exitTraceData.stop();
                                    head.topOfTheStackElement = exitTraceData.getParent();
                                    if (head.minDuratinToCapture > 0 &&
                                        head.minDuratinToCapture > (exitTraceData.getEndTime() - exitTraceData.getStartTime())) {
                                        exitTraceData.seperateFromParent();
                                    }
                                } else {
                                    logger.logWarn("exitCheckpoint called without a checkpoint available.  monitorName = " +
                                        timerKey.monitorName);
                                }
                            }
                        }
                    }
	        	} finally {
	        		insideThreadTracesOnStack = false;
	        	}
        	}
        }

		public ExternalThreadTraceConfig popExternalConfig() {
			ExternalThreadTraceConfig result = externalConfig;
			externalConfig = null;
			return result;
		}

		public void setExternalConfig(ExternalThreadTraceConfig externalConfig) {
			this.externalConfig = externalConfig;
		}
    }
    
    static class UniqueThreadTraceTimerKey {
        private static long nextID = 0;
        
        private final long id = ++nextID;
        private final String monitorName;
        private final String hashString;
        
        UniqueThreadTraceTimerKey(String monitorName) {
            this.monitorName = monitorName;
            hashString = monitorName + Long.toString(id);
        }
        
        private String toHashString() {
            return hashString;
        }
    }
}
