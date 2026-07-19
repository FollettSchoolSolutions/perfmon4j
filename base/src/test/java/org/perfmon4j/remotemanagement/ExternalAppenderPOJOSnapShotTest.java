package org.perfmon4j.remotemanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.perfmon4j.POJOSnapShotRegistry;
import org.perfmon4j.PerfMonTestCase;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotPOJO;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Verifies that POJO snapshot instances registered with the (singleton)
 * POJOSnapShotRegistry are visible/subscribable through the ExternalAppender
 * remote-management path used by the VisualVM and Hawtio plugins.
 */
public class ExternalAppenderPOJOSnapShotTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    private String sessionID = null;
    private boolean originalExternalAppenderEnabled = false;
    /** (pojo, instanceName) pairs to deRegister in tearDown -- the singleton registry leaks across tests otherwise. */
    private final List<Object[]> registeredPOJOs = new ArrayList<Object[]>();

    public ExternalAppenderPOJOSnapShotTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        originalExternalAppenderEnabled = ExternalAppender.isEnabled();
        ExternalAppender.setEnabled(true);
        sessionID = ExternalAppender.connect();
    }

    public void tearDown() throws Exception {
        for (Object[] pair : registeredPOJOs) {
            POJOSnapShotRegistry.getSingleton().deRegister(pair[0], (String)pair[1]);
        }
        registeredPOJOs.clear();

        ExternalAppender.disconnect(sessionID);
        ExternalAppender.setEnabled(originalExternalAppenderEnabled);
        sessionID = null;
        super.tearDown();
    }

    /** Register with a strong reference and remember for tearDown cleanup. */
    private void registerPOJO(Object pojo, String instanceName) throws Exception {
        POJOSnapShotRegistry.getSingleton().register(pojo, instanceName, false);
        registeredPOJOs.add(new Object[]{pojo, instanceName});
    }

    @SnapShotPOJO
    public static class ExternalTestPOJO {
        private long counter = 0;

        public void increment() {
            counter++;
        }

        @SnapShotCounter
        public long getCounter() {
            return counter;
        }

        @SnapShotGauge
        public int getActiveThreads() {
            return 5;
        }

        @SnapShotString
        public String getStatus() {
            return "OK";
        }
    }

    /** Used ONLY by the legacy-dedup test; keeps the static legacy snapshot
     * registry pollution away from the other tests' POJO class. */
    @SnapShotPOJO
    public static class LegacyRegisteredPOJO {
        @SnapShotCounter
        public long getCounter() {
            return 0;
        }
    }

    private List<MonitorKey> getSnapShotKeysForClass(String className) {
        List<MonitorKey> result = new ArrayList<MonitorKey>();
        for (MonitorKey key : ExternalAppender.getSnapShotMonitorKeys()) {
            if (className.equals(key.getName())) {
                result.add(key);
            }
        }
        return result;
    }

/*----------------------------------------------------------------------------*/
    public void testPOJOKeyAppearsOnRegisterAndDisappearsOnDeRegister() throws Exception {
        final String className = ExternalTestPOJO.class.getName();

        assertEquals("No POJO registered -- no keys expected",
            0, getSnapShotKeysForClass(className).size());

        ExternalTestPOJO pojo = new ExternalTestPOJO();
        registerPOJO(pojo, null);

        List<MonitorKey> keys = getSnapShotKeysForClass(className);
        assertEquals("Registered POJO should surface exactly one key", 1, keys.size());
        assertEquals("type", MonitorKey.SNAPSHOT_TYPE, keys.get(0).getType());
        assertNull("Unnamed instance -- no instance attribute", keys.get(0).getInstance());

        POJOSnapShotRegistry.getSingleton().deRegister(pojo, null);
        registeredPOJOs.clear();

        assertEquals("Key should disappear when the POJO is deRegistered",
            0, getSnapShotKeysForClass(className).size());
    }

/*----------------------------------------------------------------------------*/
    public void testNamedPOJOInstancesEachGetOwnKey() throws Exception {
        final String className = ExternalTestPOJO.class.getName();

        registerPOJO(new ExternalTestPOJO(), "InstanceA");
        registerPOJO(new ExternalTestPOJO(), "InstanceB");
        registerPOJO(new ExternalTestPOJO(), null);

        List<MonitorKey> keys = getSnapShotKeysForClass(className);
        assertEquals("One key per registered instance", 3, keys.size());

        List<String> instances = new ArrayList<String>();
        for (MonitorKey key : keys) {
            instances.add(key.getInstance());
        }
        assertTrue("Should contain InstanceA", instances.contains("InstanceA"));
        assertTrue("Should contain InstanceB", instances.contains("InstanceB"));
        assertTrue("Should contain the unnamed instance", instances.contains(null));
    }

/*----------------------------------------------------------------------------*/
    public void testPOJOKeyDisappearsWhenWeakInstanceGCd() throws Exception {
        final String className = ExternalTestPOJO.class.getName();

        ExternalTestPOJO pojo = new ExternalTestPOJO();
        POJOSnapShotRegistry.getSingleton().register(pojo, null, true); // weak reference
        try {
            assertEquals("Weakly registered POJO should surface a key while alive",
                1, getSnapShotKeysForClass(className).size());

            pojo = null;
            System.gc();

            assertEquals("Key should disappear once the weakly held POJO is GC'd",
                0, getSnapShotKeysForClass(className).size());
        } finally {
            // In case GC did not collect (test failure path) -- deRegister by a fresh key lookup is
            // not possible without the original reference; the weak instance is harmless once cleared.
        }
    }

