package org.perfmon4j;

import java.util.Map;

import junit.framework.TestCase;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;

public class PerfMonObservableDatumTest extends TestCase {
	public PerfMonObservableDatumTest(String name) {
		super(name);
	}

 	static public void validateObservation(Map<String, PerfMonObservableDatum<?>> observations, String label, String expectedValue) {
 		PerfMonObservableDatum<?> observation = observations.get(label);
 		assertNotNull("observation should have been included: " + label, observation);
 		assertEquals(label, expectedValue, observation.toString());
    }

	
	public void testRatio() {
		Ratio ratio = new Ratio(2, 3);
		
		PerfMonObservableDatum<Ratio> obv = PerfMonObservableDatum.newDatum(ratio);
		assertFalse("isDelta",  obv.isDelta());
		assertTrue("isRatio",  obv.isRatio());
		assertEquals("value", 1, Math.round(obv.getValue().doubleValue()));
		assertEquals("complexValue", ratio, obv.getComplexObject());
		assertEquals("toString should round to 3 decimal places", "0.667", obv.toString());
	}

	public void testDelta() {
		Delta delta = new Delta(0, 100, 1000);
		
		PerfMonObservableDatum<Delta> obv = PerfMonObservableDatum.newDatum(delta);
		assertTrue("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value by default will be throughput per second", 100, Math.round(obv.getValue().doubleValue()));
		assertEquals("complexValue", delta, obv.getComplexObject());
		assertEquals("toString", "100.000", obv.toString());
	}

	public void testInteger() {
		Integer value = Integer.valueOf(100);
		
		PerfMonObservableDatum<? extends Number> obv = PerfMonObservableDatum.newDatum(value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", value, obv.getValue());		
		assertNull("complexValue", obv.getComplexObject());
		assertEquals("toString", "100", obv.toString());
	}
	
	public void testDouble() {
		Double value = Double.valueOf(12345.6789);
		
		PerfMonObservableDatum<? extends Number> obv = PerfMonObservableDatum.newDatum(value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", value, obv.getValue());		
		assertNull("complexValue", obv.getComplexObject());
		assertEquals("toString", "12345.679", obv.toString());
	}
}
