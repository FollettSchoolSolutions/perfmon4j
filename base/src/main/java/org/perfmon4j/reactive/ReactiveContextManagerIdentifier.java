package org.perfmon4j.reactive;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class ReactiveContextManagerIdentifier {
	private static final AtomicLong referenceIDs = new AtomicLong();
	
	private final long id = referenceIDs.incrementAndGet();
	private String threadName;
	private final WeakReference<Thread> thread;
	private final ReactiveContextManager threadContextManager;
	
	ReactiveContextManagerIdentifier(Thread thread, ReactiveContextManager threadContextManager) {
		this.threadName = thread.getName();
		this.thread = new WeakReference<Thread>(thread);
		this.threadContextManager = threadContextManager; 
	}

	boolean threadMatches(Thread callingThread) {
		return callingThread.equals(thread.get());
	}
	
	public String getThreadName() {
		Thread savedThread = thread.get();
		if (savedThread != null) {
			threadName = savedThread.getName();
		}
		return threadName;
	}
	
	public Thread getThread() {
		return thread.get();
	}

	public ReactiveContextManager getThreadContextManager() {
		return threadContextManager;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReactiveContextManagerIdentifier other = (ReactiveContextManagerIdentifier) obj;
		return id == other.id;
	}
}
