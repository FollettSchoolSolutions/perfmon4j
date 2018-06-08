package org.perfmon4j.hystrix;

public class CommandStats {
	private final long successCount;
	private final long failureCount;
	private final long timeoutCount;
	private final long shortCircuitedCount;
	private final long threadPoolRejectedCount;
	private final long semaphoreRejectedCount;

	/*
	 * To construct use CommandStats.builder().  Example:
	 * 	CommandStats myStats = CommandStats.builder()
	 * 		.setSuccessCount(10)
	 * 		.setFailureCount(3)
	 * 		.build();
	 */
	private CommandStats(CommandStats.Builder builder) {
		successCount = builder.getSuccessCount();
		failureCount = builder.getFailureCount();
		timeoutCount = builder.getTimeoutCount();
		shortCircuitedCount = builder.getShortCircuitedCount();
		threadPoolRejectedCount = builder.getThreadPoolRejectedCount();
		semaphoreRejectedCount = builder.getSemaphoreRejectedCount();
	}

	public static CommandStats.Builder builder() {
		return new CommandStats.Builder();
	}
	
	public long getSuccessCount() {
		return successCount;
	}

	public long getFailureCount() {
		return failureCount;
	}

	public long getTimeoutCount() {
		return timeoutCount;
	}

	public long getShortCircuitedCount() {
		return shortCircuitedCount;
	}

	public long getThreadPoolRejectedCount() {
		return threadPoolRejectedCount;
	}

	public long getSemaphoreRejectedCount() {
		return semaphoreRejectedCount;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (failureCount ^ (failureCount >>> 32));
		result = prime
				* result
				+ (int) (semaphoreRejectedCount ^ (semaphoreRejectedCount >>> 32));
		result = prime * result
				+ (int) (shortCircuitedCount ^ (shortCircuitedCount >>> 32));
		result = prime * result + (int) (successCount ^ (successCount >>> 32));
		result = prime
				* result
				+ (int) (threadPoolRejectedCount ^ (threadPoolRejectedCount >>> 32));
		result = prime * result + (int) (timeoutCount ^ (timeoutCount >>> 32));
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
		CommandStats other = (CommandStats) obj;
		if (failureCount != other.failureCount)
			return false;
		if (semaphoreRejectedCount != other.semaphoreRejectedCount)
			return false;
		if (shortCircuitedCount != other.shortCircuitedCount)
			return false;
		if (successCount != other.successCount)
			return false;
		if (threadPoolRejectedCount != other.threadPoolRejectedCount)
			return false;
		if (timeoutCount != other.timeoutCount)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "CommandStats [successCount=" + successCount + ", failureCount="
				+ failureCount + ", timeoutCount=" + timeoutCount
				+ ", shortCircuitedCount=" + shortCircuitedCount
				+ ", threadPoolRejectedCount=" + threadPoolRejectedCount
				+ ", semaphoreRejectedCount=" + semaphoreRejectedCount + "]";
	}

	public CommandStats add(CommandStats stats) {
		return CommandStats.builder()
			.setSuccessCount(this.getSuccessCount() + ((CommandStats)stats).getSuccessCount())
			.setFailureCount(this.getFailureCount() + ((CommandStats)stats).getFailureCount())
			.setTimeoutCount(this.getTimeoutCount() + ((CommandStats)stats).getTimeoutCount())
			.setShortCircuitedCount(this.getShortCircuitedCount() + ((CommandStats)stats).getShortCircuitedCount())
			.setThreadPoolRejectedCount(this.getThreadPoolRejectedCount() + ((CommandStats)stats).getThreadPoolRejectedCount())
			.setSemaphoreRejectedCount(this.getSemaphoreRejectedCount() + ((CommandStats)stats).getSemaphoreRejectedCount())
			.build();
	}
	
	public static final class Builder {
		private long successCount = 0;
		private long failureCount = 0;
		private long timeoutCount = 0;
		private long shortCircuitedCount = 0;
		private long threadPoolRejectedCount = 0;
		private long semaphoreRejectedCount = 0;

		public long getSuccessCount() {
			return successCount;
		}
		
		public Builder setSuccessCount(long successCount) {
			this.successCount = successCount;
			return this;
		}
		
		private long getFailureCount() {
			return failureCount;
		}
		
		public Builder setFailureCount(long failureCount) {
			this.failureCount = failureCount;
			return this;
		}
		
		private long getTimeoutCount() {
			return timeoutCount;
		}
		
		public Builder setTimeoutCount(long timeoutCount) {
			this.timeoutCount = timeoutCount;
			return this;
		}
		
		private long getShortCircuitedCount() {
			return shortCircuitedCount;
		}
		
		public Builder setShortCircuitedCount(long shortCircuitedCount) {
			this.shortCircuitedCount = shortCircuitedCount;
			return this;
		}
		
		private long getThreadPoolRejectedCount() {
			return threadPoolRejectedCount;
		}
		
		public Builder setThreadPoolRejectedCount(long threadPoolRejectedCount) {
			this.threadPoolRejectedCount = threadPoolRejectedCount;
			return this;
		}
		
		private long getSemaphoreRejectedCount() {
			return semaphoreRejectedCount;
		}
		
		public Builder setSemaphoreRejectedCount(long semaphoreRejectedCount) {
			this.semaphoreRejectedCount = semaphoreRejectedCount;
			return this;
		}
		
		public CommandStats build() {
			return new CommandStats(this);
		}
	}

}
