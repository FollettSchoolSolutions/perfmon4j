package org.perfmon4j.extras.hystrix;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.management.MalformedObjectNameException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.InvalidConfigException;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.TextAppender;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.JDBCHelper;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixEventType;

@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR, 
	dataInterface=HystrixCommandMonitor.class,
	sqlWriter=HystrixCommandMonitorImpl.SQLWriter.class)

public class HystrixCommandMonitorImpl {
	private final String instanceName;
	protected HystrixCommandMetrics metrics = null;
	
	public HystrixCommandMonitorImpl() throws InvalidConfigException {
		throw new org.perfmon4j.InvalidConfigException("Must specify a HystrixCommand instanceName");
	}
	
	public HystrixCommandMonitorImpl(String instanceName) {
		this.instanceName = instanceName;
	}
	
	/* package level for unit test*/ 
	HystrixCommandMetrics getOrCreateMetrics() {
		if (metrics == null) {
			metrics = HystrixCommandMetrics.getInstance(HystrixCommandKey.Factory.asKey(instanceName));
		}
		return metrics;
	}
	
	@SnapShotInstanceDefinition
	static public String[] getInstanceNames() throws MalformedObjectNameException, NullPointerException {
		Set<String> result = new HashSet<String>();
		Iterator<HystrixCommandMetrics> itr = HystrixCommandMetrics.getInstances().iterator();
		while(itr.hasNext()) {
			HystrixCommandMetrics m = itr.next();
			result.add(m.getCommandKey().name());
		}
		return result.toArray(new String[]{});
	}
	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		return instanceName;
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getSuccessCount() {
		long result = 0L;
		
		HystrixCommandMetrics metrics = getOrCreateMetrics(); 
		if (metrics != null) {
			result = metrics.getCumulativeCount(HystrixEventType.SUCCESS);
		}
		
		return result;
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getFailureCount() {
		long result = 0L;
		
		HystrixCommandMetrics metrics = getOrCreateMetrics(); 
		if (metrics != null) {
			result = metrics.getCumulativeCount(HystrixEventType.FAILURE);
		}
		
		return result;
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getTimeoutCount() {
		long result = 0L;
		
		HystrixCommandMetrics metrics = getOrCreateMetrics(); 
		if (metrics != null) {
			result = metrics.getCumulativeCount(HystrixEventType.TIMEOUT);
		}
		
		return result;
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getShortCircuitedCount() {
		long result = 0L;
		
		HystrixCommandMetrics metrics = getOrCreateMetrics(); 
		if (metrics != null) {
			result = metrics.getCumulativeCount(HystrixEventType.SHORT_CIRCUITED);
		}
		
		return result;
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getThreadPoolRejectedCount() {
		long result = 0L;
		
		HystrixCommandMetrics metrics = getOrCreateMetrics(); 
		if (metrics != null) {
			result = metrics.getCumulativeCount(HystrixEventType.THREAD_POOL_REJECTED);
		}
		
		return result;
	}

	@SnapShotCounter(preferredDisplay=SnapShotCounter.Display.DELTA_PER_MIN)
	public long getSemaphoreRejectedCount() {
		long result = 0L;
		
		HystrixCommandMetrics metrics = getOrCreateMetrics(); 
		if (metrics != null) {
			result = metrics.getCumulativeCount(HystrixEventType.SEMAPHORE_REJECTED);
		}
		
		return result;
	}

	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
			throws SQLException {
			writeToSQL(conn, schema, (HystrixCommandMonitor)data, systemID);
		}
		
		public void writeToSQL(Connection conn, String schema, HystrixCommandMonitor data, long systemID)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JThreadPoolMonitor " +
				"(SystemID, ThreadPoolOwner, InstanceName, StartTime, EndTime, Duration,  " +
				"CurrentThreadsBusy, CurrentThreadCount) " +
				"VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement stmt = null;
			try {
//				stmt = conn.prepareStatement(SQL);
//				
//				int index = 1;
//				stmt.setLong(index++, systemID);
//				stmt.setString(index++, "Apache/Tomcat");
//				stmt.setString(index++, data.getInstanceName());
//				stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
//				stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
//				stmt.setLong(index++, data.getDuration());
//				stmt.setLong(index++, data.getCurrentThreadsBusy());
//				stmt.setLong(index++, data.getCurrentThreadCount());
//				
//				int count = stmt.executeUpdate();
//				if (count != 1) {
//					throw new SQLException("ThreadPoolMonitor failed to insert row");
//				}
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		}
	}
	
    public static void main(String args[]) throws Exception {
    	System.setProperty("PERFMON_APPENDER_ASYNC_TIMER_MILLIS", "5");

    	HystrixCommandKey myCommandKey = HystrixCommandKey.Factory.asKey("MyCommand");
        HystrixCommandGroupKey myGroupKey = HystrixCommandGroupKey.Factory.asKey("MyGroup");
        
    	
    	BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("org.perfmon4j").setLevel(Level.DEBUG);
    	
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineAppender("SimpleAppender", TextAppender.class.getName(), "1 second");
        Properties snapShotProperties = new Properties();
        snapShotProperties.setProperty("instanceName", myCommandKey.name());
        
        config.defineSnapShotMonitor("monitor", HystrixCommandMonitorImpl.class.getName(), 
        		snapShotProperties);
        config.attachAppenderToSnapShotMonitor("monitor", "SimpleAppender");

        // Execute the command 1 time.
        new MyCommand(myCommandKey, myGroupKey).execute();

        PerfMon.configure(config);
        
        Thread.sleep(2050);
        
        new MyCommand(myCommandKey, myGroupKey).execute();
        new MyCommand(myCommandKey, myGroupKey).execute();
        new MyCommand(myCommandKey, myGroupKey).execute();
        new MyCommand(myCommandKey, myGroupKey).execute();
        new MyCommand(myCommandKey, myGroupKey).execute();
        
        Thread.sleep(3020);
    }
    
	private static class MyCommand extends HystrixCommand<String> {

		MyCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey groupKey) {
			super(HystrixCommand.Setter.withGroupKey(groupKey).andCommandKey(commandKey));
		}

		@Override
		protected String run() throws Exception {
			System.err.println("***My Command Exectued***");        
			return "";
		}
	}

}
