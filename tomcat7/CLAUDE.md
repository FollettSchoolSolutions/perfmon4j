# tomcat7

## Overview
- Tomcat 7-specific integration module for Perfmon4j performance monitoring
- Provides a custom Tomcat Valve (`PerfMonValve`) for monitoring HTTP request/response metrics
- Monitors Tomcat-specific JMX metrics including thread pools and request processors
- Wraps servlet filter functionality (`PerfMonFilter` and `PerfMonNDCFilter`) into Tomcat's valve architecture
- Primary consumers are Tomcat 7.x application servers requiring performance monitoring
- Integrates with perfmon4j base library and perfmon4j-servlet module
- Key domain terms: Valve (Tomcat's request/response interceptor), GlobalRequestProcessor (HTTP metrics), ThreadPool (connection metrics), NDC (Nested Diagnostic Context)

## Architecture & Patterns
- Folder structure: `src/main/java/org/perfmon4j/extras/tomcat7/` (main code), `src/test/java/org/perfmon4j/extras/tomcat7/` (tests)
- Module boundaries: Valve implementation (`PerfMonValve`), JMX monitors (`GlobalRequestProcessorMonitorImpl`, `ThreadPoolMonitorImpl`), base abstraction (`JMXMonitorBase`)
- Communication patterns: Uses Tomcat Valve chain pattern to intercept requests, delegates to servlet filters internally
- External service integrations:
  - Tomcat 7 Catalina API for valve lifecycle management
  - JMX (Java Management Extensions) for accessing Tomcat runtime metrics via MBeans
  - JBoss detection and compatibility for running in JBoss AS
- JMX monitoring uses query patterns to find and aggregate metrics across multiple MBean instances

## Stack Best Practices
- Java servlet and Tomcat APIs (Valve, Lifecycle, Request/Response)
- Annotation-driven snapshot providers: `@SnapShotProvider`, `@SnapShotCounter`, `@SnapShotGauge`, `@SnapShotString`, `@SnapShotInstanceDefinition`
- JMX MBean querying via `MBeanServer` and `ObjectName` patterns
- Filter chain delegation pattern: wraps filters to work within valve architecture (see `FilterChainImpl` inner class in `PerfMonValve.java`)
- Lifecycle management: implements Tomcat `Lifecycle` interface with `initInternal()` and `destroyInternal()` methods
- Configuration via JavaBean properties on valve (e.g., `setBaseFilterCategory()`, `setAbortTimerOnRedirect()`)
- Instance-per-monitor pattern: monitors can track individual protocol handlers or aggregate all instances

## Anti-Patterns
- Avoid using this module with Tomcat versions other than 7.x (API compatibility issues)
- Do not bypass valve configuration - all settings should go through valve properties, not filter init params directly
- Avoid modifying MBean query patterns without understanding JBoss vs. Catalina naming differences
- Do not hardcode "Catalina:" or "jboss.web:" prefixes - use `MiscHelper.isRunningInJBossAppServer()` detection
- Never call filter methods directly - always use valve's invoke method which handles casting and delegation
- Avoid querying MBeans synchronously in request path - monitors are designed for periodic snapshot collection

## Data Models
- `GlobalRequestProcessorMonitor`: Interface for HTTP request metrics
  - `Delta getRequestCount()`: Request count with delta calculation
  - `Delta getBytesSent()`: Bytes sent (formatted as KB in storage)
  - `Delta getBytesReceived()`: Bytes received (formatted as KB in storage)
  - `Delta getProcessingTimeMillis()`: Processing time in milliseconds
  - `Delta getErrorCount()`: Error count
  - `String getInstanceName()`: Protocol handler instance name (e.g., "http-8080")
- `ThreadPoolMonitor`: Interface for thread pool metrics
  - `long getCurrentThreadsBusy()`: Current busy thread count (gauge)
  - `long getCurrentThreadCount()`: Total thread count (gauge)
  - `String getInstanceName()`: Thread pool instance name
- `JMXMonitorBase`: Base class for JMX monitoring with `MBeanServer` and `ObjectName` query support
- Storage: SQL writers persist data to `P4JGlobalRequestProcessor` and `P4JThreadPoolMonitor` tables
- Delta vs. Gauge: Counters use `Delta` for rate calculations, gauges use raw `long` values

## Security & Configuration
- `pom.xml` intentionally pins `org.apache.tomcat:tomcat-catalina` at `provided` scope to
  `7.0.2`, the oldest 7.x API surface - this is a compile-time-only dependency (the real
  jar comes from whatever Tomcat 7 server perfmon4j is deployed into), and the pin exists
  so the compiled Valve classes stay binary-compatible with actual Tomcat 7 deployments.
  GitHub Dependabot flags many CVEs against this pin, but bumping it to the versions
  Dependabot suggests (up to 9.0.118, since the 7.0.x line is long EOL and receives no
  further patches) would compile in Tomcat 9 API calls that don't exist in a real Tomcat 7
  runtime - i.e. it would *break* this module for its actual target servers, not fix a real
  exposure. These alerts are dismissed on GitHub with that rationale rather than resolved
  by a version bump - see `wildfly8/CLAUDE.md` for the equivalent Undertow situation.
- No environment variables or secrets required - configuration via Tomcat server.xml
- Valve configuration properties (set in server.xml `<Valve>` element):
  - `baseFilterCategory`: Base category for monitor naming (default: `PerfMonFilter.BASE_FILTER_CATEGORY`)
  - `abortTimerOnRedirect`: Skip timing for redirect responses (boolean)
  - `abortTimerOnImageResponse`: Skip timing for image responses (boolean)
  - `abortTimerOnURLPattern`: Regex pattern to abort timing (e.g., ".*\\.css$")
  - `skipTimerOnURLPattern`: Regex pattern to skip timing entirely
  - `outputRequestAndDuration`: Log request URLs and durations (boolean)
  - `servletPathTransformationPattern`: Pattern to normalize servlet paths for grouping
  - `pushCookiesOnNDC`: Comma-separated cookie names to push to NDC
  - `pushSessionAttributesOnNDC`: Comma-separated session attribute names to push to NDC
  - `pushClientInfoOnNDC`: Push client IP/hostname to NDC (boolean)
- MBean access requires JMX permissions (typically granted to application server process)
- No authentication/authorization - relies on container security

## Commands & Scripts
- Build: `mvn clean install` (from tomcat7 directory or parent)
- Test: `mvn test` (runs SQLTest-based integration tests with Derby)
- Package: `mvn package` (creates perfmon4j-tomcat7-{version}.jar)
- Install in Tomcat: Copy JAR to `$CATALINA_HOME/lib` and configure in `server.xml`:
  ```xml
  <Valve className="org.perfmon4j.extras.tomcat7.PerfMonValve"
         baseFilterCategory="MyApp"
         abortTimerOnImageResponse="true"/>
  ```
- Dependencies: Requires perfmon4j.jar and perfmon4j-servlet.jar in classpath
- No deployment scripts - manual JAR placement and configuration
