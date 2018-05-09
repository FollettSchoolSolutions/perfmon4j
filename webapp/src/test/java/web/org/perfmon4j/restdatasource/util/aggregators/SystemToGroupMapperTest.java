package web.org.perfmon4j.restdatasource.util.aggregators;

import javax.ws.rs.BadRequestException;

import junit.framework.TestCase;

import org.perfmon4j.RegisteredDatabaseConnections;

import web.org.perfmon4j.restdatasource.BaseDatabaseSetup;
import web.org.perfmon4j.restdatasource.data.GroupID;
import web.org.perfmon4j.restdatasource.data.ID;
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

}
