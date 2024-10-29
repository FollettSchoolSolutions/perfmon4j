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
	
	
	
	
	public void testBuildDataWithRatio() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setRatios("longRatio=nativeLong/long").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertNotNull("should never return null", dataDefinition);
		
		// Should have 3 data definitions - one for the Ratio, one for the numerator, and one for the denominator.
		assertEquals("Expected data definition length", 3, dataDefinition.length);

		DatumDefinition longRatio = findDefinition(dataDefinition, "longRatio");
		assertNotNull(longRatio);
		assertEquals("Ratio output type is always a Ratio", OutputType.RATIO, longRatio.getOutputType());
		assertEquals("Ratio attribute type is always a java.lang.Double", AttributeType.DOUBLE, longRatio.getAttributeType());
		
		DatumDefinition nativeLongForNominator = findDefinition(dataDefinition, "nativeLong", OutputType.VOID);
		DatumDefinition longForDenominator = findDefinition(dataDefinition, "long", OutputType.VOID);
		
		assertNotNull("nativeLongForNominator", nativeLongForNominator);
		assertNotNull("longForDenominator", longForDenominator);
	}

	public void testBuildDataWithRatioFromCompositeObject() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setRatios("failedRatio=compositeData.failed/compositeData.completed").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertNotNull("should never return null", dataDefinition);
		
		// Should have 3 data definitions - one for the Ratio, one for the numerator, and one for the denominator.
		assertEquals("Expected data definition length", 3, dataDefinition.length);

		DatumDefinition failedRatio = findDefinition(dataDefinition, "failedRatio");
		assertNotNull(failedRatio);
		assertEquals("Ratio output type is always a Ratio", OutputType.RATIO, failedRatio.getOutputType());
		assertEquals("Ratio attribute type is always a java.lang.Double", AttributeType.DOUBLE, failedRatio.getAttributeType());
		
		DatumDefinition longForNominator = findDefinition(dataDefinition, "compositeData.failed", OutputType.VOID);
		DatumDefinition longForDenominator = findDefinition(dataDefinition, "compositeData.completed", OutputType.VOID);
		
		assertNotNull("nativeLongForNominator", longForNominator);
		assertNotNull("longForDenominator", longForDenominator);
	}
	
	public void testRatioWillNotLoadWithMissingNumerator() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setRatios("longRatio=wontBefound/long").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertNotNull("should never return null", dataDefinition);
		
		// Should have only found the denominator and should not have loaded the ratio.
		assertEquals("Expected data definition length", 1, dataDefinition.length);

		DatumDefinition longForDenominator = findDefinition(dataDefinition, "long", OutputType.VOID);
		assertNotNull("longForDenominator", longForDenominator);
	}

	public void testRatioWillNotLoadWithMissingDenomenator() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setRatios("longRatio=long/wontBefound").build();
	
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServerFinder, objectName, query);
		assertNotNull("should never return null", dataDefinition);
		
		// Should have only found the numerator and should not have loaded the ratio.
		assertEquals("Expected data definition length", 1, dataDefinition.length);

		DatumDefinition longForNumerator = findDefinition(dataDefinition, "long", OutputType.VOID);
		assertNotNull("longForNumerator", longForNumerator);
	}

	public void testExtractRatio() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder
				.setCounters("nativeLong,long")  // Make sure the "VOID" types used to calculate the ratio do NOT replace Counters or Gauges.
				.setRatios("longRatio=nativeLong/long").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		MBeanDatum<?>[] data = extractor.extractAttributes();
		
		assertNotNull(data);
//		for (MBeanDatum<?> d : data) {
//		System.out.println(d);
//		}		
		assertEquals("Expected number of data elements", 3, data.length);
		
		MBeanDatum<?> ratio = findDatum(data, "longRatio", OutputType.RATIO);
		assertNotNull("Should have returned the Ratio", ratio);
		assertEquals("datum attribute type", AttributeType.DOUBLE, ratio.getAttributeType());
		assertEquals("datum value", Double.valueOf(0.5d), ratio.getValue());
		
		assertNotNull("'long' was defined both as a Counter and a denominator in the Ratio. The Counter must also be returned", 
				findDatum(data, "long", OutputType.COUNTER));
		assertNotNull("'nativeLong' was defined both as a Counter and a numinator in the Ratio. The Counter must also be returned", 
				findDatum(data, "long", OutputType.COUNTER));
	}

	public void testExtractRatio_WithCompositeObject() throws Exception { 	
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder
			.setCounters("compositeData.failed,compositeData.completed")
			.setRatios("failedRatio=compositeData.failed/compositeData.completed").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		MBeanDatum<?>[] data = extractor.extractAttributes();
		
		assertNotNull(data);
//		for (MBeanDatum<?> d : data) {
//			System.out.println(d);
//		}		
		assertEquals("Expected number of data elements", 3, data.length);
		MBeanDatum<?> ratio = findDatum(data, "failedRatio", OutputType.RATIO);
		
		assertNotNull("Should have returned the Ratio", ratio);
		assertEquals("datum attribute type", AttributeType.DOUBLE, ratio.getAttributeType());
		assertEquals("datum value", Double.valueOf(0.2d), ratio.getValue());

		assertNotNull("'compositeData.completed' was defined both as a Counter and a denominator in the Ratio. The Counter must also be returned", 
				findDatum(data, "compositeData.completed", OutputType.COUNTER));
		assertNotNull("'compositeData.failed' was defined both as a Counter and a numinator in the Ratio. The Counter must also be returned", 
				findDatum(data, "compositeData.failed", OutputType.COUNTER));
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
		return findDefinition(def, name, null);
	}
	
	private DatumDefinition findDefinition(DatumDefinition[] def, String name, OutputType outputType) {
		for (DatumDefinition d : def) {
			if (name.equalsIgnoreCase(d.getName()) 
				&& (outputType == null || d.getOutputType().equals(outputType))) {
				return d;
			}
		}
		return null;
	}
	
	private MBeanDatum<?> findDatum(MBeanDatum<?>[] data, String name) {
		return findDatum(data, name, null);
	}
	
	private MBeanDatum<?> findDatum(MBeanDatum<?>[] data, String name, OutputType outputType) {
		for (MBeanDatum<?> d : data) {
			if (name.equalsIgnoreCase(d.getName())) {
				if (outputType == null || outputType.equals(d.getOutputType())) {
					return d;
				}
			}
		}
		return null;
	}
	
	
	
	
}
