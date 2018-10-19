package org.perfmon4j;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.instrument.JavassistRuntimeTimerInjector;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;
import org.perfmon4j.util.JDBCHelper;

public abstract class PerfMonTestCase extends TestCase {
    private Level originalAppender = null;
    private Level originalPerfMon = null;
    private Level originalJSS = null;
    
    private Level originalJDBCHelper = null;
    private Level originalJDBCSQLAppender = null;
    private Level originalJRTTI = null;
    
	public PerfMonTestCase() {
		super();
	}

	public PerfMonTestCase(String name) {
		super(name);
	}

	/*----------------------------------------------------------------------------*/    
    protected void setUp() throws Exception {
        originalAppender = resetLogLevel(Appender.class, Level.INFO);
        originalPerfMon = resetLogLevel(PerfMon.class, Level.INFO);
        originalJSS = resetLogLevel(JavassistSnapShotGenerator.class, Level.INFO);

        originalJDBCHelper = resetLogLevel(JDBCHelper.class, Level.INFO);
        originalJDBCSQLAppender = resetLogLevel(JDBCSQLAppender.class, Level.INFO);
        originalJRTTI = resetLogLevel(JavassistRuntimeTimerInjector.class, Level.INFO);

        super.setUp();
    }
    
/*----------------------------------------------------------------------------*/    
    protected void tearDown() throws Exception {
        super.tearDown();
        
        resetLogLevel(Appender.class, originalAppender);
        resetLogLevel(PerfMon.class, originalPerfMon);
        resetLogLevel(JavassistSnapShotGenerator.class, originalJSS);

        resetLogLevel(JDBCHelper.class, originalJDBCHelper);
        resetLogLevel(JDBCSQLAppender.class, originalJDBCSQLAppender);
        resetLogLevel(JavassistRuntimeTimerInjector.class, originalJRTTI);
    }
    
    /*----------------------------------------------------------------------------*/    
    private Level resetLogLevel(Class<?> clazz, Level level) {
    	Logger logger = Logger.getLogger(clazz);
    	Level originalLevel = logger.getLevel();
    	if (level != null) {
    		logger.setLevel(level);
    	}
    	
    	return originalLevel;
    }    
}