/*----------------------------------------------------------------------------*/
    public void testGetFieldsForPOJOMonitor() throws Exception {
        final String className = ExternalTestPOJO.class.getName();
        registerPOJO(new ExternalTestPOJO(), null);

        MonitorKey monitorKey = MonitorKey.newSnapShotKey(className);
        FieldKey[] fields = ExternalAppender.getFieldsForSnapShotMonitor(monitorKey);

        FieldKey counterField = FieldKey.getFieldByName(fields, "counterPerSecond");
        assertNotNull("counterPerSecond field", counterField);
        assertEquals("counter type", FieldKey.DOUBLE_TYPE, counterField.getFieldType());

        FieldKey gaugeField = FieldKey.getFieldByName(fields, "activeThreads");
        assertNotNull("activeThreads field", gaugeField);
        assertEquals("gauge type (int return)", FieldKey.INTEGER_TYPE, gaugeField.getFieldType());

        FieldKey stringField = FieldKey.getFieldByName(fields, "status");
        assertNotNull("status field", stringField);
        assertEquals("string type", FieldKey.STRING_TYPE, stringField.getFieldType());
    }

/*----------------------------------------------------------------------------*/
    public void testSubscribeAndTakeSnapShotPOJO() throws Exception {
        final String className = ExternalTestPOJO.class.getName();

        ExternalTestPOJO pojo = new ExternalTestPOJO();
        registerPOJO(pojo, "MyInstance");

        MonitorKey monitorKey = MonitorKey.newSnapShotKey(className, "MyInstance");
        FieldKey[] fields = ExternalAppender.getFieldsForSnapShotMonitor(monitorKey);
        assertTrue("Should have found fields for the named POJO instance", fields.length > 0);

        MonitorKeyWithFields[] grouped = MonitorKeyWithFields.groupFields(fields);
        assertEquals("grouped.length", 1, grouped.length);

        ExternalAppender.subscribe(sessionID, grouped[0]);

        for (int i = 0; i < 5; i++) {
            pojo.increment();
        }
        Thread.sleep(1000);

        Map<FieldKey, Object> data = ExternalAppender.takeSnapShot(sessionID, grouped[0]);
        assertNotNull("takeSnapShot data", data);

        Object counterPerSecond = data.get(FieldKey.getFieldByName(fields, "counterPerSecond"));
        assertNotNull("counterPerSecond value", counterPerSecond);
        assertTrue("counterPerSecond should reflect the increments (got: " + counterPerSecond + ")",
            ((Number)counterPerSecond).doubleValue() > 0.0);

        Object activeThreads = data.get(FieldKey.getFieldByName(fields, "activeThreads"));
        assertNotNull("activeThreads value", activeThreads);
        assertEquals("activeThreads", 5, ((Number)activeThreads).intValue());

        Object status = data.get(FieldKey.getFieldByName(fields, "status"));
        assertEquals("status", "OK", status);
    }

/*----------------------------------------------------------------------------*/
    public void testTakeSnapShotAfterPOJODeRegisteredIsGraceful() throws Exception {
        final String className = ExternalTestPOJO.class.getName();

        ExternalTestPOJO pojo = new ExternalTestPOJO();
        registerPOJO(new ExternalTestPOJO(), "KeepEntryAlive"); // keeps the registry entry (Bundle) alive
        registerPOJO(pojo, "GoingAway");

        MonitorKey monitorKey = MonitorKey.newSnapShotKey(className, "GoingAway");
        FieldKey[] fields = ExternalAppender.getFieldsForSnapShotMonitor(monitorKey);
        MonitorKeyWithFields[] grouped = MonitorKeyWithFields.groupFields(fields);
        // groupFields may return multiple keys (one per instance); find ours.
        MonitorKeyWithFields subscribeKey = null;
        for (MonitorKeyWithFields g : grouped) {
            if (monitorKey.equals(g.getMonitorKeyOnly())) {
                subscribeKey = g;
            }
        }
        assertNotNull("Should have a key for the GoingAway instance", subscribeKey);

        ExternalAppender.subscribe(sessionID, subscribeKey);

        POJOSnapShotRegistry.getSingleton().deRegister(pojo, "GoingAway");
        registeredPOJOs.remove(registeredPOJOs.size() - 1);

        // The subscription outlives the instance -- sampling must not throw.
        Map<FieldKey, Object> data = ExternalAppender.takeSnapShot(sessionID, subscribeKey);
        assertNotNull("takeSnapShot should still return a (possibly empty) data map", data);
    }

/*----------------------------------------------------------------------------*/
    public void testLegacyRegistrationOfPOJOClassDoesNotDuplicateKeys() throws Exception {
        final String className = LegacyRegisteredPOJO.class.getName();

        // Simulate the pre-fix behavior of SnapShotManager: the POJO class name
        // lands in the ExternalAppender's legacy class-name registry.
        ExternalAppender.registerSnapShotClass(className);

        registerPOJO(new LegacyRegisteredPOJO(), "OnlyInstance");

        List<MonitorKey> keys = getSnapShotKeysForClass(className);
        assertEquals("Only the POJO-derived key should be visible -- no phantom no-instance key",
            1, keys.size());
        assertEquals("instance", "OnlyInstance", keys.get(0).getInstance());
    }

/*----------------------------------------------------------------------------*/
    public static void main(String[] args) {
        String[] testCaseName = {ExternalAppenderPOJOSnapShotTest.class.getName()};

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
//        newSuite.addTest(new ExternalAppenderPOJOSnapShotTest("testSubscribeAndTakeSnapShotPOJO"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(ExternalAppenderPOJOSnapShotTest.class);
        }

        return(newSuite);
    }
}
