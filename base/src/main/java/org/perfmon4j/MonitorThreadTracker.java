package org.perfmon4j;

/**
 * This class is used to track instances of all active
 * threads within each PerfMon monitor.
 * 
 * @author ddeucher
 *
 */
public class MonitorThreadTracker {
	public static final String REMOVED_THREAD = "REMOVED_THREAD";
	
	private final Object trackerSemaphore = new Object() {}; 
	
	private final PerfMon monitor;
	private Tracker head = null;
	private Tracker tail = null;
	private int length = 0;
	
	MonitorThreadTracker(PerfMon monitor) {
		this.monitor = monitor;
	}
	
	final void addTracker(Tracker tracker) {
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
		}
	}
	
	final void removeTracker(Tracker tracker) {
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
		}
	}
	
	public final TrackerValue getLongestRunning() {
		synchronized(trackerSemaphore) {
			if (length > 0) {
				return new TrackerValue(head);
			} else {
				return null;
			}
		}
	}
	
	public final TrackerValue[] getAllRunning() {
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

	public PerfMon getMonitor() {
		return monitor;
	}

	public static interface Tracker {
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
