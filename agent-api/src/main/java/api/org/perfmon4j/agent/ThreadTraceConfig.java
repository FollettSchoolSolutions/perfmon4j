package api.org.perfmon4j.agent;

/**
 * This class will execute in one of two modes:
 * 	Unattached - When running in a JVM that was not booted with the Perfmon4j instrumentation agent this class will 
 * 		execute the code declared within this source file.  Essentially it will be running in a non-operative state.
 * 	Attached - When this class is loaded in a JVM that was booted with the Perfmon4j instrumentation agent, The agent  will
 * 		re-write this class and it will be in an operating state.
 * 
 *  The code is instrumented in method JavassistRuntimeTimerInjector.attachAgentToThreadTraceConfigAPIClass(). 
 */
public class ThreadTraceConfig {
	public static void pushValidator(SimpleTriggerValidator validator) {
		pushValidator(validator, null);
	}
	
	public static void pushValidator(SimpleTriggerValidator validator, String reactiveContextID) {
		// This method will be rewritten when the Perfmon4j JavaAgent is loaded.
	}

	public static void popValidator() {
		popValidator(null);
	}

	public static void popValidator(String reactiveContextID) {
		// This method will be rewritten when the Perfmon4j JavaAgent is loaded.
	}
	
	/**
     * If true this class has been rewritten by the Perfmon4j agent.
     * @return
     */
    public static boolean isAttachedToAgent() {
    	return false;
    }
}
