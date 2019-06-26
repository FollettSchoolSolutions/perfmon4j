package org.perfmon4j.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
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

	private final org.perfmon4j.agent.api.instrument.SnapShotProvider snapShotProvider = new org.perfmon4j.agent.api.instrument.SnapShotProvider() {

		public Class<? extends Annotation> annotationType() {
			return org.perfmon4j.agent.api.instrument.SnapShotProvider.class;
		}
	};
	
	public void testTransformSnapShotProvider() {
		SnapShotProvider impl = t.transform(SnapShotProvider.class, snapShotProvider);
		
		assertNotNull(impl);
		assertFalse("Should not use priorityTimer", impl.usePriorityTimer());
		assertEquals("Type should be INSTANCE_PER_MONITOR", SnapShotProvider.Type.INSTANCE_PER_MONITOR,
				impl.type());
		assertEquals("DataInterface should be a void class", void.class,
				impl.dataInterface());
		assertNull("SQLWriter", impl.sqlWriter());
	}
	
	private final org.perfmon4j.agent.api.instrument.SnapShotCounter snapShotCounter = new org.perfmon4j.agent.api.instrument.SnapShotCounter() {

		public Class<? extends Annotation> annotationType() {
			return org.perfmon4j.agent.api.instrument.SnapShotCounter.class;
		}
	};

	
	public void testSnapShotCounter() {
		SnapShotCounter impl = t.transform(SnapShotCounter.class, snapShotCounter); 
		assertNotNull(impl);
		assertEquals("preferredDisplay", SnapShotCounter.Display.DELTA, impl.preferredDisplay());
		assertEquals("formatter", NumberFormatter.class, impl.formatter());
		assertEquals("suffix", "", impl.suffix());
	}

	private final org.perfmon4j.agent.api.instrument.SnapShotGauge snapShotGauge = new org.perfmon4j.agent.api.instrument.SnapShotGauge() {

		public Class<? extends Annotation> annotationType() {
			return org.perfmon4j.agent.api.instrument.SnapShotGauge.class;
		}
	};

	public void testSnapShotGauge() {
		SnapShotGauge impl = t.transform(SnapShotGauge.class, snapShotGauge); 
		assertNotNull(impl);
		assertEquals("formatter", NumberFormatter.class, impl.formatter());
	}
	
	public void testMissCastTransform() {
		SnapShotCounter impl = t.transform(SnapShotCounter.class, snapShotGauge); 
		assertNull("Shouldn't expect an API Gauge to be transformed into a SnapShotCounter", impl);
	}

	private final org.perfmon4j.agent.api.instrument.SnapShotString snapShotString = new org.perfmon4j.agent.api.instrument.SnapShotString() {

		public Class<? extends Annotation> annotationType() {
			return org.perfmon4j.agent.api.instrument.SnapShotString.class;
		}
	};
	
	public void testSnapShotString() {
		SnapShotString impl = t.transform(SnapShotString.class, snapShotString); 
		assertNotNull(impl);
		assertFalse("Should not be identified as a instanceName by default", impl.isInstanceName());
		assertEquals("Should use default formatter", SnapShotStringFormatter.class, impl.formatter());
	}

	private final org.perfmon4j.agent.api.instrument.SnapShotRatio snapShotRatio = new org.perfmon4j.agent.api.instrument.SnapShotRatio() {
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

	public void testSnapShotRatio() {
		SnapShotRatio impl = t.transform(SnapShotRatio.class, snapShotRatio); 
		assertNotNull(impl);
		assertEquals("name", "MyName", impl.name());
		assertEquals("denominator", "MyDenominator", impl.denominator());
		assertEquals("numerator", "MyNumerator", impl.numerator());
		assertTrue("displayAsPercentage", impl.displayAsPercentage());
		assertTrue("displayAsDuration", impl.displayAsDuration());
	}

	private final org.perfmon4j.agent.api.instrument.SnapShotRatios snapShotRatios = new org.perfmon4j.agent.api.instrument.SnapShotRatios() {

		public Class<? extends Annotation> annotationType() {
			return org.perfmon4j.agent.api.instrument.SnapShotRatios.class;
		}

		public org.perfmon4j.agent.api.instrument.SnapShotRatio[] values() {
			return new org.perfmon4j.agent.api.instrument.SnapShotRatio[]{snapShotRatio};
		}
	};
	
	public void testSnapShotRatios() {
		SnapShotRatios impl = t.transform(SnapShotRatios.class, snapShotRatios); 
		assertNotNull(impl);
		SnapShotRatio[] values = impl.value();
		assertNotNull(values);
		assertEquals("values length", 1, values.length);
		assertEquals("value name", "MyName", values[0].name());
	}
	
	
//	public void testSnapShotInstanceDefinition() {
//		fail("Not implemented");
//	}

}
