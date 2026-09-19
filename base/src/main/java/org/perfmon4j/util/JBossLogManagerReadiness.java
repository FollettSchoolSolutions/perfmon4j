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

import java.util.function.BooleanSupplier;

/**
 * Waits for JBoss LogManager to really be installable before perfmon4j does
 * anything at -javaagent premain time that touches {@code java.util.logging}.
 * <p>
 * Something as innocent as the first {@code ManagementFactory.getPlatformMBeanServer()}
 * call (it builds a PlatformLoggingMXBean) runs {@code java.util.logging.LogManager}'s
 * static initializer. If {@code java.util.logging.manager} names
 * {@code org.jboss.logmanager.LogManager} but that class is not yet resolvable via the
 * system classloader, JUL prints "Could not load Logmanager" and silently installs the
 * JDK default manager instead - once per JVM, unrecoverable, and it breaks WildFly's own
 * logging subsystem boot later (WFLYLOG0078).
 * <p>
 * jboss-modules sets the system property and makes the class loadable in two separate,
 * non-atomic steps, so the property alone is not proof of readiness. This class polls
 * two side channels - never a JUL method, never the {@link Logger} abstraction (which
 * can itself fall through to JUL) - until <em>both</em> agree: the property names JBoss
 * LogManager, and the class is actually loadable via the system classloader. That
 * replaces the fixed "wait 5 seconds and hope" delay perfmon4j used previously.
 * <p>
 * Never block premain on this: the class may only become loadable after premain returns.
 * {@link #runWhenReady} does its waiting on its own daemon thread.
 */
public final class JBossLogManagerReadiness {
	public static final String JBOSS_LOG_MANAGER = "org.jboss.logmanager.LogManager";
	public static final String MANAGER_PROPERTY = "java.util.logging.manager";

	static final long POLL_INTERVAL_MILLIS = 100;
	static final long MAX_WAIT_MILLIS = 60000; // WildFly normally installs it within the first second or two.

	private JBossLogManagerReadiness() {
	}

	/**
	 * @return true if the JVM was launched asking for JBoss LogManager
	 * (i.e. we are running under JBoss/WildFly).
	 */
	public static boolean isJBossLogManagerRequested() {
		return JBOSS_LOG_MANAGER.equals(System.getProperty(MANAGER_PROPERTY));
	}

	/**
	 * True once {@code org.jboss.logmanager.LogManager} is resolvable via the system
	 * classloader - the same lookup {@code java.util.logging.LogManager}'s static
	 * initializer performs. Non-initializing ({@code Class.forName(..., false, ...)}):
	 * no static block runs and no {@code java.util.logging} class is touched.
	 */
	public static boolean isJBossLogManagerLoadable() {
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

	static boolean isReady() {
		return isJBossLogManagerRequested() && isJBossLogManagerLoadable();
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
	 * Runs {@code task} on a new daemon thread once JBoss LogManager is ready,
	 * followed by {@code settleMillis} to let jboss-modules finish. Returns immediately.
	 *
	 * @param description What the task does; used in diagnostics.
	 * @param settleMillis Extra wait after readiness is confirmed. Zero or negative for none.
	 * @param runIfTimedOut If JBoss LogManager never becomes ready (not really JBoss after all,
	 * or a broken launch) run the task anyway? Choose true when skipping the work is worse than
	 * the risk; choose false when the work is what would break JBoss's logging.
	 */
	public static void runWhenReady(String description, long settleMillis, boolean runIfTimedOut, Runnable task) {
		runWhenReady(description, settleMillis, runIfTimedOut, task,
			JBossLogManagerReadiness::isReady, POLL_INTERVAL_MILLIS, MAX_WAIT_MILLIS);
	}

	static void runWhenReady(final String description, final long settleMillis, final boolean runIfTimedOut,
			final Runnable task, final BooleanSupplier readyCheck, final long pollIntervalMillis, final long maxWaitMillis) {
		Thread thread = new Thread("Perfmon4j.JBossLogManagerReadiness") {
			@Override
			public void run() {
				boolean ready = awaitReady(readyCheck, pollIntervalMillis, maxWaitMillis);
				if (ready) {
					if (settleMillis > 0) {
						try {
							Thread.sleep(settleMillis);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
				} else {
					System.err.println("Perfmon4j: " + JBOSS_LOG_MANAGER + " did not become loadable within "
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
