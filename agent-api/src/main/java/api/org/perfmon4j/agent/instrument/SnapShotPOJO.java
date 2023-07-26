package api.org.perfmon4j.agent.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SnapShotPOJO {
    Class<?> dataInterface() default void.class;
    boolean usePriorityTimer() default false; // Indicate interval of snapshot very important.
}

