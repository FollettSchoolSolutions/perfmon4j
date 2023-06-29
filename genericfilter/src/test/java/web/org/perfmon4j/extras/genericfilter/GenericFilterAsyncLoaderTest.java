package web.org.perfmon4j.extras.genericfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import junit.framework.TestCase;

public class GenericFilterAsyncLoaderTest extends TestCase {
	private static final Logger logger = LoggerFactory.initLogger(GenericFilterAsyncLoaderTest.class);
	private static final AtomicReference<Properties> mockConfiguredSettings = new AtomicReference<Properties>(new Properties());

	public void setUp() throws Exception {
		mockConfiguredSettings.set(new Properties());
		super.setUp();
	}
	
	
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testLoadGenericFilter_PerfmonConfigNotLoaded() throws Exception {
		TestLoader loader = new TestLoader();
		loader.scheduleLoad();
		
		Thread.sleep(300);
		
		String[] logOutput = loader.getInfoMsgs();
//for (String msg : logOutput) {
//	System.out.println(msg);
//}
		
		assertNull("Filter should not have been initialized", loader.getGenericFilterIfInitialized());
		assertTrue("Last message should indicate filter will not be installed", 
				logOutput[logOutput.length-1].endsWith("Will not be installed."));
	}
	
	public void testLoadGenericFilter_PerfmonConfigNotLoaded_DefaultLoaded() throws Exception {
		TestLoader loader = new TestLoader(true);
		loader.scheduleLoad();
		
		Thread.sleep(300);
		
		String[] logOutput = loader.getInfoMsgs();
//for (String msg : logOutput) {
//	System.out.println(msg);
//}
		assertNotNull("Filter should not have been initialized", loader.getGenericFilterIfInitialized());
		assertTrue("Second to last message should indicate filter will be loaded using defaults", 
				logOutput[logOutput.length-2].endsWith("Will load using default parameters."));
		assertTrue("Last message should indicate filter was installed", 
				logOutput[logOutput.length-1].endsWith("GenericFilter installed."));
	}

	public void testLoadGenericFilter_PerfmonConfigLoadedBeforeSchedule() throws Exception {
		TestLoader loader = new TestLoader(true);
		loader.scheduleLoad();
		
		Properties props = new Properties();
		props.setProperty("perfmon4j.bootConfigSettings.loaded", Boolean.TRUE.toString());
		mockConfiguredSettings.set(props);
		
		
		Thread.sleep(300);
		
		String[] logOutput = loader.getInfoMsgs();
//for (String msg : logOutput) {
//	System.out.println(msg);
//}
		assertNotNull("Filter should not have been initialized", loader.getGenericFilterIfInitialized());
		assertEquals("Expected number of log messages", 1, logOutput.length);
		assertTrue("Message should indicate filter was installed", 
				logOutput[0].endsWith("GenericFilter installed."));
	}
	
	public void testLoadGenericFilter_PerfmonConfigNotInitiallyLoaded() throws Exception {
		TestLoader loader = new TestLoader(true);
		loader.scheduleLoad();
	
		Thread.sleep(70);
		
		Properties props = new Properties();
		props.setProperty("perfmon4j.bootConfigSettings.loaded", Boolean.TRUE.toString());
		mockConfiguredSettings.set(props); // Simulate perfmonconfig.xml being loaded after first attempt
		
		Thread.sleep(225);
		
		String[] logOutput = loader.getInfoMsgs();
//for (String msg : logOutput) {
//	System.out.println(msg);
//}
		assertEquals("Expected number of log messages", 2, logOutput.length);
		assertTrue("First message should indicate filter was not installed and we'll try again", 
				logOutput[0].endsWith("Will attempt to load again in 0 second(s).")); // We are only waiting 3 milliseconds, so this will be rounded down to 0 seconds
		assertTrue("Last message should indicate filter was installed", 
				logOutput[1].endsWith("GenericFilter installed."));
	}
	
	private class TestLoader extends GenericFilterAsyncLoader {
		private final List<String> infoMsgs = new ArrayList<String>();
		
		public TestLoader() {
			this(false);
		}
		
		public TestLoader(boolean loadWithDefaults) {
			super("GenericFilter", loadWithDefaults, 50, 3);
		}

		public String[] getInfoMsgs() {
			synchronized (infoMsgs) {
				return infoMsgs.toArray(new String[] {});
			}
		}
		
		@Override
		protected Properties retrieveConfiguredSettings() {
			// TODO Auto-generated method stub
			return mockConfiguredSettings.get();
		}

		@Override
		protected GenericFilter initGenericFilter(FilterParams params) {
			return new GenericFilter(params) {

				@Override
				protected void logInfo(String value) {
					logger.logInfo(value);
				}

				@Override
				protected void logInfo(String value, Exception ex) {
					logger.logInfo(value, ex);
				}
			};
		}

		@Override
		protected void logInfo(String value) {
			synchronized (infoMsgs) {
				infoMsgs.add(value);
			}
		}
	}
}
