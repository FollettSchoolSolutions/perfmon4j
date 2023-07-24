package org.perfmon4j;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;

public class POJOSnapShotRegistry {
	private static final POJOSnapShotRegistry singleton = new POJOSnapShotRegistry();
	
	private final Object entriesLockToken = new Object();
	private final Map<String, RegistryEntry> entries = new HashMap<String, POJOSnapShotRegistry.RegistryEntry>(); 
	private static final POJOInstance[] EMPTY_INSTANCES = new POJOInstance[] {};
	private static final SnapShotGenerator generator = new JavassistSnapShotGenerator();
	
	public static POJOSnapShotRegistry getSingleton() {
		return singleton;
	}

	POJOSnapShotRegistry() {
	}

	public void register(Object snapShotPOJO) throws GenerateSnapShotException {
		register(snapShotPOJO, true);
	}

	public void register(Object snapShotPOJO, boolean weakReference) throws GenerateSnapShotException {
		Bundle bundle = generator.generateBundle(snapShotPOJO.getClass());
	
		
		
		synchronized(entriesLockToken) {
			final String className = snapShotPOJO.getClass().getName();
			
			RegistryEntry entry = entries.get(className);
			if (entry == null) {
				entry = new RegistryEntry(className);
				entries.put(className, entry);
			}
			entry.addOrUpdateInstance(snapShotPOJO, weakReference);
		}
	}
	
	public void deRegister(Object snapShotPOJO) {
		synchronized(entriesLockToken) {
			final String className = snapShotPOJO.getClass().getName();
			
			RegistryEntry entry = entries.get(className);
			if (entry != null) {
				if (entry.removeInstance(snapShotPOJO)) {
					entries.remove(className);
				}
			}
		}
	}
	
	
	public static class RegistryEntry {
		private final String className;
		private final Object instancesLockToken = new Object();
		private final Map<String, Object> instances = new HashMap<String, Object>(); 
		
		RegistryEntry(String className) {
			this.className = className;
		}

		public String getClassName() {
			return className;
		}
		
		void addOrUpdateInstance(Object pojo, boolean weakReference) {
			final String instanceName = "bogus"; // = pojo.getInstanceName();
			synchronized (instancesLockToken) {
				instances.put(instanceName, weakReference ? new WeakPOJOInstance(instanceName, pojo) : new StrongPOJOInstance(instanceName, pojo));
			}
		}
		
		boolean removeInstance(Object pojo) {
			String instanceName = "bogus";
			synchronized (instancesLockToken) {
				instances.remove(instanceName);
				return instances.isEmpty();
			}
		}
		
		public POJOInstance[] getInstances() {
			synchronized (instancesLockToken) {
				return instances.values().toArray(EMPTY_INSTANCES);
			}
		}
	}

	public POJOInstance[] getInstances(String className) {
		RegistryEntry entry = null;
		
		synchronized (entriesLockToken) {
			entry = entries.get(className);
		}
		
		return entry != null ? entry.getInstances() : EMPTY_INSTANCES;
	}
	
	public abstract static class POJOInstance {
		private static final AtomicLong nextPojoID = new AtomicLong(); 
		
		private final long pojoID = nextPojoID.incrementAndGet(); 
		private final String instanceName;
		
		protected POJOInstance(String instanceName) {
			this.instanceName = instanceName;
		}
		
		public String getInstanceName() {
			return instanceName;
		}
		
		abstract Object getPOJO();
		
	    boolean isActive() {
	    	return getPOJO() != null;
	    }
		
		public long getPojoID() {
			return pojoID;
		}

	    public SnapShotData initSnapShot(long currentTimeMillis) {
	    	return null;
	    }
	    
	    
	/*----------------------------------------------------------------------------*/    
	    /**
	     * @param data - Returns SnapShotData returned by initSnapShot() or
	     * null if initSnapShot() did not return a SnapShotData.
	     * @param currentTimeMillis - Current System Time
	     * @return - Return a SnapShotData instance that will be passed to the appender.
	     */
	    public SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis) {
	    	return null;
	    }
		
		@Override
		public int hashCode() {
			return Objects.hash(instanceName, Long.valueOf(pojoID));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			POJOInstance other = (POJOInstance) obj;
			return Objects.equals(instanceName, other.instanceName) && pojoID == other.pojoID;
		}
	}
	
	private static class StrongPOJOInstance extends POJOInstance {
		private final Object pojo;
		
		StrongPOJOInstance(String instanceName, Object pojo) {
			super(instanceName);
			this.pojo = pojo;
		}

		public Object getPOJO() {
			return pojo;
		}
	}
	
	private static class WeakPOJOInstance extends POJOInstance {
		private final WeakReference<Object> pojoReference;
		
		WeakPOJOInstance(String instanceName, Object pojo) {
			super(instanceName);
			this.pojoReference = new WeakReference<Object>(pojo);
		}

		public Object getPOJO() {
			return pojoReference.get();
		}
	}
	
	
//    private static class MultiMonitorTimerTask extends FailSafeTimerTask {
//    	private final Map<SnapShotManager, V>
//    	private final WeakReference<RegistryEntry> entry;
//        private final AppenderID appenderID;
//        private final long intervalMillis;
//        private final boolean usePriorityTimer;
//        
//        MultiMonitorTimerTask(RegistryEntry entry, AppenderID appenderID, boolean usePriorityTimer) throws InvalidConfigException {
//            this.entry = new WeakReference(entry);
//            this.appenderID = appenderID;
//            this.usePriorityTimer = usePriorityTimer;
//            intervalMillis = appenderID.getIntervalMillis();
//            Timer timerToUse = usePriorityTimer ? PerfMon.getPriorityTimer() : PerfMon.getUtilityTimer();
//            long now = MiscHelper.currentTimeWithMilliResolution();
//            timerToUse.schedule(this, PerfMon.roundInterval(now, intervalMillis));
//        }
//        
//        public void failSafeRun() {
//            Appender appender = Appender.getAppender(appenderID);
//            SnapShotMonitor monitor = (SnapShotMonitor)monitorReference.get();
//
//            boolean cancelTask = true;
//            if (monitor != null && monitor.isActive() && PerfMon.isConfigured()) {
//                try {
//                    data = monitor.takeSnapShot(data, MiscHelper.currentTimeWithMilliResolution());
//                } catch (Exception ex) {
//                    data = null;
//                    logger.logError("Error taking snap shot for monitor: " + monitor.getName(), ex);
//                }
//                if (appender != null) {
//                    cancelTask = false;
//                    if (data != null) {
//                        data.setName(monitor.getName());
//                        try {
//                            appender.appendData(data);
//                        } catch (Exception ex) {
//                            logger.logError("Error appending snapshot data for monitor: " + monitor.getName(), ex);
//                        }
//                    }
//                    try {
//                    	// Reschedule task
//                        new MonitorTimerTask(monitor, appenderID,  usePriorityTimer);
//                    } catch (Exception ex) {
//                        data = null;
//                        logger.logError("Error in initSnapShot for monitor: " + monitor.getName(), ex);
//                    }
//                }
//            } 
//            
//            if (cancelTask) {
//                if (monitor != null) {
//                    synchronized(monitor.lockToken) {
//                        monitor.map.remove(appenderID);
//                    }
//                }
//            }
//        }
//    }
	
}
