package web.org.perfmon4j.restdatasource.dataproviders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.RegisteredDatabaseConnections.Database;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import web.org.perfmon4j.restdatasource.DataProvider;
import web.org.perfmon4j.restdatasource.DataSourceRestImpl.SystemID;
import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.data.Category;
import web.org.perfmon4j.restdatasource.data.CategoryTemplate;
import web.org.perfmon4j.restdatasource.data.Field;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.query.advanced.ResultAccumulator;
import web.org.perfmon4j.restdatasource.util.SeriesField;

public class JVMDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "JVM";
	private final JVMTemplate categoryTemplate;
	private static final Logger logger = LoggerFactory.initLogger(JVMDataProvider.class);
	
	public JVMDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new JVMTemplate(TEMPLATE_NAME);
	}

	@Override
	public void processResults(Connection conn, RegisteredDatabaseConnections.Database db, ResultAccumulator accumulator, SeriesField[] fields, long start, 
			long end) throws SQLException {
		String schemaPrefix = fixupSchema(db.getSchema());
		Set<String> selectList = buildSelectListAndPopulateAccumulators(accumulator, fields);
		
		selectList.add("EndTime");
		
		String query =
			"SELECT " + commaSeparate(selectList) + "\r\n"
			+ "FROM " + schemaPrefix + "P4JVMSnapShot pid\r\n"
			+ "WHERE pid.systemID IN " + buildSystemIDSet(fields) + "\r\n"
			+ "AND pid.EndTime >= ?\r\n"
			+ "AND pid.EndTime <= ?\r\n";
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(query);
			stmt.setTimestamp(1, new Timestamp(start));
			stmt.setTimestamp(2, new Timestamp(end));
			rs = stmt.executeQuery();
			while (rs.next()) {
				accumulator.accumulateResults(TEMPLATE_NAME, rs);
			}
		} finally {
			JDBCHelper.closeNoThrow(stmt);
			JDBCHelper.closeNoThrow(rs);
		}
	}

	@Override
	public CategoryTemplate getCategoryTemplate() {
		return categoryTemplate;
	}

	@Override
	public Set<MonitoredSystem> lookupMonitoredSystems(Connection conn, RegisteredDatabaseConnections.Database database, 
			long start, long end) throws SQLException {
		Set<MonitoredSystem> result = new HashSet<MonitoredSystem>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			String schema = fixupSchema(database.getSchema());
			
			String SQL = "SELECT SystemID, SystemName "
				+ " FROM " + schema + "P4JSystem s "
				+ " WHERE EXISTS (SELECT SystemID " 
				+ " FROM " + schema + "P4JVMSnapShot pid WHERE pid.SystemID = s.SystemID "
				+ "	AND pid.EndTime >= ? AND pid.EndTime <= ?)";
			if (logger.isDebugEnabled()) {
				logger.logDebug("getSystems SQL: " + SQL);
			}
			
			stmt = conn.prepareStatement(SQL);
			stmt.setTimestamp(1, new Timestamp(start));
			stmt.setTimestamp(2, new Timestamp(end));
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				MonitoredSystem ms = new MonitoredSystem(rs.getString("SystemName").trim(), database.getID() + "." + rs.getLong("SystemID"));
				result.add(ms);
			}
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmt);
		}
		
		return result;
	}

	@Override
	public Set<Category> lookupMonitoredCategories(Connection conn,
			Database db, SystemID[] systems, long start, long end)
			throws SQLException {
			Set<Category> result = new HashSet<Category>();
		
			PreparedStatement stmt = null;	
			try {
				String schema = fixupSchema(db.getSchema());
				
				String SQL = "SELECT COUNT(*) "
					+ " FROM " + schema + "P4JVMSnapShot pid "
					+ "	WHERE pid.EndTime >= ? "
					+ " AND pid.EndTime <= ?"
					+ " AND pid.SystemID IN " + buildInArrayForSystems(systems);
				if (logger.isDebugEnabled()) {
					logger.logDebug("getSystems SQL: " + SQL);
				}
				stmt = conn.prepareStatement(SQL);
				stmt.setTimestamp(1, new Timestamp(start));
				stmt.setTimestamp(2, new Timestamp(end));
				if (JDBCHelper.getQueryCount(stmt) > 0) {
					result.add(new Category(TEMPLATE_NAME, TEMPLATE_NAME));
				}
			} finally {
				JDBCHelper.closeNoThrow(stmt);
			}
		
		return result;
	}

	private static class JVMTemplate extends CategoryTemplate {

		private JVMTemplate(String templateName) {
			super(templateName, buildFields());
		}
		
		private static final Field[] buildFields() {
			List<Field> fields = new ArrayList<Field>();

			fields.add(new ProviderField("currentClassLoadCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "CurrentClassLoadCount", false));
			fields.add(new ProviderField("pendingClassFinalizationCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "PendingClassFinalizationCount", false));
			fields.add(new ProviderField("currentThreadCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "CurrentThreadCount", false));
			fields.add(new ProviderField("currentDaemonThreadCount", AggregationMethod.DEFAULT, AggregationMethod.SUM, "CurrentDaemonThreadCount", false));
			fields.add(new ProviderField("heapMemUsedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "HeapMemUsedMB", true));
			fields.add(new ProviderField("heapMemCommitedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "HeapMemCommitedMB", true));
			fields.add(new ProviderField("heapMemMaxMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "HeapMemMaxMB", true));
			fields.add(new ProviderField("nonHeapMemUsedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "NonHeapMemUsedMB", true));
			fields.add(new ProviderField("nonHeapMemCommittedUsedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "NonHeapMemCommittedUsedMB", true));
			fields.add(new ProviderField("nonHeapMemMaxUsedMB", AggregationMethod.DEFAULT, AggregationMethod.MAX, "NonHeapMemMaxUsedMB", true));
			fields.add(new ProviderField("systemCpuLoad", AggregationMethod.DEFAULT, AggregationMethod.MAX, "systemCpuLoad", true));
			fields.add(new ProviderField("processCpuLoad", AggregationMethod.DEFAULT, AggregationMethod.MAX, "processCpuLoad", true));
			fields.add(new NaturalPerMinuteProviderField("classLoadCountPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, "ClassLoadCountPerMinute", "startTime", "endTime", "ClassLoadCountInPeriod", true));
			fields.add(new NaturalPerMinuteProviderField("classUnloadCountPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, "ClassUnloadCountPerMinute", "startTime", "endTime", "ClassUnloadCountInPeriod", true));
			fields.add(new NaturalPerMinuteProviderField("threadStartCountPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, "ThreadStartCountPerMinute", "startTime", "endTime", "ThreadStartCountInPeriod", true));
			fields.add(new NaturalPerMinuteProviderField("compilationMillisPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL, "CompilationMillisPerMinute", "startTime", "endTime", "CompilationMillisInPeriod", true));
			
			return fields.toArray(new Field[]{});
		}
	}
}
