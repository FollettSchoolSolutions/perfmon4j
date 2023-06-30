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
	
	public static void pushValidator(TriggerValidator validator, String reactiveContextID) {
	}
	
	public static void popValidator(String reactiveContextID) {
	}
	
	public static interface TriggerValidator {
	}
	
	public static interface RequestTriggerValidator extends TriggerValidator {
		public boolean isValid(String parameterName, String parameterValue);
	}

	public static interface SessionTriggerValidator extends TriggerValidator {
		public boolean isValid(String attributeName, Object attributeValue);
	}

	public static interface CookieTriggerValidator extends TriggerValidator {
		public boolean isValid(String cookieName, String cookieValue);
	}
	
	/**
     * If true this class has been rewritten by the Perfmon4j agent.
     * @return
     */
    public static boolean isAttachedToAgent() {
    	return false;
    }
}
