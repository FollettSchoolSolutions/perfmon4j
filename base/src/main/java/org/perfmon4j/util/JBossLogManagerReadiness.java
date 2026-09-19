/*
 *	Copyright 2026 Follett Software Company
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at
 * 	http://www.gnu.org/licenses/
 *
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
*/
package org.perfmon4j.util;

import java.lang.instrument.Instrumentation;
import java.util.function.BooleanSupplier;

/**
 * Makes sure perfmon4j never lets {@code java.util.logging} initialize with the wrong
 * LogManager while running as a -javaagent under JBoss/WildFly.
 * <p>
 * Something as innocent as the first {@code ManagementFactory.getPlatformMBeanServer()}
 * call (it builds a PlatformLoggingMXBean) runs {@code java.util.logging.LogManager}'s
 * static initializer. That initializer loads the class named by
 * {@code java.util.logging.manager} from the <em>system classloader</em>, and if that fails
 * falls back to the <em>calling thread's context classloader</em>. If both fail JUL prints
 * "Could not load Logmanager" and silently installs the JDK default manager instead - once
 * per JVM, unrecoverable, and it breaks WildFly's own logging boot later (WFLYLOG0078).
 * <p>
 * Under WildFly {@code org.jboss.logmanager.LogManager} is <em>never</em> visible to the
 * system classloader. WildFly gets it because jboss-modules' main thread has a module
 * classloader as its context classloader. So the class being loadable via the system
 * classloader is not a usable readiness signal (waiting for that never succeeds). What is
 * observable is that jboss-modules has loaded the class, through some classloader, which we
 * find via {@link Instrumentation#getAllLoadedClasses()}. Once it has, this class
 * initializes JUL on our own thread with the context classloader temporarily set to that
 * class's loader, exactly as WildFly does. That both waits for JBoss and removes the race
 * with it: whichever thread wins, JUL gets the JBoss LogManager. Afterwards every other
 * thread can safely touch JUL.
 * <p>
 * Nothing here ever uses a JUL method before that point, nor the {@link Logger} abstraction
 * (which can itself fall through to JUL). Never block premain on this: the class only appears
 * after premain returns. {@link #runWhenReady} does its waiting on its own daemon thread.
 */
public final class JBossLogManagerReadiness {
	public static final String JBOSS_LOG_MANAGER = "org.jboss.logmanager.LogManager";
	public static final String MANAGER_PROPERTY = "java.util.logging.manager";

	static final long POLL_INTERVAL_MILLIS = 100;
	static final long MAX_WAIT_MILLIS = 60000; // WildFly normally loads it within the first second or two.

	private static volatile Instrumentation instrumentation = null;

	private JBossLogManagerReadiness() {
	}

	/**
	 * Called from premain so readiness can be observed. Without it (perfmon4j used
	 * as a plain library, loaded after the server has booted) there is nothing to wait for.
	 */
	public static void setInstrumentation(Instrumentation inst) {
		instrumentation = inst;
	}

	/**
	 * @return true if the JVM was launched asking for JBoss LogManager
	 * (i.e. we are running under JBoss/WildFly).
	 */
	public static boolean isJBossLogManagerRequested() {
		return JBOSS_LOG_MANAGER.equals(System.getProperty(MANAGER_PROPERTY));
	}

	/**
	 * True if {@code org.jboss.logmanager.LogManager} is resolvable via the system
	 * classloader (for example jboss-logmanager on the application classpath, as
	 * with Quarkus). Non-initializing ({@code Class.forName(..., false, ...)}):
	 * no static block runs and no {@code java.util.logging} class is touched.
	 * <p>
	 * NOT true under WildFly - see the class comment.
	 */
	public static boolean isJBossLogManagerLoadableBySystemClassLoader() {
		return isClassLoadable(JBOSS_LOG_MANAGER);
	}

	static boolean isClassLoadable(String className) {
		try {
			Class.forName(className, false, ClassLoader.getSystemClassLoader());
			return true;
		} catch (Throwable notYetLoadable) {
			return false;
		}
	}

	/**
	 * @return the class with the given name, or null if it is not in {@code classes}.
	 */
	static Class<?> findClassByName(Class<?>[] classes, String className) {
		for (Class<?> clazz : classes) {
			if (className.equals(clazz.getName())) {
				return clazz;
			}
		}
		return null;
	}

	/**
	 * @return JBoss's LogManager class if some classloader has loaded it, otherwise null
	 * (also null when perfmon4j is not running as an agent).
	 */
	static Class<?> findLoadedJBossLogManager() {
		Instrumentation inst = instrumentation;
		return inst == null ? null : findClassByName(inst.getAllLoadedClasses(), JBOSS_LOG_MANAGER);
	}

