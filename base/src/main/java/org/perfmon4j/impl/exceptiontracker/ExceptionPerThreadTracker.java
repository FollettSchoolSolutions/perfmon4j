package org.perfmon4j.impl.exceptiontracker;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.SQLTime;

/**
 * This class prevents counting a single Exception
 * multiple times when an exception is constructed
 * using nested constructors. 
 */
public class ExceptionPerThreadTracker {
	public static final ThreadLocal<ExceptionPerThreadTracker> threadExceptionTracker = new ThreadLocal<ExceptionPerThreadTracker>() {

		@Override
		protected ExceptionPerThreadTracker initialValue() {
			return new ExceptionPerThreadTracker();
		}
	};
	
	public static final Counter NO_VALUE_COUNTER = new Counter();
	private static final Object globalCounterMapLockToken = new Object() {};
	private static final Map<String, Counter> globalCounterMap = new HashMap<String, Counter>();

	private final Map<String, ThreadBasedCounter> threadBasedMap = new HashMap<String, ThreadBasedCounter>();

	public Counter getThreadCount(String className) {
		Counter result = getThreadBasedCounter(className, false);

		return result != null ? result : NO_VALUE_COUNTER; 
	}
	
	public static Counter getGlobalCount(String className) {
		Counter result = getGlobalCounter(className, false);

		return result != null ? result : NO_VALUE_COUNTER; 
	}
			
	public void increment(String className, Object newCurrent) {
		ThreadBasedCounter counter = getThreadBasedCounter(className, true);
		
		if (counter.updateCurrent(newCurrent)) {
			Counter globalCounter = getGlobalCounter(className, true);
			
			counter.incrementCount();  // Increment count on this thread.
			globalCounter.incrementCount();
			if (SQLTime.isThreadInSQL()) {
				counter.incrementSQLCount();
				globalCounter.incrementSQLCount();
			}
		} 
	}
	
	private ThreadBasedCounter getThreadBasedCounter(String className, boolean createOnNull) {
		ThreadBasedCounter result = threadBasedMap.get(className);
		if (createOnNull && result == null) {
			result = new ThreadBasedCounter();
			threadBasedMap.put(className, result);
		}
		return result;
	}
	
	private static Counter getGlobalCounter(String className, boolean createOnNull) {
		synchronized (globalCounterMapLockToken) {
			Counter result = globalCounterMap.get(className);
			if (createOnNull && result == null) {
				result = new Counter();
				globalCounterMap.put(className, result);
			}
			return result;
		}
	}
	
	private static class ThreadBasedCounter extends Counter {
		private WeakReference<Object> current = new WeakReference<Object>(null);
		
		/**
		 * Will return true if newCurrent != current
		 * @param newCurrent
		 * @return
		 */
		boolean updateCurrent(Object newCurrent) {
			if (newCurrent != current.get()) {
				current = new WeakReference<Object>(newCurrent);
				return true;
			} 
			return false;
		}
	}
}

