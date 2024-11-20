package org.perfmon4j.demo;

import org.perfmon4j.Appender;
import org.perfmon4j.POJOSnapShotRegistry;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.TextAppender;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotPOJO;

public class POJOSnapShotDemo {
	@SnapShotPOJO
	public static class MySnapShotPOJO {
		private long counter = 0;

		@SnapShotCounter
		public long getCounter() {
			return ++counter;
		}
	}

	static void configurePerfmon4j() throws Exception {
		final String monitorName = "MySnapShot";
		final String appenderName = "textAppender";
		PerfMonConfiguration config = new PerfMonConfiguration();

		config.defineAppender(appenderName, TextAppender.class.getName(), "5 seconds", null);

		config.defineSnapShotMonitor(monitorName, MySnapShotPOJO.class.getName());
		config.attachAppenderToSnapShotMonitor(monitorName, appenderName);

		PerfMon.configure(config);
	}

	public static void main(String[] args) {
		try {
			MySnapShotPOJO pojo = new MySnapShotPOJO();
			POJOSnapShotRegistry.getSingleton().register(pojo);
			configurePerfmon4j();

			Thread.sleep(60000);

			Appender.flushAllAppenders();

			// DeRegister our pojo
			POJOSnapShotRegistry.getSingleton().deRegister(pojo);
				
				// Clear out lastAppender output.
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
