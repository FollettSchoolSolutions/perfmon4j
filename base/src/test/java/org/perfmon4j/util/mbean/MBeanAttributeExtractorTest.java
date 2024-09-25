package org.perfmon4j.util.mbean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.mockito.Mockito;
import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;
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
		
		mBeanServer = MBeanServerFactory.createMBeanServer();
		mBeanServerFinder = Mockito.mock(MBeanServerFinder.class);
		Mockito.when(mBeanServerFinder.getMBeanServer()).thenReturn(mBeanServer);
		engine = new MBeanQueryEngine(mBeanServerFinder);
		objectName = new ObjectName(BASE_OBJECT_NAME);
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
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("THIS WILL NOT MATCH ANYTHING").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 0, dataDefinition.length);
	}
	
	public void testBuildData_ExactMatch() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("NextValue").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
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
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("nextValue").build();  // Lower case first letter does not strictly match, but we want to be forgiving.
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 1, dataDefinition.length);
	}

	public void testBuildData_FindGauge() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("nextValue").build();
		
		DatumDefinition[] dataDefinition = MBeanAttributeExtractor.buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
		assertNotNull("should never return null", dataDefinition);
		assertEquals("No matching attributes", 1, dataDefinition.length);
		assertEquals("Should be the actual name of the MBean Attribute", "NextValue", dataDefinition[0].getName());
		assertEquals("Type of measurement", OutputType.GAUGE, dataDefinition[0].getOutputType());
	}

	public void testExtractData() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
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
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("byte").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		DatumDefinition[] def = extractor.getDatumDefinition();
		
		assertEquals(MBeanDatum.OutputType.GAUGE, findDefinition(def, "byte").getOutputType());
	}
	
	public void testInvalidCounterTypeDemotedToString() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("string").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		DatumDefinition[] def = extractor.getDatumDefinition();
		
		assertEquals(MBeanDatum.OutputType.STRING, findDefinition(def, "string").getOutputType());
	}

	public void testInvalidGaugeTypeDemotedToString() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("object").build();

		MBeanAttributeExtractor extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		DatumDefinition[] def = extractor.getDatumDefinition();
		
		assertEquals(MBeanDatum.OutputType.STRING, findDefinition(def, "object").getOutputType());
	}
	
	public void testExtractDataType() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
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
