# Plan: Expose MBean/JMX SnapShots through ExternalAppender (VisualVM / Hawtio visibility)

Status: **NOT STARTED** — deferred follow-up to the `feature/POJOSnapShotExternalAppender`
epic (merged to `develop` 2026-07-18). This document is the pick-up point.

## Context

The POJOSnapShotExternalAppender epic made POJO-style snapshots (live objects
registered with `POJOSnapShotRegistry`) visible to remote-management clients
(VisualVM plugin, Hawtio plugin, RemoteManagement MBean) through
`ExternalAppender`. MBean-backed registry entries — created by
`MBeanSnapShotManager` from `<snapShotMonitor>` JMX query configuration — were
**deliberately excluded** from that epic and remain invisible to remote clients.

The exclusion marker is `POJOSnapShotRegistry.POJORegistryEntry.getPOJOClass()`
returning `null`: `buildRegistryEntry` passes a null `pojoClass` for
`MBeanInstance` items, and every new ExternalAppender code path skips entries
with a null POJO class.

**Goal:** MBean snapshot instances appear in / disappear from the remote
monitor list as their queries match/unmatch live MBeans, with field enumeration
and subscribe/sample support — zero client-side changes, same as the POJO epic.

## How MBean registry entries differ from POJO entries (verified in source)

| Aspect | POJO entry | MBean entry |
|---|---|---|
| Registry key (`className`) | real class name (`item.getClass().getName()`) | **effective class name** `org.perfmon4j.util.mbean.MBeanInstance:<querySignature>` (via `OverrideClassNameForWrappedObject.getEffectiveClassName()` → `MBeanInstance.buildEffectiveClassName(query)`) — not a loadable class |
| Field source | `@SnapShotCounter/Gauge/String/Ratio` annotations on the class | `MBeanInstance.getDatumDefinition()` → `DatumDefinition[]` (name, `OutputType` COUNTER/GAUGE/STRING/RATIO, `AttributeType`); **per-instance**, can throw `MBeanQueryException` |
| Bundle | generated data class via `generateBundleForPOJO` | `new Bundle(MBeanInstanceData.class, null, false)` |
| `getFieldData(FieldKey[])` on the data class | works (generated) | **unsupported** — `MBeanInstanceData.getFieldData` logs a warning and returns an empty map (`MBeanInstanceData.java:100`) |
| Registration lifecycle | app code registers/deregisters | `MBeanSnapShotManager` registers **weakly** on a 60s refresh timer (`RELOAD_SECONDS`) as MBeans matching the query appear/disappear |
| Display name | class name | `MBeanInstance.getName()` (friendly name from the query) + `getInstanceName()` |

## Work items

### 1. Field enumeration from DatumDefinition (the core new piece)
Annotation scanning (`generateExternalMonitorKeysForPOJO`) cannot work — there is
no annotated class. Add a parallel path (suggested: a static helper in
`ExternalAppender` or a new method near it) that builds `MonitorKeyWithFields`
from `MBeanInstance.getDatumDefinition()`:
- COUNTER → follow the POJO convention: `<name>PerSecond`, `FieldKey.DOUBLE_TYPE`
  (verify against what `MBeanDatum.toPerfMonObservableDatum(before, durationMillis)`
  actually emits — field naming must match what sampling returns, see item 2).
- GAUGE → LONG/INTEGER/DOUBLE per `DatumDefinition.getAttributeType()`.
- STRING → `FieldKey.STRING_TYPE`.
- RATIO → `FieldKey.DOUBLE_TYPE`.
- `getDatumDefinition()` throws `MBeanQueryException` — tolerate per-instance
  failure (skip that instance, log debug, keep enumerating others).
- Definitions are per-instance, and instances of one query can differ; since each
  `MonitorKeyWithFields` carries its own field set per key, emit one key per
  instance with that instance's fields (no union/merge needed).

### 2. Implement `MBeanInstanceData.getFieldData(FieldKey[])`
Currently returns an empty map. Implement by reusing the logic in
`getObservations()` (`MBeanInstanceData.java:127`) — it already computes
counter deltas (before/after + durationMillis) and gauge/string/ratio values as
`PerfMonObservableDatum`. Map each datum to the matching `FieldKey` by field
name + type. Field names produced here MUST match item 1's enumeration exactly,
or subscribed clients get empty columns.

