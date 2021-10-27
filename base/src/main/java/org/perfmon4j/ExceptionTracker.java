package org.perfmon4j;

import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotCounter.Display;
import org.perfmon4j.instrument.SnapShotProvider;

@SnapShotProvider(type = SnapShotProvider.Type.STATIC)
public class ExceptionTracker {
	private static final AtomicLong exceptionCount = new AtomicLong(0);
	private static final AtomicLong sqlExceptionCount = new AtomicLong(0);
	
	private static final ThreadLocal<ExceptionPerThreadCounter> threadBasedCounter = new ThreadLocal<ExceptionTracker.ExceptionPerThreadCounter>() {
		@Override
		protected ExceptionPerThreadCounter initialValue() {
			return new ExceptionPerThreadCounter();
		}
	};

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getExceptionCount() {
		return exceptionCount.get();
	}

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getSQLExceptionCount() {
		return sqlExceptionCount.get();
	}
	
	public static long getExceptionCountForThread() {
		return threadBasedCounter.get().getExceptionCount();
	}

	public static long getSQLExceptionCountForThread() {
		return threadBasedCounter.get().getSQLExceptionCount();
	}
	
	/**
	 * This should ONLY be called from the instrumented Exception
	 * constructor.
	 * @param exception
	 */
	public static void notifyInExceptionConstructor(Exception exception) {
		threadBasedCounter.get().increment(exception);
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

		private void increment(Object current) {
			if (current != this.current) {
				exceptionCount.incrementAndGet();  // Increment count on this thread.
				ExceptionTracker.exceptionCount.incrementAndGet(); // Increment global count.
				if (SQLTime.isThreadInSQL()) {
					sqlExceptionCount.incrementAndGet();
					ExceptionTracker.sqlExceptionCount.incrementAndGet();
				}
				this.current = current;
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
