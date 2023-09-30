package org.perfmon4j.snapshot.emitter;


public class SnapShotEmitterRegistry {
	
	public static void register(SnapShotEmitter snapShotEmitter) {
		register(snapShotEmitter, null, true);
	}
	
	public static void register(SnapShotEmitter snapShotEmitter, boolean useWeakReference) {
		register(snapShotEmitter, null, useWeakReference);
	}
	
	public static void register(SnapShotEmitter snapShotEmitter, String instanceName) {
		register(snapShotEmitter, instanceName, true);
	}

	public static void register(SnapShotEmitter snapShotEmitter, String instanceName, boolean useWeakReference) {
		// Will be implemented when the Perfmon4j Javaagent is installed. 
	}
	
	public static void deRegister(SnapShotEmitter snapShotEmitter) {
		deRegister(snapShotEmitter, null);
	}
	
	public static void deRegister(SnapShotEmitter snapShotPOJO, String instanceName) {
		// Will be implemented when the Perfmon4j Javaagent is installed. 
	}

}
