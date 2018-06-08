package org.perfmon4j.hystrix;

import org.perfmon4j.InvalidConfigException;
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
	
	HystrixCommandMonitorImpl() throws InvalidConfigException {
		throw new org.perfmon4j.InvalidConfigException("Must specify a HystrixCommand instanceName");
	}
	
	public HystrixCommandMonitorImpl(String instanceName) {
		this.instanceName = instanceName;
	}
	
	/* package level for unit test*/ 
	CommandStats getStats() {
		return registry.getStats().getStats(instanceName);
	}
	
	@SnapShotInstanceDefinition
	static public String[] getInstanceNames() {
		CommandStatsAccumulator accumulator = registry.getStats();
		
		return accumulator.getContexts().toArray(new String[]{});
	}
	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		return instanceName;
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
