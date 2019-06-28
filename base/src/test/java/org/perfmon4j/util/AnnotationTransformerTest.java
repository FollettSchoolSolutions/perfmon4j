/*
 *	Copyright 2019 Follett School Solutions 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	ddeuchert@follett.com
 * 	David Deuchert
 * 	Follett School Solutions
*/
package org.perfmon4j.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
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
	
	@api.org.perfmon4j.agent.instrument.SnapShotCounter
	public void testTransformAPICounter() throws Exception {
		Method m = this.getClass().getMethod("testTransformAPICounter", new Class[]{});
		
		SnapShotCounter counter =  t.transform(SnapShotCounter.class, m.getAnnotations()[0]);
		assertNotNull(counter);
	}

	private final api.org.perfmon4j.agent.instrument.SnapShotProvider snapShotProvider = new api.org.perfmon4j.agent.instrument.SnapShotProvider() {

		public Class<? extends Annotation> annotationType() {
			return api.org.perfmon4j.agent.instrument.SnapShotProvider.class;
		}

		public Type type() {
			return api.org.perfmon4j.agent.instrument.SnapShotProvider.Type.STATIC;
		}
	};
	
	public void testTransformSnapShotProvider() {
		SnapShotProvider impl = t.transform(SnapShotProvider.class, snapShotProvider);
		
		assertNotNull(impl);
		assertFalse("Should not use priorityTimer", impl.usePriorityTimer());
		assertEquals("Type should be STATIC", SnapShotProvider.Type.STATIC,
				impl.type());
		assertEquals("DataInterface should be a void class", void.class,
				impl.dataInterface());
		assertNull("SQLWriter", impl.sqlWriter());
	}
	
	private final api.org.perfmon4j.agent.instrument.SnapShotCounter snapShotCounter = new api.org.perfmon4j.agent.instrument.SnapShotCounter() {

		public Class<? extends Annotation> annotationType() {
			return api.org.perfmon4j.agent.instrument.SnapShotCounter.class;
		}

		public Display preferredDisplay() {
			return api.org.perfmon4j.agent.instrument.SnapShotCounter.Display.DELTA_PER_MIN;
		}
	};

	
	public void testSnapShotCounter() {
		SnapShotCounter impl = t.transform(SnapShotCounter.class, snapShotCounter); 
		assertNotNull(impl);
		assertEquals("preferredDisplay", SnapShotCounter.Display.DELTA_PER_MIN, impl.preferredDisplay());
		assertEquals("formatter", NumberFormatter.class, impl.formatter());
		assertEquals("suffix", "", impl.suffix());
	}

	private final api.org.perfmon4j.agent.instrument.SnapShotGauge snapShotGauge = new api.org.perfmon4j.agent.instrument.SnapShotGauge() {

		public Class<? extends Annotation> annotationType() {
			return api.org.perfmon4j.agent.instrument.SnapShotGauge.class;
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

	private final api.org.perfmon4j.agent.instrument.SnapShotString snapShotString = new api.org.perfmon4j.agent.instrument.SnapShotString() {

		public Class<? extends Annotation> annotationType() {
			return api.org.perfmon4j.agent.instrument.SnapShotString.class;
		}

		public boolean isInstanceName() {
			return true;
		}
	};
	
	public void testSnapShotString() {
		SnapShotString impl = t.transform(SnapShotString.class, snapShotString); 
		assertNotNull(impl);
		assertTrue("Should be flagged as an instance name", impl.isInstanceName());
		assertEquals("Should use default formatter", SnapShotStringFormatter.class, impl.formatter());
	}

	private final api.org.perfmon4j.agent.instrument.SnapShotRatio snapShotRatio = new api.org.perfmon4j.agent.instrument.SnapShotRatio() {
		public Class<? extends Annotation> annotationType() {
			return api.org.perfmon4j.agent.instrument.SnapShotRatio.class;
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

	private final api.org.perfmon4j.agent.instrument.SnapShotRatios snapShotRatios = new api.org.perfmon4j.agent.instrument.SnapShotRatios() {

		public Class<? extends Annotation> annotationType() {
			return api.org.perfmon4j.agent.instrument.SnapShotRatios.class;
		}

		public api.org.perfmon4j.agent.instrument.SnapShotRatio[] values() {
			return new api.org.perfmon4j.agent.instrument.SnapShotRatio[]{snapShotRatio};
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
	
	
	private final api.org.perfmon4j.agent.instrument.SnapShotInstanceDefinition snapShotInstanceDefinition = 
			new api.org.perfmon4j.agent.instrument.SnapShotInstanceDefinition() {

		public Class<? extends Annotation> annotationType() {
			return api.org.perfmon4j.agent.instrument.SnapShotInstanceDefinition.class;
		}
	};
	
	public void testSnapShotInstanceDefinition() {
		SnapShotInstanceDefinition impl = t.transform(SnapShotInstanceDefinition.class, snapShotInstanceDefinition); 
		assertNotNull(impl);
	}
	
	
	enum API_ENUM {
		valueA,
		valueB,
		valueC
	}

	enum AGENT_ENUM {
		valueA,
		valueB,
	}
	
	
	public void testTransformEnumValue() {
		AGENT_ENUM value = AnnotationTransformer.transformEnumValue(AGENT_ENUM.class, API_ENUM.valueA, AGENT_ENUM.valueA);
		assertEquals("API valueA should map to agent valueA", AGENT_ENUM.valueA, value);
		
		value = AnnotationTransformer.transformEnumValue(AGENT_ENUM.class, API_ENUM.valueB, AGENT_ENUM.valueA);
		assertEquals("API valueB should map to agent valueB", AGENT_ENUM.valueB, value);

		value = AnnotationTransformer.transformEnumValue(AGENT_ENUM.class, API_ENUM.valueC, AGENT_ENUM.valueA);
		assertEquals("Since agent does not have a valueC we expect the result", AGENT_ENUM.valueA, value);

		value = AnnotationTransformer.transformEnumValue(AGENT_ENUM.class, null, AGENT_ENUM.valueA);
		assertEquals("null should just return the default", AGENT_ENUM.valueA, value);
		
	}
	
}
