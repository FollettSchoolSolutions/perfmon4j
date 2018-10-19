package web.org.perfmon4j.restdatasource.util.aggregators;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.InternalServerErrorException;

import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import web.org.perfmon4j.restdatasource.data.GroupID;
import web.org.perfmon4j.restdatasource.data.ID;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.SystemID;

public class SystemToGroupMapper {
	private static final Logger logger = LoggerFactory.initLogger(SystemToGroupMapper.class);
	private final RegisteredDatabaseConnections.Database db;
	public static final double MIN_DATABASE_VERSION = 6.0;
	
	public SystemToGroupMapper(RegisteredDatabaseConnections.Database db) {
		this.db = db;
	}
	
	
	/* *
	 * Removes all of the GroupIDs in the incoming array
	 * and replaces them with the Systems associated with the Group.
	 */
	public SystemID[] resolveGroupsToSystems(ID[] ids) {
		Set<SystemID> result = new TreeSet<SystemID>();
		Set<GroupID> groups = new HashSet<GroupID>();
		
		for (ID id : ids) {
			id.validateMatchesDatabase(db);
			if (id.isSystem()) {
				result.add((SystemID)id);
			} else if (id.isGroup()){
				groups.add((GroupID)id);
			} else {
				logger.logWarn("Unexpected ID type.  Expected SystemID or GroupID");
			}
		}
		
		if (!groups.isEmpty() && (db.getDatabaseVersion() >= MIN_DATABASE_VERSION)) {
			String sql = "SELECT SystemID FROM "+ fixupSchema(db.getSchema())+ "P4JGroupSystemJoin WHERE GroupID IN (";
			boolean firstTime = true;
			for (GroupID group : groups) {
				if (!firstTime) {
					sql += ", ";
 				} else {
 					firstTime = false;
 				}
				sql += Long.toString(group.getGroupID());
			}
			sql += ")";

			try {
				Connection conn = db.openConnection();
				Statement stmt = null;
				ResultSet rs = null;
				try {
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while (rs.next()) {
						result.add(new SystemID(db.getID(), rs.getLong(1)));
					}
				} finally {
					JDBCHelper.closeNoThrow(rs);
					JDBCHelper.closeNoThrow(stmt);
					JDBCHelper.closeNoThrow(conn);
				}
			} catch (SQLException s) {
				throw new InternalServerErrorException(s);
			}
		}
		return result.toArray(new SystemID[]{});
	}
	

	public Set<MonitoredSystem> resolveGroups(Set<MonitoredSystem> systems, boolean returnOnlyGroups) {
		if (!systems.isEmpty()&& (db.getDatabaseVersion() >= MIN_DATABASE_VERSION)) {
			MonitoredSystem[] inSystems = systems.toArray(new MonitoredSystem[systems.size()]);
			if (returnOnlyGroups) {
				systems.clear();
			}
			String sql = "SELECT g.GroupName, g.GroupID "
					+ "FROM P4JGroup g "
					+ "JOIN P4JGroupSystemJoin j ON j.GroupID = g.GroupID "
					+ "WHERE j.SystemID IN (";
			boolean firstTime = true;
			for (MonitoredSystem s : inSystems) {
				ID id = ID.parse(s.getID());
				if (!id.isSystem()) {
					throw new InternalServerErrorException("Did not expect group in MonitoredSystem set");
				}
				SystemID systemID = (SystemID)id;
				
				if (!firstTime) {
					sql += ", ";
				} else {
					firstTime = false;
				}
				sql += Long.toString(systemID.getSystemID());
			}
			sql += ") GROUP BY g.GroupName, g.GroupID";
			try {
				Connection conn = db.openConnection();
				Statement stmt = null;
				ResultSet rs = null;
				try {
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while (rs.next()) {
						GroupID groupID = new GroupID(db.getID(), rs.getLong(2));
						systems.add(new MonitoredSystem(rs.getString(1), groupID));
					}
				} finally {
					JDBCHelper.closeNoThrow(rs);
					JDBCHelper.closeNoThrow(stmt);
					JDBCHelper.closeNoThrow(conn);
				}
			} catch (SQLException s) {
				throw new InternalServerErrorException(s);
			}
		}
		return systems;
	}
	
	
	protected String fixupSchema(String schema) {
		return schema == null ? "" : schema + ".";
	}	
}
