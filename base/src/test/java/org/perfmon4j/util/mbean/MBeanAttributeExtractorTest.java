package org.perfmon4j.util.mbean;

import java.util.concurrent.TimeUnit;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.mockito.Mockito;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;
import org.perfmon4j.util.mbean.MBeanDatum.AttributeType;
import org.perfmon4j.util.mbean.MBeanDatum.OutputType;

import junit.framework.TestCase;

public class MBeanAttributeExtractorTest extends TestCase {
	private MBeanServer mBeanServer = null; 
	private MBeanServerFinder mBeanServerFinder = null;
	private MBeanQueryEngine engine;
	private static final String BASE_OBJECT_NAME = "org.perfmon4j.util.mbean:type=TestExample";
	private ObjectName objectName;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		objectName = new ObjectName(BASE_OBJECT_NAME);
		mBeanServer = MBeanServerFactory.createMBeanServer();
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));

		mBeanServerFinder = Mockito.spy(new MBeanServerFinderImpl(null));
		Mockito.when(mBeanServerFinder.getMBeanServer()).thenReturn(mBeanServer);
		engine = new MBeanQueryEngine(mBeanServerFinder);
	}

	@Override
	protected void tearDown() throws Exception {
		objectName = null;
		engine = null;
		mBeanServerFinder = null;
		mBeanServer = null;
		
		super.tearDown();
	}
	
	public void testBuildData_NoMatchingAttributes() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("THIS WILL NOT MATCH ANYTHING").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 0, dataDefinition.length);
	}
	
	public void testBuildData_ExactMatch() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("NextValue").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 1, dataDefinition.length);
		assertEquals("Should be the actual name of the MBean Attribute", "NextValue", dataDefinition[0].getName());
		assertEquals("Type of measurement", OutputType.COUNTER, dataDefinition[0].getOutputType());
	}
	
	/**
	 * JMX Attribute names are commonly in camel case, however there seems to be an inconsistency
	 * regarding the first letter being capitalized -- sometimes it is, sometimes not.  
	 * We want to be forgiving an match a definition of "nextValue" to "NextValue" or "nextValue".
	 * @throws Exception
	 */
	public void testBuildData_IncorrectCapitalizationMatch() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("nextValue").build();  // Lower case first letter does not strictly match, but we want to be forgiving.
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 1, dataDefinition.length);
	}

	public void testBuildData_FindGauge() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("nextValue").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 1, dataDefinition.length);
		assertEquals("Should be the actual name of the MBean Attribute", "NextValue", dataDefinition[0].getName());
		assertEquals("Type of measurement", OutputType.GAUGE, dataDefinition[0].getOutputType());
	}

	public void testExtractData() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("nativeShort, nativeInteger, nativeLong, nativeFloat, nativeDouble, nativeBoolean, nativeCharacter, nativeByte, " +
				"short, integer, long, float, double, boolean, character, byte, " +
				"string, object").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		MBeanDatum<?>[] data = extractor.extractAttributes();
		
		assertEquals(Long.valueOf(1), findDatum(data, "nativeLong").getValue());
		assertEquals(Long.valueOf(2), findDatum(data, "long").getValue());
		assertEquals(Integer.valueOf(3), findDatum(data, "nativeInteger").getValue());
		assertEquals(Integer.valueOf(4), findDatum(data, "integer").getValue());
		assertEquals(Short.valueOf((short)5), findDatum(data, "nativeShort").getValue());
		assertEquals(Short.valueOf((short)6), findDatum(data, "short").getValue());
		assertEquals(Double.valueOf(7.7d), findDatum(data, "nativeDouble").getValue());
		assertEquals(Double.valueOf(8.8d), findDatum(data, "double").getValue());
		assertEquals(Float.valueOf(9.9f), findDatum(data, "nativeFloat").getValue());
		assertEquals(Float.valueOf(10.10f), findDatum(data, "float").getValue());
		assertEquals(Boolean.TRUE, findDatum(data, "nativeBoolean").getValue());
		assertEquals(Boolean.TRUE, findDatum(data, "boolean").getValue());
		assertEquals(Character.valueOf('a'), findDatum(data, "nativeCharacter").getValue());
		assertEquals(Character.valueOf('b'), findDatum(data, "character").getValue());
		assertEquals(Byte.valueOf((byte)11), findDatum(data, "nativeByte").getValue());
		assertEquals(Byte.valueOf((byte)12), findDatum(data, "byte").getValue());
		assertEquals("13", findDatum(data, "string").getValue());
		assertEquals("14", findDatum(data, "object").getValue());
	}

	public void testInvalidCounterTypeDemotedToGauge() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("byte").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		DatumDefinition[] def = extractor.getDatumDefinition();
		
		assertEquals(MBeanDatum.OutputType.GAUGE, findDefinition(def, "byte").getOutputType());
	}
	
	public void testInvalidCounterTypeDemotedToString() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("string").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		DatumDefinition[] def = extractor.getDatumDefinition();
		
		assertEquals(MBeanDatum.OutputType.STRING, findDefinition(def, "string").getOutputType());
	}

	public void testInvalidGaugeTypeDemotedToString() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("object").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		DatumDefinition[] def = extractor.getDatumDefinition();
		
		assertEquals(MBeanDatum.OutputType.STRING, findDefinition(def, "object").getOutputType());
	}
	
	public void testExtractDataType() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("nativeShort, nativeInteger, nativeLong, nativeFloat, nativeDouble, nativeBoolean, nativeCharacter, nativeByte, " +
			"short, integer, long, float, double, boolean, character, byte, " +
			"string, object").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		DatumDefinition[] def = extractor.getDatumDefinition();
		
		assertEquals(MBeanDatum.AttributeType.NATIVE_SHORT, findDefinition(def, "nativeShort").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.NATIVE_INTEGER, findDefinition(def, "nativeInteger").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.NATIVE_LONG, findDefinition(def, "nativeLong").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.NATIVE_FLOAT, findDefinition(def, "nativeFloat").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.NATIVE_DOUBLE, findDefinition(def, "nativeDouble").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.NATIVE_BOOLEAN, findDefinition(def, "nativeBoolean").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.NATIVE_CHARACTER, findDefinition(def, "nativeCharacter").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.NATIVE_BYTE, findDefinition(def, "nativeByte").getAttributeType());
		
		assertEquals(MBeanDatum.AttributeType.SHORT, findDefinition(def, "short").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.INTEGER, findDefinition(def, "integer").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.LONG, findDefinition(def, "long").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.FLOAT, findDefinition(def, "float").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.DOUBLE, findDefinition(def, "double").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.BOOLEAN, findDefinition(def, "boolean").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.CHARACTER, findDefinition(def, "character").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.BYTE, findDefinition(def, "byte").getAttributeType());

		assertEquals(MBeanDatum.AttributeType.STRING, findDefinition(def, "string").getAttributeType());
		assertEquals(MBeanDatum.AttributeType.STRING, findDefinition(def, "object").getAttributeType());
	}
	
	public void testMBeanDataumToPerfMonObservableDatum_Long() {
		Long value = Long.valueOf(2);
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.LONG, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertTrue("Should be numeric type", datum.isNumeric());
		assertEquals("Expected value", value, datum.getValue());
		assertEquals("toString should always return a string representation of the object", "2", 
				datum.toString());
	}
	
	public void testMBeanDataumToPerfMonObservableDatum_Integer() {
		Integer value = Integer.valueOf(3);
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.INTEGER, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertTrue("Should be numeric type", datum.isNumeric());
		assertEquals("Expected value", value, datum.getValue());
		assertEquals("toString should always return a string representation of the object", "3", 
				datum.toString());
	}
	
	public void testMBeanDataumToPerfMonObservableDatum_Short() {
		Short value = Short.valueOf((short)4);
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.SHORT, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertTrue("Should be numeric type", datum.isNumeric());
		assertEquals("Expected value", value, datum.getValue());
		assertEquals("toString should always return a string representation of the object", "4", 
				datum.toString());
	}

	public void testMBeanDataumToPerfMonObservableDatum_Float() {
		Float value = Float.valueOf(5.0f);
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.FLOAT, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertTrue("Should be numeric type", datum.isNumeric());
		assertEquals("Expected value", value, datum.getValue());
		assertEquals("toString should always return a string representation of the object", "5.000", 
				datum.toString());
	}
	
	public void testMBeanDataumToPerfMonObservableDatum_Double() {
		Double value = Double.valueOf(6.0d);
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.DOUBLE, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertTrue("Should be numeric type", datum.isNumeric());
		assertEquals("Expected value", value, datum.getValue());
		assertEquals("toString should always return a string representation of the object", "6.000", 
				datum.toString());
		
	}

	public void testMBeanDataumToPerfMonObservableDatum_Boolean() {
		Boolean value = Boolean.TRUE;
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.BOOLEAN, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertTrue("Should be numeric type", datum.isNumeric());
		assertEquals("Expected value - PerfMonObservableDatum converts boolean to 0 or 1"
			, Short.valueOf((short)1), datum.getValue());
		assertEquals("toString should always return a string representation of the object", "1", 
				datum.toString());
	}

	public void testMBeanDataumToPerfMonObservableDatum_Character() {
		Character value = Character.valueOf('x');
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.CHARACTER, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertFalse("Should NOT be numeric type", datum.isNumeric());
		assertNull("Value will be null, as it is treated as a non-numeric object", datum.getValue());
		assertEquals("A character is treated as an object by PerfMonObservableDatum "
				+ "and stored in the complexObject attribute", value, datum.getComplexObject());
		assertEquals("toString should always return a string representation of the object", "x", 
				datum.toString());
	}

	public void testMBeanDataumToPerfMonObservableDatum_Byte() {
		Byte value = Byte.valueOf((byte)7);
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.BYTE, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertTrue("Should be numeric type", datum.isNumeric());
		assertEquals("Expected value", value, datum.getValue());
		assertEquals("toString should always return a string representation of the object", "7", 
				datum.toString());
	}
	
	public void testMBeanDataumToPerfMonObservableDatum_String() {
		String value = "StringValue";
		PerfMonObservableDatum<?> datum = buildMBeanDatum("MyField", AttributeType.STRING, value).toPerfMonObservableDatum();
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertFalse("Should be numeric type", datum.isNumeric());
		assertNull("Value will be null, as a String is treated as a non-numeric object", datum.getValue());
		assertEquals("A String is treated as an object by PerfMonObservableDatum "
				+ "and stored in the complexObject attribute", value, datum.getComplexObject());
		assertEquals("toString should always return a string representation of the object", value, 
			datum.toString());
	}
	
	public void testMBeanDataumToPerfMonObservableDatum_Delta() {
		MBeanDatum<?> datumBefore = buildMBeanDatum("MyField", AttributeType.LONG, Long.valueOf(2));
		MBeanDatum<?> datumAfter = buildMBeanDatum("MyField", AttributeType.LONG, Long.valueOf(3));
		
		long durationMillis = TimeUnit.MINUTES.toMillis(1);
		PerfMonObservableDatum<?> datum = datumAfter.toPerfMonObservableDatum(datumBefore, durationMillis);
		
		
		assertNotNull("Should have created datum", datum);
		assertEquals("Field Name", "MyField", datum.getFieldName());
		assertTrue("Should be numeric type", datum.isNumeric());
		assertEquals("Value should be after - before", Long.valueOf(1), datum.getValue());
		assertEquals("toString should always return a string representation of the object", "1", 
				datum.toString());
		assertEquals("The complexObject attribute should be a Delta ", 
				new Delta(2L, 3L, durationMillis), datum.getComplexObject());
	}	
	
	public void testExtractAttributesWithCompositeAttributes() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder
			.setCounters("compositeData.completed")
			.setGauges("CompositeData.Status,compositeData.DOES_NOT_EXIST")
			.build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		DatumDefinition[] def = extractor.getDatumDefinition();
		assertEquals("DatumDefinition length", 2, def.length);
		
		assertEquals(MBeanDatum.OutputType.COUNTER, findDefinition(def, "CompositeData.completed").getOutputType());
		assertEquals(MBeanDatum.OutputType.GAUGE, findDefinition(def, "CompositeData.status").getOutputType());
		
		
		MBeanDatum<?> data[] = extractor.extractAttributes();
		assertEquals("Expected number of data elements", 2, data.length);
	}
	
	public void testBuildData_SpecifyAlternateDisplayName() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = 
			builder.setCounters("nextValue(displayName=\"myValue\")")
			.build();  
		
		DatumDefinition dataDefinition[] = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertEquals("expected dataDefinition length", 1, dataDefinition.length);
		assertEquals("myValue", dataDefinition[0].getDisplayName());
	}	

	public void testBuildData_SpecifyAlternateDisplayNameCompositeData() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = 
			builder.setCounters("compositeData.status(displayName=\"myStatus\")")
			.build();  
		
		DatumDefinition dataDefinition[] = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertEquals("expected dataDefinition length", 1, dataDefinition.length);
		assertEquals("myStatus", dataDefinition[0].getDisplayName());
	}	
	
	private MBeanDatum<?> buildMBeanDatum(String attributeName, AttributeType attributeType, Object value) {
		return new MBeanAttributeExtractor.MBeanDatumImpl<>(buildDatumDefinition(attributeName, attributeType), value);
	}
	
	private DatumDefinition buildDatumDefinition(String attributeName, AttributeType attributeType) {
		MBeanAttributeInfo info = Mockito.mock(MBeanAttributeInfo.class);
		Mockito.when(info.getName()).thenReturn(attributeName);
		Mockito.when(info.getType()).thenReturn(attributeType.getJmxType());

		return new DatumDefinition(info, OutputType.GAUGE);
	}
	
	
	private DatumDefinition findDefinition(DatumDefinition[] def, String name) {
		for (DatumDefinition d : def) {
			if (name.equalsIgnoreCase(d.getName())) {
				return d;
			}
		}
		return null;
	}
	
	
	private MBeanDatum<?> findDatum(MBeanDatum<?>[] data, String name) {
		for (MBeanDatum<?> d : data) {
			if (name.equalsIgnoreCase(d.getName())) {
				return d;
			}
		}
		return null;
	}
	
	
	
	
}
