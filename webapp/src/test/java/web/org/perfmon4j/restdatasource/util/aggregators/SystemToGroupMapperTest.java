package web.org.perfmon4j.restdatasource.util.aggregators;

import java.util.Set;

import junit.framework.TestCase;

import org.perfmon4j.RegisteredDatabaseConnections;

import web.org.perfmon4j.restdatasource.BaseDatabaseSetup;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;

public class SystemToGroupMapperTest extends TestCase {
	private static String DATABASE_NAME = "Production";
	private final BaseDatabaseSetup databaseSetup = new BaseDatabaseSetup();
	private MonitoredSystem defaultSystem = null;
	private SystemToGroupMapper mapper = null;
	private long defaultSystemID;

	public SystemToGroupMapperTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();

		databaseSetup.setUpDatabase();
		defaultSystemID = databaseSetup.addSystem("default");
		

		defaultSystem = new MonitoredSystem("default", "aaaa.bbbb." + Long.toString(defaultSystemID), false);
		
		RegisteredDatabaseConnections.Database database = RegisteredDatabaseConnections.addDatabase(DATABASE_NAME, true, BaseDatabaseSetup.JDBC_DRIVER, 
				null, BaseDatabaseSetup.JDBC_URL, null, null, null, null, null, null);
		mapper = new SystemToGroupMapper(database);
	}

	protected void tearDown() throws Exception {
		RegisteredDatabaseConnections.removeDatabase(DATABASE_NAME);
		
		super.tearDown();
	}

	public void testSystemsToGroupsNoGroups() {
		MonitoredSystem[] systems = new MonitoredSystem[]{defaultSystem};
		
		Set<MonitoredSystem> result = mapper.systemsToGroups(systems);
		assertEquals("Should only have the single system no groups exist", 1, result.size());
	}

}
