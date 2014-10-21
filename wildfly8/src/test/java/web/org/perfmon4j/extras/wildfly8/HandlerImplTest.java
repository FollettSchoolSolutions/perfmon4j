package web.org.perfmon4j.extras.wildfly8;

import junit.framework.TestCase;

//public class HandlerImplTest {
//}
public class HandlerImplTest extends TestCase {

	public HandlerImplTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	public void testMaskPasswordInQueryString() {
		assertEquals("Password only parameter", "?password=*******", HandlerImpl.maskPassword("?password=dave"));
		assertEquals("Password second parameter", "?user=dave&password=*******", HandlerImpl.maskPassword("?user=dave&password=dave"));
		assertEquals("Password middle parameter", "?user=dave&password=*******&time=now", HandlerImpl.maskPassword("?user=dave&password=dave&time=now"));
		assertEquals("Multiple password parameters", "?password=*******&password=*******&password=*******&password=*******", HandlerImpl.maskPassword("?password=dave&password=this is a test&password=&password=t"));
		assertEquals("Password in value is ignored", "?word=password", HandlerImpl.maskPassword("?word=password"));
	}

}
