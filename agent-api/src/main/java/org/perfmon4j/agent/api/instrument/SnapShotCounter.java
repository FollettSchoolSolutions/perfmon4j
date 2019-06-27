package org.perfmon4j.agent.api.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * This class serves as an alias for the SnapShotCounter Annotation
 * implemented in the perfmon4j agent. 
 * 
 * The alias provides a simplified implementation using only default
 * values for the following attributes:
 * 	formatter = NumberFormatter.class
 * 	suffix = ""
 * 
 * @author perfmon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SnapShotCounter {
	public enum Display {
		DELTA,
		DELTA_PER_MIN,
		DELTA_PER_SECOND,
		INITIAL_VALUE,
		FINAL_VALUE
	}
	
	public Display preferredDisplay() default Display.DELTA;
}
