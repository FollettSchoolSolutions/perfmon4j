package org.perfmon4j.emitter;

import org.perfmon4j.GenericItemRegistry;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;

public class EmitterRegistry extends GenericItemRegistry<Emitter> {
	private static final EmitterRegistry singleton = new EmitterRegistry();
	
	public static EmitterRegistry getSingleton() {
		return singleton;
	}
	
	private EmitterRegistry() {
	}
	
	@Override
	public EmitterRegistryEntry buildRegistryEntry(Emitter item) throws GenerateSnapShotException {
		return new EmitterRegistryEntry(item.getClass().getName());
	}

	@Override
	public ItemInstance<Emitter>[] getInstances(String className) {
		EmitterRegistryEntry entry = (EmitterRegistryEntry)lookupItemRegistry(className);
		return entry != null ? entry.getInstances() : new EmitterInstance[]{};		
	}

	public static class EmitterRegistryEntry extends GenericItemRegistry.ItemRegistry<Emitter> {
		private final EmitterBridge bridge; 
		
		EmitterRegistryEntry(String className) {
			super(className);
			bridge = new EmitterBridge(className);
		}

		@Override
		protected EmitterInstance buildItemInstance(Emitter item, String instanceName,
				boolean weakReference) {
			item.acceptController(bridge);
			return new EmitterInstance(item, instanceName, weakReference);
		}

		@Override
		public EmitterInstance[] getInstances() {
			synchronized (instancesLockToken) {
				return instances.values().toArray(new EmitterInstance[]{});
			}		
		}
	}
	
	public static class EmitterInstance extends GenericItemRegistry.ItemInstance<Emitter>{
		protected EmitterInstance(Emitter item, String instanceName, boolean weakReference) {
			super(item, instanceName, weakReference);
		}
	}
	
	private static class EmitterBridge implements EmitterController {
		private final String className;
		
		EmitterBridge(String className) {
			this.className = className;
		}
		
		@Override
		public void emit(EmitterData data) {
			getControllerImpl().emit(data);		
		}

		@Override
		public EmitterData initData() {
			return getControllerImpl().initData();		
		}

		@Override
		public EmitterData initData(String instanceName) {
			return getControllerImpl().initData(instanceName);		
		}

		@Override
		public EmitterData initData(long timestamp) {
			return getControllerImpl().initData(timestamp);		
		}

		@Override
		public EmitterData initData(String instanceName, long timeStamp) {
			return getControllerImpl().initData(instanceName, timeStamp);
		}

		@Override
		public boolean isActive() {
			return getControllerImpl().isActive();
		}
		
		public EmitterController getControllerImpl() {
			EmitterController monitor = EmitterSnapShotMonitor.lookUpEmitterMonitor(className);
			return monitor != null ? monitor : EmitterController.NO_OP_CONTROLLER;
		}
	}
}
