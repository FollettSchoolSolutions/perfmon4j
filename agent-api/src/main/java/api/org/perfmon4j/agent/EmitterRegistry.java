package api.org.perfmon4j.agent;

/**
 * This class will execute in one of two modes:
 * 	Unattached - When running in a JVM that was not booted with the Perfmon4j instrumentation agent this class will 
 * 		execute the code declared within this source file.  Essentially it will be running in a non-operative state.
 * 	Attached - When this class is loaded in a JVM that was booted with the Perfmon4j instrumentation agent, The agent  will
 * 		re-write this class and it will be in an operating state.
 * 
 *  The code is instrumented in method JavassistRuntimeTimerInjector.attachAgentToEmitterRegistryAPIClass(). 
 * 
 */
public class EmitterRegistry {
	
	public static void register(Emitter emitter) {
		register(emitter, null, true);
	}
	
	public static void register(Emitter emitter, boolean useWeakReference) {
		register(emitter, null, useWeakReference);
	}
	
	public static void register(Emitter emitter, String instanceName) {
		register(emitter, instanceName, true);
	}

	public static void register(Emitter emitter, String instanceName, boolean useWeakReference) {
		// Will be implemented when the Perfmon4j Javaagent is installed.
	}
	
	public static void deRegister(Emitter emitter) {
		deRegister(emitter, null);
	}
	
	public static void deRegister(Emitter emitter, String instanceName) {
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
