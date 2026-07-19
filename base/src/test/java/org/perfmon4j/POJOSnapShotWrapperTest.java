package org.perfmon4j;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotPOJO;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.SnapShotLifecycle;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.SnapShotPOJOLifecycle;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class POJOSnapShotWrapperTest extends TestCase {

	@SnapShotPOJO
	public static class TestPOJO {
		private final AtomicLong counter = new AtomicLong();

		public void increment() {
			counter.incrementAndGet();
		}

		@SnapShotCounter
		public long getCounter() {
			return counter.get();
		}

		@SnapShotGauge
		public int getActiveThreads() {
			return 3;
		}
	}

	private POJOSnapShotRegistry registry;
	private final String pojoClassName = TestPOJO.class.getName();

	public void setUp() throws Exception {
		super.setUp();
		registry = new POJOSnapShotRegistry();
	}

	public void tearDown() throws Exception {
		registry = null;
		super.tearDown();
	}

	public void testInitAndTakeSnapShotRoundTrip() throws Exception {
		TestPOJO pojo = new TestPOJO();
		registry.register(pojo, false);

		POJOSnapShotWrapper wrapper = new POJOSnapShotWrapper("", pojoClassName, null, registry);

		SnapShotData data = wrapper.initSnapShot(1000);
		assertNotNull("initSnapShot must never return null", data);
		assertEquals("init should have been invoked against the live POJO",
			1000, ((SnapShotLifecycle)data).getStartTime());

		pojo.increment();
		data = wrapper.takeSnapShot(data, 2000);
		assertNotNull("takeSnapShot result", data);
		assertEquals("takeSnapShot should have been invoked against the live POJO",
			2000, ((SnapShotLifecycle)data).getEndTime());
	}

	public void testConstructorThrowsWhenClassNotRegistered() throws Exception {
		try {
			new POJOSnapShotWrapper("", "com.acme.NeverRegistered", null, registry);
			fail("Expected GenerateSnapShotException for an unregistered POJO class");
		} catch (GenerateSnapShotException ex) {
			// Expected.
		}
	}

	public void testNamedInstanceNamePropagation() throws Exception {
		registry.register(new TestPOJO(), "InstanceA", false);

		POJOSnapShotWrapper wrapper = new POJOSnapShotWrapper("", pojoClassName, "InstanceA", registry);
		SnapShotData data = wrapper.initSnapShot(1000);

		assertEquals("instanceName should be propagated onto the snapshot data",
			"InstanceA", ((SnapShotPOJOLifecycle)data).getInstanceName());
	}

	public void testGCdInstanceIsGraceful() throws Exception {
		TestPOJO pojo = new TestPOJO();
		registry.register(pojo, true);  // weak reference

		POJOSnapShotWrapper wrapper = new POJOSnapShotWrapper("", pojoClassName, null, registry);
		SnapShotData data = wrapper.initSnapShot(1000);

		pojo = null;
		System.gc();

		// POJO is gone mid-window.  Must not throw and must return the data.
		data = wrapper.takeSnapShot(data, 2000);
		assertNotNull("takeSnapShot must return non-null data even after the POJO is GC'd", data);
	}

	public void testInstanceRegisteredAfterSubscribeAppearsNextWindow() throws Exception {
		// Another instance of the class keeps the registry entry (and Bundle) alive.
		registry.register(new TestPOJO(), "AlreadyHere", false);

		POJOSnapShotWrapper wrapper = new POJOSnapShotWrapper("", pojoClassName, "Latecomer", registry);

		SnapShotData data = wrapper.initSnapShot(1000);
		assertEquals("'Latecomer' is not registered yet -- init should be skipped",
			PerfMon.NOT_SET, ((SnapShotLifecycle)data).getStartTime());
		data = wrapper.takeSnapShot(data, 2000);
		assertEquals("Still not registered -- takeSnapShot should be skipped",
			PerfMon.NOT_SET, ((SnapShotLifecycle)data).getEndTime());

		registry.register(new TestPOJO(), "Latecomer", false);

		data = wrapper.initSnapShot(3000);
		assertEquals("Newly registered instance should be picked up on the next window",
			3000, ((SnapShotLifecycle)data).getStartTime());
		data = wrapper.takeSnapShot(data, 4000);
		assertEquals("endTime", 4000, ((SnapShotLifecycle)data).getEndTime());
	}

/*----------------------------------------------------------------------------*/
    public static void main(String[] args) {
        String[] testCaseName = {POJOSnapShotWrapperTest.class.getName()};

        BasicConfigurator.configure();

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new POJOSnapShotWrapperTest("testInitAndTakeSnapShotRoundTrip"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(POJOSnapShotWrapperTest.class);
        }

        return(newSuite);
    }
}
