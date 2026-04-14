# Configuring the Perfmon4j Java Agent

Perfmon4j is attached to a Java application as a standard JVM Java agent. All configuration begins with the `-javaagent` JVM argument, optionally followed by agent parameters that control instrumentation behavior, configuration file loading, and remote management.

---

## Table of Contents

- [Basic Syntax](#basic-syntax)
- [Agent Parameters](#agent-parameters)
  - [Class Instrumentation](#class-instrumentation)
  - [SQL Monitoring](#sql-monitoring)
  - [Servlet / Web Request Monitoring](#servlet--web-request-monitoring)
  - [Hystrix Monitoring](#hystrix-monitoring)
  - [Configuration File](#configuration-file)
  - [Logging and Debug](#logging-and-debug)
  - [Remote Management](#remote-management)
  - [Miscellaneous](#miscellaneous)
- [Pattern-Based Matching](#pattern-based-matching)
- [Configuration File Overview](#configuration-file-overview)
- [System Properties](#system-properties)
- [Common Examples](#common-examples)
- [Getting Started — Commonly Used Options](#getting-started--commonly-used-options)

---

## Basic Syntax

```
java -javaagent:/path/to/perfmon4j.jar[=<parameters>] <MainClass>
```

Parameters are separated by commas. Quoting the parameter string is recommended when it contains spaces or special characters:

```
java -javaagent:perfmon4j.jar="-ecom.example.app,-eSQL,-p5945" com.example.Main
```

No parameters are required. Without them, the agent loads but performs no class instrumentation.

---

## Agent Parameters

### Class Instrumentation

These parameters control which classes and packages are instrumented for performance monitoring.

#### `-a` — Annotate Mode

Instruments only methods annotated with `@DeclarePerfMonTimer` in the specified packages or classes.

```
-a<package.or.ClassName>
```

| Aspect | Detail |
|--------|--------|
| Format | `-acom.example.app` |
| Multiple | `-acom.example.app,-aorg.other.pkg` |
| Default | Disabled |

Use this mode when you want fine-grained, developer-controlled instrumentation.

#### `-e` — Extreme Mode

Instruments **all** methods in the specified packages or classes automatically.

```
-e<package.or.ClassName>
```

| Aspect | Detail |
|--------|--------|
| Format | `-ecom.example.app` |
| Multiple | `-ecom.example.app,-eorg.other.pkg` |
| Default | Disabled |

By default, getter and setter methods are excluded. To include them:

```
-e(+getter)com.example.app        # Include getters
-e(+setter)com.example.app        # Include setters
-e(+getter,+setter)com.example.app # Include both
```

#### `-i` — Ignore List

Excludes the specified packages or classes from all instrumentation. Takes higher priority than `-a` and `-e`.

```
-i<package.or.ClassName>
```

| Aspect | Detail |
|--------|--------|
| Format | `-icom.example.app.internal` |
| Multiple | `-icom.example.util,-ijava.lang.String` |
| Default | Empty (nothing excluded) |

#### `-b` — Bootstrap Instrumentation

Attempts to instrument classes that were already loaded before the agent started.

```
-b<true|false>
```

| Aspect | Detail |
|--------|--------|
| Default | `false` |
| Example | `-btrue` |

---

### SQL Monitoring

#### `-eSQL` — JDBC SQL Monitoring

Enables performance monitoring for JDBC database calls.

```
-eSQL                        # All supported JDBC drivers
-eSQL(<driver>)              # Specific built-in driver
-eSQL(<custom.package>)      # Custom JDBC package
```

**Built-in driver shortcuts:**

| Keyword | Package Monitored |
|---------|-------------------|
| `JTDS` | `net.sourceforge.jtds` |
| `POSTGRESQL` | `org.postgresql` |
| `MYSQL` | `com.mysql.jdbc` |
| `DERBY` | `org.apache.derby` |
| `ORACLE` | `oracle.jdbc` |
| `MICROSOFT` | `com.microsoft.sqlserver.jdbc` |

**Examples:**

```
-eSQL                          # All drivers
-eSQL(POSTGRESQL)              # PostgreSQL only
-eSQL(ORACLE)                  # Oracle only
-eSQL(org.my.custom.jdbc)      # Custom driver package
```

---

### Servlet / Web Request Monitoring

#### `-eVALVE` — Servlet Valve

Installs a servlet valve to monitor incoming HTTP requests. Compatible with Tomcat, JBoss, and WildFly.

```
-eVALVE
```

| Aspect | Detail |
|--------|--------|
| Default | Disabled |
| Requires | Servlet valve configured in `perfmonconfig.xml` |

The valve intercepts HTTP requests and records their duration under the `WebRequest` monitor category. Additional valve behavior (URL pattern rewriting, NDC push, redirect handling) is configured in the [boot section](#configuration-file-overview) of the XML config.

---

### Hystrix Monitoring

#### `-eHYSTRIX` — Hystrix Circuit Breaker

Enables instrumentation of Hystrix circuit breaker commands. Case-insensitive.

```
-eHYSTRIX
-eHystrix      # Also valid
```

| Aspect | Detail |
|--------|--------|
| Default | Disabled |

---

### Configuration File

#### `-f` — File System Config Path

Loads the XML configuration file from the specified file system path.

```
-f<path/to/perfmonconfig.xml>
```

| Aspect | Detail |
|--------|--------|
| Default | Not set (falls back to classpath) |
| Example | `-f/etc/perfmon4j/perfmonconfig.xml` |

#### `-c` — Classpath Config Resource

Loads the XML configuration file from the specified classpath resource. Use `-cfalse` to disable classpath loading entirely.

```
-c<resource/path/perfmonconfig.xml>
-cfalse
```

| Aspect | Detail |
|--------|--------|
| Default | `perfmonconfig.xml` (root of classpath) |
| Example | `-ccom/example/config/perfmonconfig.xml` |
| Disable | `-cfalse` |

#### `-r` — Config Reload Interval

Checks the configuration file for changes every N seconds and reloads if modified.

```
-r<seconds>
```

| Aspect | Detail |
|--------|--------|
| Default | `60` (seconds) |
| Minimum | `10` seconds |
| Disable | `-r0` (no reload) |
| Example | `-r30` |

---

### Logging and Debug

#### `-d` — Debug Mode

Enables detailed logging of agent instrumentation activity.

```
-d<true|false>
```

| Aspect | Detail |
|--------|--------|
| Default | `false` |
| Example | `-dtrue` |

#### `-v` — Verbose Mode

Enables enhanced instrumentation diagnostic logging (more detailed than `-d`).

```
-v<true|false>
```

| Aspect | Detail |
|--------|--------|
| Default | `false` |
| Example | `-vtrue` |

---

### Remote Management

#### `-p` — Remote Management Port

Enables the remote management interface on the specified TCP port. Use `AUTO` to let the agent select an available port automatically.

```
-p<port>
-pAUTO
```

| Aspect | Detail |
|--------|--------|
| Default | Disabled (`-1`) |
| Example | `-p5945`, `-pAUTO` |

---

### Miscellaneous

#### `-g` — Disable System.gc()

Suppresses `System.gc()` calls made by the application.

```
-g<true|false>
```

| Aspect | Detail |
|--------|--------|
| Default | `false` |
| Example | `-gtrue` |

---

## Pattern-Based Matching

For `-a`, `-e`, and `-i` parameters, you can use regular expression patterns by wrapping the pattern in `P(...)`:

```
-aP(Buffer$)          # Annotate all classes ending with "Buffer"
-eP(\.service\.)      # Extreme-monitor all classes with ".service." in the package
-iP(.*Test.*)         # Ignore all test classes
```

---

## Configuration File Overview

Perfmon4j expects an XML configuration file named `perfmonconfig.xml` on the classpath by default. Use `-f` or `-c` to override the location.

### Minimal Example

```xml
<Perfmon4JConfig enabled='true'>
    <appender name='text-out' className='org.perfmon4j.TextAppender' interval='1 minute'/>

    <monitor name='com.example'>
        <appender name='text-out' pattern="./*"/>
    </monitor>
</Perfmon4JConfig>
```

### With Web Request Monitoring

```xml
<Perfmon4JConfig enabled='true'>
    <boot>
        <!-- Requires -eVALVE on the command line -->
        <servletValve outputRequestAndDuration='true'/>
    </boot>

    <appender name='text-out' className='org.perfmon4j.TextAppender' interval='1 minute'>
        <attribute name='thresholdCalculator'>2 seconds, 5 seconds, 10 seconds</attribute>
    </appender>

    <monitor name='WebRequest'>
        <appender name='text-out' pattern="."/>
    </monitor>
</Perfmon4JConfig>
```

### Boot Section — javaAgentParameters

Parameters can also be supplied via the `boot` section of the config file. Command-line parameters take precedence over config file parameters.

```xml
<Perfmon4JConfig enabled='true'>
    <boot>
        <javaAgentParameters>-ecom.example.app,-vtrue</javaAgentParameters>
    </boot>
</Perfmon4JConfig>
```

### Property Substitution

The config file supports property substitution with optional defaults:

```xml
<appender name='text-out' className='org.perfmon4j.TextAppender'
    interval='${MONITOR_INTERVAL:1 minute}'/>
```

---

## System Properties

In addition to agent parameters, the following JVM system properties influence agent behavior:

| Property | Description |
|----------|-------------|
| `Perfmon4j.HideBanner` | Suppresses the agent startup banner |
| `Perfmon4j.RemoteInterfaceDelaySeconds` | Seconds to wait before starting the remote interface (default: `30`) |
| `Perfmon4j.DisableClassInstrumentation` | Completely disables class instrumentation when set |
| `PerfMon4j.debugEnabled` | Enables debug mode (alternative to `-dtrue`) |
| `PERFMON4J_FORCE_EXTERNAL_JAVASSIST_JAR` | Use an external `javassist.jar` instead of the embedded one |
| `PERFMON4J_FILTER_PASSWORD_PATTERN` | Regex pattern used to mask password parameters in logged output |
| `org.perfmon4j.PerfMonTimer.DisableRecursionPrevention` | Disables recursion prevention in timer logic |
| `org.perfmon4j.MonitorThreadTracker.DisableThreadTracking` | Disables active thread tracking |

---

## Common Examples

### Monitor specific packages (annotate mode)

Only instrument methods explicitly annotated with `@DeclarePerfMonTimer`:

```
java -javaagent:perfmon4j.jar="-acom.example.service,-acom.example.dao" com.example.Main
```

### Monitor all methods in packages (extreme mode)

Automatically instrument every method in the target packages:

```
java -javaagent:perfmon4j.jar="-ecom.example.service,-ecom.example.dao" com.example.Main
```

### Web application with SQL and HTTP monitoring

```
java -javaagent:perfmon4j.jar="-eVALVE,-eSQL,-ecom.example" -jar myapp.jar
```

### Remote management with debug logging

```
java -javaagent:perfmon4j.jar="-p5945,-dtrue,-r30" com.example.Main
```

### Exclude sub-packages from extreme monitoring

```
java -javaagent:perfmon4j.jar="-ecom.example,-icom.example.generated,-icom.example.util" com.example.Main
```

### Custom config file location with no reload

```
java -javaagent:perfmon4j.jar="-f/etc/myapp/perfmonconfig.xml,-r0" com.example.Main
```

### Pattern matching — monitor all service classes

```
java -javaagent:perfmon4j.jar="-eP(\.service\.)" com.example.Main
```

### Full production setup

```
java \
  -javaagent:/opt/perfmon4j/perfmon4j.jar="-ecom.example,-eSQL(POSTGRESQL),-eVALVE,-p5945,-r60" \
  -Dperfmon4j.HideBanner=true \
  com.example.Main
```

---

## Parameter Quick Reference

| Parameter | Description | Default |
|-----------|-------------|---------|
| `-a<pkg>` | Annotate mode: instrument `@DeclarePerfMonTimer` methods | Disabled |
| `-e<pkg>` | Extreme mode: instrument all methods | Disabled |
| `-e(+getter)<pkg>` | Extreme mode including getter methods | Disabled |
| `-e(+setter)<pkg>` | Extreme mode including setter methods | Disabled |
| `-i<pkg>` | Ignore (exclude) from instrumentation | Empty |
| `-btrue` | Bootstrap: instrument pre-loaded classes | `false` |
| `-eSQL` | Monitor all JDBC drivers | Disabled |
| `-eSQL(<driver>)` | Monitor specific JDBC driver | Disabled |
| `-eVALVE` | Install servlet valve for HTTP monitoring | Disabled |
| `-eHYSTRIX` | Monitor Hystrix circuit breakers | Disabled |
| `-f<path>` | Config file path (file system) | Not set |
| `-c<resource>` | Config resource (classpath) | `perfmonconfig.xml` |
| `-cfalse` | Disable classpath config loading | — |
| `-r<seconds>` | Config reload interval (0 = disabled) | `60` |
| `-dtrue` | Debug logging | `false` |
| `-vtrue` | Verbose instrumentation logging | `false` |
| `-p<port>` | Remote management port (`AUTO` = auto-select) | Disabled |
| `-gtrue` | Disable `System.gc()` calls | `false` |

---

## Getting Started — Commonly Used Options

If you are a developer enabling perfmon4j for the first time on your organization's codebase, the most practical starting point combines three parameters:

- **`-f`** to point the agent at your configuration file
- **`-a`** to instrument only methods your team has explicitly annotated with `@DeclarePerfMonTimer`
- **`-e`** to automatically instrument every method in your organization's packages

### Step 1 — Basic instrumentation startup

```
java -javaagent:perfmon4j.jar="-f./perfmonconfig.xml,-acom.follettsoftware,-ecom.follettsoftware" com.follettsoftware.Main
```

What each parameter does here:

| Parameter | Effect |
|-----------|--------|
| `-f./perfmonconfig.xml` | Loads `perfmonconfig.xml` from the current working directory. The config file controls what gets logged and where (text output, database, etc.). |
| `-acom.follettsoftware` | Activates annotate mode for all classes under `com.follettsoftware`. Methods annotated with `@DeclarePerfMonTimer` will be tracked. |
| `-ecom.follettsoftware` | Activates extreme mode for all classes under `com.follettsoftware`. Every method in your codebase gets a performance timer automatically — no annotations required. |

Both `-a` and `-e` apply to the same package prefix here, which is intentional: annotate mode respects developer-placed annotations, while extreme mode catches everything else. Third-party libraries and JDK classes are left untouched because neither flag covers those packages.

### Step 2 — Add verbose logging while getting started

```
java -javaagent:perfmon4j.jar="-f./perfmonconfig.xml,-acom.follettsoftware,-ecom.follettsoftware,-vtrue" com.follettsoftware.Main
```

The `-vtrue` parameter tells perfmon4j to log a message to the console for every class it instruments and every timer it creates. This gives you immediate, readable confirmation that the agent is doing what you expect — which classes are being picked up, which methods are being wrapped, and whether your config file loaded successfully.

> **Note:** `-vtrue` is intentionally too noisy for production. In a real application, instrumenting thousands of methods produces thousands of startup log lines. Use it while getting oriented, confirm the agent is behaving correctly, then remove it before deploying.

### Typical workflow

1. Start with the three-parameter form above (no `-v`). Verify the application runs normally.
2. Add `-vtrue` temporarily and inspect the output to confirm the expected classes and methods are being instrumented.
3. Tune the package prefixes — use `-i` to exclude packages that generate noise or that you do not care about.
4. Remove `-vtrue` before committing or deploying to any shared environment.
