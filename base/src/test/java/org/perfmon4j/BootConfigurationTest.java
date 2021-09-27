package org.perfmon4j;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class BootConfigurationTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";
    
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public static class TestAllProperties {
		private String baseFilterCategory = null;
	    private String abortTimerOnURLPattern = null;
	    private String skipTimerOnURLPattern = null;
	    private String pushCookiesOnNDC = null;
	    private String pushSessionAttributesOnNDC = null;
	    
	    private Boolean abortTimerOnRedirect = null;
	    private Boolean abortTimerOnImageResponse = null;
	    private Boolean outputRequestAndDuration = null;
	    private Boolean pushClientInfoOnNDC = null;
	    private String servletPathTransformationPattern = null;
	    
		public void setBaseFilterCategory(String baseFilterCategory) {
			this.baseFilterCategory = baseFilterCategory;
		}
		public void setAbortTimerOnRedirect(boolean abortTimerOnRedirect) {
			this.abortTimerOnRedirect = Boolean.valueOf(abortTimerOnRedirect);
		}
		public void setAbortTimerOnImageResponse(boolean abortTimerOnImageResponse) {
			this.abortTimerOnImageResponse = Boolean.valueOf(abortTimerOnImageResponse);
		}
		public void setAbortTimerOnURLPattern(String abortTimerOnURLPattern) {
			this.abortTimerOnURLPattern = abortTimerOnURLPattern;
		}
		public void setSkipTimerOnURLPattern(String skipTimerOnURLPattern) {
			this.skipTimerOnURLPattern = skipTimerOnURLPattern;
		}
		public void setOutputRequestAndDuration(boolean outputRequestAndDuration) {
			this.outputRequestAndDuration = Boolean.valueOf(outputRequestAndDuration);
		}
		public void setPushCookiesOnNDC(String pushCookiesOnNDC) {
			this.pushCookiesOnNDC = pushCookiesOnNDC;
		}
		public void setPushSessionAttributesOnNDC(String pushSessionAttributesOnNDC) {
			this.pushSessionAttributesOnNDC = pushSessionAttributesOnNDC;
		}
		public void setPushClientInfoOnNDC(boolean pushClientInfoOnNDC) {
			this.pushClientInfoOnNDC = Boolean.valueOf(pushClientInfoOnNDC);
		}
		public String getServletPathTransformationPattern() {
			return servletPathTransformationPattern;
		}
		public void setServletPathTransformationPattern(String servletPathTransformationPattern) {
			this.servletPathTransformationPattern = servletPathTransformationPattern;
		}
	}
	
	public void testCopyServletValveProperties() throws Exception {
		BootConfiguration.ServletValveConfig valveConfig = new BootConfiguration.ServletValveConfig();
		
		valveConfig.setAbortTimerOnURLPattern("");
		valveConfig.setPushCookiesOnNDC("");
		valveConfig.setPushSessionAttributesOnNDC("");
		valveConfig.setSkipTimerOnURLPattern("");
		valveConfig.setServletPathTransformationPattern("/this/ => /that/");		
		TestAllProperties props = new TestAllProperties();
		valveConfig.copyProperties(props);
		
		assertNotNull(props.baseFilterCategory);
		assertNotNull(props.abortTimerOnURLPattern);
		assertNotNull(props.skipTimerOnURLPattern);
		assertNotNull(props.pushCookiesOnNDC);
		assertNotNull(props.pushSessionAttributesOnNDC);
		assertNotNull(props.abortTimerOnRedirect);
		assertNotNull(props.abortTimerOnImageResponse);
		assertNotNull(props.outputRequestAndDuration);
		assertNotNull(props.pushClientInfoOnNDC);
		assertNotNull(props.servletPathTransformationPattern);
	}
	
	public static class TestOneProperty {
		private String baseFilterCategory = null;

		public void setBaseFilterCategory(String baseFilterCategory) {
			this.baseFilterCategory = baseFilterCategory;
		}
	}

	public void testCopyServletValveSingleProperty() throws Exception {
		BootConfiguration.ServletValveConfig valveConfig = new BootConfiguration.ServletValveConfig();
		
		TestOneProperty props = new TestOneProperty();
		valveConfig.copyProperties(props);
		
		assertNotNull(props.baseFilterCategory);
	}
	
	/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(BootConfigurationTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {BootConfigurationTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new BootConfigurationTest("testGetAppendersForMonitor"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(BootConfigurationTest.class);
        }

        return( newSuite);
    }
}
