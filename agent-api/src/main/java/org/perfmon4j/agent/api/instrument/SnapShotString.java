package org.perfmon4j.agent.api.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * This class serves as an alias for the SnapShotString Annotation
 * implemented in the perfmon4j agent. 
 * 
 * The alias provides a simplified implementation using only default
 * values for the following attributes:
 * 	formatter = SnapShotStringFormatter.class
 *  isInstanceName = false;
 * 
 * @author perfmon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SnapShotString {
}
