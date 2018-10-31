package org.perfmon4j;

import junit.framework.TestCase;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;

public class PerfMonObservableDatumTest extends TestCase {
	private final String FIELD_NAME = "fieldName";
	
	public PerfMonObservableDatumTest(String name) {
		super(name);
	}
	
	public void testRatio() {
		Ratio ratio = new Ratio(2, 3);
		
		PerfMonObservableDatum<Ratio> obv = PerfMonObservableDatum.newDatum(FIELD_NAME, ratio);
		assertFalse("isDelta",  obv.isDelta());
		assertTrue("isRatio",  obv.isRatio());
		assertEquals("value", 1, Math.round(obv.getValue().doubleValue()));
		assertEquals("complexValue", ratio, obv.getComplexObject());
		assertEquals("toString should round to 3 decimal places", "0.667", obv.toString());
	}

	public void testDelta() {
		Delta delta = new Delta(0, 100, 1000);
		
		PerfMonObservableDatum<Delta> obv = PerfMonObservableDatum.newDatum(FIELD_NAME, delta);
		assertTrue("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value by default will be throughput per second", 100, Math.round(obv.getValue().doubleValue()));
		assertEquals("complexValue", delta, obv.getComplexObject());
		assertEquals("toString", "100.000", obv.toString());
	}

	public void testInteger() {
		Integer value = Integer.valueOf(100);
		
		PerfMonObservableDatum<? extends Number> obv = PerfMonObservableDatum.newDatum(FIELD_NAME, value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", value, obv.getValue());		
		assertNull("complexValue", obv.getComplexObject());
		assertEquals("toString", "100", obv.toString());
	}
	
	public void testDouble() {
		Double value = Double.valueOf(12345.6789);
		
		PerfMonObservableDatum<? extends Number> obv = PerfMonObservableDatum.newDatum(FIELD_NAME, value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", value, obv.getValue());		
		assertNull("complexValue", obv.getComplexObject());
		assertEquals("toString", "12345.679", obv.toString());
	}
}
