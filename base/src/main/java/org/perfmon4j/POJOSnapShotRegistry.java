package org.perfmon4j;

import java.util.Objects;

import org.perfmon4j.instrument.PerfMonTimerTransformer;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;
import org.perfmon4j.util.mbean.MBeanInstance;
import org.perfmon4j.util.mbean.MBeanInstanceData;

public class POJOSnapShotRegistry extends GenericItemRegistry<Object> {
	private static final POJOSnapShotRegistry singleton = new POJOSnapShotRegistry();
	private static final SnapShotGenerator generator = PerfMonTimerTransformer.snapShotGenerator;
	
	public static POJOSnapShotRegistry getSingleton() {
		return singleton;
	}

	POJOSnapShotRegistry() {
	}

	@Override
	public ItemRegistry<Object> buildRegistryEntry(Object item) throws GenerateSnapShotException {
		Bundle bundle = null;
		Class<?> pojoClass = null;

		if (item instanceof MBeanInstance) {
			bundle = new Bundle(MBeanInstanceData.class, null, false);
		} else {
			pojoClass = item.getClass();
			bundle = generator.generateBundleForPOJO(pojoClass);
		}

		return new POJORegistryEntry(item.getClass().getName(), pojoClass, bundle);
	}

	@Override
	public POJOInstance[] getInstances(String className) {
		POJORegistryEntry entry = (POJORegistryEntry)lookupItemRegistry(className);
		return entry != null ? entry.getInstances() : new POJOInstance[]{};
	}

	public POJOInstance getInstance(String className, String instanceName) {
		for (POJOInstance instance : getInstances(className)) {
			if (Objects.equals(instance.getInstanceName(), instanceName)) {
				return instance;
			}
		}
		return null;
	}

	Bundle lookupSnapShotBundle(String className) {
		POJORegistryEntry entry = (POJORegistryEntry)lookupItemRegistry(className);
		return entry != null ? entry.getSnapShotBundle() : null;
	}

	public POJORegistryEntry lookupRegistryEntry(String className) {
		return (POJORegistryEntry)lookupItemRegistry(className);
	}

	public static class POJORegistryEntry extends GenericItemRegistry.ItemRegistry<Object> {
		private final Bundle snapShotBundle;
		private final Class<?> pojoClass;

		POJORegistryEntry(String className, Class<?> pojoClass, Bundle snapShotBundle) {
			super(className);
			this.pojoClass = pojoClass;
			this.snapShotBundle = snapShotBundle;
		}

		/**
		 * @return null when this entry wraps an MBeanInstance rather than a
		 * true POJO snapshot class.
		 */
		public Class<?> getPOJOClass() {
			return pojoClass;
		}

		Bundle getSnapShotBundle() {
			return snapShotBundle;
		}

		@Override
		protected POJOInstance buildItemInstance(Object item, String instanceName, boolean weakReference) {
			return new POJOInstance(item, instanceName, weakReference, snapShotBundle);
		}

		@Override
		public POJOInstance[] getInstances() {
			synchronized (instancesLockToken) {
				return instances.values().toArray(new POJOInstance[]{});
			}
		}
	}
	
	public static class POJOInstance extends GenericItemRegistry.ItemInstance<Object>{
		private final Bundle snapShotBundle;

		protected POJOInstance(Object item, String instanceName, boolean weakReference, Bundle snapShotBundle) {
			super(item, instanceName, weakReference);
			this.snapShotBundle = snapShotBundle;
		}

		Bundle getSnapShotBundle() {
			return snapShotBundle;
		}
	}
}
