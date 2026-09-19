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
		JBossLogManagerReadiness.setInstrumentation(null);
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

	public void testFindClassByName() {
		Class<?>[] loaded = new Class<?>[] {String.class, Integer.class, InitializationProbe.class};

		assertSame(Integer.class, JBossLogManagerReadiness.findClassByName(loaded, "java.lang.Integer"));
		assertSame(InitializationProbe.class, JBossLogManagerReadiness.findClassByName(loaded, InitializationProbe.class.getName()));
		assertNull(JBossLogManagerReadiness.findClassByName(loaded, JBossLogManagerReadiness.JBOSS_LOG_MANAGER));
		assertNull(JBossLogManagerReadiness.findClassByName(new Class<?>[0], "java.lang.String"));
	}

	public void testFindLoadedJBossLogManagerIsNullWithoutInstrumentation() {
		JBossLogManagerReadiness.setInstrumentation(null);
		assertNull(JBossLogManagerReadiness.findLoadedJBossLogManager());
	}

	public void testIsJBossLogManagerRequested() {
		System.clearProperty(JBossLogManagerReadiness.MANAGER_PROPERTY);
		assertFalse(JBossLogManagerReadiness.isJBossLogManagerRequested());

		System.setProperty(JBossLogManagerReadiness.MANAGER_PROPERTY, "some.other.LogManager");
		assertFalse(JBossLogManagerReadiness.isJBossLogManagerRequested());

		System.setProperty(JBossLogManagerReadiness.MANAGER_PROPERTY, JBossLogManagerReadiness.JBOSS_LOG_MANAGER);
		assertTrue(JBossLogManagerReadiness.isJBossLogManagerRequested());
	}

	public void testNotReadyWhenJBossLogManagerNotRequested() {
		System.clearProperty(JBossLogManagerReadiness.MANAGER_PROPERTY);
		assertFalse(JBossLogManagerReadiness.isReady());
	}

	public void testReadyImmediatelyWhenNotRunningAsAgent() {
		// Perfmon4j used as a plain library is loaded after the server has booted, so there is nothing to wait for.
		System.setProperty(JBossLogManagerReadiness.MANAGER_PROPERTY, JBossLogManagerReadiness.JBOSS_LOG_MANAGER);
		JBossLogManagerReadiness.setInstrumentation(null);
		assertTrue(JBossLogManagerReadiness.isReady());
	}

	public void testInitializeJavaUtilLoggingRestoresContextClassLoader() {
		Thread thread = Thread.currentThread();
		ClassLoader original = thread.getContextClassLoader();
		ClassLoader marker = new ClassLoader(original) {};
		thread.setContextClassLoader(marker);
		try {
			assertNotNull(JBossLogManagerReadiness.initializeJavaUtilLogging());
			assertSame("Context classloader must be restored", marker, thread.getContextClassLoader());
		} finally {
			thread.setContextClassLoader(original);
		}
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

	public void testRunWhenReadyRunsOnReadyThenTaskOnAnotherThread() throws Exception {
		final CountDownLatch ran = new CountDownLatch(1);
		final AtomicInteger checks = new AtomicInteger();
		final AtomicBoolean onReadyRanBeforeTask = new AtomicBoolean(false);
		final AtomicBoolean onReadyRan = new AtomicBoolean(false);
		final Thread[] runner = new Thread[1];

		JBossLogManagerReadiness.runWhenReady("test task", 0, false, () -> {
			onReadyRanBeforeTask.set(onReadyRan.get());
			runner[0] = Thread.currentThread();
			ran.countDown();
		}, () -> checks.incrementAndGet() >= 3, () -> onReadyRan.set(true), 10, 5000);

		assertTrue("Task should run once ready", ran.await(5, TimeUnit.SECONDS));
		assertTrue(checks.get() >= 3);
		assertTrue("The on-ready step must complete before the task starts", onReadyRanBeforeTask.get());
		assertNotSame("Must not run on the caller's thread (would block premain)", Thread.currentThread(), runner[0]);
		assertTrue("Should be a daemon thread so it can never hold the JVM open", runner[0].isDaemon());
	}

	public void testRunWhenReadyStillRunsTaskIfOnReadyFails() throws Exception {
		final CountDownLatch ran = new CountDownLatch(1);
		JBossLogManagerReadiness.runWhenReady("test task", 0, false, () -> ran.countDown(),
			() -> true, () -> { throw new RuntimeException("Expected failure from test"); }, 10, 1000);
		assertTrue("A failing on-ready step must not prevent the task", ran.await(5, TimeUnit.SECONDS));
	}

	public void testRunWhenReadyRunsTaskOnTimeoutWhenRequested() throws Exception {
		final CountDownLatch ran = new CountDownLatch(1);
		final AtomicBoolean onReadyRan = new AtomicBoolean(false);
		JBossLogManagerReadiness.runWhenReady("test task", 0, true, () -> ran.countDown(),
			() -> false, () -> onReadyRan.set(true), 10, 100);
		assertTrue("Task should run after timeout when runIfTimedOut is true", ran.await(5, TimeUnit.SECONDS));
		assertFalse("On-ready step is only for a confirmed-ready JBoss", onReadyRan.get());
	}

	public void testRunWhenReadySkipsTaskOnTimeoutWhenNotRequested() throws Exception {
		final CountDownLatch ran = new CountDownLatch(1);
		JBossLogManagerReadiness.runWhenReady("test task", 0, false, () -> ran.countDown(),
			() -> false, () -> {}, 10, 100);
		assertFalse("Task must not run after timeout when runIfTimedOut is false", ran.await(600, TimeUnit.MILLISECONDS));
	}

	public void testRunWhenReadySwallowsTaskFailure() throws Exception {
		// A failing task must not surface an exception on the caller or kill anything else.
		final CountDownLatch ran = new CountDownLatch(1);
		JBossLogManagerReadiness.runWhenReady("failing task", 0, false, () -> {
			ran.countDown();
			throw new RuntimeException("Expected failure from test");
		}, () -> true, () -> {}, 10, 1000);
		assertTrue(ran.await(5, TimeUnit.SECONDS));
	}
}
