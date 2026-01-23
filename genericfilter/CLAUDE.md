# genericfilter

## Overview
- Provides a framework-agnostic HTTP filter for performance monitoring that can be integrated into any Java web container
- Supports two usage modes: traditional synchronous filter chain and async/reactive request handling (e.g., Vert.x)
- Wraps HTTP requests with PerfMon4j timers to measure request duration and SQL time
- Transforms servlet paths into hierarchical monitoring categories (e.g., `/api/users/123` becomes `WebRequest.api.users`)
- Key consumers: servlet, tomcat7, wildfly8, quarkus2x, quarkus3x modules which provide container-specific implementations
- Integrates with perfmon4j-agent-api for timer management and thread trace triggers
- Provides configurable request filtering, URL pattern matching, and path transformation rules

## Architecture & Patterns
- **Abstract base class pattern**: `GenericFilter` is abstract, requiring subclasses to implement logging methods
- **Interface-based abstraction**: `HttpRequest`, `HttpResponse`, and `HttpRequestChain` interfaces decouple from specific servlet APIs
- **Async callback pattern**: `AsyncFinishRequestCallback` inner class supports reactive/async frameworks
- **Lazy initialization**: `GenericFilterAsyncLoader` retries filter installation until perfmonconfig.xml is loaded
- **Recursion protection**: Uses `ThreadLocal<AtomicInteger>` to track recursive filter invocations (e.g., request forwards)
- **Package structure**: All classes in `web.org.perfmon4j.extras.genericfilter` namespace
- **Validator pattern**: `RequestValidator`, `SessionValidator`, `CookieValidator` for thread trace trigger matching
- **Transformation pipeline**: `ServletPathTransformer` applies regex-based path transformations with wildcard support

## Stack Best Practices
- Uses Java 8+ features: lambdas, method references (`callback::finishRequest`), streams
- **ThreadLocal usage**: Properly initializes ThreadLocal with `initialValue()` override for thread safety
- **Immutable configuration**: Filter parameters set at construction time via `FilterParams` interface
- **Null object pattern**: Returns `PerfMonTimer.getNullTimer()` when monitoring is disabled
- **Properties-based configuration**: Loads settings from `Properties` with prefix `perfmon4j.bootconfiguration.servlet-valve.*`
- **System property overrides**: Supports JVM property overrides for loader behavior (e.g., `RETRY_LOAD_WAIT_MILLIS`)
- **Abstract factory methods**: Package-private methods (`startTimer`, `abortTimer`, `stopTimer`, `isPerfMonConfigured`) enable test mocking
- **Atomic reference pattern**: Uses `AtomicReference<GenericFilter>` for thread-safe lazy initialization

## Anti-Patterns
- **Avoid direct servlet API dependencies**: This module intentionally uses interface abstractions to remain container-agnostic
- **Don't bypass recursion protection**: The recurse counter prevents double-monitoring of forwarded requests
- **Don't log passwords**: Always use `maskPassword()` when logging query strings containing `password=` parameters
- **Don't skip async cleanup**: Always call `finishRequest()` on `AsyncFinishRequestCallback` to pop validators and complete timers
- **Don't create filter instances per request**: Filter instances are expensive, reuse a single instance per application
- **Don't override timer methods in production**: Package-private timer methods are only for test implementations
- **Avoid hardcoded category names**: Use `baseFilterCategory` configuration instead of hardcoding "WebRequest"

## Data Models
- **FilterParams**: Configuration interface with implementations `FilterParamsVO` (mutable) and factory methods
  - `baseFilterCategory`: Root monitor category (default: "WebRequest")
  - `abortTimerOnImageResponse`: Skip timing for image responses (default: true)
  - `abortTimerOnRedirect`: Skip timing for 3xx redirects (default: true)
  - `abortTimerOnURLPattern`: Regex pattern to abort timing for matching URLs
  - `skipTimerOnURLPattern`: Regex pattern to completely skip monitoring
  - `outputRequestAndDuration`: Enable request logging with duration/SQL time (default: true)
  - `servletPathTransformationPattern`: Comma-separated transformations (e.g., `**/api/**=>$API/`)
- **HttpRequest**: Minimal interface exposing servlet path, method, query string, session attributes, cookies
- **HttpResponse**: Minimal interface for status code and headers (used for abort conditions)
- **HttpRequestChain**: Chain-of-responsibility pattern for filter chaining
- **RequestSkipLogTracker**: ThreadLocal tracker to suppress log output for specific requests via `PERFMON4J_SKIP_LOG_FOR_REQUEST` attribute

## Security & Configuration
- **Password masking**: Automatically masks `password=` query parameters in logs using `passwordParamPattern` regex
- **Configuration properties**: Loaded from `perfmon4j.bootconfiguration.servlet-valve.*` namespace
  - Example: `perfmon4j.bootconfiguration.servlet-valve.baseFilterCategory=WebRequest`
- **No environment variables**: Configuration comes from PerfMon boot settings or Properties objects
- **Thread trace validation**: Supports HTTP request parameter, session attribute, and cookie-based triggers
  - Trigger format: `HTTP:<param>=<value>`, `HTTP_SESSION:<attr>=<value>`, `HTTP_COOKIE:<name>=<value>`
- **Reactive context isolation**: Async mode supports `reactiveContext` parameter for request-scoped validation
- **No authentication**: Filter focuses on monitoring, not security enforcement

## Commands & Scripts
- **Build**: `mvn clean install` from genericfilter directory
- **Run tests**: `mvn test` (runs GenericFilterTest and GenericFilterAsyncLoaderTest)
- **Test with specific Java**: Requires Java 8+ (uses lambda expressions)
- **Maven coordinates**: `org.perfmon4j:perfmon4j-generic-filter:2.2.1-SNAPSHOT`
- **Dependencies**:
  - `perfmon4j-agent-api` (compile scope)
  - `perfmon4j` (test scope only)
- **System property for testing**:
  - `web.org.perfmon4j.servlet.PerfMonFilter.SKIP_HTTP_METHOD_ON_LOG_OUTPUT=true` to exclude HTTP method from logs
  - `GenericFilterAsyncLoader.RETRY_LOAD_WAIT_MILLIS=15000` (default: 15 seconds)
  - `GenericFilterAsyncLoader.MAX_RETRY_LOAD_ATTEMPTS=12` (default: 12 attempts)
  - `GenericFilterAsyncLoader.LOAD_DEFAULT_AFTER_MAX_ATTEMPTS=true` (default: true)
