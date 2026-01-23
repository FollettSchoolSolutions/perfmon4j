# base

## Overview
- Core performance monitoring library for Java applications providing runtime metrics and instrumentation
- Implements a Java agent that uses bytecode instrumentation (via Javassist) to inject performance timers into application code
- Provides hierarchical monitoring with parent/child relationships through PerfMon instances (`org.perfmon4j.PerfMon`)
- Collects interval-based metrics (hits, completions, duration statistics, active threads, SQL time) and outputs to configurable appenders
- Supports multiple output formats including text, database, InfluxDB, and Azure Log Analytics via appender pattern (`org.perfmon4j.Appender`)
- Primary consumers: Java applications needing performance monitoring, particularly web apps (Tomcat, WildFly, Quarkus modules depend on this)
- Integration points: `agent-api` module (test scope only), `dbupgrader` (test scope), external modules for servlet/app server integration

## Architecture & Patterns
- `src/main/java/org/perfmon4j/` - Core monitoring classes (PerfMon, PerfMonTimer, Appender, SnapShotManager)
- `src/main/java/org/perfmon4j/config/xml/` - XML configuration parsing and management
- `src/main/java/org/perfmon4j/instrument/` - Java agent and bytecode instrumentation (Javassist-based)
- `src/main/java/org/perfmon4j/instrument/snapshot/` - Snapshot generation for POJOs
- `src/main/java/org/perfmon4j/instrument/jmx/` - JMX snapshot proxy support
- `src/main/java/org/perfmon4j/reactive/` - Reactive context support for async/reactive applications
- `src/main/java/org/perfmon4j/util/` - Utility classes (logging, helpers, thread monitoring)
- `src/main/java/org/perfmon4j/azure/`, `influxdb/`, `hystrix/` - Integration with external systems
- `src/sql/` - Database schema scripts for multiple databases (MySQL, MSSQL, Oracle, PostgreSQL)
- Singleton pattern used extensively (PerfMon monitors, Appenders) with thread-safe singleton maps
- Timer pattern with priority/utility timers for scheduling data collection tasks
- Hierarchical monitor structure with lazy initialization and dynamic monitor creation
- Java agent loads via `-javaagent` flag, uses premain-class manifest entry pointing to `PerfMonTimerTransformer`

## Stack Best Practices
- Uses JUnit 3 style tests (methods named `public void testXxx()`) rather than annotations - see `ServletPathTransformerTest.java`
- Thread-safety via `ReadWriteLock` for monitor maps, `Lock` objects for critical sections
- Weak references (`WeakReference<Thread>`, `WeakHashMap`) to avoid memory leaks when threads/objects are GC'd
- Atomic operations (`AtomicInteger`, `AtomicLong`) for counters and reference counts
- Fail-safe timer tasks (`FailSafeTimerTask`) that catch and log exceptions to prevent timer thread death
- Custom classloader isolation (`IsolateJavassistClassLoader`) to prevent Javassist conflicts
- System properties for feature flags (e.g., `PERFMON4J_FORCE_EXTERNAL_JAVASSIST_JAR`, `Perfmon4j.DisableClassInstrumentation`)
- Logger abstraction (`org.perfmon4j.util.Logger`) instead of direct Log4j dependency to avoid classloading issues
- Maven Ant plugin embeds Javassist JAR into the perfmon4j JAR at compile time (see `pom.xml` line 28)

## Anti-Patterns
- Avoid logging in constructors that could create infinite loops (see comment in `PerfMon.java` line 243)
- Never use `-uall` flag with `git status` as it can cause memory issues on large repos
- Don't create thread-local storage without weak references - can cause memory leaks in application servers
- Avoid recursion in timer start/stop logic - use `RecursionPreventor` pattern (see `PerfMonTimer.java`)
- Don't depend on `agent-api` module at runtime - it's test scope only; agent must work in root classloader
- Don't use system-wide locks for monitoring operations - prefer read/write locks or lock-free structures
- Avoid using `@Test` annotations - this codebase uses JUnit 3 convention with method names starting with `test`
- Never call `PerfMon.class.getPackage().getImplementationVersion()` without null checks (see line 1708)

## Data Models
- **PerfMon** - Core monitor entity tracking hits, completions, duration (min/max/avg/stddev), active threads, SQL time
- **IntervalData** - Time-windowed performance metrics for a specific monitor and appender
- **PerfMonTimer** - Timer wrapper for start/stop operations, supports nested timers and reactive contexts
- **Appender** - Abstract base for output destinations, has AppenderID (className + interval + attributes)
- **ThreadTraceConfig** - Configuration for thread trace capturing with triggers (HTTP request/session/cookie)
- **SnapShotMonitor** - Point-in-time snapshots of system metrics (JVM memory, thread pools, etc.)
- **ReferenceCount** - Tracks nested timer invocations per thread or reactive context
- **ReactiveContext** - Context object for tracking timers across async boundaries in reactive applications
- Monitor hierarchy uses dot notation (e.g., `com.example.MyClass.myMethod`) with parent/child relationships
- SQL metrics tracked separately: `totalSQLDuration`, `maxSQLDuration`, `minSQLDuration`, `sumOfSQLSquares`

## Security & Configuration
- Configuration loaded via XML files parsed by `XMLConfigurator` and `XMLConfigurationParser2`
- Boot configuration supports servlet valve hooks, URL transformation patterns, NDC push
- Environment variables: `PERFMON4J_FORCE_EXTERNAL_JAVASSIST_JAR` (boolean), `JAVASSIST_JAR` (path), `DERBY_EMBEDDED_DRIVER` (path)
- System properties: `PERFMON_APPENDER_QUEUE_SIZE` (default 500), `PERFMON_APPENDER_ASYNC_TIMER_MILLIS` (default 5000)
- Database connection details managed via `RegisteredDatabaseConnections` class
- Remote management interface with configurable delay: `Perfmon4j.RemoteInterfaceDelaySeconds` (default 30)
- No authentication/authorization built into core - relies on JVM security and application-level controls
- SQL scripts in `src/sql/` folder for database appender schema creation (MySQL, MSSQL, Oracle, PostgreSQL)
- Example config at `src/main/resources/democonfig.xml` shows monitor and appender setup

## Commands & Scripts
- **Build**: `mvn clean install` from base directory (embeds Javassist, creates test-jar and monitor-agent JAR)
- **Run tests**: `mvn test` (requires system properties: JAVASSIST_JAR, DERBY_EMBEDDED_DRIVER, LOG4J_JAR, UNIT=x)
- **Package agent**: Creates main JAR, test JAR, and monitor-agent classifier JAR (remote management interfaces only)
- **Run as Java agent**: `-javaagent:perfmon4j.jar` (premain-class: `org.perfmon4j.instrument.PerfMonTimerTransformer`)
- **Demo**: Run classes in `org.perfmon4j.demo` package (DemoRunner, XMLConfigDemo, POJOSnapShotDemo, etc.)
- Maven dependencies: Derby (test), perfmon4j-dbupgrader (test), perfmon4j-agent-api (test scope only)
- Parent POM: `org.perfmon4j:perfmon4j-project:2.2.1-SNAPSHOT`
