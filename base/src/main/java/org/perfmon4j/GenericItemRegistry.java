package org.perfmon4j;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;

public abstract class GenericItemRegistry <T>{
	private final Object entriesLockToken = new Object();
	private final Map<String, ItemRegistry<T>> entries = new HashMap<String, ItemRegistry<T>>(); 
	
	protected GenericItemRegistry() {
	}

	public void register(T item) throws GenerateSnapShotException {
		register(item, null, true);
	}
	
	public void register(T item, boolean useWeakReference) throws GenerateSnapShotException {
		register(item, null, useWeakReference);
	}
	
	public void register(T item, String instanceName) throws GenerateSnapShotException {
		register(item, instanceName, true);
	}

	public void register(T item, String instanceName, boolean useWeakReference) throws GenerateSnapShotException {
		synchronized(entriesLockToken) {
			final String className = item.getClass().getName();
			
			ItemRegistry<T> entry = entries.get(className);
			if (entry == null) {
				entry = buildRegistryEntry(item);
				entries.put(className, entry);
			}
			entry.addOrUpdateInstance(item, instanceName, useWeakReference);
		}
	}
	
	public abstract ItemRegistry<T> buildRegistryEntry(T item);
//				entry = new RegistryEntry(className, generator.generateBundleForPOJO(item.getClass()));
	
	public void deRegister(T item) {
		deRegister(item, null);
	}
	
	public void deRegister(T item, String instanceName) {
		synchronized(entriesLockToken) {
			final String className = item.getClass().getName();
			
			ItemRegistry<T> entry = entries.get(className);
			if (entry != null) {
				if (entry.removeInstance(item, instanceName)) {
					entries.remove(className);
				}
			}
		}
	}
	
	public abstract static class ItemRegistry<T> {
		private final String className;
		private final Object instancesLockToken = new Object();
		private final Map<ItemInstanceKey, ItemInstance<T>> instances = new HashMap<ItemInstanceKey, ItemInstance<T>>();
		
		ItemRegistry(String className /*, Bundle snapShotBundle*/) {
			this.className = className;
		}

		public String getClassName() {
			return className;
		}
		
		void addOrUpdateInstance(T pojo, String instanceName, boolean weakReference) {
			synchronized (instancesLockToken) {
				instances.put(new ItemInstanceKey(pojo, instanceName), buildPOJOInstance(pojo, instanceName, weakReference)) ;
			}
		}
		
		abstract protected ItemInstance<T> buildPOJOInstance(T pojo, String instanceName, boolean weakReference);
		
		boolean removeInstance(Object pojo, String instanceName) {
			synchronized (instancesLockToken) {
				instances.remove(new ItemInstanceKey(pojo, instanceName));
				return instances.isEmpty();
			}
		}
		
		@SuppressWarnings("unchecked")
		public ItemInstance<T>[] getInstances() {
			synchronized (instancesLockToken) {
				return instances.values().toArray((ItemInstance<T>[])new ItemInstance<?>[]{});
			}
		}
	}

	@SuppressWarnings("unchecked")
	public ItemInstance<T>[] getInstances(String className) {
		ItemRegistry<T> entry = null;
		
		synchronized (entriesLockToken) {
			entry = entries.get(className);
		}
		
		return entry != null ? entry.getInstances() : (ItemInstance<T>[])new ItemInstance<?>[]{};
	}
	
	public static class ItemInstance<T> {
		private static final AtomicLong nextItemID = new AtomicLong(); 
		
		private final WeakReference<T> itemWeakReference;
		private final T itemStrongReference;
		private final long itemID = nextItemID.incrementAndGet(); 
		private final String instanceName;
		
		protected ItemInstance(String instanceName, T item, boolean weakReference) {
			this.instanceName = instanceName;
			if (weakReference) {
				itemWeakReference = new WeakReference<T>(item);
				itemStrongReference = null;
			} else {
				itemWeakReference = null;
				itemStrongReference = item;
			}
		}
		
		public String getInstanceName() {
			return instanceName;
		}
		
		T getItem() {
			return itemWeakReference != null ? itemWeakReference.get() : itemStrongReference;
		}
		
	    boolean isActive() {
	    	return getItem() != null;
	    }
		
		public long getItemID() {
			return itemID;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(instanceName, Long.valueOf(itemID));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ItemInstance<?> other = (ItemInstance<?>) obj;
			return Objects.equals(instanceName, other.instanceName) && itemID == other.itemID;
		}
	}
	
	
	static class ItemInstanceKey {
		private final String key;
		
		ItemInstanceKey(Object item, String instanceName) {
			key = item.getClass().getName() + (instanceName == null ? "" : ("$$." + instanceName));
		}

		@Override
		public int hashCode() {
			return Objects.hash(key);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ItemInstanceKey other = (ItemInstanceKey) obj;
			return Objects.equals(key, other.key);
		}
	}
}