	/**
	 * True when it is safe to have {@code java.util.logging} initialize (see {@link #initializeJavaUtilLogging}).
	 */
	static boolean isReady() {
		if (!isJBossLogManagerRequested()) {
			return false;
		}
		return instrumentation == null // Not running as an agent, the server has already booted.
			|| isJBossLogManagerLoadableBySystemClassLoader()
			|| findLoadedJBossLogManager() != null;
	}

	/**
	 * Makes sure {@code java.util.logging} is initialized with JBoss LogManager, from the current
	 * thread. If JBoss has already initialized it this just returns the installed manager. Otherwise
	 * the context classloader is set to JBoss LogManager's classloader for the duration of the call,
	 * so JUL's lookup succeeds exactly as it does on WildFly's own main thread.
	 *
	 * @return the class name of the installed LogManager.
	 */
	static String initializeJavaUtilLogging() {
		Thread thread = Thread.currentThread();
		ClassLoader original = thread.getContextClassLoader();
		try {
			Class<?> jbossLogManager = findLoadedJBossLogManager();
			if (jbossLogManager != null && jbossLogManager.getClassLoader() != null) {
				thread.setContextClassLoader(jbossLogManager.getClassLoader());
			}
			return java.util.logging.LogManager.getLogManager().getClass().getName();
		} finally {
			thread.setContextClassLoader(original);
		}
	}

	/**
	 * Polls {@code ready} until it returns true or {@code maxWaitMillis} elapses.
	 *
	 * @return true if ready, false if it timed out (or the thread was interrupted).
	 */
	static boolean awaitReady(BooleanSupplier ready, long pollIntervalMillis, long maxWaitMillis) {
		long deadline = System.currentTimeMillis() + maxWaitMillis;
		while (!ready.getAsBoolean()) {
			if (System.currentTimeMillis() >= deadline) {
				return false;
			}
			try {
				Thread.sleep(pollIntervalMillis);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return true;
	}

	/**
	 * Waits, on a new daemon thread, until JBoss LogManager has been loaded; makes sure
	 * {@code java.util.logging} is initialized with it; waits {@code settleMillis}; then runs
	 * {@code task}. Returns immediately.
	 *
	 * @param description What the task does; used in diagnostics.
	 * @param settleMillis Extra wait after readiness is confirmed. Zero or negative for none.
	 * @param runIfTimedOut If JBoss LogManager never appears (not really JBoss after all,
	 * or a broken launch) run the task anyway? Choose true when skipping the work is worse than
	 * the risk; choose false when the work is what would break JBoss's logging.
	 */
	public static void runWhenReady(String description, long settleMillis, boolean runIfTimedOut, Runnable task) {
		runWhenReady(description, settleMillis, runIfTimedOut, task,
			JBossLogManagerReadiness::isReady, JBossLogManagerReadiness::reportAndInitializeJavaUtilLogging,
			POLL_INTERVAL_MILLIS, MAX_WAIT_MILLIS);
	}

	private static void reportAndInitializeJavaUtilLogging() {
		Class<?> loaded = findLoadedJBossLogManager();
		String installed = initializeJavaUtilLogging();
		System.err.println("Perfmon4j: " + JBOSS_LOG_MANAGER + " is available (classloader: "
			+ (loaded == null ? "system/none" : String.valueOf(loaded.getClassLoader()))
			+ "). java.util.logging is using: " + installed);
		if (!JBOSS_LOG_MANAGER.equals(installed)) {
			System.err.println("Perfmon4j: WARNING java.util.logging.manager is " + JBOSS_LOG_MANAGER
				+ " but " + installed + " is installed. Something initialized java.util.logging too early.");
		}
	}

	static void runWhenReady(final String description, final long settleMillis, final boolean runIfTimedOut,
			final Runnable task, final BooleanSupplier readyCheck, final Runnable onReady,
			final long pollIntervalMillis, final long maxWaitMillis) {
		Thread thread = new Thread("Perfmon4j.JBossLogManagerReadiness") {
			@Override
			public void run() {
				boolean ready = awaitReady(readyCheck, pollIntervalMillis, maxWaitMillis);
				if (ready) {
					try {
						onReady.run();
					} catch (Throwable th) {
						System.err.println("Perfmon4j: unable to confirm java.util.logging state before: " + description + " - " + th);
					}
					if (settleMillis > 0) {
						try {
							Thread.sleep(settleMillis);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
				} else {
					System.err.println("Perfmon4j: " + JBOSS_LOG_MANAGER + " did not appear within "
						+ maxWaitMillis + "ms. " + (runIfTimedOut ? "Proceeding with: " : "Skipping: ") + description);
					if (!runIfTimedOut) {
						return;
					}
				}

				try {
					task.run();
				} catch (Throwable th) {
					System.err.println("Perfmon4j: unexpected failure in: " + description + " - " + th);
					th.printStackTrace();
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}
}
