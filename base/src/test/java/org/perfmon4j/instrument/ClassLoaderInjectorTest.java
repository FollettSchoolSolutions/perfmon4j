package org.perfmon4j.instrument;

import junit.framework.TestCase;

public class ClassLoaderInjectorTest extends TestCase {

	public void testGetInstance() {
		ClassLoaderInjector injector = ClassLoaderInjector.getInstance();
		
		assertNotNull("ClassLoaderInjector should return an instance", injector);
		assertTrue("Should be javassist injector", injector instanceof JavassistClassLoaderInjector);
	}
}
