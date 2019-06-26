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
 * 	type (default) INSTANCE_PER_MONITOR
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
}
