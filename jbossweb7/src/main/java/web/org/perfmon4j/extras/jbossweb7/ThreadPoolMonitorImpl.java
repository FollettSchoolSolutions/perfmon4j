package web.org.perfmon4j.extras.jbossweb7;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

@SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR, 
		dataInterface=ThreadPoolMonitor.class,
		sqlWriter=ThreadPoolMonitorImpl.SQLWriter.class)
public class ThreadPoolMonitorImpl extends JMXMonitorBase {
	final private static Logger logger = LoggerFactory.initLogger(ThreadPoolMonitorImpl.class);
	
	private static String buildBaseObjectName() {
		return "jboss.as:subsystem=threads";
	}
	
	/**
	 * HACK HACK HACK!  This only supports unbounded queue thread pools... Should be able 
	 * to support all JBoss types!
	 */
	public ThreadPoolMonitorImpl() {
		super(buildBaseObjectName(), "unbounded-queue-thread-pool", null);
	}
	
	public ThreadPoolMonitorImpl(String instanceName) {
		super(buildBaseObjectName(), "unbounded-queue-thread-pool", instanceName);
	}

	@SnapShotInstanceDefinition
	static public String[] getInstanceNames() throws MalformedObjectNameException, NullPointerException {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		return MiscHelper.getAllObjectName(mBeanServer, new ObjectName(buildBaseObjectName()), "unbounded-queue-thread-pool");
	}
	
	@SnapShotString(isInstanceName=true)
	public String getInstanceName() {
		return MiscHelper.getInstanceNames(getMBeanServer(), getQueryObjectName(), "unbounded-queue-thread-pool");
	}

	@SnapShotGauge
	public long getCurrentThreadsBusy() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "activeCount");
	}
	
	@SnapShotGauge
	public long getCurrentThreadCount() {
		return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadCount");
	}
	
	public static class SQLWriter implements SnapShotSQLWriter {
		public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
			throws SQLException {
			writeToSQL(conn, schema, (ThreadPoolMonitor)data, systemID);
		}
		
		public void writeToSQL(Connection conn, String schema, ThreadPoolMonitor data, long systemID)
			throws SQLException {
			schema = (schema == null) ? "" : (schema + ".");
			
			final String SQL = "INSERT INTO " + schema + "P4JThreadPoolMonitor " +
				"(SystemID, ThreadPoolOwner, InstanceName, StartTime, EndTime, Duration,  " +
				"CurrentThreadsBusy, CurrentThreadCount) " +
				"VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement(SQL);
				
				int index = 1;
				stmt.setLong(index++, systemID);
				stmt.setString(index++, "JBoss");
				stmt.setString(index++, data.getInstanceName());
				stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
				stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
				stmt.setLong(index++, data.getDuration());
				stmt.setLong(index++, data.getCurrentThreadsBusy());
				stmt.setLong(index++, data.getCurrentThreadCount());
				
				int count = stmt.executeUpdate();
				if (count != 1) {
					throw new SQLException("ThreadPoolMonitor failed to insert row");
				}
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		}
	}
}
