package org.perfmon4j.agent.api.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class serves as an alias for the SnapShotProvider Annotation
 * implemented in the perfmon4j agent. 
 * 
 * The alias provides a simplified implementation using only default
 * values for the following attributes:
 * 	dataInterface(default) = void.class
 * 	usePriorityTimer(default) = false 
 *  sqlWriter = null
 * 
 * @author perfmon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SnapShotProvider {
	public static enum Type {
		INSTANCE_PER_MONITOR,	// A new provider will be instantiated for each monitor (DEFAULT)
		STATIC,		// The SnapShotManager will access static methods on the class.
		FACTORY,	// The SnapShotManager will invoke the static method getInstance() OR getInstance(String instanceName) ;
	}
	Type type() default Type.INSTANCE_PER_MONITOR;
}
