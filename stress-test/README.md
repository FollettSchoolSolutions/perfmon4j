# Perfmon4j Stress Testers

Standalone diagnostics for exercising Perfmon4j's timer path under load. These are
**not** part of the Maven build — the project compiles to Java 11, whereas these tools
use JDK 21+ APIs (virtual threads, JFR virtual-thread events). They are run directly
with the [single-file source-code launcher](https://openjdk.org/jeps/330) against a
built `perfmon4j` jar.

| File | Purpose |
|------|---------|
| `VirtualThreadStressTester.java` | Drive `PerfMonTimer.start/stop` from many virtual (and platform) threads while a JFR listener counts `jdk.VirtualThreadPinned` events. Answers "does Perfmon4j pin virtual threads?" and benchmarks the reactive vs. non-reactive path. |

See also the in-repo library benchmark `base/src/main/java/org/perfmon4j/demo/DynamicStressTester.java`
(platform threads, timer-overhead only) and the wiki page
[Virtual Threads and Perfmon4j](../wiki/Virtual-Threads-and-Perfmon4j.md).

---

## Requirements

- **JDK 21 or newer.** Virtual threads and the `jdk.VirtualThreadPinned` JFR event are JDK 21+.
- A built `perfmon4j` jar on the classpath.

## Build the jar

From the repo root:

```
mvn -q -pl base -am -DskipTests install
```

This produces `base/target/perfmon4j-<version>.jar` (currently `2.2.2-SNAPSHOT`).

## Run

```
java -cp base/target/perfmon4j-2.2.2-SNAPSHOT.jar stress-test/VirtualThreadStressTester.java
```

The program runs a warmup, then five scenarios, and prints a results table plus a
breakdown of any pinning events by the code that caused them.

### Options (system properties)

| Property | Default | Meaning |
|----------|---------|---------|
| `-Dp4j.stress.seconds` | `5` | Measured seconds per scenario. |
| `-Dp4j.stress.warmupSeconds` | `3` | Warmup seconds (x2, discarded). |
| `-Dp4j.stress.vthreads` | `2000` | Concurrent virtual threads. |
| `-Dp4j.stress.pthreads` | `200` | Concurrent platform threads. |
| `-Dp4j.stress.blockMillis` | `2` | Simulated application work inside each timed region. |

Example — a longer, heavier run:

```
java -Dp4j.stress.seconds=15 -Dp4j.stress.vthreads=10000 \
     -cp base/target/perfmon4j-2.2.2-SNAPSHOT.jar \
     stress-test/VirtualThreadStressTester.java
```

---

## Interpreting the output

```
scenario                        ops/sec    start(us)     stop(us)     pins
----------------------------------------------------------------------------------
virtual  / non-reactive         357,248         8.79        10.67        0
virtual  / reactive             281,835        20.60        42.30        0
platform / non-reactive          69,614        23.28        32.22        0
platform / reactive              63,729       108.87       195.61        0
virtual  / PIN CONTROL              354            -            -     2950
```

- **`pins` column** — the count of `jdk.VirtualThreadPinned` events during that scenario.
  For the real Perfmon4j scenarios this should be **0**. The final **PIN CONTROL**
  scenario deliberately pins (a `synchronized` block wrapping a sleep); it **must** report
  a non-zero count, which is what proves the detector is actually working. If PIN CONTROL
  shows 0 pins on a JDK ≤ 23, something is wrong with the measurement.
- **`start(us)` / `stop(us)`** — average wall-clock time inside `PerfMonTimer.start` /
  `stop`. The gap between the non-reactive and reactive rows is the cost of the reactive
  context path (which serializes every start/stop through a single JVM-wide lock in
  `ReactiveContextManager`).
- **Pinning culprit breakdown** — every pinning event is attributed to its most specific
  application frame. Any line prefixed `perfmon4j:` is a real pin caused by Perfmon4j code
  and should be investigated. On a clean run the only culprit is
  `VirtualThreadStressTester.sleep` (the PIN CONTROL).

## Capturing a JFR file for deeper analysis

The tester detects pins in-process, but you can also record a full flight-recording and
inspect it with `jfr`:

```
java -XX:StartFlightRecording=filename=pin.jfr,settings=profile \
     -cp base/target/perfmon4j-2.2.2-SNAPSHOT.jar \
     stress-test/VirtualThreadStressTester.java

jfr print --events jdk.VirtualThreadPinned pin.jfr
```

On JDK 21–23 you can alternatively use `-Djdk.tracePinnedThreads=full` for a quick
stdout dump on each pin (removed in JDK 24).

## Running on JDK 24+

[JEP 491](https://openjdk.org/jeps/491) (JDK 24) removed `synchronized` pinning. Re-running
the tester on JDK 24+ should show **0 pins even for the PIN CONTROL scenario** — a useful
way to confirm that the pinning concern is scoped to JDK 21–23.
