package org.perfmon4j.emitter;

import java.util.Timer;

import org.perfmon4j.GenericItemRegistry;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.util.FailSafeTimerTask;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class EmitterRegistry extends GenericItemRegistry<Emitter> {
	private static final Logger logger = LoggerFactory.initLogger(EmitterRegistry.class);
	
	private static final EmitterRegistry singleton = new EmitterRegistry();
	private long defaultTimerIntervalMillis = 1000 * 60;
	private static final Timer runTimer = new Timer("PerfMon4j.EmitterThread", true);
	
	public static EmitterRegistry getSingleton() {
		return singleton;
	}
	
	private EmitterRegistry() {
	}
	
	public long getDefaultTimerIntervalMillis() {
		return defaultTimerIntervalMillis;
	}
	
	public void setDefaultTimerIntervalMillis(long defaultTimerIntervalMillis) {
		this.defaultTimerIntervalMillis = defaultTimerIntervalMillis; 
	}
	
	@Override
	public EmitterRegistryEntry buildRegistryEntry(Emitter item) throws GenerateSnapShotException {
		return new EmitterRegistryEntry(this, getEffectiveClassName(item));
	}

	@Override
	public ItemInstance<Emitter>[] getInstances(String className) {
		EmitterRegistryEntry entry = (EmitterRegistryEntry)lookupItemRegistry(className);
		return entry != null ? entry.getInstances() : new EmitterInstance[]{};		
	}

	public static class EmitterRegistryEntry extends GenericItemRegistry.ItemRegistry<Emitter> {
		private final EmitterRegistry registry;
		private final EmitterBridge bridge; 
		
		EmitterRegistryEntry(EmitterRegistry registry, String className) {
			super(className);
			this.bridge = new EmitterBridge(className);
			this.registry = registry;
			
			logger.logDebug("Created registryEntry: " + this);
		}

		@Override
		protected EmitterInstance buildItemInstance(Emitter item, String instanceName,
				boolean weakReference) {
			return new EmitterInstance(registry, item, instanceName, weakReference, bridge);
		}

		@Override
		public EmitterInstance[] getInstances() {
			synchronized (instancesLockToken) {
				return instances.values().toArray(new EmitterInstance[]{});
			}		
		}
	}
	
	public static class EmitterInstance extends GenericItemRegistry.ItemInstance<Emitter> implements Runnable {
		private final Emitter item;
		private final EmitterController itemController;
		private final FailSafeTimerTask timerTask;
		
		protected EmitterInstance(EmitterRegistry registry, Emitter item, String instanceName, boolean weakReference, EmitterController controller) {
			super(item, instanceName, weakReference);
			this.item = item;
			this.itemController = controller;

			logger.logDebug("Creating itemInstance: " + this);
			item.acceptController(controller);
			
			if (itemIsRunnable(item)) {
				timerTask = new InstanceFailSafeTimerTask(this);
				long intervalMillis = registry.getDefaultTimerIntervalMillis();
				EmitterRegistry.runTimer.schedule(timerTask, intervalMillis, intervalMillis);
				logger.logDebug("Scheduled timer for: " + this + " (interval=" + MiscHelper.getMillisDisplayable(intervalMillis) + ")");
			} else {
				timerTask = null;
			}
		}

		@Override
		public void run() {
			if (itemController.isActive()) {
				logger.logVerbose("Invoking run for: " + this);
				((Runnable)item).run();
			} else {
				logger.logVerbose("Skipping run for: " + this + " (No active appender)");
			}
		}

		@Override
		protected void deInit() {
			if (timerTask != null) {
				timerTask.cancel();
				logger.logDebug("Cancelled timer for: " + this);
			}
			super.deInit();
		}
		
		private boolean itemIsRunnable(Emitter item) {
			boolean result = item instanceof Runnable;
			
			if (item instanceof ConditionalRunnable) {
				result = ((ConditionalRunnable)item).delegatedEmitterImplementsRunnable();
			}
			return result;
		}
	}
	
	private static final class InstanceFailSafeTimerTask extends FailSafeTimerTask {
		private final EmitterInstance owner;

		InstanceFailSafeTimerTask(EmitterInstance owner) {
			this.owner = owner;
		}
		
		@Override
		public void failSafeRun() {
			owner.run();
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
			EmitterController monitor = EmitterMonitor.lookUpEmitterMonitor(className);
			return monitor != null ? monitor : EmitterController.NO_OP_CONTROLLER;
		}
	}
	
	public static interface ConditionalRunnable extends Runnable {
		boolean delegatedEmitterImplementsRunnable();
	}
}
