package org.perfmon4j.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Package level class.
 */
class SingletonTrackerImpl implements SingletonTracker {
	private static final boolean TRACK_SINGLETONS = Boolean.getBoolean(SINGLETON_TRACKER_ENABLED_KEY) 
			|| Boolean.getBoolean(ALTERNATE_SINGLETON_TRACKER_ENABLED_KEY);
	
    private static final Logger logger = LoggerFactory.initLogger(SingletonTrackerImpl.class);
    private final Set<String> registeredSingletons;
    
    /**
     * If the SingletonTrackerImpl is loaded by multiple classLoaders
     * each classLoader will have its own trackerSingleton. However,
     * since we share the Set of registered singletons within the
     * System.properties they will all work together as a virtual
     * JVM wide singleton. 
     */
    private static SingletonTracker singleton = null;
   
    @SuppressWarnings("unchecked")
	static SingletonTracker getSingleton() {
    	if (!TRACK_SINGLETONS) {
    		if (singleton == null) {
    			logger.logInfo("*********** SingletonTracker Disabled *************");
    			singleton = new SingletonTrackerImpl(null);
    		} 
    	} else {
	    	synchronized (System.class) {
	    		if (singleton == null) {
	    			logger.logInfo("*********** SingletonTracker Enabled *************");
	    			
	    			// Try to get the instance from system properties
			    	Set<String> setRegisteredSingletons = (Set<String>)System.getProperties().get(SINGLETON_TRACKER_REGISTERED_SINGLETONS_KEY);
			
			        if (setRegisteredSingletons == null) {
			        	// Double-checked locking to ensure only one instance is created
			        	setRegisteredSingletons = (Set<String>) System.getProperties().get(SINGLETON_TRACKER_REGISTERED_SINGLETONS_KEY);
		                if (setRegisteredSingletons == null) {
		                	setRegisteredSingletons = Collections.synchronizedSet(new HashSet<String>());
		                    System.getProperties().put(SINGLETON_TRACKER_REGISTERED_SINGLETONS_KEY, setRegisteredSingletons);
		                }
			        }
			        singleton = new SingletonTrackerImpl(setRegisteredSingletons);
	    		}
	    	}
    	}
    	return singleton;
    }
    
    private SingletonTrackerImpl(Set<String> setRegisteredSingletons) {
    	this.registeredSingletons = setRegisteredSingletons;
    }
    
    @Override
    public SingletonTrackerImpl register(Class<?> clazz) {
    	if (registeredSingletons != null) {
    		synchronized(registeredSingletons) {
	    		String className = clazz.getName();
	    		if (registeredSingletons.contains(className)) {
	    			logger.logError("**** Duplicate Singleton Detected! **** Existing singleton class '" + className + "' reloaded by classLoader: " + clazz.getClassLoader());
	    		} else {
	    			logger.logInfo("**** SingletonTracker registered singleton '" + className + "' ****. Loaded by classLoader: " + clazz.getClassLoader().getName());
	    			registeredSingletons.add(className);
	    		}
    		}
    	}
    	return this;
    }

	@Override
	public boolean isEnabled() {
		return registeredSingletons != null;
	}
}
