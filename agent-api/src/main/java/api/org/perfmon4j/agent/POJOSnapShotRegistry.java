package api.org.perfmon4j.agent;

/**
 * This class will execute in one of two modes:
 * 	Unattached - When running in a JVM that was not booted with the Perfmon4j instrumentation agent this class will 
 * 		execute the code declared within this source file.  Essentially it will be running in a non-operative state.
 * 	Attached - When this class is loaded in a JVM that was booted with the Perfmon4j instrumentation agent, The agent  will
 * 		re-write this class and it will be in an operating state.
 * 
 *  The code is instrumented in method JavassistRuntimeTimerInjector.attachAgentToPOJOSnapShotRegistryAPIClass(). 
 * 
 */
public class POJOSnapShotRegistry {
	
	public static void register(Object snapShotPOJO) {
		register(snapShotPOJO, null, true);
	}
	
	public static void register(Object snapShotPOJO, boolean useWeakReference) {
		register(snapShotPOJO, null, useWeakReference);
	}
	
	public static void register(Object snapShotPOJO, String instanceName) {
		register(snapShotPOJO, instanceName, true);
	}

	public static void register(Object snapShotPOJO, String instanceName, boolean useWeakReference) {
		// Will be implemented when the Perfmon4j Javaagent is installed. 
	}
	
	public static void deRegister(Object snapShotPOJO) {
		deRegister(snapShotPOJO, null);
	}
	
	public static void deRegister(Object snapShotPOJO, String instanceName) {
		// Will be implemented when the Perfmon4j Javaagent is installed. 
	}
	
    /**
     * If true this class has been rewritten by the Perfmon4j agent.
     * @return
     */
    public static boolean isAttachedToAgent() {
    	return false;
    }
}
