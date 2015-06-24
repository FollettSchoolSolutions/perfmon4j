package web.org.perfmon4j.console.app.spring.security;

import junit.framework.TestCase;

public class Perfmon4jUserConsoleLoginServiceTest extends TestCase {

	public Perfmon4jUserConsoleLoginServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGenerateMD5Hash() {
		final String expected = "81dc9bdb52d04dc20036dbd8313ed055";  // MD5 Hash for "1234"
		final String result = Perfmon4jUserConsoleLoginService.generateMD5Hash("1234");
			
		assertEquals("Should have generated correct hash", expected, result);
	}

}
