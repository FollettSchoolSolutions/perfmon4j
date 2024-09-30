package org.perfmon4j;

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
		
		if (item instanceof MBeanInstance) {
			bundle = new Bundle(MBeanInstanceData.class, null, false);
		} else {
			bundle = generator.generateBundleForPOJO(item.getClass());
		}
		
		return new POJORegistryEntry(item.getClass().getName(), bundle);
	}
	
	@Override
	public POJOInstance[] getInstances(String className) {
		POJORegistryEntry entry = (POJORegistryEntry)lookupItemRegistry(className);
		return entry != null ? entry.getInstances() : new POJOInstance[]{};	
	}
	
	public static class POJORegistryEntry extends GenericItemRegistry.ItemRegistry<Object> {
		private final Bundle snapShotBundle;
		
		POJORegistryEntry(String className, Bundle snapShotBundle) {
			super(className);
			this.snapShotBundle = snapShotBundle;
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
