package org.perfmon4j.util.mbean;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.mockito.Mockito;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;

import junit.framework.TestCase;

public class MBeanInstanceDataTest extends TestCase {
	private final SnapShotGenerator snapShotGenerator = new JavassistSnapShotGenerator();
	private MBeanServer mBeanServer = null;
	private MBeanServerFinder mBeanServerFinder;
	private MBeanQueryEngine engine;
	private static final String BASE_OBJECT_NAME = "org.perfmon4j.util.mbean:type=TestExample";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mBeanServer = MBeanServerFactory.createMBeanServer();
		mBeanServerFinder = Mockito.spy(new MBeanServerFinderImpl(null));
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
	
	public void testExampleGauge() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("long, nativeLong").build();
		
		MBeanInstance mBeanInstance = engine.doQuery(query).getInstances()[0];
		
		MBeanInstanceData data = new MBeanInstanceData();
		
		long now = System.currentTimeMillis();
		
		data.init(mBeanInstance, now);
		data.takeSnapShot(mBeanInstance, now + TimeUnit.MINUTES.toMicros(1));
		
		Set<PerfMonObservableDatum<?>> observations = data.getObservations();
		assertEquals(Long.valueOf(2), getObservation(observations, "long").getValue());
		assertEquals(Long.valueOf(1), getObservation(observations, "nativeLong").getValue());
	}	
	
	public void testExampleCounter() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setCounters("nextValue").build();
		
		MBeanInstance mBeanInstance = engine.doQuery(query).getInstances()[0];
		
		MBeanInstanceData data = new MBeanInstanceData();
		
		long now = System.currentTimeMillis();
		data.init(mBeanInstance, now);
		data.takeSnapShot(mBeanInstance, now + TimeUnit.MINUTES.toMillis(1));
		
		Set<PerfMonObservableDatum<?>> observations = data.getObservations();
		
		PerfMonObservableDatum<?> datum = getObservation(observations, "nextValue");
		assertTrue(datum.getComplexObject() instanceof Delta);
		assertEquals(Long.valueOf(1), datum.getValue());
	}	

	
	public void testToAppenderString() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("nextValue,nativeLong,long,nativeShort,short,nativeInteger,integer,"
			+ "nativeDouble,double,nativeFloat,float,nativeBoolean,boolean,nativeCharacter,"
			+ "character,nativeByte,byte,string,object")
			.setCounters("nextValue")
			.build();
		
		MBeanInstance mBeanInstance = engine.doQuery(query).getInstances()[0];
		
		MBeanInstanceData data = new MBeanInstanceData();
		
		long now = System.currentTimeMillis();
		data.setName("MyCategoryName");
		data.setInstanceName("MyInstanceName");
		
		data.init(mBeanInstance, now);
		data.takeSnapShot(mBeanInstance, now + TimeUnit.MINUTES.toMillis(1));

		String appenderString =  data.toAppenderString();
System.out.println(appenderString);
		
		// Check that we properly converted the non-numeric types.
		assertTrue("should have nativeCharacter", appenderString.contains("NativeCharacter.......... a"));
		assertTrue("should have Character", appenderString.contains("Character................ b"));
		assertTrue("should have String", appenderString.contains("String................... 13"));
		assertTrue("should have Object", appenderString.contains("Object................... 14"));
	}	
	
	public void testGetNonNumericObservations() throws Exception { 	
		mBeanServer.registerMBean(new TestExample(), new ObjectName(BASE_OBJECT_NAME));
		
		MBeanQueryBuilder builder = new MBeanQueryBuilder(BASE_OBJECT_NAME);
		MBeanQuery query = builder.setGauges("nativeCharacter,character,string,object")
			.build();
		
		MBeanInstance mBeanInstance = engine.doQuery(query).getInstances()[0];
		
		MBeanInstanceData data = new MBeanInstanceData();
		
		long now = System.currentTimeMillis();
		data.init(mBeanInstance, now);
		data.takeSnapShot(mBeanInstance, now + TimeUnit.MINUTES.toMillis(1));
		
		Set<PerfMonObservableDatum<?>> observations = data.getObservations();
		
		
	}	
		
	PerfMonObservableDatum<?> getObservation(Set<PerfMonObservableDatum<?>> observations, String name) {
		for (PerfMonObservableDatum<?> observation : observations) {
			if (name.equalsIgnoreCase(observation.getFieldName())) {
				return observation;
			}
		}
		return null;
	}
}
