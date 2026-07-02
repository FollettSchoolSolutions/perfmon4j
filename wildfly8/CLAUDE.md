# wildfly8

## Overview
- WildFly 8 integration module for Perfmon4j performance monitoring
- Provides HTTP request monitoring via Undertow HandlerWrapper integration
- Monitors web request timing, SQL duration, and thread traces for WildFly 8 applications
- Integrates with Undertow's HttpHandler chain to intercept and time HTTP requests
- Part of the perfmon4j-project monorepo (artifact: perfmon4j-wildfly8)
- Used by WildFly 8 applications to enable transparent HTTP request performance monitoring

## Architecture & Patterns
- **Entry Point**: `PerfmonHandlerWrapper` implements Undertow's `HandlerWrapper` interface
- **Request Handler**: `HandlerImpl` implements `HttpHandler` to intercept HTTP requests
- **Package Structure**: `web.org.perfmon4j.extras.wildfly8`
  - `PerfmonHandlerWrapper.java` - Configuration and handler factory
  - `HandlerImpl.java` - Core request monitoring logic
  - `RequestSkipLogTracker.java` - ThreadLocal tracker for conditional logging
- **Handler Chain Pattern**: Wraps next HttpHandler in chain to monitor request/response lifecycle
- **Recursive Request Handling**: Uses ThreadLocal counter to only monitor initial requests, not forwards
- **Validator Pattern**: Implements TriggerValidator for HTTP request, session, and cookie-based thread traces

## Stack Best Practices
- **Undertow Integration**: Uses Undertow 1.0.16.Final HttpHandler and HandlerWrapper APIs
- **ThreadLocal Usage**: Properly manages ThreadLocal state for recurse counts and skip log tracking
- **Configuration via Setters**: Bean-style configuration with getters/setters on PerfmonHandlerWrapper
- **Password Masking**: Automatically masks password query parameters in logs using regex pattern
- **NDC (Nested Diagnostic Context)**: Pushes request context to JBoss Logging NDC for correlation
- **Servlet Path Transformation**: Uses `ServletPathTransformer` to normalize request paths for monitoring
- **Conditional Timer Abort**: Supports aborting timers for images, redirects, and URL patterns
- **System Property Configuration**: Supports `web.org.perfmon4j.servlet.PerfMonFilter.SKIP_HTTP_METHOD_ON_LOG_OUTPUT`

## Anti-Patterns
- Avoid creating HandlerImpl directly; always use PerfmonHandlerWrapper.wrap()
- Do not modify ThreadLocal state outside the handler's try/finally blocks
- Never skip validator cleanup in finally blocks (lines 177-189)
- Avoid logging raw query strings without password masking
- Do not use abortTimerOnURLPattern for business logic; use skipTimerOnURLPattern instead
- Never call PerfMonTimer.stop() without corresponding start or abort in finally
- Avoid using SKIP_HTTP_METHOD_ON_LOG_OUTPUT in production without understanding log format impact

## Data Models
- **HttpServerExchange**: Undertow's request/response object containing headers, cookies, query params
- **PerfMonTimer**: Timer object tracking request duration and categorization
- **ServletRequestContext**: Undertow servlet context attachment for session access
- **ThreadTraceConfig Validators**: RequestValidator, SessionValidator, CookieValidator
  - Implement TriggerValidator interface
  - Validate HTTP request params, session attributes, and cookies against triggers
- **RequestSkipLogTracker**: ThreadLocal state holder for conditional log output
  - `skipLogOutput` boolean flag
  - Controlled via PERFMON4J_SKIP_LOG_FOR_REQUEST attribute

## Security & Configuration
- **Password Masking**: Regex pattern `[\\?|\\&]{1}password\\=([^\\&\\;]*)` masks password query params
- **Configuration Properties**:
  - `baseFilterCategory` - Base monitor category (default: "WebRequest")
  - `outputRequestAndDuration` - Enable request logging with timing
  - `abortTimerOnImageResponse` - Skip timing for image/* content types
  - `abortTimerOnRedirect` - Skip timing for 3xx redirects
  - `abortTimerOnURLPattern` - Regex pattern to abort timing
  - `skipTimerOnURLPattern` - Regex pattern to skip monitoring entirely
  - `pushURLOnNDC` - Push request URL to NDC
  - `pushCookiesOnNDC` - CSV list of cookie names to push to NDC
  - `pushSessionAttributesOnNDC` - CSV list of session attribute names to push to NDC
  - `pushClientInfoOnNDC` - Push client IP and X-Forwarded-For to NDC
  - `servletPathTransformationPattern` - Pattern for transforming servlet paths
- **Dependency Scope**: perfmon4j and Undertow dependencies are "provided" scope
- **JBoss Repository**: Requires JBoss Nexus repository for Undertow artifacts

## Commands & Scripts
- **Build**: `mvn clean install` (from wildfly8 directory)
- **Test**: `mvn test` (runs HandlerImplTest suite)
- **Package**: Creates perfmon4j-wildfly8-2.2.1-SNAPSHOT.jar
- **Parent Build**: Builds as part of perfmon4j-project parent POM
- **Test Execution**: Uses JUnit 4 with Mockito 5.3.0 for unit testing
- **Key Test Cases**:
  - `testSimpleMonitor()` - Basic request monitoring
  - `testAbortTimerOnImageResponse()` - Image response handling
  - `testAbortTimerOnRedirect()` - Redirect handling
  - `testServletPathTransformationPattern()` - Path transformation
  - `testThreadTraceHttpRequestTrigger()` - Request parameter triggers
  - `testBuildNDC()` - NDC context building
