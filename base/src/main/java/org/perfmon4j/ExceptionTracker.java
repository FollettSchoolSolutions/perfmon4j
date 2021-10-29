package org.perfmon4j;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotCounter.Display;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;

@SnapShotProvider(type = SnapShotProvider.Type.STATIC)
public class ExceptionTracker  {
	private static boolean enabled = false;

	public static boolean isEnabled() {
		return enabled;
	}
	
	public static final String BRIDGE_CLASS_NAME = "generated.perfmon4j.ExceptionBridge";
	
	/**
	 * PerfMonTimerTransformer will use this method to connect the
	 * ExceptionTracker when the Exception class is successfully 
	 * instrumented.
	 * 
	 * If this connection is made the Tracker will be enabled.
	 *   
	 * @throws Exception
	 */
	public static void registerWithBridge() throws Exception {
		Class<?> clazz = ExceptionTracker.class.getClassLoader().loadClass(BRIDGE_CLASS_NAME);
		Method method = clazz.getDeclaredMethod("registerConsumer" , new Class[] {Consumer.class});
		method.invoke(null, new Object[] {new BridgeConsumer()});
		enabled = true;
	}
	
	public static final class BridgeConsumer implements Consumer {
		@Override
		public void accept(Object exception) {
			ExceptionTracker.notifyInExceptionConstructor(exception);
		}
	}
	
	private static final AtomicLong exceptionCount = new AtomicLong(0);
	private static final AtomicLong sqlExceptionCount = new AtomicLong(0);
	
	private static final ThreadLocal<ExceptionPerThreadCounter> threadBasedCounter = new ThreadLocal<ExceptionTracker.ExceptionPerThreadCounter>() {
		@Override
		protected ExceptionPerThreadCounter initialValue() {
			return new ExceptionPerThreadCounter();
		}
	};

	@SnapShotString
	public static String getEnabled() {
		return enabled ? "enabled" : "disabled";
	}
	
	
	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getExceptionCount() {
		return enabled ? exceptionCount.get() : 0L;
	}

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getSQLExceptionCount() {
		return enabled ? sqlExceptionCount.get() : 0L;
	}
	
	public static long getExceptionCountForThread() {
		return enabled ? threadBasedCounter.get().getExceptionCount() : 0L;
	}

	public static long getSQLExceptionCountForThread() {
		return enabled ? threadBasedCounter.get().getSQLExceptionCount() : 0L;
	}
	
	/**
	 * This should ONLY be called from the instrumented Exception
	 * constructor.
	 * @param exception
	 */
	public static void notifyInExceptionConstructor(Object exception) {
		if (enabled) {
			threadBasedCounter.get().increment(exception);
		}
	}
	
	/**
	 * This class prevents counting a single Exception
	 * multiple times when an exception is constructed
	 * using nested constructors. 
	 * 
	 * @param current
	 */
	private static final class ExceptionPerThreadCounter {
		private final AtomicLong exceptionCount = new AtomicLong(0);
		private final AtomicLong sqlExceptionCount = new AtomicLong(0);
		private Object current = null;

		private void increment(Object newCurrent) {
			if (newCurrent != this.current) {
				exceptionCount.incrementAndGet();  // Increment count on this thread.
				ExceptionTracker.exceptionCount.incrementAndGet(); // Increment global count.
				if (SQLTime.isThreadInSQL()) {
					sqlExceptionCount.incrementAndGet();
					ExceptionTracker.sqlExceptionCount.incrementAndGet();
				}
				this.current = newCurrent;
			} 
		}
		
		private long getExceptionCount() {
			return exceptionCount.get();
		}
		
		private long getSQLExceptionCount() {
			return sqlExceptionCount.get();
		}
	}

}
