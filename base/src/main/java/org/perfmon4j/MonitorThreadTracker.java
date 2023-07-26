package org.perfmon4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotCounter.Display;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.reactive.ReactiveContext;
import org.perfmon4j.util.MiscHelper;

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
	public static final boolean GLOBAL_DISABLE_THREAD_TRACKER = Boolean.getBoolean(DISABLE_KEY);
	
	/**
	 * To disable thread tracking for your application you
	 * must include
	 * -Dorg.perfmon4j.MonitorThreadTracker.DisableThreadTracking=true" on
	 * the java command line.
	 */
	private final boolean disableThreadTracker;

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
		this.disableThreadTracker = GLOBAL_DISABLE_THREAD_TRACKER;
	}
	
	/**
	 * This constructor is ONLY declared for usage in Unit Tests.
	 * 
	 * @param monitor
	 * @param disableThreadTracker
	 */
	MonitorThreadTracker(PerfMon monitor, boolean disableThreadTracker) {
		this.monitor = monitor;
		this.disableThreadTracker = disableThreadTracker;
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
	            	tracker.setNext(null);
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

	            	// Make sure that this tracker was captured,
	            	// It's associated thread might have been
	            	// started before we were monitoring. 
	            	// This should only happen if a PerfMon monitor is 
	            	// enabled after system has started.
	            	if (beforeUs != null && afterUs != null) {
		            	beforeUs.setNext(afterUs);
		            	afterUs.setPrevious(beforeUs);
		            	length--;
	            	}
	            }
	            tracker.setNext(null);
	            tracker.setPrevious(null);
	            
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
	
	public final PerfMonObservableDatum<?>[] getDataOverThresholds(long[] thresholds) {
		return buildDatumForTrackerList(getAllRunning(), thresholds);
	}
	
	static PerfMonObservableDatum<?>[] buildDatumForTrackerList(TrackerValue[] activeThreads, long[] thresholds) {
		return buildDatumForTrackerList(activeThreads, thresholds, System.currentTimeMillis());
	}
	
	static PerfMonObservableDatum<?>[] buildDatumForTrackerList(TrackerValue[] activeThreads, long[] thresholds, long now) {
		List<PerfMonObservableDatum<?>> result = new ArrayList<PerfMonObservableDatum<?>>();
		
		for (long threshold : thresholds) {
			int countOver = 0;
			String threadNames = "";
			for (TrackerValue value : activeThreads) {
				if ((now - value.getStartTime()) > threshold) {
					if (countOver++ > 0) {
						threadNames += ",";
					}
					threadNames += value.getThreadName();
				}
			}
			result.add(PerfMonObservableDatum.newDatum("numActive > " + MiscHelper.getMillisDisplayable(threshold), 
				countOver));
			result.add(PerfMonObservableDatum.newDatum("active > " +  MiscHelper.getMillisDisplayable(threshold), 
				threadNames.isBlank() ? "N/A" : threadNames));
		}
		
		return result.toArray(new PerfMonObservableDatum<?>[]{});
	}

	public PerfMon getMonitor() {
		return monitor;
	}

	static interface Tracker {
		public String getReactiveCategoryName();
		public boolean isReactiveRequest();
		public ReactiveContext getOwningContext();

		public Thread getThread();  
		
		public void setPrevious(Tracker previous);
		public Tracker getPrevious();
		
		public void setNext(Tracker next);
		public Tracker getNext();
		
		public long getStartTime();

		/**
		 * If SQL Time tracking is not enabled, this should
		 * always return 0;
		 * 
		 * If the Tracker is associated with a thread (default)
		 * it will return the current accumulated SQL duration on the thread,
		 * using SQLTime.getCurrentSQLMillis.
		 * 
		 * If the Tracker is associated with a reactiveContext
		 * it will return the accumulated SQL duration on the
		 * context.
		 * 
		 * @return
		 */
		public long getCurrentSQLMillis();
	}

	public static class TrackerValue {
		private final Thread thread;
		private final String threadName;
		private final long startTime;
		
		private TrackerValue(Tracker tracker) {
			this.thread = tracker.getThread();
			String reactiveCategoryName = tracker.getReactiveCategoryName();
			if (reactiveCategoryName != null) {
				this.threadName = reactiveCategoryName;
			} else {
				this.threadName = this.thread != null ?  thread.getName() : MonitorThreadTracker.REMOVED_THREAD;
			}
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
