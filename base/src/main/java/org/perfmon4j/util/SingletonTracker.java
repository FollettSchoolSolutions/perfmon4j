package org.perfmon4j.util;


public interface SingletonTracker {
	public static final String SINGLETON_TRACKER_REGISTERED_SINGLETONS_KEY = SingletonTracker.class.getName() + ".registeredSingletons.key";
	
	/**
	 * Setting either of the below system properties to true will enable the SingletonTracker.
	 */
	public static final String SINGLETON_TRACKER_ENABLED_KEY = SingletonTracker.class.getName() + ".enabled";
	public static final String ALTERNATE_SINGLETON_TRACKER_ENABLED_KEY =  "api.org.perfmon4j.agent.util.SingletonTracker.enabled";

	public static SingletonTracker getSingleton() {
		return SingletonTrackerImpl.getSingleton();
	}
	
	public SingletonTracker register(Class<?> clazz);
	public boolean isEnabled();
}
