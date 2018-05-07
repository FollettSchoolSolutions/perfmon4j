package web.org.perfmon4j.restdatasource.data;

import junit.framework.TestCase;

public class GroupIDTest extends TestCase {
	private static final String validDatabaseID = "ABCD-EFGH";

	public GroupIDTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testDisplayable() {
		assertEquals("Displayable", "ABCD-EFGH.GROUP.8", new GroupID(validDatabaseID, 8L).getDisplayable());
	}
	
	public void testSortable() {
		assertEquals("Sortable should zero pad up to 20 digits", "ABCD-EFGH.GROUP.00000000000000000008", 
				new GroupID(validDatabaseID, 8L).getSortable());
		assertEquals("Sortable should zero pad up to 20 digits", "ABCD-EFGH.GROUP.00000000000000009000", 
				new GroupID(validDatabaseID, 9000L).getSortable());
	}

	public void testParseGroupID() {
		GroupID id = new GroupID(validDatabaseID, 10);
		
		ID parsed = ID.parse(id.getDisplayable());
		assertEquals("should match the systemID", id, parsed);
		
		// Should also be able to parse the sortable
		parsed = ID.parse(id.getSortable());
		assertEquals("should match the systemID", id, parsed);
	}


}
