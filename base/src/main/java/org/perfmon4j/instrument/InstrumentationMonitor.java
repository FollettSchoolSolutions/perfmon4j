package org.perfmon4j.instrument;

import org.perfmon4j.PerfMon;
import org.perfmon4j.instrument.SnapShotCounter.Display;
import org.perfmon4j.util.GlobalClassLoader;
import org.perfmon4j.util.LoggerFactory;



@SnapShotProvider(type = SnapShotProvider.Type.STATIC)
public class InstrumentationMonitor {
	private static long classesInst = 0;
	private static long methodsInst = 0;
	private static long classInstFailures = 0;
	private static long bootstrapClassesInst = 0;
	private static long instrumentationMillis = 0;
	
	/**
	 * Indicates the number of classes that were not instrumented 
	 * because of potential instrumentation recursion issues...
	 */
	private static long recursionSkipCount = 0;
	
	private static int maxInstThreads = 0;
	private static int currentInstThreads = 0;
	
	
	@SnapShotCounter(preferredDisplay=Display.DELTA_PER_MIN)
	public static long getNumClassesInst() {
		return classesInst;
	}
	
	@SnapShotGauge
	public static long getTotalClassesInst() {
		return classesInst;
	}
	
	public static void incClassesInst() {
		classesInst++;
	}
	
	@SnapShotCounter(preferredDisplay=Display.DELTA_PER_MIN)
	public static long getNumMethodsInst() {
		return methodsInst;
	}
	
	@SnapShotGauge
	public static long getTotalMethodsInst() {
		return methodsInst;
	}
	
	public static void incMethodsInst(int incValue) {
		methodsInst += incValue;
	}

	@SnapShotCounter(preferredDisplay=Display.DELTA_PER_MIN)
	public static long getNumClassInstFailures() {
		return classInstFailures;
	}
	
	@SnapShotGauge
	public static long getTotalClassInstFailures() {
		return classInstFailures;
	}
	
	public static void incClassInstFailures() {
		classInstFailures++;
	}
	
	@SnapShotCounter(preferredDisplay=Display.DELTA_PER_MIN)
	public static long getNumBootstrapClassesInst() {
		return bootstrapClassesInst;
	}
	
	@SnapShotGauge
	public static long getTotalBootstrapClassesIns() {
		return bootstrapClassesInst;
	}
	
	public static void incBootstrapClassesInst() {
		bootstrapClassesInst++;
	}

	@SnapShotCounter(preferredDisplay=Display.DELTA_PER_MIN)
	public static long getInstrumentationMillis() {
		return instrumentationMillis;
	}
	
	@SnapShotGauge
	public static long getTotalInstrumentationMillis() {
		return instrumentationMillis;
	}
	
	public static void incInstrumentationMillis(long incValue) {
		instrumentationMillis += incValue;
	}

	@SnapShotCounter(preferredDisplay=Display.DELTA_PER_MIN)
	public static long getRecursionSkipCount() {
		return recursionSkipCount;
	}
	
	@SnapShotGauge
	public static long getTotalRecursionSkipCount() {
		return recursionSkipCount;
	}
	
	public static void incRecursionSkipCount() {
		recursionSkipCount++;
	}
	
	private static Object INST_THREAD_LOCK_TOKEN = new Object(); 
	
	public static void incCurrentInstThreads() {
		synchronized(INST_THREAD_LOCK_TOKEN) {
			currentInstThreads++;
			if (currentInstThreads > maxInstThreads) {
				maxInstThreads = currentInstThreads;
			}
		}
	}

	public static void decCurrentInstThreads() {
		synchronized(INST_THREAD_LOCK_TOKEN) {
			currentInstThreads--;
		}
	}
	
	/**
	 * The current number of concurrent instrumentation threads
	 */
	@SnapShotGauge
	public static int getCurrentInstThreads() {
		return currentInstThreads;
	}
	
	/**
	 * The maximum number (all time) of concurrent instrumentation threads 
	 */
	@SnapShotGauge
	public static int getMaxInstThreads() {
		return maxInstThreads;
	}
	
	@SnapShotString
	public static String getLoggingFramework() {
		return LoggerFactory.getLoggingFramework();
	}

	@SnapShotCounter(preferredDisplay=Display.DELTA_PER_MIN)
	public static long getCLLoadRequestCount() {
		return GlobalClassLoader.getClassLoader().getLoadRequestCount();
	}

	@SnapShotCounter(preferredDisplay=Display.DELTA_PER_MIN)
	public static long getCLLoadAttemptsCount() {
		return GlobalClassLoader.getClassLoader().getLoadAttemptsCount();
	}

	@SnapShotGauge
	public static long getCLTotalClassLoaders() {
		return GlobalClassLoader.getClassLoader().getTotalClassLoaders();
	}

	@SnapShotGauge
	public static long getCLCurrentClassLoaders() {
		return GlobalClassLoader.getClassLoader().getCurrentClassLoaders();
	}

	@SnapShotGauge
	public static int getNumMonitors() {
		return PerfMon.getNumMonitors();
	}

}
