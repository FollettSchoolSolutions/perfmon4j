package web.org.perfmon4j.restdatasource.data;

import junit.framework.TestCase;

public class IDTest extends TestCase {
	public IDTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGoodDatabaseID() {
		try {
			new ConcreteID("ABCD-EFGH");
			new ConcreteID("1111-2222");
			new ConcreteID("abcd-33BC");
		} catch (BadIDException b) {
			fail("This was a good database ID - BadIDException not expected");
		}
	}
	
	public void testBadDatabaseID() {
		try {
			new ConcreteID("ABC-EFGH");
			fail("Expected a BadIDException not long enough");
		} catch (BadIDException b) {
			// Expected
		}
		
		try {
			new ConcreteID("ABCDEFGH");
			fail("Expected a BadIDException no dash");
		} catch (BadIDException b) {
			// Expected
		}

		try {
			new ConcreteID("ABC$-EFGH");
			fail("Expected a BadIDException invalid character");
		} catch (BadIDException b) {
			// Expected
		}
	}

	
	/**
	 * Need a concrete class to test the
	 * abstract ID class
	 *
	 */
	private static class ConcreteID extends ID {
		public ConcreteID(String databaseID) {
			this(databaseID, "", "");
		}
		public ConcreteID(String databaseID, String sortable,
				String displayable) throws BadIDException {
			super(databaseID, sortable, displayable);
		}
	}
}
