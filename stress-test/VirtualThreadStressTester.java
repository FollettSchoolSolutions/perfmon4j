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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 */

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingStream;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.TextAppender;

/**
 * VirtualThreadStressTester
 * =========================
 *
 * A JDK 21+ diagnostic that drives Perfmon4j's timer start/stop path from a large
 * number of virtual (and, for comparison, platform) threads while a JFR listener
 * counts {@code jdk.VirtualThreadPinned} events. It answers two questions the
 * static code review of Perfmon4j's synchronization could only reason about:
 *
 *   1. PINNING  -- Does calling PerfMonTimer.start/stop cause a virtual thread to
 *                  pin its carrier? (It should not: the hot path uses ReentrantLock,
 *                  and no synchronized block in the path blocks while held.)
 *   2. CONTENTION -- How does the REACTIVE timer path (which funnels every start/stop
 *                  through a single JVM-wide bindToken monitor in ReactiveContextManager)
 *                  compare to the NON-REACTIVE path under heavy virtual-thread load?
 *                  This is the baseline to optimize against.
 *
 * A deliberately-pinning control scenario (synchronized + sleep) is included to prove
 * the JFR detector is working -- it MUST report pins, or the "0 pins" results elsewhere
 * are not trustworthy.
 *
 * This file is intentionally NOT part of the Maven build (the project compiles to
 * Java 11). Run it with the single-file source launcher against a built perfmon4j jar:
 *
 *   mvn -q -pl base -am -DskipTests install
 *   java -cp base/target/perfmon4j-<version>.jar stress-test/VirtualThreadStressTester.java
 *
 * See stress-test/README.md for full instructions, including capturing a JFR file and
 * running the same test on JDK 24+ (where JEP 491 removes synchronized pinning).
 */
public class VirtualThreadStressTester {

    // ---- Tunables (overridable via -D system properties) -------------------
    private static final int    SECONDS_PER_SCENARIO = intProp("p4j.stress.seconds", 5);
    private static final int    WARMUP_SECONDS       = intProp("p4j.stress.warmupSeconds", 3);
    private static final int    VIRTUAL_CONCURRENCY  = intProp("p4j.stress.vthreads", 2_000);
    private static final int    PLATFORM_CONCURRENCY = intProp("p4j.stress.pthreads", 200);
    private static final int    BLOCK_MILLIS         = intProp("p4j.stress.blockMillis", 2);
    private static final long   SETTLE_MILLIS        = 1_500; // let JFR flush pinned events between scenarios

    enum ThreadKind { PLATFORM, VIRTUAL }
    enum TimerMode  { NON_REACTIVE, REACTIVE, PIN_CONTROL }

    public static void main(String[] args) throws Exception {
        banner();

        configurePerfmon();
        PerfMon monitor = PerfMon.getMonitor("WebRequest");

        // --- JFR pinning detector ------------------------------------------
        LongAdder pinCount = new LongAdder();
        Map<String, LongAdder> pinCulprits = new TreeMap<>();
        RecordingStream rs = new RecordingStream();
        rs.enable("jdk.VirtualThreadPinned").withStackTrace().withThreshold(Duration.ofMillis(0));
        rs.onEvent("jdk.VirtualThreadPinned", e -> {
            pinCount.increment();
            String culprit = topApplicationFrame(e.getStackTrace());
            synchronized (pinCulprits) {
                pinCulprits.computeIfAbsent(culprit, k -> new LongAdder()).increment();
            }
        });
        rs.startAsync();

        // Warm up the JIT for the timer path so measured latencies are representative
        // rather than dominated by first-touch compilation.
        System.out.println("warming up (" + WARMUP_SECONDS + "s x 2)...");
        runScenario(new Scenario("warmup / non-reactive ", ThreadKind.VIRTUAL, TimerMode.NON_REACTIVE, VIRTUAL_CONCURRENCY), monitor, WARMUP_SECONDS);
        runScenario(new Scenario("warmup / reactive     ", ThreadKind.VIRTUAL, TimerMode.REACTIVE, VIRTUAL_CONCURRENCY), monitor, WARMUP_SECONDS);
        System.out.println();

        List<Scenario> scenarios = List.of(
            new Scenario("virtual  / non-reactive", ThreadKind.VIRTUAL,  TimerMode.NON_REACTIVE, VIRTUAL_CONCURRENCY),
            new Scenario("virtual  / reactive    ", ThreadKind.VIRTUAL,  TimerMode.REACTIVE,     VIRTUAL_CONCURRENCY),
            new Scenario("platform / non-reactive", ThreadKind.PLATFORM, TimerMode.NON_REACTIVE, PLATFORM_CONCURRENCY),
            new Scenario("platform / reactive    ", ThreadKind.PLATFORM, TimerMode.REACTIVE,     PLATFORM_CONCURRENCY),
            new Scenario("virtual  / PIN CONTROL ", ThreadKind.VIRTUAL,  TimerMode.PIN_CONTROL,  VIRTUAL_CONCURRENCY)
        );

        List<Result> results = new ArrayList<>();
        for (Scenario s : scenarios) {
            long pinsBefore = pinCount.sum();
            Result r = runScenario(s, monitor, SECONDS_PER_SCENARIO);
            Thread.sleep(SETTLE_MILLIS); // allow JFR to flush events attributable to this scenario
            r.pins = pinCount.sum() - pinsBefore;
            results.add(r);
            System.out.printf("  done: %s -> %,d ops, %d pins%n", s.name, r.ops, r.pins);
        }

        rs.close();

        printResults(results);
        printPinCulprits(pinCount.sum(), pinCulprits);
    }

