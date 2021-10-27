package org.perfmon4j;

import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotCounter.Display;
import org.perfmon4j.instrument.SnapShotProvider;

/**
 * This class is used to track instances of all active
 * threads within each PerfMon monitor.
 * 
 * @author ddeucher
 *
 */
@SnapShotProvider(type = SnapShotProvider.Type.STATIC)
public class MonitorThreadTracker {
	public static final String REMOVED_THREAD = "REMOVED_THREAD";
	public static final String DISABLE_KEY = MonitorThreadTracker.class.getName() + ".DisableThreadTracking";
	
	/**
	 * This variable is declared at the instance level, but once a monitor is 
	 * created it will not be disabled/enabled within the life of the JVM. 
	 * This means if you want to disable thread tracking for your application you
	 * must include
	 * -Dorg.perfmon4j.MonitorThreadTracker.DisableThreadTracking=true" on
	 * the java command line.
	 */
	public final boolean disableThreadTracker = Boolean.getBoolean(DISABLE_KEY);

	private final Object trackerSemaphore = new Object() {}; 
	
	private final PerfMon monitor;
	private Tracker head = null;
	private Tracker tail = null;
	private int length = 0;
	private static final AtomicLong numTrackersAdded = new AtomicLong(0);
	private static final AtomicLong numTrackersRemoved = new AtomicLong(0);
	private static final AtomicLong numCallsToGetLongestRunning = new AtomicLong(0);
	private static final AtomicLong numCallsToGetAllRunning = new AtomicLong(0);
	
	MonitorThreadTracker(PerfMon monitor) {
		this.monitor = monitor;
	}
	
	final int getLength() {
		synchronized(trackerSemaphore) {
			return length;
		}
	}
	
	final int addTracker(Tracker tracker) {
		if (disableThreadTracker) {
			return ++length;
		} else {
			numTrackersAdded.incrementAndGet();
			synchronized(trackerSemaphore) {
	            if (head == null) {
	            	// We know the "list" is empty;
	            	head = tail = tracker;
	            	tracker.setNext(null);
	            	tracker.setPrevious(null);
	            	length = 1;
	            } else {
	            	// We know at least one element is in the "list"
	            	// and we want to append to the end.
	            	tail.setNext(tracker);
	            	tracker.setPrevious(tail);
	            	tail = tracker;
	                length++;
	            }
	    		return length;
			}
		}
	}
	
	final int removeTracker(Tracker tracker) {
		if (disableThreadTracker) {
			return --length;
		} else {
			numTrackersRemoved.incrementAndGet();
			synchronized(trackerSemaphore) {
	            if (tracker == head && tracker == tail) {
	            	head = tail = null;
	            	length = 0;
	            } else if (tracker == head) {
	            	// We know there is, at least, one element after us.
	            	head = tracker.getNext();
	            	head.setPrevious(null);
	            	length--;
	            } else if (tracker == tail) {
	            	// We know there is at least one element before us.
	            	tail = tracker.getPrevious();
	            	tail.setNext(null);
	            	length--;
	            } else {
	            	// We know we have at least one element before and after us.
	            	Tracker beforeUs = tracker.getPrevious();
	            	Tracker afterUs = tracker.getNext();
	            	
	            	beforeUs.setNext(afterUs);
	            	afterUs.setPrevious(beforeUs);
	            	length--;
	            }
	            return length;
			}
		}
	}
	
	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getNumTrackersAdded() {
		return numTrackersAdded.get();
	}
	
	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_MIN)
	public static long getNumTrackersRemoved() {
		return numTrackersRemoved.get();
	}

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_SECOND)
	public static long getNumCallsToGetLongestRunning() {
		return numCallsToGetLongestRunning.get();
	}

	@SnapShotCounter(preferredDisplay = Display.DELTA_PER_SECOND)
	public static long getNumCallsToGetAllRunning() {
		return numCallsToGetAllRunning.get();
	}
	
	public final TrackerValue getLongestRunning() {
		if (disableThreadTracker) {
			return null;
		} else {
			numCallsToGetLongestRunning.incrementAndGet();
			synchronized(trackerSemaphore) {
				if (length > 0) {
					return new TrackerValue(head);
				} else {
					return null;
				}
			}
		}
	}
	
	public final TrackerValue[] getAllRunning() {
		if (disableThreadTracker) {
			return new TrackerValue[0];
		} else {
			numCallsToGetAllRunning.incrementAndGet();
			synchronized(trackerSemaphore) {
				TrackerValue[] result = new TrackerValue[length];
				int offset = 0;
				Tracker current = head;
				while (current != null) {
					result[offset++] = new TrackerValue(current);
					current = current.getNext();
				}
				return result;
			}
		}
	}

	public PerfMon getMonitor() {
		return monitor;
	}

	static interface Tracker {
		public Thread getThread();  
		
		public void setPrevious(Tracker previous);
		public Tracker getPrevious();
		
		public void setNext(Tracker next);
		public Tracker getNext();
		
		public long getStartTime();
	}

	public static class TrackerValue {
		private final Thread thread;
		private final String threadName;
		private final long startTime;
		
		private TrackerValue(Tracker tracker) {
			this.thread = tracker.getThread();
			this.threadName = this.thread != null ?  thread.getName() : MonitorThreadTracker.REMOVED_THREAD;
			this.startTime = tracker.getStartTime(); 
		}
		/**
		 * May return null if the Thread has been garbage collected.
		 * @return
		 */
		public Thread getThread() {
			return thread;
		}
		
		public String getThreadName() {
			return threadName;
		}

		public long getStartTime() {
			return startTime;
		}
	}
}
