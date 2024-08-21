package api.org.perfmon4j.agent.util;

/**
 * The SingletonTracker functionality in the agent is inactive
 * by default.  To activate you must:
 * 	1) Load the perfmon4j javaagent on JVM start.
 *  2) Launch the JVM with one (or both) of the following system properties: 
 *  	org.perfmon4j.util.SingletonTracker.enabled=true
 *  			OR
 *  	api.org.perfmon4j.agent.util.SingletonTracker.enabled=true
 */
public interface SingletonTracker {
	public static SingletonTracker getSingleton() {
		return SingletonTrackerImpl.singleton;
	}
	public static boolean isAttachedToAgent() {
		return SingletonTrackerImpl.isAttachedToAgent();
	}
	
	public SingletonTracker register(Class<?> clazz);
	public boolean isEnabled();
}
