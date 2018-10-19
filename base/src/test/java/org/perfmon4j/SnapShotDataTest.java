package org.perfmon4j;

import java.util.Map;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;

public class SnapShotDataTest extends PerfMonTestCase {

    public SnapShotDataTest(String name) {
        super(name);
    }
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public static class DeltaTestClass extends SnapShotData {

		public Delta getValue() {
			return new Delta(0, 6000, 1000);
		}
		
		public double getOtherValuePerSecond() {
			return 100.0d;
		}
		
		public String toAppenderString() {
			return "";
		}
		
	}
	
	public void testGetFieldDataForDelta() throws Exception {
		MonitorKey monitorKey = MonitorKey.newSnapShotKey("dave");
		FieldKey deltaKey = new FieldKey(monitorKey, "valuePerSecond", FieldKey.DOUBLE_TYPE);
		FieldKey doubleKey = new FieldKey(monitorKey, "otherValuePerSecond", FieldKey.DOUBLE_TYPE);
		
		SnapShotData d = new DeltaTestClass();
		
		Map<FieldKey, Object> data = d.getFieldData(new FieldKey[]{deltaKey, doubleKey});
		
		// Should be able to find "normal" double field that ends in "PerSecond".
		Double v1 = (Double)data.get(doubleKey);
		
		assertNotNull("Value for otherValuePerSecond", v1);
		assertEquals("Value for otherValuePerSecond", Double.valueOf(100.0d), v1);

		// Should be able to find Delta double field where the getter does NOT end in perSecond.
		Double v2 = (Double)data.get(deltaKey);
		
		assertNotNull("Value for value", v2);
		assertEquals("Value for ovalue", Double.valueOf(6000.0d), v2);
	
	}

	public static class RatioTestClass extends SnapShotData {

		public Ratio getValue() {
			return new Ratio(100, 5);
		}
		
		public float getOtherValuePercent() {
			return 100.0f;
		}
		
		public String toAppenderString() {
			return "";
		}
	}
	
	public void testGetFieldDataForRatio() throws Exception {
		MonitorKey monitorKey = MonitorKey.newSnapShotKey("dave");
		FieldKey ratioKey = new FieldKey(monitorKey, "value", FieldKey.DOUBLE_TYPE);
		FieldKey doubleKey = new FieldKey(monitorKey, "otherValuePercent", FieldKey.DOUBLE_TYPE);
		
		SnapShotData d = new RatioTestClass();
		
		Map<FieldKey, Object> data = d.getFieldData(new FieldKey[]{ratioKey, doubleKey});
		
		// Should be able to find "normal" double field that ends in "Percent".
		Double v1 = (Double)data.get(doubleKey);
		
		assertNotNull("Value for otherValuePercent", v1);
		assertEquals("Value for otherValuePercent", Double.valueOf(100.0d), Double.valueOf(Math.round(v1.doubleValue())));


		// Should be able to find Ratio double field where the getter does NOT end in percent.
		Double v2 = (Double)data.get(ratioKey);
		
		assertNotNull("Value for value", v2);
		assertEquals("Value for ovalue", Double.valueOf(20.0d), Double.valueOf(Math.round(v2.doubleValue())));
	}
	
	
	/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {SnapShotDataTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(SnapShotDataTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new SnapShotDataTest("testSimpleGetMonitorKeyWithFields"));
        
        
//        newSuite.addTest(new SnapShotDataTest("testSimpleGetMonitorKeyWithStringField"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(SnapShotDataTest.class);
        }

        return(newSuite);
    }	
}
