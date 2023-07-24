package org.perfmon4j;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotPOJO;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class POJOSnapShotRegistryTest extends TestCase {
	
	@SnapShotPOJO
	public static class POJO {
		@SnapShotCounter
		public long getCounter() {
			return 0;
		}
	}
	
	public void testWeakReferenceRegistration() throws Exception {
		/*
		SnapShotPOJORegistry registry = new SnapShotPOJORegistry();
		final String pojoClassName = POJO.class.getName(); 
		
		SnapShotPOJORegistry.POJOInstance[] pojoInstances = registry.getInstances(pojoClassName);
		assertNotNull(pojoInstances);
		assertEquals("pojoInstances.length", 0, pojoInstances.length);
		
		POJO pojo = new POJO();
		registry.register(pojo, true);
		
		pojoInstances = registry.getInstances(pojoClassName);
		assertEquals("pojoInstances.length", 1, pojoInstances.length);
		
		POJOInstance instance = pojoInstances[0];
		assertTrue("Should be active as long as it is not dereferrenced", instance.isActive());
		
		pojo = null;
		System.gc();
		
		assertFalse("Should no longer be active once it is dreferenced", instance.isActive());
		*/
	}
	
	public void testStrongReferenceRegistration() throws Exception {
		/*
		SnapShotPOJORegistry registry = new SnapShotPOJORegistry();
		final String pojoClassName = POJO.class.getName(); 
		
		POJO pojo = new POJO();
		registry.register(pojo, false);
		
		SnapShotPOJORegistry.POJOInstance[] pojoInstances = registry.getInstances(pojoClassName);
		assertEquals("pojoInstances.length", 1, pojoInstances.length);
		
		POJOInstance instance = pojoInstances[0];
		assertTrue("Should be active as long as it is not dereferrenced", instance.isActive());
		
		pojo = null;
		System.gc();
		
		assertTrue("Since it was not registered as a weak reference, registry should maintain a strong reference", instance.isActive());
		*/
	}
	
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {SnapShotManagerTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(POJOSnapShotRegistryTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new SnapShotManagerTest("testSimpleGetMonitorKeyWithFieldsFromMonitorWithInstances"));
//        newSuite.addTest(new SnapShotManagerTest("testSimpleGetMonitorKeyWithStringField"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(POJOSnapShotRegistryTest.class);
        }

        return(newSuite);
    }
}
