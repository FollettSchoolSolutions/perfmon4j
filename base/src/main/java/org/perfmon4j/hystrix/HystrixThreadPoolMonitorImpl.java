package org.perfmon4j.hystrix;

import java.util.HashSet;
import java.util.Set;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;

@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR, 
	dataInterface=HystrixThreadPoolData.class,
	sqlWriter=SQLWriter.class)

public class HystrixThreadPoolMonitorImpl {
	private static final String COMPOSITE_INSTANCE_NAME = HystrixCommandMonitorImpl.COMPOSITE_INSTANCE_NAME;
	private final String instanceName;
	private static final ThreadPoolStatsRegistry registry = ThreadPoolStatsRegistry.getRegistry();
	
	public HystrixThreadPoolMonitorImpl() {
		instanceName = null;
	}
	
	public HystrixThreadPoolMonitorImpl(String instanceName) {
		this.instanceName = COMPOSITE_INSTANCE_NAME.equals(instanceName) ? null : instanceName;
	}
	
	/* package level for unit test*/ 
	ThreadPoolStats getStats() {
		ThreadPoolStats result = null;
		if (instanceName == null) {
			result = ThreadPoolStats.builder().build();
			ThreadPoolStatsAccumulator allStats = registry.getStats();
			for (String context : allStats.getContexts()) {
				result = result.add(allStats.getStats(context));
			}
		} else {
			result = registry.getStats().getStats(instanceName);
		}
		return result;
	}
		
	@SnapShotInstanceDefinition
	static public String[] getInstanceNames() {
		return getInstanceNames(true);
	}	

	static private String[] getInstanceNames(boolean includeComposite) {
		Set<String> result = new HashSet<String>(); 
		ThreadPoolStatsAccumulator accumulator = registry.getStats();
		
		if (includeComposite) {
			result.add(COMPOSITE_INSTANCE_NAME);
		}
		result.addAll(accumulator.getContexts());
		
		return result.toArray(new String[]{});
	}	

	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		String result = instanceName;
		
		if (result == null) {
			result = COMPOSITE_INSTANCE_NAME + "(";
			String[] names = getInstanceNames(false);
			for (int i = 0; i < names.length; i++) {
				if (i > 0) {
					result += ", ";
				}
				result += "\"" + names[i] + "\"";
			}
			result += ")";
		}
		return result;
	}
	
	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getExecutedThreadCount() {
		return getStats().getExecutedThreadCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getRejectedThreadCount() {
		return getStats().getRejectedThreadCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getCompletedTaskCount() {
		return getStats().getCompletedTaskCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getScheduledTaskCount() {
		return getStats().getScheduledTaskCount();
	}

	@SnapShotGauge
	public long getMaxActiveThreads() {
		return getStats().getMaxActiveThreads();
	}

	@SnapShotGauge
	public long getCurrentQueueSize() {
		return getStats().getCurrentQueueSize();
	}
	
	@SnapShotGauge
	public long getCurrentPoolSize() {
		return getStats().getCurrentPoolSize();
	}	
}