    // ------------------------------------------------------------------------
    private static Result runScenario(Scenario s, PerfMon monitor, int seconds) throws InterruptedException {
        AtomicBoolean stop = new AtomicBoolean(false);
        LongAdder ops = new LongAdder();
        LongAdder startNanos = new LongAdder();
        LongAdder startCnt = new LongAdder();
        LongAdder stopNanos = new LongAdder();
        LongAdder stopCnt = new LongAdder();
        Object pinLock = new Object(); // used only by PIN_CONTROL

        CountDownLatch ready = new CountDownLatch(s.concurrency);
        CountDownLatch go = new CountDownLatch(1);

        Runnable worker = () -> {
            ready.countDown();
            awaitQuietly(go);
            while (!stop.get()) {
                if (s.mode == TimerMode.PIN_CONTROL) {
                    // Deliberately pin: block while holding a monitor. Proves the detector works.
                    synchronized (pinLock) {
                        sleep(BLOCK_MILLIS);
                    }
                    ops.increment();
                    continue;
                }
                long t0 = System.nanoTime();
                PerfMonTimer timer = (s.mode == TimerMode.REACTIVE)
                        ? PerfMonTimer.startReactive(monitor)
                        : PerfMonTimer.start(monitor);
                startNanos.add(System.nanoTime() - t0);
                startCnt.increment();
                try {
                    sleep(BLOCK_MILLIS); // simulate the application's own blocking work (I/O, etc.)
                } finally {
                    long t1 = System.nanoTime();
                    PerfMonTimer.stop(timer);
                    stopNanos.add(System.nanoTime() - t1);
                    stopCnt.increment();
                }
                ops.increment();
            }
        };

        ThreadFactory tf = (s.kind == ThreadKind.VIRTUAL)
                ? Thread.ofVirtual().name("vt-", 0).factory()
                : Thread.ofPlatform().daemon(true).name("pt-", 0).factory();

        List<Thread> threads = new ArrayList<>(s.concurrency);
        for (int i = 0; i < s.concurrency; i++) {
            threads.add(tf.newThread(worker));
        }
        threads.forEach(Thread::start);

        ready.await();          // all workers created and parked at the gate
        long wall0 = System.nanoTime();
        go.countDown();         // release them together
        Thread.sleep(seconds * 1000L);
        stop.set(true);
        for (Thread t : threads) {
            t.join();
        }
        long wallNanos = System.nanoTime() - wall0;

        Result r = new Result(s);
        r.ops = ops.sum();
        r.wallSeconds = wallNanos / 1_000_000_000.0;
        r.avgStartMicros = micros(startNanos, startCnt);
        r.avgStopMicros = micros(stopNanos, stopCnt);
        return r;
    }

    // ------------------------------------------------------------------------
    private static void configurePerfmon() throws Exception {
        // Populate the monitor map so lookups look like a warmed-up production system.
        for (int i = 0; i < 10_000; i++) {
            PerfMon.getMonitor("Filler.Monitor" + i);
        }
        PerfMonConfiguration config = new PerfMonConfiguration();
        // Long interval: keep the timer "active" (locks exercised) without flooding stdout.
        config.defineAppender("Appender", TextAppender.class.getName(), "1 hour");
        config.defineMonitor("WebRequest");
        config.attachAppenderToMonitor("WebRequest", "Appender", ".");
        PerfMon.configure(config);
    }

