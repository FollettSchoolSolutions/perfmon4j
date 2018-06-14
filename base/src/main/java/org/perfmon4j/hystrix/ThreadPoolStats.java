package org.perfmon4j.hystrix;

public class ThreadPoolStats {
	public final long executedThreadCount;
	public final long rejectedThreadCount;
	public final long completedTaskCount;
	public final long scheduledTaskCount;
	
	//Gauges (no deltas)
	public final long maxActiveThreads;
	public final long currentQueueSize;
	public final long currentPoolSize;
	
	
	/*
	 * To construct use ThreadPoolStats.builder().  Example:
	 * 	ThreadPoolStats myStats = ThreadPoolStats.builder()
	 * 		.setExecutedThreadCount(10)
	 * 		.setRejectedThreadCount(3)
	 * 		.build();
	 */
	private ThreadPoolStats(ThreadPoolStats.Builder builder) {
		executedThreadCount = builder.getExecutedThreadCount();
		rejectedThreadCount = builder.getRejectedThreadCount();
		completedTaskCount = builder.getCompletedTaskCount();
		scheduledTaskCount = builder.getScheduledTaskCount();
		maxActiveThreads = builder.getMaxActiveThreads();
		currentQueueSize = builder.getCurrentQueueSize();
		currentPoolSize = builder.getCurrentPoolSize();
	}
	
	public long getExecutedThreadCount() {
		return executedThreadCount;
	}

	public long getRejectedThreadCount() {
		return rejectedThreadCount;
	}

	public long getCompletedTaskCount() {
		return completedTaskCount;
	}

	public long getScheduledTaskCount() {
		return scheduledTaskCount;
	}

	public long getMaxActiveThreads() {
		return maxActiveThreads;
	}

	public long getCurrentQueueSize() {
		return currentQueueSize;
	}

	public long getCurrentPoolSize() {
		return currentPoolSize;
	}

	public static ThreadPoolStats.Builder builder() {
		return new ThreadPoolStats.Builder();
	}

	public ThreadPoolStats add(ThreadPoolStats stats) {
		return ThreadPoolStats.builder()
			.setExecutedThreadCount(this.getExecutedThreadCount() + stats.getExecutedThreadCount())
			.setRejectedThreadCount(this.getRejectedThreadCount() + stats.getRejectedThreadCount())
			.setCompletedTaskCount(this.getCompletedTaskCount() + stats.getCompletedTaskCount())
			.setScheduledTaskCount(this.getScheduledTaskCount() + stats.getScheduledTaskCount())
			.setMaxActiveThreads(this.getMaxActiveThreads() + stats.getMaxActiveThreads())
			.setCurrentQueueSize(this.getCurrentQueueSize() + stats.getCurrentQueueSize())
			.setCurrentPoolSize(this.getCurrentPoolSize() + stats.getCurrentPoolSize())
			.build();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (completedTaskCount ^ (completedTaskCount >>> 32));
		result = prime * result
				+ (int) (currentPoolSize ^ (currentPoolSize >>> 32));
		result = prime * result
				+ (int) (currentQueueSize ^ (currentQueueSize >>> 32));
		result = prime * result
				+ (int) (executedThreadCount ^ (executedThreadCount >>> 32));
		result = prime * result
				+ (int) (maxActiveThreads ^ (maxActiveThreads >>> 32));
		result = prime * result
				+ (int) (rejectedThreadCount ^ (rejectedThreadCount >>> 32));
		result = prime * result
				+ (int) (scheduledTaskCount ^ (scheduledTaskCount >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ThreadPoolStats other = (ThreadPoolStats) obj;
		if (completedTaskCount != other.completedTaskCount)
			return false;
		if (currentPoolSize != other.currentPoolSize)
			return false;
		if (currentQueueSize != other.currentQueueSize)
			return false;
		if (executedThreadCount != other.executedThreadCount)
			return false;
		if (maxActiveThreads != other.maxActiveThreads)
			return false;
		if (rejectedThreadCount != other.rejectedThreadCount)
			return false;
		if (scheduledTaskCount != other.scheduledTaskCount)
			return false;
		return true;
	}

	public static final class Builder {
		public long executedThreadCount = 0;
		public long rejectedThreadCount = 0;
		public long completedTaskCount = 0;
		public long scheduledTaskCount = 0;
		public long maxActiveThreads = 0;
		public long currentQueueSize = 0;
		public long currentPoolSize = 0;

		public long getExecutedThreadCount() {
			return executedThreadCount;
		}

		public Builder setExecutedThreadCount(long executedThreadCount) {
			this.executedThreadCount = executedThreadCount;
			return this;
		}

		public long getRejectedThreadCount() {
			return rejectedThreadCount;
		}

		public Builder setRejectedThreadCount(long rejectedThreadCount) {
			this.rejectedThreadCount = rejectedThreadCount;
			return this;
		}

		public long getCompletedTaskCount() {
			return completedTaskCount;
		}

		public Builder setCompletedTaskCount(long completedTaskCount) {
			this.completedTaskCount = completedTaskCount;
			return this;
		}

		public long getScheduledTaskCount() {
			return scheduledTaskCount;
		}

		public Builder setScheduledTaskCount(long scheduledTaskCount) {
			this.scheduledTaskCount = scheduledTaskCount;
			return this;
		}

		public long getMaxActiveThreads() {
			return maxActiveThreads;
		}

		public Builder setMaxActiveThreads(long maxActiveThreads) {
			this.maxActiveThreads = maxActiveThreads;
			return this;
		}

		public long getCurrentQueueSize() {
			return currentQueueSize;
		}

		public Builder setCurrentQueueSize(long currentQueueSize) {
			this.currentQueueSize = currentQueueSize;
			return this;
		}

		public long getCurrentPoolSize() {
			return currentPoolSize;
		}

		public Builder setCurrentPoolSize(long currentPoolSize) {
			this.currentPoolSize = currentPoolSize;
			return this;
		}


		public ThreadPoolStats build() {
			return new ThreadPoolStats(this);
		}
	}

}
