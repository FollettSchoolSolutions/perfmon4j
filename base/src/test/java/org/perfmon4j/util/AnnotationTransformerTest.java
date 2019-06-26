package org.perfmon4j.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.SnapShotStringFormatter;

import junit.framework.TestCase;

public class AnnotationTransformerTest extends TestCase {
	private final AnnotationTransformer t = new AnnotationTransformer();
	
	
	@SnapShotCounter
	public void testNoTransformCounterNeeded() throws Exception {
		Method m = this.getClass().getMethod("testNoTransformCounterNeeded", new Class[]{});
		
		SnapShotCounter counter =  t.transform(SnapShotCounter.class, m.getAnnotations()[0]);
		assertNotNull(counter);
	}
	
	@org.perfmon4j.agent.api.instrument.SnapShotCounter
	public void testTransformAPICounter() throws Exception {
		Method m = this.getClass().getMethod("testTransformAPICounter", new Class[]{});
		
		SnapShotCounter counter =  t.transform(SnapShotCounter.class, m.getAnnotations()[0]);
		assertNotNull(counter);
	}

	
	public void testTransformSnapShotProvider() {
		org.perfmon4j.agent.api.instrument.SnapShotProvider aAPI = new org.perfmon4j.agent.api.instrument.SnapShotProvider() {

			public Class<? extends Annotation> annotationType() {
				return org.perfmon4j.agent.api.instrument.SnapShotProvider.class;
			}
		};
		
		SnapShotProvider impl = t.transform(SnapShotProvider.class, aAPI);
		
		assertNotNull(impl);
		assertFalse("Should not use priorityTimer", impl.usePriorityTimer());
		assertEquals("Type should be INSTANCE_PER_MONITOR", SnapShotProvider.Type.INSTANCE_PER_MONITOR,
				impl.type());
		assertEquals("DataInterface should be a void class", void.class,
				impl.dataInterface());
		assertNull("SQLWriter", impl.sqlWriter());
	}
	
	public void testSnapShotCounter() {
		org.perfmon4j.agent.api.instrument.SnapShotCounter aAPI = new org.perfmon4j.agent.api.instrument.SnapShotCounter() {

			public Class<? extends Annotation> annotationType() {
				return org.perfmon4j.agent.api.instrument.SnapShotCounter.class;
			}
		};

		SnapShotCounter impl = t.transform(SnapShotCounter.class, aAPI); 
		assertNotNull(impl);
		assertEquals("preferredDisplay", SnapShotCounter.Display.DELTA, impl.preferredDisplay());
		assertEquals("formatter", NumberFormatter.class, impl.formatter());
		assertEquals("suffix", "", impl.suffix());
	}
	
	public void testSnapShotGauge() {
		org.perfmon4j.agent.api.instrument.SnapShotGauge aAPI = new org.perfmon4j.agent.api.instrument.SnapShotGauge() {

			public Class<? extends Annotation> annotationType() {
				return org.perfmon4j.agent.api.instrument.SnapShotGauge.class;
			}
		};

		SnapShotGauge impl = t.transform(SnapShotGauge.class, aAPI); 
		assertNotNull(impl);
		assertEquals("formatter", NumberFormatter.class, impl.formatter());
	}
	
	public void testMissCastTransform() {
		org.perfmon4j.agent.api.instrument.SnapShotGauge aAPI = new org.perfmon4j.agent.api.instrument.SnapShotGauge() {

			public Class<? extends Annotation> annotationType() {
				return org.perfmon4j.agent.api.instrument.SnapShotGauge.class;
			}
		};

		SnapShotCounter impl = t.transform(SnapShotCounter.class, aAPI); 
		assertNull("Shouldn't expect an API Gauge to be transformed into a SnapShotCounter", impl);
	}

	
	public void testSnapShotString() {
		org.perfmon4j.agent.api.instrument.SnapShotString aAPI = new org.perfmon4j.agent.api.instrument.SnapShotString() {

			public Class<? extends Annotation> annotationType() {
				return org.perfmon4j.agent.api.instrument.SnapShotString.class;
			}
		};

		SnapShotString impl = t.transform(SnapShotString.class, aAPI); 
		assertNotNull(impl);
		assertFalse("Should not be identified as a instanceName by default", impl.isInstanceName());
		assertEquals("Should use default formatter", SnapShotStringFormatter.class, impl.formatter());
	}
	
	public void testSnapShotRatio() {
		org.perfmon4j.agent.api.instrument.SnapShotRatio aAPI = new org.perfmon4j.agent.api.instrument.SnapShotRatio() {

			public Class<? extends Annotation> annotationType() {
				return org.perfmon4j.agent.api.instrument.SnapShotRatio.class;
			}

			public String name() {
				return "MyName";
			}

			public String denominator() {
				return "MyDenominator";
			}

			public String numerator() {
				return "MyNumerator";
			}

			public boolean displayAsPercentage() {
				return true;
			}

			public boolean displayAsDuration() {
				return true;
			}
		};

		SnapShotRatio impl = t.transform(SnapShotRatio.class, aAPI); 
		assertNotNull(impl);
		assertEquals("name", "MyName", impl.name());
		assertEquals("denominator", "MyDenominator", impl.denominator());
		assertEquals("numerator", "MyNumerator", impl.numerator());
		assertTrue("displayAsPercentage", impl.displayAsPercentage());
		assertTrue("displayAsDuration", impl.displayAsDuration());
	}

	
	
//	public void testSnapShotInstanceDefinition() {
//		fail("Not implemented");
//	}
//	
//	
//	public void testSnapShotRatio() {
//		fail("Not implemented");
//	}
//	
//	public void testSnapShotRatios() {
//		fail("Not implemented");
//	}
}
