# Perfmon4j API and Agent Architecture

Perfmon4j ships as two separate jars with a deliberate **stub-and-rewrite**
relationship: `perfmon4j-agent-api.jar` (module `agent-api`, package
`api.org.perfmon4j.agent.*`) contains no-op stub classes that application
code compiles against directly, and the main `perfmon4j.jar` (module
`base`, package `org.perfmon4j.*`) contains the real implementation, loaded
via `-javaagent`. This page explains how the two are connected at runtime,
so a class declared against `agent-api` behaves as a no-op when the agent
isn't loaded, and as a fully functional monitor when it is.

> **TL;DR** — Application code always compiles against `agent-api`, never
> against `base`. When the Perfmon4j javaagent attaches, it rewrites the
> `agent-api` stub classes' *bytecode in place* — at class-load time — so
> their methods delegate into the real `org.perfmon4j.*` implementation.
> Without the agent, the stubs simply run the harmless no-op code written
> in their source files. This is **four distinct mechanisms**, not one
> generic engine — see [Four Distinct Mechanisms](#four-distinct-mechanisms-not-one).

---

## Table of Contents

- [The Two-Jar Deployment Model](#the-two-jar-deployment-model)
- [Unattached vs. Attached, and `isAttachedToAgent()`](#unattached-vs-attached-and-isattachedtoagent)
- [How Attachment Actually Happens](#how-attachment-actually-happens)
- [The Emitter Wrapper Family](#the-emitter-wrapper-family)
- [Registries and Weak References](#registries-and-weak-references)
- [Four Distinct Mechanisms, Not One](#four-distinct-mechanisms-not-one)
- [Escape Hatches](#escape-hatches)
- [Where to Look for Runnable Examples](#where-to-look-for-runnable-examples)
- [See Also](#see-also)

---

## The Two-Jar Deployment Model

Applications add `perfmon4j-agent-api.jar` as a normal compile-time
dependency and write code against it directly — declaring monitors,
timers, and `@SnapShot*` annotations with zero risk if the agent is never
present. The Perfmon4j javaagent (`-javaagent:/path/to/perfmon4j.jar`) is
attached separately at JVM boot.

This split exists for a concrete historical reason, recorded in
`readme.txt`'s changelog:

- **v1.4.2** — *"Added the perfmon4j-agent-api.jar. Using this jar you can
  optionally add Perfmon4j timers and annotations to classes (bundled in
  jar, war or ear files). If the Perfmon4j java agent is loaded at boot
  time the functionality represented by these classes will become
  activated. When the Perfmon4j agent is not loaded, these classes will be
  inactive."*
- **v1.4.3** — *"Renamed the package of the api jar from
  'org.perfmon4j.agent.api' to 'api.org.perfmon4j.agent'. This is required
  because when running under JBoss/Wildfly classes can only be loaded from
  the 'org.perfmon4j.\*' package when loaded by the perfmon4j javaagent."*
  — i.e. the inverted-looking package name (`api.org.perfmon4j.agent`
  rather than `org.perfmon4j.agent.api`) is intentional, not a typo: it
  keeps `agent-api` classes *outside* the `org.perfmon4j.*` namespace that
  some application servers restrict to agent-loaded code.

`base/CLAUDE.md` reinforces the runtime boundary: `agent-api` is only a
`test`-scope Maven dependency of `base` (exercised by
`PerfMonAgentAPITest`) — the agent itself must never depend on `agent-api`
at runtime, since it needs to work from the root classloader before any
application class is visible.

## Unattached vs. Attached, and `isAttachedToAgent()`

Every dual-mode stub class in `agent-api` carries the same Javadoc
(verbatim, e.g. from `PerfMon.java`):

> "This class will execute in one of two modes: Unattached — When running
> in a JVM that was not booted with the Perfmon4j instrumentation agent
> this class will execute the code declared within this source file.
> Essentially it will be running in a non-operative state. Attached — When
> this class is loaded in a JVM that was booted with the Perfmon4j
> instrumentation agent, The agent will re-write this class and it will be
> in an operating state. The code is instrumented in method
> JavassistRuntimeTimerInjector.attachAgentToPerfMonAPIClass()."

The unattached bodies are literally inert. `PerfMonTimer` keeps one shared
sentinel instance and every `start*` overload just returns it; `stop`/
`abort` are empty:

```java
// agent-api/src/main/java/api/org/perfmon4j/agent/PerfMonTimer.java
private static final PerfMonTimer noOpTimer = new PerfMonTimer();

public static PerfMonTimer start(PerfMon mon) {
    return noOpTimer;
}

public static void stop(PerfMonTimer timer) {
}
```

`isAttachedToAgent()` is how code (yours or Perfmon4j's own tests) checks
which mode is active:

```java
public static boolean isAttachedToAgent() {
    return false;
}
```

There is no mutable flag anywhere — "attaching" means the agent rewrites
this exact method's body to `return true;` at class-load time (see below).
Calling `isAttachedToAgent()` after that point returns `true` simply
because the bytecode itself changed.

## How Attachment Actually Happens

Two collaborating classes in `base` do the work, both under
`base/src/main/java/org/perfmon4j/instrument/`:

- **`PerfMonTimerTransformer`** — the `-javaagent` entry point (`premain`).
  It registers a nested `ClassFileTransformer`, `PerfMom4JAPIInserter`,
  with the JVM's `Instrumentation` instance. The JVM calls this
  transformer for **every** class loaded anywhere in the process.
- **`JavassistRuntimeTimerInjector`** — does the actual Javassist bytecode
  surgery once a target class is identified.

Detection is a hardcoded, exact class-name allow-list — not reflection or
package scanning:

```java
// PerfMonTimerTransformer.java, inside PerfMom4JAPIInserter.transform(...)
if ("api/org/perfmon4j/agent/PerfMon".equals(className)) {
    result = runtimeTimerInjector.attachAgentToPerfMonAPIClass(classfileBuffer, loader, protectionDomain);
} else if ("api/org/perfmon4j/agent/PerfMonTimer".equals(className)) {
    result = runtimeTimerInjector.attachAgentToPerfMonTimerAPIClass(classfileBuffer, loader, protectionDomain);
} else if ("api/org/perfmon4j/agent/SQLTime".equals(className)) { ... }
  else if ("api/org/perfmon4j/agent/ThreadTraceConfig".equals(className)) { ... }
  else if ("api/org/perfmon4j/agent/POJOSnapShotRegistry".equals(className)) { ... }
  else if ("api/org/perfmon4j/agent/EmitterRegistry".equals(className)) { ... }
  else if ("api/org/perfmon4j/agent/util/SingletonTrackerImpl".equals(className)) { ... }
  else if (className.startsWith("api/org/perfmon4j/agent/impl/EmitterInstrumentationHelper$")) { ... }
```

Adding a *new* dual-mode stub class means manually adding a new branch here
and a matching `attachAgentTo...APIClass()` method — the mechanism is not
generic or reflective by design.

Each `attachAgentTo...APIClass()` method follows the same recipe, shown
here for `PerfMon`/`PerfMonTimer` (`JavassistRuntimeTimerInjector.java`):

```java
public byte[] attachAgentToPerfMonAPIClass(byte[] classfileBuffer, ClassLoader loader,
        ProtectionDomain protectionDomain) throws Exception {
    CtClass clazz = getClazz(classPool, classfileBuffer);

    updateIsAttachedToAgent(clazz);                                          // (1)
    clazz.addInterface(classPool.getCtClass(PerfMonAgentApiWrapper.class.getName())); // (2)
    clazz.addField(CtField.make("private org.perfmon4j.PerfMon nativeObject = null;", clazz)); // (3)

    String src = "{"
            + "org.perfmon4j.PerfMon p = org.perfmon4j.PerfMon.getMonitor($1, $2);\r\n"
            + "return new  api.org.perfmon4j.agent.PerfMon(p);"
            + "}";
    replaceMethodIfExists(clazz, "getMonitor", src, String.class.getName(), CtClass.booleanType.getName()); // (4)
    ...
    return clazz.toBytecode();                                               // (5)
}
```

1. **Flip the flag** — `updateIsAttachedToAgent` finds `isAttachedToAgent`
   and calls `method.setBody("return true;")`. If the method is missing,
   this *throws* — every targeted stub class must declare it.
2. **Add a marker interface** (`PerfMonAgentApiWrapper` /
   `PerfMonTimerAgentApiWrapper`, defined in `base`) exposing
   `getNativeObject()`, so other rewritten classes can reach the wrapped
   real object.
3. **Add a field** holding the real `org.perfmon4j.*` instance — each
   `api.org.perfmon4j.agent.PerfMon`/`PerfMonTimer` object becomes a thin
   per-instance wrapper, not a static/global redirect.
4. **Replace method bodies** with Javassist-compiled Java source that
   delegates into the real implementation, e.g. `PerfMonTimer.start(...)`
   becomes:
   ```java
   org.perfmon4j.PerfMon nativePerfMon =
       ((org.perfmon4j.instrument.PerfMonAgentApiWrapper)$1).getNativeObject();
   return new api.org.perfmon4j.agent.PerfMonTimer(org.perfmon4j.PerfMonTimer.start(nativePerfMon));
   ```
   `replaceMethodIfExists` looks up methods by name and exact parameter
   types and simply logs and skips ones it can't find — tolerant of minor
   API drift between `agent-api` versions, unlike `isAttachedToAgent`.
5. The rewritten bytecode is returned from `transform(...)`, and the JVM
   loads it **instead of** the original stub bytes.

## The Emitter Wrapper Family

`Emitter` is the API for pushing custom, application-defined metrics.
Because the *application* implements `api.org.perfmon4j.agent.Emitter`
while `base`'s registry and scheduler expect `org.perfmon4j.emitter.*`
types, this case needs bidirectional adapters rather than a simple
delegate — three nested classes in
`agent-api/src/main/java/api/org/perfmon4j/agent/impl/EmitterInstrumentationHelper.java`:

- **`APIEmitterWrapper`** — wraps an app-supplied `Emitter` so it can
  satisfy `org.perfmon4j.emitter.Emitter` once rewritten.
- **`EmitterControllerWrapper`** — wraps the agent's
  `org.perfmon4j.emitter.EmitterController` as the API's
  `EmitterController`, so app code's `initData()`/`emit()`/`isActive()`
  calls reach the real controller.
- **`EmitterDataWrapper`** — wraps `org.perfmon4j.emitter.EmitterData`,
  forwarding all six `addData(...)` overloads.

Unattached, every method body is empty, with the exact code the agent will
splice in written out **as a comment** — a self-documenting convention
worth knowing when reading this module:

```java
@Override
public void addData(String fieldName, long value) {
    /* The code in the comment below will be added by the Perfmon4j instrumentation agent */
    /*
        ((org.perfmon4j.emitter.EmitterData)getDelegate()).addData($1, $2);
    */
}
```

Each nested wrapper class is rewritten by its own
`attachAgentToAPIEmitterWrapperClass` / `attachAgentToAPIEmitterControllerWrapperClass`
/ `attachAgentToAPIEmitterDataWrapperClass` method, using the same
flip-the-flag-then-replace-method-bodies recipe as above, dispatched via
the `className.startsWith("api/org/perfmon4j/agent/impl/EmitterInstrumentationHelper$")`
branch shown earlier.

## Registries and Weak References

`EmitterRegistry` and `POJOSnapShotRegistry` (both in `agent-api`, with
real counterparts in `base`) let application code register POJOs and
`Emitter` instances with a **static, JVM-lifetime-scoped** agent registry.
Both real-side registries extend
`base/src/main/java/org/perfmon4j/GenericItemRegistry.java`, which by
default stores registered items behind a `WeakReference`:

```java
// GenericItemRegistry.ItemInstance
protected ItemInstance(T item, String instanceName, boolean weakReference) {
    if (weakReference) {
        itemWeakReference = new WeakReference<T>(item);
        itemStrongReference = null;
    } else {
        itemWeakReference = null;
        itemStrongReference = item;
    }
}
```

This matters specifically because of *who* registers with these
registries: application objects, potentially loaded by an application
server's per-deployment classloader. A strong reference here would pin
that object — and transitively its classloader — alive for the life of the
JVM, a classic classloader-leak pattern on app-server redeploy. Defaulting
to a weak reference lets `isActive()`/`getItem()` treat a collected
reference as "gone" once the application itself drops its references,
without requiring explicit deregistration. The `useWeakReference=false`
overload remains available for callers who genuinely want a JVM-wide
permanent singleton.

## Four Distinct Mechanisms, Not One

It's tempting to assume the PerfMon/PerfMonTimer story generalizes to
everything in `agent-api`. It doesn't — there are four separate mechanisms,
unified only by the `isAttachedToAgent()` convention and by living in
`PerfMonTimerTransformer`/`JavassistRuntimeTimerInjector`:

| # | Mechanism | Applies to | When it runs |
|---|-----------|------------|---------------|
| 1 | Class-identity stub rewriting (hardcoded allow-list) | `PerfMon`, `PerfMonTimer`, `SQLTime`, `ThreadTraceConfig`, `POJOSnapShotRegistry`, `EmitterRegistry`, `SingletonTrackerImpl` | Class-load time, via `ClassFileTransformer` |
| 2 | Emitter wrapper adapters (a refinement of #1) | `EmitterInstrumentationHelper$APIEmitterWrapper` / `$EmitterControllerWrapper` / `$EmitterDataWrapper` | Class-load time |
| 3 | `@DeclarePerfMonTimer` method-level annotation scanning | Arbitrary **application** methods annotated `@DeclarePerfMonTimer` (native or API-mirrored annotation) | Normal bytecode instrumentation of app classes — injects timer start/stop directly into the annotated method, no wrapper object at all |
| 4 | SnapShot annotation mirroring (`@SnapShotProvider`, `@SnapShotCounter`, `@SnapShotString`, etc.) | Data-provider classes/methods | Snapshot-generation time, via reflection in `AnnotationTransformer` — not class-load time at all |

Mechanism 4 is the one already covered in depth elsewhere: each
`@SnapShot*` annotation exists as two hand-maintained, independently
compiled copies (one in `agent-api`, one in `base`), bridged by
`base/src/main/java/org/perfmon4j/util/AnnotationTransformer.java`, and
consumed by
`base/src/main/java/org/perfmon4j/instrument/snapshot/JavassistSnapShotGenerator.java`
when it generates a snapshot data class. See the `SnapShotRatio`/
`SnapShotString` annotations for the established pattern: an attribute
must exist verbatim (same name/type/default) on both copies to be usable
from application code, and there's no build-time sync — see the git
history around `@SnapShotString`'s `outputAsTag` attribute for a worked
example of adding one safely.

## Escape Hatches

Two independent system properties disable different parts of this story:

- **`-DPerfMon4J.IgnoreAgentAPIClasses=true`** — disables mechanism 1 (and
  2) entirely; `agent-api` stub classes are never rewritten, so they run
  as pure no-ops even with the agent attached.
- **`-DPerfmon4j.DisableClassInstrumentation=true`** — disables bytecode
  instrumentation of **application** classes (mechanism 3 and general
  timer injection), but does **not** affect agent-api stub rewriting.

These two flags are independent switches, not a single on/off toggle —
don't assume one implies the other.

## Where to Look for Runnable Examples

`base/src/test/java/org/perfmon4j/instrument/PerfMonAgentAPITest.java` is
the canonical, living reference: it actually invokes the transformer and
asserts on the rewritten class's behavior for every scenario above
(`testAttachedPerfMonAPI`, `testAttachedPerfMonTimerAPI`,
`testAttachedSQLTimeAPI`, `testAttachedDeclarePerfmonTimerAPI`,
`testAPISnapShotAnnotations`, `testPOJOSnapShotRegistry`,
`testEmitterRegistryAPI`, `testSingletonTrackerAPIEnabled`, and more). When
in doubt about how a specific stub class behaves once attached, start
there rather than re-deriving it from the transformer source.

---

## See Also

- [Configuring the Java Agent](Configuring-the-Java-Agent.md)
- [Configuration Properties & Conditional Activation](Configuration-Properties-and-Conditional-Activation.md)
