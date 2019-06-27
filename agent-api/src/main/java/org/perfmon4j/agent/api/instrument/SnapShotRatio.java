package org.perfmon4j.agent.api.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * This class serves as an alias for the SnapShotRatio Annotation
 * implemented in the perfmon4j agent. 
 * 
 * 
 * @author perfmon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SnapShotRatio {
	String name();
	String denominator();
	String numerator();
	boolean displayAsPercentage() default false;
	boolean displayAsDuration() default false;
}
