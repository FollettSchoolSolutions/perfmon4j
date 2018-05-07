package web.org.perfmon4j.restdatasource.util.aggregators;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.perfmon4j.RegisteredDatabaseConnections;

import web.org.perfmon4j.restdatasource.data.ID;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.SystemID;

public class SystemToGroupMapper {
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
		
		for (ID id : ids) {
			if (id.isSystem()) {
				result.add((SystemID)id);
			} else {
				// Here is where we do the work!
			}
		}
		
		return result.toArray(new SystemID[]{});
	}
	
	
	/**
	 * We are going to assume that at this point all off the
	 * systems are from the same database.
	 * 
	 * @param systems
	 * @return
	 */
	public Set<MonitoredSystem> systemsToGroups(MonitoredSystem[] systems) {
		Set<MonitoredSystem> result = new HashSet<MonitoredSystem>();
		
		for (MonitoredSystem s : systems) {
			result.add(s);
		}
		
		return result;
	}

	protected String fixupSchema(String schema) {
		return schema == null ? "" : schema + ".";
	}	
}
