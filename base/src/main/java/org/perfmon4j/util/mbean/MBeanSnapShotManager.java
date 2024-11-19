package org.perfmon4j.util.mbean;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.perfmon4j.POJOSnapShotRegistry;
import org.perfmon4j.PerfMon;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.mbean.QueryToInstanceSnapShot.MBeanInstanceToSnapShotRegistrar;
import org.perfmon4j.util.mbean.QueryToInstanceSnapShot.MBeanQueryRunner;

public class MBeanSnapShotManager implements MBeanInstanceToSnapShotRegistrar, MBeanQueryRunner {
	final private static Logger logger = LoggerFactory.initLogger(MBeanSnapShotManager.class); 
	private final Map<MBeanQuery, QueryToInstanceSnapShot> activeMBeanInstances = new HashMap<>();
	private final POJOSnapShotRegistry registry = POJOSnapShotRegistry.getSingleton();
	final private static long RELOAD_SECONDS = Long.getLong(MBeanSnapShotManager.class.getName() + ".RELOAD_SECONDS", TimeUnit.SECONDS.toSeconds(60));
	
	public MBeanSnapShotManager() {
		logger.logDebug("Scheduling timerTask. Will run every " + RELOAD_SECONDS + " seconds.");
		PerfMon.utilityTimer.schedule(new TimerTaskImpl(this), TimeUnit.SECONDS.toMillis(RELOAD_SECONDS), TimeUnit.SECONDS.toMillis(RELOAD_SECONDS));
	}
	
	public synchronized void deInit() {
		logger.logDebug("deInit()");
		for (QueryToInstanceSnapShot snapShot : activeMBeanInstances.values()) {
			snapShot.deInit();
		}
		activeMBeanInstances.clear();
	}
	
	public synchronized void updateSnapShotsFromConfig(Set<MBeanQuery> updateQuerysFromConfig ) {
		logger.logDebug("updateSnapShotsFromConfig()");
        // First clear out any activelyRegisteredMBeanInstances that are no longer included in the configuration;
        for (MBeanQuery activeKey : activeMBeanInstances.keySet().toArray(new MBeanQuery[]{})) {
        	if (!updateQuerysFromConfig.contains(activeKey)) {
        		QueryToInstanceSnapShot snapShot = activeMBeanInstances.get(activeKey);
        		snapShot.deInit();
        		activeMBeanInstances.remove(activeKey);
        	}
        }
        
        // Now create MBeanPOJOs for each MBeanQuery that has not already been configured and registered.
        for (MBeanQuery queryFromConfig : updateQuerysFromConfig) {
        	if (!activeMBeanInstances.containsKey(queryFromConfig)) {
    			QueryToInstanceSnapShot snapShot = QueryToInstanceSnapShot.newQueryToInstanceSnapShot(queryFromConfig, this, this);
    			snapShot.refresh();
    			activeMBeanInstances.put(queryFromConfig, snapShot);
        	}
        }
	}
	
	private synchronized void refreshSnapShots() {
		logger.logDebug("refreshSnapShots()");
        for (QueryToInstanceSnapShot snapShot : activeMBeanInstances.values()) {
        	snapShot.refresh(); // Check to see if a new MBean has been registered matching any of our querys
        }
	}
	
		
	private static class TimerTaskImpl extends TimerTask {
		private final WeakReference<MBeanSnapShotManager> managerReference;
		
		TimerTaskImpl(MBeanSnapShotManager manager) {
			this.managerReference = new WeakReference<MBeanSnapShotManager>(manager);
		}

		@Override
		public void run() {
			MBeanSnapShotManager manager = managerReference.get();
			if (manager != null) {
				try {
					manager.refreshSnapShots();
				} catch (ThreadDeath td) {
					throw td;
				} catch (Throwable throwable) {
					logger.logWarn("Error in MBeanSnapShotManager timer task", throwable);
				}
			} else {
				logger.logDebug("Cancelling " + MBeanSnapShotManager.class.getSimpleName() + " Timer task.");
				this.cancel();
			}
		}
	}
	
	
	@Override
	public void registerSnapShot(MBeanInstance instance) {
		try {
			registry.register(instance);
		} catch (GenerateSnapShotException e) {
			logger.logWarn("Error registering MBeanSnapShot: " + instance);
		}
	}

	@Override
	public void deRegisterSnapShot(MBeanInstance instance) {
		registry.deRegister(instance);
	}
	
	@Override
	public MBeanQueryResult doQuery(MBeanQuery query) throws MBeanQueryException {
		MBeanQueryEngine queryEngine = new MBeanQueryEngine(new MBeanServerFinderImpl(query.getDomain()));
		return queryEngine.doQuery(query);
	}	
}
