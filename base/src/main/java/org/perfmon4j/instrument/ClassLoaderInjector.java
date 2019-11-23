package org.perfmon4j.instrument;

import org.perfmon4j.PerfMon;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public abstract class ClassLoaderInjector {
	private static final Logger logger = LoggerFactory.initLogger(ClassLoaderInjector.class);
	private static ClassLoaderInjector instance = null;
	
	@SuppressWarnings("unchecked")
	public static synchronized ClassLoaderInjector getInstance() {
		final String JAVASSIST_INJECTOR_NAME = "org.perfmon4j.instrument.JavassistClassLoaderInjector";
		
		if (instance == null) {
			try {
				Class<? extends ClassLoaderInjector> clazz = (Class<? extends ClassLoaderInjector>)PerfMon.getClassLoader().loadClass(JAVASSIST_INJECTOR_NAME);
				instance = clazz.newInstance();
			} catch (ClassNotFoundException e) {
				logger.logWarn("Forced to use NoOpClassLoaderInjector, unable to find class ClassLoaderInjector: " + JAVASSIST_INJECTOR_NAME, e, true);
			} catch (InstantiationException e) {
				logger.logWarn("Forced to use NoOpClassLoaderInjector, unable to instantiate ClassLoaderInjector: " + JAVASSIST_INJECTOR_NAME, e, true);
			} catch (IllegalAccessException e) {
				logger.logWarn("Forced to use NoOpClassLoaderInjector, unable to access ClassLoaderInjector: " + JAVASSIST_INJECTOR_NAME, e, true);
			}
			
			if (instance == null) {
				instance = new NoOpClassLoaderInjector();
			}
		}
		
		return instance;
	}

	abstract public void InjectClassLoader(ClassLoader target, ClassLoader src);

	public static interface ClassLoaderExtension {
	}
	
	
	private static class NoOpClassLoaderInjector extends ClassLoaderInjector {
		@Override
		public void InjectClassLoader(ClassLoader src, ClassLoader target) {
			// Don't do anything.
		}
	}
	
}
