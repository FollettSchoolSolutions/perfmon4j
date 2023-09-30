package org.perfmon4j.snapshot.emitter;

import org.perfmon4j.GenericItemRegistry;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;

public class SnapShotEmitterRegistry extends GenericItemRegistry<SnapShotEmitter> {
	private static final SnapShotEmitterRegistry singleton = new SnapShotEmitterRegistry();

	public static SnapShotEmitterRegistry getSingleton() {
		return singleton;
	}
	
	private SnapShotEmitterRegistry() {
	}
	
	@Override
	public EmitterRegistryEntry buildRegistryEntry(SnapShotEmitter item) throws GenerateSnapShotException {
		return new EmitterRegistryEntry(item.getClass().getName());
	}

	@Override
	public ItemInstance<SnapShotEmitter>[] getInstances(String className) {
		EmitterRegistryEntry entry = (EmitterRegistryEntry)lookupItemRegistry(className);
		return entry != null ? entry.getInstances() : new EmitterInstance[]{};		
	}
	
	public static class EmitterRegistryEntry extends GenericItemRegistry.ItemRegistry<SnapShotEmitter> {
		EmitterRegistryEntry(String className) {
			super(className);
		}

		@Override
		protected EmitterInstance buildItemInstance(SnapShotEmitter item, String instanceName,
				boolean weakReference) {
			return new EmitterInstance(item, instanceName, weakReference);
		}

		@Override
		public EmitterInstance[] getInstances() {
			synchronized (instancesLockToken) {
				return instances.values().toArray(new EmitterInstance[]{});
			}		
		}
	}
	
	public static class EmitterInstance extends GenericItemRegistry.ItemInstance<SnapShotEmitter>{

		protected EmitterInstance(SnapShotEmitter item, String instanceName, boolean weakReference) {
			super(item, instanceName, weakReference);
		}
	}
}
