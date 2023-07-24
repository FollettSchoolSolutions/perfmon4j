package org.perfmon4j;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.instrument.PerfMonTimerTransformer;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;

public class POJOSnapShotRegistry {
	private static final POJOSnapShotRegistry singleton = new POJOSnapShotRegistry();
	
	private final Object entriesLockToken = new Object();
	private final Map<String, RegistryEntry> entries = new HashMap<String, POJOSnapShotRegistry.RegistryEntry>(); 
	private static final POJOInstance[] EMPTY_INSTANCES = new POJOInstance[] {};
	private static final SnapShotGenerator generator = PerfMonTimerTransformer.snapShotGenerator;
	
	public static POJOSnapShotRegistry getSingleton() {
		return singleton;
	}

	POJOSnapShotRegistry() {
	}

	public void register(Object snapShotPOJO) throws GenerateSnapShotException {
		register(snapShotPOJO, null, true);
	}
	
	public void register(Object snapShotPOJO, boolean useWeakReference) throws GenerateSnapShotException {
		register(snapShotPOJO, null, useWeakReference);
	}
	
	public void register(Object snapShotPOJO, String instanceName) throws GenerateSnapShotException {
		register(snapShotPOJO, instanceName, true);
	}

	public void register(Object snapShotPOJO, String instanceName, boolean useWeakReference) throws GenerateSnapShotException {
		synchronized(entriesLockToken) {
			final String className = snapShotPOJO.getClass().getName();
			
			RegistryEntry entry = entries.get(className);
			if (entry == null) {
				entry = new RegistryEntry(className, generator.generateBundleForPOJO(snapShotPOJO.getClass()));
				entries.put(className, entry);
			}
			entry.addOrUpdateInstance(snapShotPOJO, instanceName, useWeakReference);
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
		private final Map<Object, Object> instances = new HashMap<Object, Object>();
		private final Bundle snapShotBundle;
		
		RegistryEntry(String className, Bundle snapShotBundle) {
			this.className = className;
			this.snapShotBundle = snapShotBundle;
		}

		public String getClassName() {
			return className;
		}
		
		void addOrUpdateInstance(Object pojo, String instanceName, boolean weakReference) {
			synchronized (instancesLockToken) {
				instances.put(pojo, weakReference ? new WeakPOJOInstance(instanceName, snapShotBundle, pojo) 
						: new StrongPOJOInstance(instanceName, snapShotBundle, pojo));
			}
		}
		
		boolean removeInstance(Object pojo) {
			synchronized (instancesLockToken) {
				instances.remove(pojo);
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
		private final Bundle snapShotBundle;
		
		protected POJOInstance(String instanceName, Bundle snapShotBundle) {
			this.instanceName = instanceName;
			this.snapShotBundle = snapShotBundle;
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
		
	    public Bundle getSnapShotBundle() {
			return snapShotBundle;
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
		
		StrongPOJOInstance(String instanceName, Bundle snapShotBundle, Object pojo) {
			super(instanceName, snapShotBundle);
			this.pojo = pojo;
		}

		public Object getPOJO() {
			return pojo;
		}
	}
	
	private static class WeakPOJOInstance extends POJOInstance {
		private final WeakReference<Object> pojoReference;
		
		WeakPOJOInstance(String instanceName, Bundle snapShotBundle, Object pojo) {
			super(instanceName, snapShotBundle);
			this.pojoReference = new WeakReference<Object>(pojo);
		}

		public Object getPOJO() {
			return pojoReference.get();
		}
	}
}
