package api.org.perfmon4j.agent.util;


/**
 * This class will execute in one of two modes:
 * 	Unattached - When running in a JVM that was not booted with the Perfmon4j instrumentation agent this class will 
 * 		execute the code declared within this source file.  Essentially it will be running in a non-operative state.
 * 	Attached - When this class is loaded in a JVM that was booted with the Perfmon4j instrumentation agent, The agent  will
 * 		re-write this class and it will be in an operating state.
 * 
 *  The code is instrumented in method JavassistRuntimeTimerInjector.attachAgentToSingletonTrackerAPIClass(). 
 * 
 */
class SingletonTrackerImpl implements SingletonTracker {
	static final SingletonTrackerImpl singleton = new SingletonTrackerImpl();
	
	private SingletonTrackerImpl() {
	}
	
	/**
     * When rewritten by the Perfmon4j agent this class will return
     * the a reference to the actual org.perfmon4j.util.SingletonTrackerImpl.
     * 
     * Remember to enable singleton tracking you must:
     * 	1) Load the perfmon4j javaagent.
     *  2) Launch the JVM with system property: org.perfmon4j.util.SingletonTracker.enabled=true
     * 
     * @return
     */	
	@Override
	public SingletonTrackerImpl register(Class<?> clazz) {
		return this;
	}

	/**
	 * Method will be rewritten by the Perfmon4j javaagent.
	 */
	@Override
	public boolean isEnabled() {
		return false;
	}
	
	
    /**
     * If true this class has been rewritten by the Perfmon4j agent.
     * @return
     */
    public static boolean isAttachedToAgent() {
    	return false;
    }
	
}
