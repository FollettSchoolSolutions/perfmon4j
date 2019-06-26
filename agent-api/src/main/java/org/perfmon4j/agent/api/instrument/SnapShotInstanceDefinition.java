package org.perfmon4j.agent.api.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * This class serves as an alias for the SnapShotInstanceDefinition Annotation
 * implemented in the perfmon4j agent. 
 * 
 * @author perfmon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SnapShotInstanceDefinition {
}
