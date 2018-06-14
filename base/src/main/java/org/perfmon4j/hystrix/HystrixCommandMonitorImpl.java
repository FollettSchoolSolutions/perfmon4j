package org.perfmon4j.hystrix;

import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;

@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR, 
	dataInterface=HystrixCommandData.class,
	sqlWriter=SQLWriter.class)

public class HystrixCommandMonitorImpl {
	private final String instanceName;
	private static final CommandStatsRegistry registry = CommandStatsRegistry.getRegistry();
	
	public HystrixCommandMonitorImpl() {
		instanceName = null;
	}
	
	public HystrixCommandMonitorImpl(String instanceName) {
		this.instanceName = instanceName;
	}
	
	/* package level for unit test*/ 
	CommandStats getStats() {
		CommandStats result = null;
		if (instanceName == null) {
			result = CommandStats.builder().build();
			CommandStatsAccumulator allStats = registry.getStats();
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
		CommandStatsAccumulator accumulator = registry.getStats();
		return accumulator.getContexts().toArray(new String[]{});
	}
	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		String result = instanceName;
		
		if (result == null) {
				result = "Composite(";
				String[] names = getInstanceNames();
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
	public long getSuccessCount() {
		return getStats().getSuccessCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getFailureCount() {
		return getStats().getFailureCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getTimeoutCount() {
		return getStats().getTimeoutCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getShortCircuitedCount() {
		return getStats().getShortCircuitedCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getThreadPoolRejectedCount() {
		return getStats().getThreadPoolRejectedCount();
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getSemaphoreRejectedCount() {
		return getStats().getSemaphoreRejectedCount();
	}

	
//    public static void main(String args[]) throws Exception {
//    	System.setProperty("PERFMON_APPENDER_ASYNC_TIMER_MILLIS", "5");
//
//    	HystrixCommandKey myCommandKey = HystrixCommandKey.Factory.asKey("MyCommand");
//        HystrixCommandGroupKey myGroupKey = HystrixCommandGroupKey.Factory.asKey("MyGroup");
//        
//    	
////    	BasicConfigurator.configure();
////        Logger.getRootLogger().setLevel(Level.INFO);
////        Logger.getLogger("org.perfmon4j").setLevel(Level.DEBUG);
//    	
//        PerfMonConfiguration config = new PerfMonConfiguration();
//        config.defineAppender("SimpleAppender", TextAppender.class.getName(), "1 second");
//        Properties snapShotProperties = new Properties();
//        snapShotProperties.setProperty("instanceName", myCommandKey.name());
//        
//        config.defineSnapShotMonitor("monitor", HystrixCommandMonitorImpl.class.getName(), 
//        		snapShotProperties);
//        config.attachAppenderToSnapShotMonitor("monitor", "SimpleAppender");
//
//        // Execute the command 1 time.
//        new MyCommand(myCommandKey, myGroupKey).execute();
//
//        PerfMon.configure(config);
//        
//        Thread.sleep(2050);
//        
//        new MyCommand(myCommandKey, myGroupKey).execute();
//        new MyCommand(myCommandKey, myGroupKey).execute();
//        new MyCommand(myCommandKey, myGroupKey).execute();
//        new MyCommand(myCommandKey, myGroupKey).execute();
//        new MyCommand(myCommandKey, myGroupKey).execute();
//        
//        Thread.sleep(3020);
//    }
    
//	private static class MyCommand extends HystrixCommand<String> {
//
//		MyCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey groupKey) {
//			super(HystrixCommand.Setter.withGroupKey(groupKey).andCommandKey(commandKey));
//		}
//
//		@Override
//		protected String run() throws Exception {
//			System.err.println("***My Command Exectued***");        
//			return "";
//		}
//	}

}
