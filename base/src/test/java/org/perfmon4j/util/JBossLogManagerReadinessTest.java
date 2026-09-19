package org.perfmon4j.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

public class JBossLogManagerReadinessTest extends TestCase {
	private String originalManagerProperty;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		originalManagerProperty = System.getProperty(JBossLogManagerReadiness.MANAGER_PROPERTY);
	}

	@Override
	protected void tearDown() throws Exception {
		if (originalManagerProperty == null) {
			System.clearProperty(JBossLogManagerReadiness.MANAGER_PROPERTY);
		} else {
			System.setProperty(JBossLogManagerReadiness.MANAGER_PROPERTY, originalManagerProperty);
		}
		super.tearDown();
	}

	// Held outside InitializationProbe: reading a field of the probe would itself initialize it.
	static final AtomicBoolean probeInitialized = new AtomicBoolean(false);

	public static class InitializationProbe {
		static {
			probeInitialized.set(true);
		}
	}

	public void testIsClassLoadableTrueForPresentClass() {
		assertTrue(JBossLogManagerReadiness.isClassLoadable("java.lang.String"));
	}

	public void testIsClassLoadableFalseForMissingClass() {
		assertFalse(JBossLogManagerReadiness.isClassLoadable("org.perfmon4j.DoesNotExist"));
	}

	public void testIsClassLoadableDoesNotInitializeTheClass() {
		String probeName = InitializationProbe.class.getName(); // A class literal does not initialize the class.
		assertFalse(probeInitialized.get());
		assertTrue(JBossLogManagerReadiness.isClassLoadable(probeName));
		assertFalse("Checking loadability must not run the class's static initializer", probeInitialized.get());
	}

	public void testIsJBossLogManagerRequested() {
		System.clearProperty(JBossLogManagerReadiness.MANAGER_PROPERTY);
		assertFalse(JBossLogManagerReadiness.isJBossLogManagerRequested());

		System.setProperty(JBossLogManagerReadiness.MANAGER_PROPERTY, "some.other.LogManager");
		assertFalse(JBossLogManagerReadiness.isJBossLogManagerRequested());

		System.setProperty(JBossLogManagerReadiness.MANAGER_PROPERTY, JBossLogManagerReadiness.JBOSS_LOG_MANAGER);
		assertTrue(JBossLogManagerReadiness.isJBossLogManagerRequested());
	}

	public void testNotReadyWhenPropertyNotSetEvenIfClassIsLoadable() {
		System.clearProperty(JBossLogManagerReadiness.MANAGER_PROPERTY);
		assertFalse(JBossLogManagerReadiness.isReady());
	}

	public void testNotReadyWhenPropertySetButClassNotYetLoadable() {
		if (JBossLogManagerReadiness.isJBossLogManagerLoadable()) {
			return; // jboss-logmanager happens to be on the test classpath; cannot simulate the race.
		}
		// This is the race that bit perfmon4j: jboss-modules has set the property
		// but the class is not yet resolvable via the system classloader.
		System.setProperty(JBossLogManagerReadiness.MANAGER_PROPERTY, JBossLogManagerReadiness.JBOSS_LOG_MANAGER);
		assertTrue(JBossLogManagerReadiness.isJBossLogManagerRequested());
		assertFalse(JBossLogManagerReadiness.isReady());
	}

	public void testAwaitReadyReturnsTrueOnceReady() {
		final AtomicInteger calls = new AtomicInteger();
		boolean ready = JBossLogManagerReadiness.awaitReady(() -> calls.incrementAndGet() >= 3, 10, 5000);
		assertTrue(ready);
		assertEquals(3, calls.get());
	}

	public void testAwaitReadyTimesOut() {
		long start = System.currentTimeMillis();
		assertFalse(JBossLogManagerReadiness.awaitReady(() -> false, 10, 200));
		assertTrue("Should have waited for the timeout", System.currentTimeMillis() - start >= 150);
	}

	public void testRunWhenReadyRunsTaskAfterReadinessOnAnotherThread() throws Exception {
		final CountDownLatch ran = new CountDownLatch(1);
		final AtomicInteger checks = new AtomicInteger();
		final Thread[] runner = new Thread[1];

		JBossLogManagerReadiness.runWhenReady("test task", 0, false, () -> {
			runner[0] = Thread.currentThread();
			ran.countDown();
		}, () -> checks.incrementAndGet() >= 3, 10, 5000);

		assertTrue("Task should run once ready", ran.await(5, TimeUnit.SECONDS));
		assertTrue(checks.get() >= 3);
		assertNotSame("Must not run on the caller's thread (would block premain)", Thread.currentThread(), runner[0]);
		assertTrue("Should be a daemon thread so it can never hold the JVM open", runner[0].isDaemon());
	}

	public void testRunWhenReadyRunsTaskOnTimeoutWhenRequested() throws Exception {
		final CountDownLatch ran = new CountDownLatch(1);
		JBossLogManagerReadiness.runWhenReady("test task", 0, true, () -> ran.countDown(), () -> false, 10, 100);
		assertTrue("Task should run after timeout when runIfTimedOut is true", ran.await(5, TimeUnit.SECONDS));
	}

	public void testRunWhenReadySkipsTaskOnTimeoutWhenNotRequested() throws Exception {
		final CountDownLatch ran = new CountDownLatch(1);
		JBossLogManagerReadiness.runWhenReady("test task", 0, false, () -> ran.countDown(), () -> false, 10, 100);
		assertFalse("Task must not run after timeout when runIfTimedOut is false", ran.await(600, TimeUnit.MILLISECONDS));
	}

	public void testRunWhenReadySwallowsTaskFailure() throws Exception {
		// A failing task must not surface an exception on the caller or kill anything else.
		final CountDownLatch ran = new CountDownLatch(1);
		JBossLogManagerReadiness.runWhenReady("failing task", 0, false, () -> {
			ran.countDown();
			throw new RuntimeException("Expected failure from test");
		}, () -> true, 10, 1000);
		assertTrue(ran.await(5, TimeUnit.SECONDS));
	}
}
