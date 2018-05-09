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
import web.org.perfmon4j.restdatasource.data.SystemID;

public class SystemToGroupMapper {
	private static final Logger logger = LoggerFactory.initLogger(SystemToGroupMapper.class);
	private final RegisteredDatabaseConnections.Database db;
	
	public SystemToGroupMapper(RegisteredDatabaseConnections.Database db) {
		this.db = db;
	}
	
	
	/**
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
		
		if (!groups.isEmpty()) {
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
	

	protected String fixupSchema(String schema) {
		return schema == null ? "" : schema + ".";
	}	
}
