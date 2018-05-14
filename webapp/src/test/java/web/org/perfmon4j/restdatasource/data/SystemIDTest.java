package web.org.perfmon4j.restdatasource.data;

import junit.framework.TestCase;

public class SystemIDTest extends TestCase {
	private static final String validDatabaseID = "ABCD-EFGH";

	public SystemIDTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testDisplayable() {
		assertEquals("Displayable", "ABCD-EFGH.8", new SystemID(validDatabaseID, 8L).getDisplayable());
	}
	
	public void testSortable() {
		assertEquals("Sortable should zero pad up to 20 digits", "ABCD-EFGH.00000000000000000008", 
				new SystemID(validDatabaseID, 8L).getSortable());
		assertEquals("Sortable should zero pad up to 20 digits", "ABCD-EFGH.00000000000000009000", 
				new SystemID(validDatabaseID, 9000L).getSortable());
	}
	
	public void testParseSystemID() {
		SystemID id = new SystemID(validDatabaseID, 10);
		
		ID parsed = ID.parse(id.getDisplayable());
		assertEquals("should match the systemID", id, parsed);
		
		// Should also be able to parse the sortable
		parsed = ID.parse(id.getSortable());
		assertEquals("should match the systemID", id, parsed);
	}
}
