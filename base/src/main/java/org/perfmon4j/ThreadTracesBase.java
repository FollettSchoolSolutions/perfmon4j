package org.perfmon4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


public abstract class ThreadTracesBase {
	private static final Logger logger = LoggerFactory.initLogger(ThreadTracesBase.class);
	
	private final int maxElements;
    private Map<String, PointerToHead> threadDataMap = new HashMap<String, PointerToHead>();
    private ExternalThreadTraceConfig externalConfig = null;
    boolean active = false;
    
    public ThreadTracesBase(int maxElements) {
    	this.maxElements = maxElements;
    }
    
    /**
     * Prevent recursion...  This can happen if multiple instrumentation
     * agents are active...
     */
     
  
    private boolean insideThreadTracesOnStack = false;
    
    protected abstract void incrementActiveThreadTraceFlag();
    protected abstract void decrementActiveThreadTraceFlag();
  
    
    boolean isActive() {
    	return active;
    }
    
    protected void start(String monitorName, int maxDepth, int minDurationToCapture, long startTime) {
        ThreadTraceData rootElement = new ThreadTraceData(new UniqueThreadTraceTimerKey(monitorName), startTime);
        threadDataMap.put(monitorName, new PointerToHead(rootElement, maxDepth, minDurationToCapture));
        active = true;
        incrementActiveThreadTraceFlag();
    }
    
    protected ThreadTraceData stop(String monitorName) {
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
        decrementActiveThreadTraceFlag();
        
        return result;
    }
    
    protected UniqueThreadTraceTimerKey enterCheckpoint(String timerMonitorName, long startTime) {
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
                        	head.numElements++;
                        	if (head.numElements > maxElements) {
                        		head.rootElement.setOverflow(true);
                        		head.checkpointsIgnoredDueToOverflow++;
                        	} else {
                        		head.topOfTheStackElement = new ThreadTraceData(result, head.topOfTheStackElement, startTime);
                        	}
                        }
                    }
                }
        	} finally {
        		insideThreadTracesOnStack = false;
        	}
    	}
        return result;
    }

    private boolean purgeMissNestedElement(ThreadTraceData data, UniqueThreadTraceTimerKey key) {
    	boolean result = false;
    	
    	ThreadTraceData parent = data;
    	while (parent != null && !result) {
    		data = parent.getParent();
    		if (data != null && key.equals(data.getKey())) {
    			result = true;
    			data.seperateFromParent(true);
    		} else {
    			parent = data;
    		}
    	}
    	
    	return result;
    }
    
    
    protected void exitCheckpoint(UniqueThreadTraceTimerKey timerKey) {
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
                        } else if(head.checkpointsIgnoredDueToOverflow > 0) {
                        	head.checkpointsIgnoredDueToOverflow--;
                        } else {
                            // Make sure we never get to the root element...
                            // That is removed via stop...  If for some reason we became 
                            // unbalanced and we are at the root element DO NOTHING.
                            if (!head.isTopOfTheStackOnRootElement()) {
                            	boolean missNestedElementPurged = false;
                                ThreadTraceData exitTraceData = head.topOfTheStackElement;
                                if (!timerKey.equals(exitTraceData.getKey())) {
                                	missNestedElementPurged = purgeMissNestedElement(exitTraceData, timerKey);
                                	head.numElements--;
                                }
                                if (!missNestedElementPurged) {
                                    exitTraceData.stop();
                                    head.topOfTheStackElement = exitTraceData.getParent();
                                    if (head.minDuratinToCapture > 0 &&
                                        head.minDuratinToCapture > (exitTraceData.getEndTime() - exitTraceData.getStartTime())) {
                                    	head.numElements--;
                                        exitTraceData.seperateFromParent();
                                    }
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
	
    public static class UniqueThreadTraceTimerKey {
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
        
        String getMonitorName() {
        	return monitorName;
        }
        
        @Override
		public int hashCode() {
			return hashString.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			return hashString.equals(((UniqueThreadTraceTimerKey)obj).hashString);
		}
    }

    
    static public class ActiveThreadTraceFlag {
    	private volatile int activeThreadTraceCount = 0;
    	
    	void incActive() {
    		activeThreadTraceCount++;
    	}
    	void decActive() {
    		activeThreadTraceCount--;
    	}
    	
    	public boolean isActive() {
    		return activeThreadTraceCount > 0;
    	}
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
        private boolean hadOverflow;
        private int numElements = 0;
        
        int checkpointsIgnoredDueToMaxDepth = 0; 
        int checkpointsIgnoredDueToOverflow = 0;
        
        
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

}
