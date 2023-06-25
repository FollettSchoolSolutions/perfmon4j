/*
 *	Copyright 2008-2013 Follett Software Company 
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
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTestCase;

import javassist.ClassPool;
import javassist.CtClass;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TransformerParamsTest extends PerfMonTestCase {
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
    
    public void testEnableHystrixIntstrumentation() {
    	TransformerParams params = new TransformerParams();
    	
    	assertFalse("hystrix instrumentation should NOT be on by default", params.isHystrixInstrumentationEnabled());
    
    	params = new TransformerParams("e=HYSTRIX");
    	assertTrue("e=HYSTRIX should enable hystrix instrumentation", params.isHystrixInstrumentationEnabled());
    	
    	params = new TransformerParams("-eHYSTRIX");
    	assertTrue("-eHYSTRIX should enable hystrix instrumentation", params.isHystrixInstrumentationEnabled());
    	
    	params = new TransformerParams("-eHystRIX");
    	assertTrue("-eHystRIX Hystrix should be case insensitive", params.isHystrixInstrumentationEnabled());
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
        TransformerParams params = new TransformerParams("-eorg.apache,-ecom.follett");
        
        assertEquals("log4j is blacklisted... Should not allow any logging regardless of the parameters", 
        		TransformerParams.MODE_NONE, 
            params.getTransformMode("org.apache.log4j.LogManager"));
        
        assertEquals("catalina is an org.apache package that is NOT blacklisted", 
        		TransformerParams.MODE_EXTREME, 
            params.getTransformMode("org.apache.catalina.RequestProcessor"));
        
        
        assertEquals("Should be included",
        		TransformerParams.MODE_EXTREME, params.getTransformMode("com.follett.yukon.session.PerfWorkerview"));
    
        assertEquals("Jboss's weld creates class files that include the pattern $Proxy$_$$.  When attempting to" +
        		"instrument these classes we get a Verify error - Inconsistent stack height 1 != 0.",
        		TransformerParams.MODE_NONE, params.getTransformMode("com.follett.yukon.util.HibernateHelper$Proxy$_$$_WeldClientProxy"));
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
    
    public void testPatternBasedAnnotation() {
    	// Perform annotation monitor on any class that ends with "Buffer"
        TransformerParams params = new TransformerParams("\"-aP(Buffer$)\"");
        
        assertEquals("String should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(String.class));
        
        assertEquals("String buffer should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(StringBuffer.class));        

        assertEquals("Must END in Buffer based on the RegEx", TransformerParams.MODE_NONE, 
                params.getTransformMode("StringBufferWithSomethingElse"));        
    
    }
    
    public void testPatternBasedExtreme() {
    	// Perform extreme monitor on any class that ends with "Buffer"
        TransformerParams params = new TransformerParams("\"-eP(Buffer$)\"");
        
        assertEquals("String should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should annotate", TransformerParams.MODE_EXTREME, 
            params.getTransformMode(StringBuffer.class));        
    }

    public void testPatternBasedIgnore() {
    	// Extreme monitor all classe in "com.acme" package, but ignore any
    	// classes in the com.acme.utils sub package
        TransformerParams params = new TransformerParams("\"-ecom,-iP(\\.acme\\.utils\\.)\"");
        
        assertEquals("This class should be included", TransformerParams.MODE_EXTREME, 
            params.getTransformMode("com.acme.MyClass"));
        assertEquals("This class should be NOT be included since it is in the utils package", TransformerParams.MODE_NONE, 
                params.getTransformMode("com.acme.utils.MyClass"));
    }
    
    public void testClassInSamePackage() {
    	// Extreme monitor all classe in "com.acme" package, but ignore any
    	// classes in the com.acme.utils sub package
        TransformerParams params = new TransformerParams("-ejava.lang.String");
        
        assertEquals("This class should be included", TransformerParams.MODE_EXTREME, 
            params.getTransformMode("java.lang.String"));
        assertEquals("This class should be NOT be included since it is in the utils package", TransformerParams.MODE_NONE, 
                params.getTransformMode("java.lang.Void"));
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
            // Made a change in version 1.5.1 - Debug NO longer includes verbose.
            assertFalse("Debug parameter DOES NOT enable verbose", params.isVerboseInstrumentationEnabled());
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
            	assertFalse("Debug system property does NOT also enable verbose", params.isVerboseInstrumentationEnabled());
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

        	public boolean isCloseOnCompletion() {
        		return false;
        	}
        	
        	public void closeOnCompletion() {
        	}
        	
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
            
            
            // Just test the rest at the class name interface...
            assertTrue("java.sql.PreparedStatement", params.isExtremeSQLInterface(java.sql.PreparedStatement.class.getName()));
            assertTrue("java.sql.CallableStatement", params.isExtremeSQLInterface(java.sql.CallableStatement.class.getName()));
            assertTrue("java.sql.Connection", params.isExtremeSQLInterface(java.sql.Connection.class.getName()));
        }
        
        /*----------------------------------------------------------------------------*/    
        public void testValidateInstallValve() throws Exception {
            TransformerParams params = new TransformerParams();
            assertFalse("Install servlet valve should be false by default", params.isInstallServletValve());
            
            params = new TransformerParams("-eVALVE");
            assertTrue("Parameter alone", params.isInstallServletValve());
            
            params = new TransformerParams("-ecom.follett.fsc,-eVALVE");
            assertTrue("Combined with anyting else...", params.isInstallServletValve());
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
//        	assertPossibleJDBCDriver(false, false, false, false, false, false, new TransformerParams());
        	assertPossibleJDBCDriver(true, true, true, true, true, false, new TransformerParams("-eSQL"));
        	assertPossibleJDBCDriver(true, false, false, false, false, false, new TransformerParams("-eSQL(JTDS)"));
        	assertPossibleJDBCDriver(false, true, false, false, false, false, new TransformerParams("-eSQL(POSTGRESQL)"));
        	assertPossibleJDBCDriver(false, false, true, false, false, false, new TransformerParams("-eSQL(MYSQL)"));
        	assertPossibleJDBCDriver(false, false, false, true, false, false, new TransformerParams("-eSQL(DERBY)"));
        	assertPossibleJDBCDriver(false, false, false, false, true, false, new TransformerParams("-eSQL(ORACLE)"));
        	// Can specify a partial package name...
        	assertPossibleJDBCDriver(false, false, false, false, false, true, new TransformerParams("-eSQL(org.perfmon4j.jdbc)"));
        }
        
        public void testMicrosoftAsPossibleJDBCDriver() {
        	TransformerParams params = new TransformerParams("-eSQL(MICROSOFT)");
        	assertTrue(params.isPossibleJDBCDriver("com.microsoft.sqlserver.jdbc.Driver"));
        	
        	params = new TransformerParams("-eSQL");
        	assertTrue(params.isPossibleJDBCDriver("com.microsoft.sqlserver.jdbc.Driver"));
        }
        
        public void testExportsParamsAnnotateClasses() {
        	TransformerParams params = new TransformerParams("-aorg.acme.products,-aorg.other.MyClass");
        	
        	Properties props = params.exportAsProperties();
        	
        	assertEquals("Annotation 1", "org.acme.products", props.get("perfmon4j.javaagent.annotation.class.1"));
        	assertEquals("Annotation 2", "org.other.MyClass", props.get("perfmon4j.javaagent.annotation.class.2"));
        }
        
        public void testExportsParamsExtremeClasses() {
        	TransformerParams params = new TransformerParams("-eorg.acme.products,-e(+getter,+setter)org.other.MyClass");
        	
        	Properties props = params.exportAsProperties();
        	
        	assertEquals("Extreme 1", "org.acme.products", props.get("perfmon4j.javaagent.extreme.class.1"));
        	assertEquals("Extreme 1", "false", props.get("perfmon4j.javaagent.extreme.class.1.option.instrumentSetters"));
        	assertEquals("Extreme 1", "false", props.get("perfmon4j.javaagent.extreme.class.1.option.instrumentGetters"));
        	
        	assertEquals("Extreme 2", "org.other.MyClass", props.get("perfmon4j.javaagent.extreme.class.2"));
        	assertEquals("Extreme 2", "true", props.get("perfmon4j.javaagent.extreme.class.2.option.instrumentSetters"));
        	assertEquals("Extreme 2", "true", props.get("perfmon4j.javaagent.extreme.class.2.option.instrumentGetters"));
        }

        public void testExportsParamsIgnoreClasses() {
        	TransformerParams params = new TransformerParams("-iorg.acme.products,-iorg.other.MyClass");
        	
        	Properties props = params.exportAsProperties();
        	
        	assertEquals("Ignore 1", "org.acme.products", props.get("perfmon4j.javaagent.ignore.class.1"));
        	assertEquals("Ignore 2", "org.other.MyClass", props.get("perfmon4j.javaagent.ignore.class.2"));
        }
        
        public void testExportsParamsExtremeSQL() {
        	// -eSQL includes packages for some commonly used JDBC drivers
        	TransformerParams params = new TransformerParams("-eSQL");
        	Properties props = params.exportAsProperties();
        	
        	// Indicate which jdbc packages should be tracked to measure SQL duration.
        	// Note: Time spent in JDBC objects is not an exact measure of SQL time, but in most
        	// applications it has a solid correlation and is a useful measure.
        	assertEquals("ExtremeSQL 1", "org.postgresql", props.get("perfmon4j.javaagent.sql.package.1"));
        	assertEquals("ExtremeSQL 2", "net.sourceforge.jtds", props.get("perfmon4j.javaagent.sql.package.2"));
        	assertEquals("ExtremeSQL 3", "com.mysql.jdbc", props.get("perfmon4j.javaagent.sql.package.3"));
        	assertEquals("ExtremeSQL 4", "org.apache.derby", props.get("perfmon4j.javaagent.sql.package.4"));
        	assertEquals("ExtremeSQL 5", "oracle.jdbc", props.get("perfmon4j.javaagent.sql.package.5"));
        	assertEquals("ExtremeSQL 6", "com.microsoft.sqlserver.jdbc", props.get("perfmon4j.javaagent.sql.package.6"));
        	
        	// You can also limit to specific supported named jdbc packages.
        	params = new TransformerParams("-eSQL(MICROSOFT)");
        	props = params.exportAsProperties();
        	
        	assertEquals("ExtremeSQL 1", "com.microsoft.sqlserver.jdbc", props.get("perfmon4j.javaagent.sql.package.1"));

        	// Finally you can name any specific package
        	params = new TransformerParams("-eSQL(org.acme.super.jdbc)");
        	props = params.exportAsProperties();
        	
        	assertEquals("ExtremeSQL 1", "org.acme.super.jdbc", props.get("perfmon4j.javaagent.sql.package.1"));
        }
        
        public void testInstallValve() {
        	// -eVALVE instructs perfmon4j to look for a location to install a "ServletValve"
        	// to monitor incoming web requests.
        	TransformerParams params = new TransformerParams("");
        	Properties props = params.exportAsProperties();

        	assertEquals("install-valve", "false", props.get("perfmon4j.javaagent.installServletValve"));
        	
        	params = new TransformerParams("-eVALVE");
        	props = params.exportAsProperties();
        	
        	assertEquals("install-valve", "true", props.get("perfmon4j.javaagent.installServletValve"));
        }

        public void testInstallHystrixMonitor() {
        	TransformerParams params = new TransformerParams("");
        	Properties props = params.exportAsProperties();

        	assertEquals("install-hystrix-monitor", "false", props.get("perfmon4j.javaagent.installHystrixMonitor"));
        	
        	params = new TransformerParams("-eHYSTRIX");
        	props = params.exportAsProperties();
        	
        	assertEquals("install-hystrix-monitor", "true", props.get("perfmon4j.javaagent.installHystrixMonitor"));
        }

        public void testInstallDebugEnabled() {
        	TransformerParams params = new TransformerParams("");
        	Properties props = params.exportAsProperties();

        	assertEquals("perfmon4j-debug-logging", "false", props.get("perfmon4j.javaagent.logging.debug"));
        	assertEquals("perfmon4j-verbose-logging", "false", props.get("perfmon4j.javaagent.logging.verbose"));
        	
        	params = new TransformerParams("-dtrue");
        	props = params.exportAsProperties();
        	
        	assertEquals("perfmon4j-debug-logging", "true", props.get("perfmon4j.javaagent.logging.debug"));
        	assertEquals("perfmon4j-verbose-logging", "false", props.get("perfmon4j.javaagent.logging.verbose"));

        	params = new TransformerParams("-vtrue"); // Verbose implicitly sets debug-logging = true
        	props = params.exportAsProperties();

        	assertEquals("perfmon4j-debug-logging", "true", props.get("perfmon4j.javaagent.logging.debug"));
        	assertEquals("perfmon4j-verbose-logging", "true", props.get("perfmon4j.javaagent.logging.verbose"));
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
