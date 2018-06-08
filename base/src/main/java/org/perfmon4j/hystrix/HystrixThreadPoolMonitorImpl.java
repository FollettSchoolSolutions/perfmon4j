package org.perfmon4j.hystrix;

import org.perfmon4j.instrument.SnapShotProvider;

@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR, 
	dataInterface=HystrixThreadPoolData.class,
	sqlWriter=SQLWriter.class)

public class HystrixThreadPoolMonitorImpl {
//	private final String instanceName;
////	protected HystrixThreadPoolMetrics metrics = null;
//	
//	public HystrixThreadPoolMonitorImpl() throws InvalidConfigException {
//		throw new org.perfmon4j.InvalidConfigException("Must specify a HystrixThreadPool instanceName");
//	}
//	
//	public HystrixThreadPoolMonitorImpl(String instanceName) {
//		this.instanceName = instanceName;
//	}
//	
//	/* package level for unit test*/ 
//	HystrixThreadPoolMetrics getOrCreateMetrics() {
//		if (metrics == null) {
//			metrics = HystrixThreadPoolMetrics.getInstance(HystrixThreadPoolKey.Factory.asKey(instanceName));
//		}
//		return metrics;
//	}
//	
//	@SnapShotInstanceDefinition
//	static public String[] getInstanceNames() {
//		Set<String> result = new HashSet<String>();
//		Iterator<HystrixThreadPoolMetrics> itr = HystrixThreadPoolMetrics.getInstances().iterator();
//		while(itr.hasNext()) {
//			HystrixThreadPoolMetrics m = itr.next();
//			result.add(m.getThreadPoolKey().name());
//		}
//		return result.toArray(new String[]{});
//	}	
//
//	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
//	public long getExecutedThreadCount() {
//		long result = 0L;
//		
//		HystrixThreadPoolMetrics metrics = getOrCreateMetrics(); 
//		if (metrics != null) {
//			result = metrics.getCumulativeCountThreadsExecuted();
//		}
//		
//		return result;
//	}
//
//	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
//	public long getRejectedThreadCount() {
//		long result = 0L;
//		
//		HystrixThreadPoolMetrics metrics = getOrCreateMetrics(); 
//		if (metrics != null) {
//			result = metrics.getCumulativeCountThreadsRejected();
//		}
//		
//		return result;
//	}
//
//	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
//	public long getCompletedTaskCount() {
//		long result = 0L;
//		
//		HystrixThreadPoolMetrics metrics = getOrCreateMetrics(); 
//		if (metrics != null) {
//			result = toLong(metrics.getCurrentCompletedTaskCount());
//		}
//		
//		return result;
//	}
//
//	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
//	public long getScheduledTaskCount() {
//		long result = 0L;
//		
//		HystrixThreadPoolMetrics metrics = getOrCreateMetrics(); 
//		if (metrics != null) {
//			result = toLong(metrics.getCurrentTaskCount());
//		}
//		
//		return result;
//	}
//
//	@SnapShotGauge
//	public long getMaxActiveThreads() {
//		long result = 0L;
//		
//		HystrixThreadPoolMetrics metrics = getOrCreateMetrics(); 
//		if (metrics != null) {
//			result = metrics.getRollingMaxActiveThreads();
//		}
//		
//		return result;
//	}
//
//	@SnapShotGauge
//	public long getCurrentQueueSize() {
//		long result = 0L;
//		
//		HystrixThreadPoolMetrics metrics = getOrCreateMetrics(); 
//		if (metrics != null) {
//			result = toLong(metrics.getCurrentQueueSize());
//		}
//		
//		return result;
//	}
//	
//	@SnapShotGauge
//	public long getCurrentPoolSize() {
//		long result = 0L;
//		
//		HystrixThreadPoolMetrics metrics = getOrCreateMetrics(); 
//		if (metrics != null) {
//			result = toLong(metrics.getCurrentPoolSize());
//		}
//		
//		return result;
//	}	
//
//	@SnapShotString(isInstanceName=true)
//	public String getInstanceName() {
//		return instanceName;
//	}
//	
//	private long toLong(Number number) {
//		if (number == null) {
//			return 0l;
//		} else {
//			return number.longValue();
//		}
//	}
	
    public static void main(String args[]) throws Exception {
//    	System.setProperty("PERFMON_APPENDER_ASYNC_TIMER_MILLIS", "5");
//
//    	HystrixCommandKey myCommandKey = HystrixCommandKey.Factory.asKey("MyCommand");
//        HystrixCommandGroupKey myGroupKey = HystrixCommandGroupKey.Factory.asKey("MyGroup");
//        HystrixThreadPoolKey myThreadPoolKey = HystrixThreadPoolKey.Factory.asKey("MyThreadPool");
//        
//    	
//    	BasicConfigurator.configure();
//        Logger.getRootLogger().setLevel(Level.INFO);
//        Logger.getLogger("org.perfmon4j").setLevel(Level.DEBUG);
//    	
//        PerfMonConfiguration config = new PerfMonConfiguration();
//        config.defineAppender("SimpleAppender", TextAppender.class.getName(), "1 second");
//        Properties snapShotProperties = new Properties();
//        snapShotProperties.setProperty("instanceName", myThreadPoolKey.name());
//        
//        config.defineSnapShotMonitor("monitor", HystrixThreadPoolMonitorImpl.class.getName(), 
//        		snapShotProperties);
//        config.attachAppenderToSnapShotMonitor("monitor", "SimpleAppender");
//
//        // Execute the command 1 time.
//        new MyCommand(myCommandKey, myGroupKey, myThreadPoolKey).execute();
//
//        PerfMon.configure(config);
//        
//        Thread.sleep(2050);
//        
//        new MyCommand(myCommandKey, myGroupKey, myThreadPoolKey).execute();
//        new MyCommand(myCommandKey, myGroupKey, myThreadPoolKey).execute();
//        new MyCommand(myCommandKey, myGroupKey, myThreadPoolKey).execute();
//        new MyCommand(myCommandKey, myGroupKey, myThreadPoolKey).execute();
//        new MyCommand(myCommandKey, myGroupKey, myThreadPoolKey).execute();
//        
//        Thread.sleep(3020);
    }
    
//	private static class MyCommand extends HystrixCommand<String> {
//
//		MyCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey groupKey, HystrixThreadPoolKey threadPoolKey) {
//			super(HystrixCommand.Setter.withGroupKey(groupKey)
//				.andCommandKey(commandKey)
//				.andThreadPoolKey(threadPoolKey));
//		}
//
//		@Override
//		protected String run() throws Exception {
//			System.err.println("***My Command Exectued***");        
//			return "";
//		}
//	}
}

