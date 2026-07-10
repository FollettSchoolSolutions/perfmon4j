# Virtual Threads and Perfmon4j

Java virtual threads (Project Loom, first shipped as a preview in JDK 19 and finalized in JDK 21) change how blocking code scales. Because Perfmon4j is an instrumentation agent whose injected code runs **on application threads** — which, under Loom, may be virtual threads — it is fair to ask whether Perfmon4j's own locking and per-thread state interfere with virtual-thread scalability.

This page explains the two virtual-thread concerns that apply to a monitoring agent, documents where Perfmon4j stands against each (backed by specific source references), and shows how to **measure** the behavior in your own deployment rather than reasoning about it from the code alone.

> **TL;DR** — The core, non-reactive interval-timing hot path is already virtual-thread friendly: its main critical section uses `java.util.concurrent.locks.ReentrantLock`, which never pins a carrier thread. The largest remaining `synchronized` surface on application threads is the **reactive** context path, which funnels every reactive start/stop through a single JVM-wide lock. On **JDK 24+**, [JEP 491](https://openjdk.org/jeps/491) removes `synchronized` pinning entirely, so the pinning concern is scoped to JDK 21–23.

---

## Table of Contents

- [Two Distinct Concerns](#two-distinct-concerns)
  - [1. Pinning](#1-pinning)
  - [2. ThreadLocal Footprint](#2-threadlocal-footprint)
- [Why the JDK Version Matters](#why-the-jdk-version-matters)
- [How Perfmon4j's Hot Path Is Built](#how-perfmon4js-hot-path-is-built)
- [`synchronized` on Application Threads — Risk Map](#synchronized-on-application-threads--risk-map)
- [The Reactive Path Is the Main Surface](#the-reactive-path-is-the-main-surface)
- [Measured Results (JDK 21)](#measured-results-jdk-21)
  - [Why the Reactive Lock Was Not Converted to a ReentrantLock](#why-the-reactive-lock-was-not-converted-to-a-reentrantlock)
- [How to Diagnose Pinning in Your Deployment](#how-to-diagnose-pinning-in-your-deployment)
  - [JFR (recommended, JDK 21+)](#jfr-recommended-jdk-21)
  - [`-Djdk.tracePinnedThreads` (JDK 21–23)](#-djdktracepinnedthreads-jdk-2123)
  - [A Targeted Reproduction Test](#a-targeted-reproduction-test)
- [Current Status Summary](#current-status-summary)

---

## Two Distinct Concerns

It is important to keep two separate virtual-thread issues apart. They are often conflated under a general "synchronization is bad for virtual threads" warning, but they have different causes, different severities, and different fixes.

### 1. Pinning

When a virtual thread executes inside a `synchronized` block or method **and then blocks** (on I/O, `Object.wait`, `LockSupport.park`, etc.), the JDK (on versions before 24) cannot unmount that virtual thread from its underlying carrier platform thread. The carrier is *pinned* for the duration of the block. If enough carriers pin simultaneously, the small carrier pool starves and virtual-thread throughput collapses.

The critical nuance: **a short `synchronized` block that never blocks does not cause the harmful kind of pinning.** The damage occurs specifically when `synchronized` wraps a *blocking* operation. A monitor held only long enough to mutate an in-memory data structure completes and releases almost instantly. This distinction is what separates a real problem from a theoretical one when auditing code.

### 2. ThreadLocal Footprint

Virtual threads work correctly with `ThreadLocal`, but they are designed to be created in very large numbers (potentially millions). Per-thread state that was cheap with a few hundred platform threads can become a memory-footprint concern at that scale. This is a *scaling* consideration, not a correctness bug, and it is entirely separate from pinning. Perfmon4j makes heavy use of `ThreadLocal` for its per-thread timer stacks, trace stacks, reference counts, and recursion guards.

---

## Why the JDK Version Matters

The single most important variable in this whole discussion is which JDK your applications run on:

| JDK | `synchronized` pinning behavior |
|-----|-------------------------------|
| 19–20 (preview) | Virtual threads pin inside `synchronized` when blocking. |
| 21–23 (LTS 21) | Same — pinning is a real, measurable scalability limiter. |
| **24+** | [JEP 491](https://openjdk.org/jeps/491) reworked the JVM so `synchronized` **no longer pins**. The concern effectively disappears. |

Practically, this means the decision is often *"which JDKs must we support?"* rather than *"must we rewrite the locking?"* If your deployments are all on JDK 24+, `synchronized`-based pinning is a non-issue regardless of how the agent is written. If you must support JDK 21, the risk is real and worth measuring.

---

## How Perfmon4j's Hot Path Is Built

The per-invocation path — the code an instrumented method runs on entry and exit, on the caller's (possibly virtual) thread — was deliberately built on `java.util.concurrent.locks`, which are **Loom-friendly and never pin**:

- The primary per-start/stop critical section in `PerfMon.start` / `PerfMon.stop` is guarded by a per-monitor `ReentrantLock` (`startStopWriteLock`), not a `synchronized` block. See `base/src/main/java/org/perfmon4j/PerfMon.java` (lock definition and the `start`/`stop` acquisitions).
- Monitor-map lookups in `getMonitor(...)` use a `ReentrantReadWriteLock`.
- `PerfMonTimer` — the direct entry point instrumented code calls — contains **no** `synchronized` at all. It relies on `AtomicLong` counters and a `ThreadLocal`-based recursion guard.

Under contention a `ReentrantLock` will *park* the caller, but parking on a `j.u.c.` lock lets a virtual thread unmount cleanly — it does not pin. So the main hot-path critical section is already correct for virtual threads even on JDK 21.

---

## `synchronized` on Application Threads — Risk Map

The `synchronized` that *does* remain on the application-thread path, ordered by concern:

| Location | When it runs | Blocks inside the monitor? | Pinning risk (JDK ≤ 23) |
|----------|--------------|----------------------------|--------------------------|
| `ReactiveContextManager` — static `bindToken` monitor | Every **reactive** timer start/stop | Only a `logger.logDebug(...)` call, and only when debug logging is enabled | **Highest** — a single JVM-wide lock; a blocking logger inside it could both pin *and* serialize all reactive timing across the JVM |
| `MonitorThreadTracker.addTracker` / `removeTracker` | Every start/stop (unless disabled) | No — pure in-memory doubly-linked-list pointer updates | Low — completes instantly; disable with `-Dorg.perfmon4j.MonitorThreadTracker.DisableThreadTracking=true` |
| `Appender.appendData` | Thread-trace **stop** only | No — only enqueues onto a bounded in-memory queue; the blocking `outputData(...)` runs **outside** the lock | Low |
| `ThreadLocal.initialValue()` methods (several classes) | Once per thread, on first touch | No — just allocates a small object | Very low |

`Collections.synchronizedMap` / `synchronizedList` / `synchronizedSet` wrappers exist on `PerfMon` (for example the `forceDynamicPathWeakMap`, `appenderList`, and child-monitor collections) but sit on **cold / administrative** paths (config load, monitor registration, dynamic-child creation) — not on the per-start/stop path.

---

## The Reactive Path Is the Main Surface

If a virtual-thread hardening effort is undertaken, the highest-value target is the reactive context path in `base/src/main/java/org/perfmon4j/reactive/`. Every reactive timer start and stop passes through a **single `static` `bindToken` monitor** in `ReactiveContextManager` (`getPayload` on start, `deletePayload` on stop, plus the context-move operations). Two things make this worth scrutinizing:

1. **Pinning** — the monitor contains `logger.logDebug(...)` / `isDebugEnabled()` calls. If the configured logger performs blocking I/O and debug logging is on, a virtual thread would pin here.
2. **Contention** — independent of Loom, a single JVM-wide lock on every reactive start/stop is a serialization point. This is worth examining for throughput even before considering virtual threads.

The nested `ReactiveContext` locks (`mutableMemberDataLockToken`, `activeThreadLockToken`, and the reactive thread-trace `tracesLockToken`) are per-context rather than global, and guard only in-memory collection operations, so they are a smaller concern than the global `bindToken`.

---

## Measured Results (JDK 21)

The analysis above was validated empirically with the
[`VirtualThreadStressTester`](https://github.com/FollettSchoolSolutions/perfmon4j/blob/develop/stress-test/VirtualThreadStressTester.java)
harness (see the `stress-test/` folder) on JDK 21 (Temurin 21.0.4, 8 cores), driving **2,000
concurrent virtual threads** through `PerfMonTimer.start`/`stop` while a JFR listener counted
`jdk.VirtualThreadPinned` events:

| scenario | throughput | start latency | pins |
|----------|-----------:|--------------:|-----:|
| virtual / non-reactive | ~350k ops/s | ~6 µs | **0** |
| virtual / reactive | ~283k ops/s | ~12 µs | **0** |
| virtual / PIN CONTROL (`synchronized` + blocking sleep) | ~350 ops/s | — | **~2,900** |

Two conclusions:

1. **Perfmon4j does not pin virtual threads.** Both timer paths recorded zero pinning events,
   while the deliberately-pinning control scenario recorded thousands — which proves the
   detector works and the zeros are real. The reactive path's `synchronized` critical sections
   are short and never block, so they never pin.
2. **The reactive path's ~20% overhead is contention, not pinning** — every start/stop funnels
   through the single global `bindToken` monitor.

### Why the Reactive Lock Was Not Converted to a ReentrantLock

The obvious "make it Loom-safe" change is to replace `synchronized(bindToken)` with a
`java.util.concurrent` `ReentrantLock` (which never pins). Measured, this made things **~2×
worse**:

| reactive path | throughput | start latency |
|---------------|-----------:|--------------:|
| `synchronized` (current) | ~283k ops/s | ~12–20 µs |
| `ReentrantLock` | ~125k ops/s | ~4,500 µs |

Under heavy contention a `ReentrantLock` *parks* (unmounts) the losing virtual thread, and the
scheduler must later remount it — enormous churn with 2,000 threads queued on one lock. A short
`synchronized` section instead resolves via brief native spinning **without** unmounting, and
never pins because nothing blocks while it is held. **The bottleneck is contention on a single
global lock, not the lock's type** — so the real optimization is to reduce that contention
(per-context locking, a concurrent registry map, a fast-path owner check), which would benefit
either lock type. Until then, `synchronized` is retained deliberately (see the comment on
`bindToken` in `ReactiveContextManager`).

> This contention-reduction work is tracked as a future enhancement in
> [issue #59](https://github.com/FollettSchoolSolutions/perfmon4j/issues/59). It is a
> performance enhancement only — not required for virtual-thread correctness (the path
> already records zero pinning events).

---

## How to Diagnose Pinning in Your Deployment

Reading the code tells you *where* to look; only measurement tells you whether it actually pins under your workload. Prefer empirical evidence.

### JFR (recommended, JDK 21+)

The JVM emits a `jdk.VirtualThreadPinned` event whenever a virtual thread pins during a blocking operation. Record it and inspect the stack traces:

```
java -XX:StartFlightRecording=filename=pin.jfr,settings=profile ...your app...
jfr print --events jdk.VirtualThreadPinned pin.jfr
```

Any Perfmon4j frame appearing in those stacks is a confirmed, real pin. **Zero events means no pinning in practice**, no matter how many `synchronized` blocks exist in the code.

### `-Djdk.tracePinnedThreads` (JDK 21–23)

A quicker first pass that prints a stack trace to standard output on each pin:

```
java -Djdk.tracePinnedThreads=full ...your app...
```

This flag is noisier than JFR and was **removed in JDK 24** (where pinning no longer occurs), so prefer JFR for anything durable.

### A Targeted Reproduction Test

To force the issue rather than wait for it, run an instrumented method on many virtual threads that also perform a blocking call inside the timed region, and compare throughput and `jdk.VirtualThreadPinned` counts across configurations — for example tracking on vs. off, and reactive vs. non-reactive timing. Run the same test on **JDK 21 and JDK 24+**: if pinning appears on 21 but vanishes on 24, that confirms it is the classic `synchronized` issue that JEP 491 already resolves.

---

## Current Status Summary

- **Measured: zero pinning.** Under 2,000 virtual threads on JDK 21, both the non-reactive and reactive timer paths produced **0** `jdk.VirtualThreadPinned` events (see [Measured Results](#measured-results-jdk-21)).
- **Non-reactive interval timing** — Already virtual-thread friendly. The main critical section uses `ReentrantLock`; the only per-invocation `synchronized` is the short, non-blocking, disableable `MonitorThreadTracker`.
- **Reactive timing** — The largest `synchronized` surface on application threads, routed through a single global lock. Its ~20% overhead is *contention*, not pinning; the fix is contention reduction, **not** a lock-type swap (a `ReentrantLock` measured ~2× slower here). The primary future optimization target, tracked in [issue #59](https://github.com/FollettSchoolSolutions/perfmon4j/issues/59).
- **ThreadLocal usage** — Pervasive and correct, but a footprint consideration at very high virtual-thread counts.
- **JDK 24+** — `synchronized` pinning is eliminated by JEP 491; the concern is scoped to JDK 21–23.
- **Build target** — Perfmon4j currently compiles for Java 11 and has no virtual-thread–specific code paths.

---

> See also: [Configuring the Java Agent](Configuring-the-Java-Agent) and [Configuration Properties & Conditional Activation](Configuration-Properties-and-Conditional-Activation).
