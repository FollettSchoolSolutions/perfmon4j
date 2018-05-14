package web.org.perfmon4j.restdatasource.util.aggregators;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.BadRequestException;

import junit.framework.TestCase;

import org.perfmon4j.RegisteredDatabaseConnections;

import web.org.perfmon4j.restdatasource.BaseDatabaseSetup;
import web.org.perfmon4j.restdatasource.data.GroupID;
import web.org.perfmon4j.restdatasource.data.ID;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.SystemID;

public class SystemToGroupMapperTest extends TestCase {
	private static String DATABASE_NAME = "Production";
	private final BaseDatabaseSetup databaseSetup = new BaseDatabaseSetup();
	private SystemToGroupMapper mapper = null;
	private SystemID defaultSystemID = null;
	private RegisteredDatabaseConnections.Database database = null;

	public SystemToGroupMapperTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();

		databaseSetup.setUpDatabase();
		
		database = RegisteredDatabaseConnections.addDatabase(DATABASE_NAME, true, BaseDatabaseSetup.JDBC_DRIVER, 
				null, BaseDatabaseSetup.JDBC_URL, null, null, null, null, null, null);
		mapper = new SystemToGroupMapper(database);
		
		long systemID = databaseSetup.addSystem("default");
		defaultSystemID = new SystemID(database.getID(), systemID);
	}

	@Override
	protected void tearDown() throws Exception {
		RegisteredDatabaseConnections.removeDatabase(DATABASE_NAME);
		
		databaseSetup.tearDownDatabase();
		
		super.tearDown();
	}

	public void testResolveGroupsToSystemsNoGroups() throws Exception {
		ID ids[] = new ID[]{defaultSystemID};
		
		SystemID mappedIds[] = mapper.resolveGroupsToSystems(ids);
		assertEquals("Should only have the single system no groups exist", 1, mappedIds.length);
		assertEquals("Should be the default system ID", defaultSystemID, mappedIds[0]);
	}
	
	public void testExpectExceptionWhenIDDoesNotBelongToTargetDb() throws Exception {
		ID ids[] = new ID[]{defaultSystemID, new SystemID("AAAA-BBBB", defaultSystemID.getSystemID())};
		
		try {
			mapper.resolveGroupsToSystems(ids);
			fail("Expected a bad request exception because ID did not map to the database");
		} catch (BadRequestException ex) {
			// Expected;
		}
	}
	
	public void testResolveGroupsToSystemsExpandsGroups() throws Exception {
		GroupID myGroupA = new GroupID(database.getID(), databaseSetup.addGroup("MyGroupA"));
		databaseSetup.addGroupToSystem(myGroupA.getGroupID(), defaultSystemID.getSystemID());
		
		ID ids[] = new ID[]{myGroupA};
		
		SystemID mappedIds[] = mapper.resolveGroupsToSystems(ids);
		assertEquals("Should have only returned the system associated with the group", 1, mappedIds.length);
		assertEquals("Should be the default system ID", defaultSystemID, mappedIds[0]);
	}

	public void testExpandGroupsDoesNoDuplicateSystem() throws Exception {
		GroupID myGroupA = new GroupID(database.getID(), databaseSetup.addGroup("MyGroupA"));
		databaseSetup.addGroupToSystem(myGroupA.getGroupID(), defaultSystemID.getSystemID());
		
		ID ids[] = new ID[]{myGroupA, defaultSystemID};
		
		SystemID mappedIds[] = mapper.resolveGroupsToSystems(ids);
		assertEquals("Default system was in group, should not be returned multiple times", 1, mappedIds.length);
		assertEquals("Should be the default system ID", defaultSystemID, mappedIds[0]);
	}

	public void testResolveGroupsReturnGroupsNoGroups() throws Exception {
		Set<MonitoredSystem> systems = new TreeSet<MonitoredSystem>();
		MonitoredSystem def = new MonitoredSystem("default", defaultSystemID);
		systems.add(def);
		
		Set<MonitoredSystem> result = mapper.resolveGroups(systems, true);
		assertEquals("No groups to return", 0, result.size());
	}
	
	public void testResolveGroupsReturnGroupsOnly() throws Exception {
		GroupID myGroupA = new GroupID(database.getID(), databaseSetup.addGroup("MyGroupA"));
		databaseSetup.addGroupToSystem(myGroupA.getGroupID(), defaultSystemID.getSystemID());
		
		Set<MonitoredSystem> systems = new TreeSet<MonitoredSystem>();
		MonitoredSystem def = new MonitoredSystem("default", defaultSystemID);
		systems.add(def);
		
		Set<MonitoredSystem> result = mapper.resolveGroups(systems, true);
		assertEquals("Should only have returned the group", 1, result.size());
		assertEquals("Should be the group", "MyGroupA", result.iterator().next().getName());
	}

	public void testResolveGroupsReturnGroupsAndSystems() throws Exception {
		GroupID myGroupA = new GroupID(database.getID(), databaseSetup.addGroup("MyGroupA"));
		databaseSetup.addGroupToSystem(myGroupA.getGroupID(), defaultSystemID.getSystemID());
		
		Set<MonitoredSystem> systems = new TreeSet<MonitoredSystem>();
		MonitoredSystem def = new MonitoredSystem("default", defaultSystemID);
		systems.add(def);
		
		Set<MonitoredSystem> result = mapper.resolveGroups(systems, false);
		assertEquals("Should only have returned the group and the system", 2, result.size());
		
		Iterator<MonitoredSystem> itr = result.iterator();
		MonitoredSystem group = itr.next();
		MonitoredSystem system = itr.next();
		
		assertEquals("Group should be first", "MyGroupA", group.getName());
		assertEquals("Systems should sort after groups", "default", system.getName());
	}

}