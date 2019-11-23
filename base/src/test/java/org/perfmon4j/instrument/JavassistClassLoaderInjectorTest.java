package org.perfmon4j.instrument;

import junit.framework.TestCase;

public class JavassistClassLoaderInjectorTest extends TestCase {
	private ClassLoaderInjector injector = ClassLoaderInjector.getInstance();
	
	
	public void testInitialInjection() {
		ClassLoader loaderTarget = new ClassLoader() {
		}; 
		ClassLoader loaderSrc = new ClassLoader() {
		}; 
		
		injector.InjectClassLoader(loaderTarget, loaderSrc);

		
		assertTrue("loaderTarget should now implement the interface", loaderTarget instanceof ClassLoaderInjector.ClassLoaderExtension);
	}
	
	
	

}
