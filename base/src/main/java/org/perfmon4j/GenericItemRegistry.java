package org.perfmon4j;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;

public abstract class GenericItemRegistry <T>{
	protected final Object entriesLockToken = new Object();
	protected final Map<String, ItemRegistry<T>> entries = new HashMap<String, ItemRegistry<T>>();
	
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
			final String className = getEffectiveClassName(item);
			
			ItemRegistry<T> entry = entries.get(className);
			if (entry == null) {
				entry = buildRegistryEntry(item);
				entries.put(className, entry);
			}
			entry.addOrUpdateInstance(item, instanceName, useWeakReference);
		}
	}
	
	public void deRegister(T item) {
		deRegister(item, null);
	}
	
	public void deRegister(T item, String instanceName) {
		synchronized(entriesLockToken) {
			final String className = getEffectiveClassName(item);
			
			ItemRegistry<T> entry = entries.get(className);
			if (entry != null) {
				if (entry.removeInstance(item, instanceName)) {
					entries.remove(className);
				}
			}
		}
	
	}
	
	protected ItemRegistry<T> lookupItemRegistry(String className) {
		ItemRegistry<T> entry = null;
		
		synchronized (entriesLockToken) {
			entry = entries.get(className);
		}
		
		return entry;
	}
	
	public abstract ItemRegistry<T> buildRegistryEntry(T item) throws GenerateSnapShotException;

	public abstract ItemInstance<T>[] getInstances(String className);

	
	public abstract static class ItemRegistry<T> {
		private final String className;
		protected final Object instancesLockToken = new Object();
		protected final Map<ItemInstanceKey, ItemInstance<T>> instances = new HashMap<ItemInstanceKey, ItemInstance<T>>();
		
		protected ItemRegistry(String className /*, Bundle snapShotBundle*/) {
			this.className = className;
		}

		public String getClassName() {
			return className;
		}
		
		void addOrUpdateInstance(T pojo, String instanceName, boolean weakReference) {
			synchronized (instancesLockToken) {
				ItemInstance<T> itemInstance = buildItemInstance(pojo, instanceName, weakReference);
				instances.put(itemInstance.getKey(), itemInstance) ;
			}
		}
		
		protected abstract ItemInstance<T> buildItemInstance(T item, String instanceName, boolean weakReference);
		
		boolean removeInstance(Object item, String instanceName) {
			boolean isEmpty = false;
			ItemInstance<T> removedItemInstance = null;
			synchronized (instancesLockToken) {
				removedItemInstance = instances.remove(new ItemInstanceKey(item, instanceName));
				isEmpty = instances.isEmpty();
			}
			
			if (removedItemInstance != null) {
				removedItemInstance.deInit();
			}
			
			return isEmpty;
		}
		
		public abstract ItemInstance<T>[] getInstances();

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + " [className=" + className + "]";
		}
	}
	
	public static class ItemInstance<T> {
		private static final AtomicLong nextItemID = new AtomicLong(); 
		
		private final WeakReference<T> itemWeakReference;
		private final T itemStrongReference;
		private final long itemID = nextItemID.incrementAndGet(); 
		private final String instanceName;
		private final ItemInstanceKey key;
		
		protected ItemInstance(T item, String instanceName, boolean weakReference) {
			this.instanceName = instanceName;
			this.key = new ItemInstanceKey(item, instanceName);
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

		protected void deInit() {
		}
		
		public ItemInstanceKey getKey() {
			return key;
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

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + " [key=" + key + "]";
		}
	}
	
	
	static class ItemInstanceKey {
		private final String key;
		
		ItemInstanceKey(Object item, String instanceName) {
			key =  getEffectiveClassName(item) + (instanceName == null ? "" : ("$$." + instanceName));
//System.out.println("key=" + key);
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

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + " [key=" + key + "]";
		}
	}
	
	public static interface OverrideClassNameForWrappedObject {
		public String getEffectiveClassName();
	}
	
	public static String getEffectiveClassName(Object item) {
		String result = null;
		
		if (item instanceof OverrideClassNameForWrappedObject) {
			result = ((OverrideClassNameForWrappedObject)item).getEffectiveClassName();
		} else {
			result = item.getClass().getName();
		}
		
		return result;
	}
}
