package org.perfmon4j.util.mbean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.mockito.Mockito;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotPOJO;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.util.BeanHelper;
import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;

import junit.framework.TestCase;

public class MBeanPojoBaseTest extends TestCase {
	private final SnapShotGenerator snapShotGenerator = new JavassistSnapShotGenerator();
	private MBeanServer mBeanServer = null;
	private MBeanServerFinder mBeanServerFinder;
	private MBeanQueryEngine engine;
	private static final String BASE_OBJECT_NAME = "org.perfmon4j.util.mbean:type=TestExample";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mBeanServer = MBeanServerFactory.createMBeanServer();
		mBeanServerFinder = Mockito.mock(MBeanServerFinder.class);
		Mockito.when(mBeanServerFinder.getMBeanServer()).thenReturn(mBeanServer);
		engine = new MBeanQueryEngine(mBeanServerFinder);
	}

	@Override
	protected void tearDown() throws Exception {
		engine = null;
		mBeanServerFinder = null;
		mBeanServer = null;
		
		super.tearDown();
	}	
	
	public void testGetEffectiveClassName()throws Exception {
		MBeanInstance mBeanInstance = Mockito.mock(MBeanInstance.class);
		Mockito.when(mBeanInstance.getDatumDefinition()).thenReturn(new DatumDefinition[]{});
		Mockito.when(mBeanInstance.getName()).thenReturn("JVMMemoryPool");
	
		Class<MBeanPojoBase> pojoClass = snapShotGenerator.generatePOJOClassForMBeanInstance(mBeanInstance);
		MBeanPojoBase pojo = MBeanPojoBase.invokeConstructor(pojoClass, mBeanInstance);
		assertEquals("JVMMemoryPool", pojo.getEffectiveClassName());
	}
	
	public void testPOJOSnapShotAnnotationIsPresent()throws Exception {
		MBeanInstance mBeanInstance = Mockito.mock(MBeanInstance.class);
		Mockito.when(mBeanInstance.getDatumDefinition()).thenReturn(new DatumDefinition[]{});
		Mockito.when(mBeanInstance.getName()).thenReturn("JVMMemoryPool");
	
		Class<MBeanPojoBase> pojoClass = snapShotGenerator.generatePOJOClassForMBeanInstance(mBeanInstance);
		MBeanPojoBase pojo = MBeanPojoBase.invokeConstructor(pojoClass, mBeanInstance);
		assertEquals("JVMMemoryPool", pojo.getEffectiveClassName());
		
		Annotation[] classLevelAnnotations = pojoClass.getAnnotations();
		
		assertEquals("Expected number of class level annotations", 1, classLevelAnnotations.length);
		assertEquals("expected annotation type", SnapShotPOJO.class, classLevelAnnotations[0].annotationType());
	}
	
	
	public void testBuildGetter() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("long, nativeLong").build();
		
		MBeanInstance mBeanInstance = engine.doQuery(query).getInstances()[0];
		
		Class<MBeanPojoBase> pojoClass = snapShotGenerator.generatePOJOClassForMBeanInstance(mBeanInstance);
		MBeanPojoBase pojo = MBeanPojoBase.invokeConstructor(pojoClass, mBeanInstance);
		
		Long longValue = (Long)BeanHelper.getValue(pojo, "long");
		assertNotNull("Should returned longValue", longValue);
		assertEquals(Long.valueOf(2), longValue);
		
		Long nativeLongValue = (Long)BeanHelper.getValue(pojo, "nativeLong");
		assertNotNull("Should returned longValue", nativeLongValue);
		assertEquals(Long.valueOf(1), nativeLongValue);
	}	
	
	public void testCounterAnnotationIsPresentOnGetter() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("long").build();
		
		MBeanInstance mBeanInstance = engine.doQuery(query).getInstances()[0];
		
		Class<MBeanPojoBase> pojoClass = snapShotGenerator.generatePOJOClassForMBeanInstance(mBeanInstance);
		MBeanPojoBase pojo = MBeanPojoBase.invokeConstructor(pojoClass, mBeanInstance);
		
		Method getter = pojoClass.getDeclaredMethod("getLong", new Class<?>[]{});
		assertNotNull(getter);
		
		Annotation[] getterAnnotations = getter.getAnnotations();
		assertEquals("Expected number of annotations", 1, getterAnnotations.length);
		assertEquals("expected annotation type", SnapShotCounter.class, getterAnnotations[0].annotationType());
	}	
	
	public void testGaugeAnnotationIsPresentOnGetter() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("float").build();
		
		MBeanInstance mBeanInstance = engine.doQuery(query).getInstances()[0];
		
		Class<MBeanPojoBase> pojoClass = snapShotGenerator.generatePOJOClassForMBeanInstance(mBeanInstance);
		MBeanPojoBase pojo = MBeanPojoBase.invokeConstructor(pojoClass, mBeanInstance);
		
		Method getter = pojoClass.getDeclaredMethod("getFloat", new Class<?>[]{});
		assertNotNull(getter);
		
		Annotation[] getterAnnotations = getter.getAnnotations();
		assertEquals("Expected number of annotations", 1, getterAnnotations.length);
		assertEquals("expected annotation type", SnapShotGauge.class, getterAnnotations[0].annotationType());
	}	
	
	public void testStringAnnotationIsPresentOnGetter() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("string").build();
		
		MBeanInstance mBeanInstance = engine.doQuery(query).getInstances()[0];
		
		Class<MBeanPojoBase> pojoClass = snapShotGenerator.generatePOJOClassForMBeanInstance(mBeanInstance);
		MBeanPojoBase pojo = MBeanPojoBase.invokeConstructor(pojoClass, mBeanInstance);
		
		Method getter = pojoClass.getDeclaredMethod("getString", new Class<?>[]{});
		assertNotNull(getter);
		
		Annotation[] getterAnnotations = getter.getAnnotations();
		assertEquals("Expected number of annotations", 1, getterAnnotations.length);
		assertEquals("expected annotation type", SnapShotString.class, getterAnnotations[0].annotationType());
	}	
}