    private static String topApplicationFrame(RecordedStackTrace st) {
        if (st == null) {
            return "(no stack)";
        }
        String firstPerfmon = null;
        for (RecordedFrame f : st.getFrames()) {
            if (!f.isJavaFrame() || f.getMethod() == null) {
                continue;
            }
            String type = f.getMethod().getType().getName();
            String frame = type + "." + f.getMethod().getName();
            if (type.startsWith("org.perfmon4j")) {
                return "perfmon4j: " + frame; // most specific perfmon4j frame wins
            }
            if (firstPerfmon == null && !type.startsWith("java.") && !type.startsWith("jdk.")) {
                firstPerfmon = frame;
            }
        }
        return firstPerfmon != null ? firstPerfmon : "(non-perfmon4j)";
    }

    // ------------------------------------------------------------------------
    private static void printResults(List<Result> results) {
        System.out.println();
        System.out.println("==================================================================================");
        System.out.println(" RESULTS  (" + SECONDS_PER_SCENARIO + "s/scenario, block=" + BLOCK_MILLIS
                + "ms, vthreads=" + VIRTUAL_CONCURRENCY + ", pthreads=" + PLATFORM_CONCURRENCY + ")");
        System.out.println("==================================================================================");
        System.out.printf("%-24s %14s %12s %12s %8s%n",
                "scenario", "ops/sec", "start(us)", "stop(us)", "pins");
        System.out.println("----------------------------------------------------------------------------------");
        for (Result r : results) {
            double opsPerSec = r.ops / r.wallSeconds;
            System.out.printf("%-24s %14s %12s %12s %8d%n",
                    r.scenario.name,
                    String.format("%,.0f", opsPerSec),
                    r.scenario.mode == TimerMode.PIN_CONTROL ? "-" : String.format("%.2f", r.avgStartMicros),
                    r.scenario.mode == TimerMode.PIN_CONTROL ? "-" : String.format("%.2f", r.avgStopMicros),
                    r.pins);
        }
        System.out.println("==================================================================================");
    }

    private static void printPinCulprits(long totalPins, Map<String, LongAdder> culprits) {
        System.out.println();
        System.out.println("Pinning events by top application/perfmon4j frame (total " + totalPins + "):");
        if (culprits.isEmpty()) {
            System.out.println("  (none)");
        } else {
            synchronized (culprits) {
                culprits.forEach((k, v) -> System.out.printf("  %,10d  %s%n", v.sum(), k));
            }
        }
        System.out.println();
        System.out.println("Interpretation:");
        System.out.println("  * PIN CONTROL should report pins -- that confirms the detector works.");
        System.out.println("  * Any 'perfmon4j:' culprit line is a real pin caused by Perfmon4j code.");
        System.out.println("  * On JDK 24+ (JEP 491) even the PIN CONTROL should report 0 pins.");
    }

    // ---- helpers -----------------------------------------------------------
    private static double micros(LongAdder totalNanos, LongAdder count) {
        long c = count.sum();
        return c == 0 ? 0.0 : (totalNanos.sum() / (double) c) / 1000.0;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static int intProp(String name, int dflt) {
        String v = System.getProperty(name);
        return v == null ? dflt : Integer.parseInt(v.trim());
    }

    private static void banner() {
        System.out.println("Perfmon4j VirtualThreadStressTester");
        System.out.println("  java.version = " + System.getProperty("java.version"));
        System.out.println("  processors   = " + Runtime.getRuntime().availableProcessors());
        System.out.println();
    }

    // ---- value holders -----------------------------------------------------
    private static final class Scenario {
        final String name;
        final ThreadKind kind;
        final TimerMode mode;
        final int concurrency;
        Scenario(String name, ThreadKind kind, TimerMode mode, int concurrency) {
            this.name = name;
            this.kind = kind;
            this.mode = mode;
            this.concurrency = concurrency;
        }
    }

    private static final class Result {
        final Scenario scenario;
        long ops;
        double wallSeconds;
        double avgStartMicros;
        double avgStopMicros;
        long pins;
        Result(Scenario scenario) {
            this.scenario = scenario;
        }
    }
}