### 3. Monitor-key naming decision (decide before coding)
The registry key is the ugly effective class name
(`org.perfmon4j.util.mbean.MBeanInstance:<querySignature>`), but clients display
the key name. Options:
- (a) Use `MBeanInstance.getName()` (the friendly query display name) as the
  MonitorKey name; keep a name→effective-class-name map when resolving
  subscribe/field lookups. Nicer UI, needs collision thought.
- (b) Use the effective class name verbatim; opaque but collision-free and
  round-trips through the existing key→registry lookup with no mapping.
Leaning (a) for UI quality — the VisualVM/Hawtio monitor tree shows this string.

### 4. ExternalAppender wiring
Extend the POJO paths added by the previous epic (all in
`base/src/main/java/org/perfmon4j/remotemanagement/ExternalAppender.java`):
- `getPOJOMonitorKeysWithFields()` — stop skipping `getPOJOClass() == null`
  entries; branch to the DatumDefinition-based enumeration for them.
- `getFieldsForSnapShotMonitor(MonitorKey)` — same branch.
- `MonitorMap.subscribe` — the existing `POJOSnapShotWrapper` may work nearly
  as-is: MBean entries DO have a Bundle (`MBeanInstanceData.class`), and
  `MBeanInstanceData.init/takeSnapShot` already cast their argument to
  `MBeanInstance`, which is exactly what `POJOInstance.getItem()` returns.
  Verify `SnapShotPOJOLifecycle.setInstanceName` propagation and the
  no-arg-constructor path in `Bundle.newSnapShotData()`.

### 5. Tests (JUnit 3 style ONLY — extends TestCase, testXxx, suite()/main())
Model on `base/src/test/java/org/perfmon4j/remotemanagement/ExternalAppenderPOJOSnapShotTest.java`
(session setUp/tearDown, singleton-registry cleanup list). Register a real
MBean on the platform MBeanServer in the test (see existing tests around
`MBeanQueryEngine`/`MBeanAttributeExtractor` for fixtures). Cases:
key appears/disappears with MBean registration; field enumeration types;
subscribe + takeSnapShot returns real values (counter delta, gauge, string);
graceful when the MBean is unregistered mid-subscription; POJO keys and MBean
keys coexist without interference.

### 6. Changelog
`readme.txt` top `** <version> - TBD` block — use the `update-change-log` skill.

## Verification

- From `base/`: `mvn test` (surefire wires JAVASSIST_JAR / DERBY_EMBEDDED_DRIVER / LOG4J_JAR).
- End-to-end smoke: WildFly at `~/platforms/wildfly-33.0.0.Final` with a
  perfmon4j config containing an MBean `<snapShotMonitor>` query (e.g. a JVM
  MemoryPool query); confirm the instances appear and chart in the Hawtio
  Monitoring tab and/or VisualVM plugin.

## Critical files

- `base/src/main/java/org/perfmon4j/util/mbean/MBeanInstanceData.java` (implement `getFieldData`)
- `base/src/main/java/org/perfmon4j/util/mbean/MBeanInstance.java` / `MBeanInstanceImpl.java`
- `base/src/main/java/org/perfmon4j/util/mbean/MBeanAttributeExtractor.java` (`DatumDefinition`)
- `base/src/main/java/org/perfmon4j/remotemanagement/ExternalAppender.java`
- `base/src/main/java/org/perfmon4j/POJOSnapShotRegistry.java` (null-`pojoClass` marker)
- `base/src/main/java/org/perfmon4j/POJOSnapShotWrapper.java` (likely reusable as-is)
- `base/src/main/java/org/perfmon4j/util/mbean/MBeanSnapShotManager.java` (lifecycle reference)

## Conventions (carried over from the POJO epic)

- One commit per task, pushed promptly, on this `feature/MBeanSnapShotExternalAppender` branch off `develop`.
- Lazy-pull discovery only — keys rebuilt from live registry state per client poll; no listeners.
- Never call registry methods while holding `ExternalAppender.snapShotLockToken`, and vice versa.
- `initSnapShot` implementations must never return null (`MonitorMap.takeSnapShot` NPEs otherwise).
