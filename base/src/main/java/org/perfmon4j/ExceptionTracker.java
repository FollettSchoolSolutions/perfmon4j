package org.perfmon4j;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotCounter.Display;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

@SnapShotProvider(type = SnapShotProvider.Type.STATIC)
public class ExceptionTracker  {
	private static final Logger logger = LoggerFactory.initLogger(ExceptionTracker.class);
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
	public static boolean registerWithBridge() throws Exception {
		ClassLoader loader = ExceptionTracker.class.getClassLoader();
		if (loader == null) {
			logger.logDebug("ExceptionTracker.class.getClassLoader() returned null, trying systemClassLoader");
			loader = ClassLoader.getSystemClassLoader();
		}
		if (loader != null) {
			Class<?> clazz = loader.loadClass(BRIDGE_CLASS_NAME);
			Method method = clazz.getDeclaredMethod("registerExceptionConsumer" , new Class[] {Consumer.class});
			method.invoke(null, new Object[] {new BridgeExceptionConsumer()});
			
			method = clazz.getDeclaredMethod("registerErrorConsumer" , new Class[] {Consumer.class});
			method.invoke(null, new Object[] {new BridgeErrorConsumer()});
			
			method = clazz.getDeclaredMethod("registerRuntimeExceptionConsumer" , new Class[] {Consumer.class});
			method.invoke(null, new Object[] {new BridgeRuntimeExceptionConsumer()});
			
			enabled = true;
		} else {
			logger.logError("Unable to find classloader to load ExceptionTracker Bridge Class");
		}
		return enabled;
	}
	
	public static final class BridgeExceptionConsumer implements Consumer {
		@Override
		public void accept(Object exception) {
			ExceptionTracker.notifyInExceptionConstructor(exception);
		}
	}

	public static final class BridgeErrorConsumer implements Consumer {
		@Override
		public void accept(Object error) {
			ExceptionTracker.notifyInErrorConstructor(error);
		}
	}
	
	public static final class BridgeRuntimeExceptionConsumer implements Consumer {
		@Override
		public void accept(Object exception) {
			ExceptionTracker.notifyInRuntimeExceptionConstructor(exception);
		}
	}

	
	private static final AtomicLong exceptionCount = new AtomicLong(0);
	private static final AtomicLong sqlExceptionCount = new AtomicLong(0);
	private static final AtomicLong errorCount = new AtomicLong(0);
	private static final AtomicLong sqlErrorCount = new AtomicLong(0);
	private static final AtomicLong runtimeExceptionCount = new AtomicLong(0);
	private static final AtomicLong sqlRuntimeExceptionCount = new AtomicLong(0);
	
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

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getErrorCount() {
		return enabled ? errorCount.get() : 0L;
	}

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getRuntimeExceptionCount() {
		return enabled ? runtimeExceptionCount.get() : 0L;
	}

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getSQLRuntimeExceptionCount() {
		return enabled ? sqlRuntimeExceptionCount.get() : 0L;
	}

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getSQLErrorCount() {
		return enabled ? sqlErrorCount.get() : 0L;
	}
	
	public static long getExceptionCountForThread() {
		return enabled ? threadBasedCounter.get().getExceptionCount() : 0L;
	}

	public static long getSQLExceptionCountForThread() {
		return enabled ? threadBasedCounter.get().getSQLExceptionCount() : 0L;
	}

	public static long getErrorCountForThread() {
		return enabled ? threadBasedCounter.get().getErrorCount() : 0L;
	}

	public static long getSQLErrorCountForThread() {
		return enabled ? threadBasedCounter.get().getSQLErrorCount() : 0L;
	}

	public static long getRuntimeExceptionCountForThread() {
		return enabled ? threadBasedCounter.get().getRuntimeExceptionCount() : 0L;
	}

	public static long getSQLRuntimeExceptionCountForThread() {
		return enabled ? threadBasedCounter.get().getSQLRuntimeExceptionCount() : 0L;
	}
	
	/**
	 * This should ONLY be called from the instrumented java.lang.Exception
	 * constructor.
	 * @param exception
	 */
	public static void notifyInExceptionConstructor(Object exception) {
		if (enabled) {
			threadBasedCounter.get().incrementException(exception);
		}
	}

	/**
	 * This should ONLY be called from the instrumented java.lang.Error
	 * constructor.
	 * @param exception
	 */
	public static void notifyInErrorConstructor(Object error) {
		if (enabled) {
			threadBasedCounter.get().incrementError(error);
		}
	}

	/**
	 * This should ONLY be called from the instrumented java.lang.Error
	 * constructor.
	 * @param exception
	 */
	public static void notifyInRuntimeExceptionConstructor(Object exception) {
		if (enabled) {
			threadBasedCounter.get().incrementRuntimeException(exception);
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
		private final AtomicLong errorCount = new AtomicLong(0);
		private final AtomicLong sqlErrorCount = new AtomicLong(0);
		private final AtomicLong runtimeExceptionCount = new AtomicLong(0);
		private final AtomicLong sqlRuntimeExceptionCount = new AtomicLong(0);
		
		private Object current = null;
		private Object currentRuntimeException = null;

		private void incrementException(Object newCurrent) {
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

		private void incrementError(Object newCurrent) {
			if (newCurrent != this.current) {
				errorCount.incrementAndGet();  // Increment count on this thread.
				ExceptionTracker.errorCount.incrementAndGet(); // Increment global count.
				if (SQLTime.isThreadInSQL()) {
					errorCount.incrementAndGet();
					ExceptionTracker.errorCount.incrementAndGet();
				}
				this.current = newCurrent;
			} 
		}

		private void incrementRuntimeException(Object newCurrent) {
			if (newCurrent != this.currentRuntimeException) {
				runtimeExceptionCount.incrementAndGet();  // Increment count on this thread.
				ExceptionTracker.runtimeExceptionCount.incrementAndGet(); // Increment global count.
				if (SQLTime.isThreadInSQL()) {
					sqlRuntimeExceptionCount.incrementAndGet();
					ExceptionTracker.sqlRuntimeExceptionCount.incrementAndGet();
				}
				this.currentRuntimeException = newCurrent;
			} 
		}
		
		private long getExceptionCount() {
			return exceptionCount.get();
		}
		
		private long getSQLExceptionCount() {
			return sqlExceptionCount.get();
		}
		
		private long getErrorCount() {
			return errorCount.get();
		}
		
		private long getSQLErrorCount() {
			return sqlErrorCount.get();
		}

		public long getRuntimeExceptionCount() {
			return runtimeExceptionCount.get();
		}

		public long getSQLRuntimeExceptionCount() {
			return sqlRuntimeExceptionCount.get();
		}
	}
}
