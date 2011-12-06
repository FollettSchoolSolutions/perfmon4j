/*
 *	Copyright 2008,2009,2011 Follett Software Company 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j.instrument;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

import javassist.ClassPool;
import javassist.CtClass;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;

public class TransformerParamsTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public TransformerParamsTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void testValidateGarbageParams() {
        try {
            new TransformerParams("this is garbage params");
            fail("Should validate params");
        } catch (RuntimeException re) {
            // Expected...
        }
    }

/*----------------------------------------------------------------------------*/
    /*
     * This is a change IN Version 1.1.1+ (Now the DEFAULT is NOT to 
     * Instrument ANY classes by default!  You must specify classes
     * with a "-a OR -e" parameter 
     */
    public void testValidateEmptyParamsAreValid() {
        TransformerParams params = new TransformerParams("");
        assertEquals("String should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(String.class));
    }
    

/*----------------------------------------------------------------------------*/
    /*
     * Empty params denotes to annotate everything
     */
    public void testPassXMLConfigFile() {
        TransformerParams params = new TransformerParams("f=c:/dave.xml");
        assertEquals("c:/dave.xml", params.getXmlFileToConfig());
    }
    
    
    public void testEnableBootstrapClassLoading() {
    	TransformerParams params = new TransformerParams();
    	
    	assertFalse("bootStrap instrumentation should NOT be on by default", params.isBootStrapInstrumentationEnabled());
    
    	params = new TransformerParams("b=anything");
    	assertFalse("b=anything should disable bootStrap instrumentation", params.isBootStrapInstrumentationEnabled());
    	
    	params = new TransformerParams("b=true");
    	assertTrue("b=true should enable bootStrap instrumentation", params.isBootStrapInstrumentationEnabled());
    	
    	params = new TransformerParams("b=TRUE");
    	assertTrue("b=TRUE should enable bootStrap instrumentation", params.isBootStrapInstrumentationEnabled());
    }

    /*----------------------------------------------------------------------------*/    
    public void testDisableSystemGC() {
    	TransformerParams params = new TransformerParams();
    	
    	assertFalse("Disable java.lang.System.gc should be off by default", params.isDisableSystemGC());
    
    	params = new TransformerParams("g=anything");
    	assertFalse("g=anything should not disable system gc", params.isDisableSystemGC());
    	
    	params = new TransformerParams("g=true");
    	assertTrue("g=true should disable system gc", params.isDisableSystemGC());
    	
    	params = new TransformerParams("g=TRUE");
    	assertTrue("g=TRUE should enable system gc", params.isDisableSystemGC());
    	
    	params = new TransformerParams("-gTRUE");
    	assertTrue("-gTRUE should enable system gc", params.isDisableSystemGC());
    }
    
    
/*----------------------------------------------------------------------------*/    
    public void testBlackList() {
        TransformerParams params = new TransformerParams("-eorg.apache");
        
        assertEquals("log4j is blacklisted... Should not allow any logging regardless of the parameters", 
        		TransformerParams.MODE_NONE, 
            params.getTransformMode("org.apache.log4j.LogManager"));
        
        assertEquals("catalina is an org.apache package that is NOT blacklisted", 
        		TransformerParams.MODE_EXTREME, 
            params.getTransformMode("org.apache.catalina.RequestProcessor"));        
    }


    /*----------------------------------------------------------------------------*/    
    public void testLimitToAnnotations() {
        TransformerParams params = new TransformerParams("\"a=java.lang.String a=org.perfmon4j\"");
        
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(StringBuffer.class));        
        assertEquals("org.perfmon4j.* classes should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(this.getClass()));        
    }
    
    

    /*----------------------------------------------------------------------------*/    
    public void testGetterSetterDisabledByDefault() {
        TransformerParams params = new TransformerParams("-ejava.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertFalse("instrument setters should be off by default", options.isInstrumentSetters());
        assertFalse("instrument getters should be off by default", options.isInstrumentGetters());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testEnableGetter() {
        TransformerParams params = new TransformerParams("-e(+getter)java.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertTrue("instrument getters should be enabled", options.isInstrumentGetters());
        assertFalse("instrument setters should default to false", options.isInstrumentSetters());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testEnableSetter() {
        TransformerParams params = new TransformerParams("-e(+setter)java.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertTrue("instrument setters should be enabled", options.isInstrumentSetters());
        assertFalse("instrument getters should default to false", options.isInstrumentGetters());
    }

    /*----------------------------------------------------------------------------*/    
    public void testEnableGetterAndSetter() {
        TransformerParams params = new TransformerParams("-e(+setter,+getter)java.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertTrue("instrument setters should be enabled", options.isInstrumentSetters());
        assertTrue("instrument getters should be enabled", options.isInstrumentGetters());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testExplicitDisable() {
        TransformerParams params = new TransformerParams("-e(-setter,-getter)java.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertFalse("instrument setters should NOT be enabled", options.isInstrumentSetters());
        assertFalse("instrument getters should NOT be enabled", options.isInstrumentGetters());
    }
    
    
    /*----------------------------------------------------------------------------*/    
    public void testLimitAnnotationsAndExtreme() {
        TransformerParams params = new TransformerParams("a=java.lang.String e=org.perfmon4j.instrument " +
        		"a=org.perfmon4j");
        
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(StringBuffer.class));        
        assertEquals("org.perfmon4j.instrument.* classes should be extreme", 
            TransformerParams.MODE_BOTH, params.getTransformMode(this.getClass()));        
        assertEquals("org.perfmon4j.* classes should be annotate", 
            TransformerParams.MODE_ANNOTATE, params.getTransformMode(PerfMon.class));        
    }    

    /*----------------------------------------------------------------------------*/    
    public void testClassInDefaultPackageNotInstrumented() throws Exception {
    	ClassPool classPool = ClassPool.getDefault();
    	CtClass cl = classPool.makeClass("$Bogus1");
    	Class<?> clazz = cl.toClass();
    	
    	TransformerParams params = new TransformerParams("");
    	
    	// Should look for Perfmon4j Timer annotations in all classes when no args are passed.
        assertEquals("Should not annotate class from the default package", 
        		TransformerParams.MODE_NONE, 
        		params.getTransformMode(clazz));
    }    
    
    
    /*----------------------------------------------------------------------------*/    
    public void testAllowsNBSP() {
        TransformerParams params = new TransformerParams("a=java.lang.String&nbsp;e=org.perfmon4j.instrument&nbsp;" +
        		"a=org.perfmon4j");
        
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(StringBuffer.class));        
        assertEquals("org.perfmon4j.instrument.* classes should be extreme", 
            TransformerParams.MODE_BOTH, params.getTransformMode(this.getClass()));        
        assertEquals("org.perfmon4j.* classes should be annotate", 
            TransformerParams.MODE_ANNOTATE, params.getTransformMode(PerfMon.class));        
    }    
    
    /*----------------------------------------------------------------------------*/    
    /**
     * In JBoss run.bat inserts the entire JAVA_OPTS in quotes.  Since nested quotes do not
     * work we need a javaagent string that does not contain whitespace.
     * 
     */
    public void testJBossCompatibleJavaAgent() {
        TransformerParams params = new TransformerParams("-ajava.lang.String,-eorg.perfmon4j.instrument,-btrue");
        
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(StringBuffer.class));        
        assertEquals("org.perfmon4j.instrument.* classes should be extreme", 
            TransformerParams.MODE_EXTREME, params.getTransformMode(this.getClass()));        
        assertTrue("Should be doing bootstrap instrumentation", params.isBootStrapInstrumentationEnabled());        
    }    

    public void testValidateReloadFileParam() {
        TransformerParams params = new TransformerParams("-r10");
        
        assertEquals(10, params.getReloadConfigSeconds());

        params = new TransformerParams("-rgarbage");
        assertEquals("If we cant parse it use the default", 60, params.getReloadConfigSeconds());
        
        params = new TransformerParams("-r1");
        assertEquals("Minimum allowed is 10 seconds", 10, params.getReloadConfigSeconds());

        params = new TransformerParams("-r0");
        assertEquals("0 OR less is used to indicate no reloading at all", 0, params.getReloadConfigSeconds());
        
        params = new TransformerParams("-r-1");
        assertEquals("0 is used to indicate no reloading at all", 0, params.getReloadConfigSeconds());
    }    
    

    public void testValidateRemoteManagementPort() {
        TransformerParams params = new TransformerParams("");
        assertFalse(params.isRemoteManagementEnabled());
        assertEquals(-1, params.getRemoteManagementPort());

    	params = new TransformerParams("-p5945");
        assertTrue(params.isRemoteManagementEnabled());
        assertEquals(5945, params.getRemoteManagementPort());
        
        
        params = new TransformerParams("-pAUTO");
        assertTrue(params.isRemoteManagementEnabled());
        assertEquals(TransformerParams.REMOTE_PORT_AUTO, params.getRemoteManagementPort());
     
        params = new TransformerParams("-pauto");
        assertTrue(params.isRemoteManagementEnabled());
        assertEquals(TransformerParams.REMOTE_PORT_AUTO, params.getRemoteManagementPort());
        
    }    
    
    
    /*----------------------------------------------------------------------------*/    
    public void testIgnoreList() {
    	// Annotate everything in java.lang except for java.lang.String
        TransformerParams params = new TransformerParams("-ajava.lang,-ijava.lang.String,-b=true");
        
        assertEquals("Object should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(Object.class));
        assertEquals("String should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(String.class));        

        // Ignore everything in java.lang EXCEPT for java.lang.String
        params = new TransformerParams("-ijava.lang,-ejava.lang.String,-b=true");
        
        assertEquals("Object should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(Object.class));
        assertEquals("String shouldannotate", TransformerParams.MODE_EXTREME, 
            params.getTransformMode(String.class));        
    }    
    
/*----------------------------------------------------------------------------*/    
    public void testValidateNoParams() {
        TransformerParams params = new TransformerParams();
        
        assertEquals("Default params should enable annotations on all classes", TransformerParams.MODE_NONE, 
            params.getTransformMode(String.class));
        
        params = new TransformerParams("");
        
        assertEquals("Default params should enable annotations on all classes", TransformerParams.MODE_NONE, 
            params.getTransformMode(String.class));
    }
    
    
    /*----------------------------------------------------------------------------*/    
        public void testValidateDebugFlag() {
            TransformerParams params = new TransformerParams();
            assertFalse("On default debug should be off", params.isDebugEnabled());
            
            params = new TransformerParams("-dtrue");
            assertTrue("Parameter alone", params.isDebugEnabled());
            assertTrue("Debug parameter also enables verbose", params.isVerboseInstrumentationEnabled());
            // When debug is enabled, verbose will also be enabled.
            
            params = new TransformerParams("-dfalse");
            assertFalse("Can force it false", params.isDebugEnabled());
            
            params = new TransformerParams("-danything");
            assertFalse("Anything value other than \"true\" is considered false", params.isDebugEnabled());
            
            
            
            // System property can change the default
            System.setProperty("PerfMon4j.debugEnabled", "true");
            try {
            	params = new TransformerParams();
            	assertTrue("System property can set debug on by default", params.isDebugEnabled());
            	assertTrue("Debug system property also enables verbose", params.isVerboseInstrumentationEnabled());
            } finally {
            	System.getProperties().remove("PerfMon4j.debugEnabled");
            }
        }
        
/*----------------------------------------------------------------------------*/    
        public void testValidateVerboseFlag() {
            TransformerParams params = new TransformerParams();
            assertFalse("On default verbose should be off", params.isVerboseInstrumentationEnabled());
            
            params = new TransformerParams("-vtrue");
            assertTrue("Parameter alone", params.isVerboseInstrumentationEnabled());
            
            params = new TransformerParams("-vfalse");
            assertFalse("Can force it false", params.isVerboseInstrumentationEnabled());
            
            params = new TransformerParams("-vanything");
            assertFalse("Anything value other than \"true\" is considered false", params.isVerboseInstrumentationEnabled());
        }
        
        
        
        private static class StatementImpl implements java.sql.Statement {

			public void addBatch(String sql) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void cancel() throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void clearBatch() throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void clearWarnings() throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void close() throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public boolean execute(String sql) throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean execute(String sql, int autoGeneratedKeys)
					throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean execute(String sql, int[] columnIndexes)
					throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean execute(String sql, String[] columnNames)
					throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public int[] executeBatch() throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}

			public ResultSet executeQuery(String sql) throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}

			public int executeUpdate(String sql) throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public int executeUpdate(String sql, int autoGeneratedKeys)
					throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public int executeUpdate(String sql, int[] columnIndexes)
					throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public int executeUpdate(String sql, String[] columnNames)
					throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public Connection getConnection() throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}

			public int getFetchDirection() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public int getFetchSize() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public ResultSet getGeneratedKeys() throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}

			public int getMaxFieldSize() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public int getMaxRows() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public boolean getMoreResults() throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean getMoreResults(int current) throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public int getQueryTimeout() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public ResultSet getResultSet() throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}

			public int getResultSetConcurrency() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public int getResultSetHoldability() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public int getResultSetType() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public int getUpdateCount() throws SQLException {
				// TODO Auto-generated method stub
				return 0;
			}

			public SQLWarning getWarnings() throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}

			public boolean isClosed() throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public boolean isPoolable() throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public void setCursorName(String name) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void setEscapeProcessing(boolean enable) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void setFetchDirection(int direction) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void setFetchSize(int rows) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void setMaxFieldSize(int max) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void setMaxRows(int max) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void setPoolable(boolean poolable) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public void setQueryTimeout(int seconds) throws SQLException {
				// TODO Auto-generated method stub
				
			}

			public boolean isWrapperFor(Class<?> iface) throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}

			public <T> T unwrap(Class<T> iface) throws SQLException {
				// TODO Auto-generated method stub
				return null;
			}
        	
        }
        
        
        
/*----------------------------------------------------------------------------*/    
        public void testValidateSQLExtreme() throws Exception {
            TransformerParams params = new TransformerParams();
            assertFalse("SQL Extreme instrumentation should be off by default", params.isExtremeSQLMonitorEnabled());
            
            params = new TransformerParams("-eSQL");
            assertTrue("Parameter alone", params.isExtremeSQLMonitorEnabled());
            
            params = new TransformerParams("-ecom.follett.fsc,-eSQL");
            assertTrue("Combined with anyting else...", params.isExtremeSQLMonitorEnabled());
            
            // Now check the classes that are used to monitor SQL statement execution time.
            assertFalse("Just a gut check with a non SQL class!", params.isExtremeSQLClass(String.class));
            
            assertTrue("java.sql.Statement", params.isExtremeSQLClass(StatementImpl.class));
            assertTrue("java.sql.Statement", params.isExtremeSQLClass(ClassPool.getDefault().get(StatementImpl.class.getName())));
            
            // Just test the rest at the class name interface...
            assertTrue("java.sql.PreparedStatement", params.isExtremeSQLInterface(java.sql.PreparedStatement.class.getName()));
            assertTrue("java.sql.CallableStatement", params.isExtremeSQLInterface(java.sql.CallableStatement.class.getName()));
            assertTrue("java.sql.Connection", params.isExtremeSQLInterface(java.sql.Connection.class.getName()));
        }
        

        private void assertPossibleJDBCDriver(boolean jtdsEnabled, boolean postgresEnabled, 
        	boolean mySQLEnabled, boolean derbyEnabled, boolean oracleEnabled, boolean otherEnabled,
        	TransformerParams params) {

            assertEquals("expected JTDS enabled state", jtdsEnabled, params.isPossibleJDBCDriver("net.sourceforge.jtds.Driver"));
            assertEquals("expected Postgres enabled state", postgresEnabled, params.isPossibleJDBCDriver("org.postgresql.Driver"));
            assertEquals("expected mysql enabled state", mySQLEnabled, params.isPossibleJDBCDriver("com.mysql.jdbc.Driver"));
            assertEquals("expected derby enabled state", derbyEnabled, params.isPossibleJDBCDriver("org.apache.derby.jdbc.EmbeddedDriver"));
            assertEquals("expected oracle enabled state", oracleEnabled, params.isPossibleJDBCDriver("oracle.jdbc.driver.OracleDriver"));
            assertEquals("expected other enabled state", otherEnabled, params.isPossibleJDBCDriver("org.perfmon4j.jdbc.Driver"));
        }
        
        
        public void testIsPossibleJDBCDriverClass() {
        	assertPossibleJDBCDriver(true, true, true, true, true, true, new TransformerParams());
        	assertPossibleJDBCDriver(true, true, true, true, true, true, new TransformerParams("-eSQL"));
        	assertPossibleJDBCDriver(true, false, false, false, false, false, new TransformerParams("-eSQL(JTDS)"));
        	assertPossibleJDBCDriver(false, true, false, false, false, false, new TransformerParams("-eSQL(POSTGRESQL)"));
        	assertPossibleJDBCDriver(false, false, true, false, false, false, new TransformerParams("-eSQL(MYSQL)"));
        	assertPossibleJDBCDriver(false, false, false, true, false, false, new TransformerParams("-eSQL(DERBY)"));
        	assertPossibleJDBCDriver(false, false, false, false, true, false, new TransformerParams("-eSQL(ORACLE)"));
        	// Can specify a partial package name...
        	assertPossibleJDBCDriver(false, false, false, false, false, true, new TransformerParams("-eSQL(org.perfmon4j.jdbc)"));
        }

/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(TransformerParamsTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {TransformerParamsTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new TransformerParamsTest("testClassInDefaultPackageNotInstrumented"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(TransformerParamsTest.class);
        }

        return( newSuite);
    }
}
