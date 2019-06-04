package org.perfmon4j;

import java.util.Set;

import junit.framework.TestCase;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;

public class PerfMonObservableDatumTest extends TestCase {
	public PerfMonObservableDatumTest(String name) {
		super(name);
	}

 	static public void validateObservation(Set<PerfMonObservableDatum<?>> observations, String fieldName, String expectedValue) {
 		PerfMonObservableDatum<?> observation = PerfMonObservableDatum.findObservationByFieldName(fieldName, observations);
 		assertNotNull("observation should have been included: " + fieldName, observation);
 		assertEquals(fieldName, expectedValue, observation.toString());
    }
 	
 	/**
 	 * HashCode (and equals) is ONLY built based on the fieldName...  This 
 	 * ensures that only one instance of any field can be included in a set. 
 	 */
 	public void testHashCodeEquals() {
		PerfMonObservableDatum<Boolean> obvA = PerfMonObservableDatum.newDatum("fieldA", true);
		PerfMonObservableDatum<Boolean> obvB = PerfMonObservableDatum.newDatum("fieldA", false);
		
		assertEquals("Hash code is based on fieldName", obvA.hashCode(), obvB.hashCode());
		assertTrue("Should be equals, because it matches the fieldName", obvA.equals(obvB));
		
		
		PerfMonObservableDatum<Boolean> obvDifferentField = PerfMonObservableDatum.newDatum("fieldB", true);
		assertFalse("even though the value is the same, 'true', should produce a differend hashCode based on fieldName", 
			obvA.hashCode() == obvDifferentField.hashCode());
		assertFalse("Should NOT be equals, because fieldNames do not match", obvA.equals(obvDifferentField));
		
 	}
 	
	
	public void testRatio() {
		Ratio ratio = new Ratio(2, 3);
		
		PerfMonObservableDatum<Ratio> obv = PerfMonObservableDatum.newDatum("FieldName", ratio);
		assertFalse("isDelta",  obv.isDelta());
		assertTrue("isRatio",  obv.isRatio());
		assertEquals("value", 1, Math.round(obv.getValue().doubleValue()));
		assertEquals("complexValue", ratio, obv.getComplexObject());
		assertEquals("toString should round to 3 decimal places", "0.667", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}

	public void testRatioFormatAsPercent() {
		Ratio ratio = new Ratio(2, 3);
		
		PerfMonObservableDatum<Ratio> obv = PerfMonObservableDatum.newDatum("FieldName", ratio, true);
		assertEquals("should return as a percentage", 67, Math.round(obv.getValue().doubleValue()));
		assertEquals("toString should round percentage to 3 decimal places", "66.667", obv.toString());
		assertEquals("FieldName%", obv.getDefaultDisplayName());
	}
	
	public void testNullRatio() {
		Ratio ratio = null;
		
		PerfMonObservableDatum<Ratio> obv = PerfMonObservableDatum.newDatum("FieldName", ratio);
		assertFalse("isDelta",  obv.isDelta());
		assertTrue("isRatio",  obv.isRatio());
		assertEquals("value", 0, Math.round(obv.getValue().doubleValue()));
		assertEquals("complexValue", PerfMonObservableDatum.NULL_RATIO, obv.getComplexObject());
		assertEquals("toString should round to 3 decimal places", "0.000", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertTrue("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}
	
	public void testDelta() {
		Delta delta = new Delta(0, 100, 5000);
		
		PerfMonObservableDatum<Delta> obv = PerfMonObservableDatum.newDatum("FieldName", delta);
		assertTrue("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value by default will be delta", 100, obv.getValue().intValue());
		assertEquals("complexValue", delta, obv.getComplexObject());
		assertEquals("toString", "100", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}

	
	public void testDeltaFormatAsSecond() {
		Delta delta = new Delta(0, 100, 5000);
		
		PerfMonObservableDatum<Delta> obv = PerfMonObservableDatum.newDatum("FieldName", delta, true);
		assertTrue("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value by default will be throughput per second", 20, Math.round(obv.getValue().doubleValue()));
		assertEquals("complexValue", delta, obv.getComplexObject());
		assertEquals("toString", "20.000", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldNamePerSec", obv.getDefaultDisplayName());
	}

	public void testNullDelta() {
		Delta delta = null;
		
		PerfMonObservableDatum<Delta> obv = PerfMonObservableDatum.newDatum("FieldName", delta);
		assertTrue("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value by default will be throughput per second", 0, Math.round(obv.getValue().doubleValue()));
		assertEquals("complexValue", PerfMonObservableDatum.NULL_DELTA, obv.getComplexObject());
		assertEquals("toString", "0", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertTrue("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}
	
	
	public void testInteger() {
		Integer value = Integer.valueOf(100);
		
		PerfMonObservableDatum<? extends Number> obv = PerfMonObservableDatum.newDatum("FieldName", value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", value, obv.getValue());		
		assertNull("complexValue", obv.getComplexObject());
		assertEquals("toString", "100", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}

	public void testNullInteger() {
		Integer value = null;
		
		PerfMonObservableDatum<? extends Number> obv = PerfMonObservableDatum.newDatum("FieldName", value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", PerfMonObservableDatum.NULL_NUMBER, obv.getValue());		
		assertNull("complexValue", obv.getComplexObject());
		assertEquals("toString", "-1", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertTrue("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}
	
	
	public void testDouble() {
		Double value = Double.valueOf(12345.6789);
		
		PerfMonObservableDatum<? extends Number> obv = PerfMonObservableDatum.newDatum("FieldName", value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", value, obv.getValue());		
		assertNull("complexValue", obv.getComplexObject());
		assertEquals("toString", "12345.679", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}

	public void testNullDouble() {
		Double value = null;
		
		PerfMonObservableDatum<? extends Number> obv = PerfMonObservableDatum.newDatum("FieldName", value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", PerfMonObservableDatum.NULL_NUMBER, obv.getValue());		
		assertNull("complexValue", obv.getComplexObject());
		assertEquals("toString", "-1", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertTrue("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}
	
	
	public void testBoolean() {
		PerfMonObservableDatum<Boolean> obv = PerfMonObservableDatum.newDatum("FieldName", true);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		
		// numeric value should be a Short with a value of 1 or 0;
		assertTrue("Numeric value should be a Short", obv.getValue() instanceof Short);
		assertEquals("true == 1", Short.valueOf((short)1), obv.getValue());
		assertEquals("complexValue should be a Boolean", Boolean.TRUE, obv.getComplexObject());
		assertEquals("toString should return the numeric value", "1", obv.toString());
		assertTrue("Since for all practical purposes, outside of the complexValue, a boolean is the "
				+ "same as a short (1 or 0) and is considered numeric", obv.isNumeric());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
		
		obv = PerfMonObservableDatum.newDatum("FieldName", false);
		
		// numeric value should be a Short with a value of 1 or 0;
		assertEquals("false == 0", Short.valueOf((short)0), obv.getValue());
		assertEquals("complexValue should be a Boolean", Boolean.FALSE, obv.getComplexObject());
		assertEquals("toString should return the numeric value", "0", obv.toString());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
	}

	public void testNullBoolean() {
		Boolean value = null;
		
		PerfMonObservableDatum<? extends Boolean> obv = PerfMonObservableDatum.newDatum("FieldName", value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertEquals("value", Short.valueOf((short)0), obv.getValue());		
		assertEquals("complexValue should be a Boolean", Boolean.FALSE, obv.getComplexObject());
		assertEquals("toString", "0", obv.toString());
		assertTrue("isNumeric", obv.isNumeric());
		assertTrue("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}
	
	
	public void testNullString() {
		String value = null;
		
		PerfMonObservableDatum<String> obv = PerfMonObservableDatum.newDatum("FieldName", value);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertNull("should be no numeric value", obv.getValue());		
		assertNull("String itself is the \"complexValue\"", obv.getComplexObject());
		assertEquals("toString", "null", obv.toString());
		assertFalse("isNumeric", obv.isNumeric());
		assertTrue("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}

	public void testString() {
		PerfMonObservableDatum<String> obv = PerfMonObservableDatum.newDatum("FieldName", "This is my string");
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertNull("should be no numeric value", obv.getValue());		
		assertEquals("String itself is the \"complexValue\"", "This is my string", obv.getComplexObject());
		assertEquals("toString", "This is my string", obv.toString());
		assertFalse("isNumeric", obv.isNumeric());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}
	
	
	public void testRandomComplexObject() {
		StringBuilder complexObject = new StringBuilder("This is from a StringBuilder");
		
		PerfMonObservableDatum<StringBuilder> obv = PerfMonObservableDatum.newDatum("FieldName", complexObject);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertNull("should be no numeric value", obv.getValue());		
		assertEquals("StringBuilder should be the complex object", complexObject, obv.getComplexObject());
		assertEquals("Should be the value of the toString from the complex object", "This is from a StringBuilder", obv.toString());
		assertFalse("isNumeric", obv.isNumeric());
		assertFalse("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}
	
	public void testNullComplexObject() {
		StringBuilder complexObject = null;
		
		PerfMonObservableDatum<StringBuilder> obv = PerfMonObservableDatum.newDatum("FieldName", complexObject);
		assertFalse("isDelta",  obv.isDelta());
		assertFalse("isRatio",  obv.isRatio());
		assertNull("should be no numeric value", obv.getValue());		
		assertNull("Complex value will be null",  obv.getComplexObject());
		assertEquals("toString", "null", obv.toString());
		assertFalse("isNumeric", obv.isNumeric());
		assertTrue("inputWasNull", obv.getInputValueWasNull());
		assertEquals("FieldName", obv.getFieldName());
		assertEquals("FieldName", obv.getDefaultDisplayName());
	}
	
}
