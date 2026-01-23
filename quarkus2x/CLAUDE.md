# quarkus2x

## Overview
- Perfmon4j integration module for Quarkus 2.x framework (version 2.16.7.Final)
- Provides HTTP request/response performance monitoring for Quarkus REST services
- Implements both JAX-RS container filters and Vert.x route handlers for flexible integration
- Supports reactive/async request processing with context propagation across threads
- Extends the generic filter framework (`perfmon4j-generic-filter`) with Quarkus-specific adapters
- Primary consumers: Quarkus 2.x applications requiring performance monitoring and request tracing
- Integration point: Uses `org.eclipse.microprofile.context.spi.ThreadContextProvider` for reactive context management

## Architecture & Patterns
- **Package structure**: Single package `web.org.perfmon4j.extras.quarkus.filter` with `impl` sub-packages for different integration approaches
- **Dual integration strategy**: Provides both JAX-RS filter (`PerfmonFilter`) and Vert.x handler (`PerfmonHandler`) implementations
  - JAX-RS filter: Active implementation using `ContainerRequestFilter` and `ContainerResponseFilter`
  - Vert.x handler: Experimental/disabled implementation at the Vert.x routing layer (`enabled = false` in `PerfmonHandler`)
- **Adapter pattern**: Separate adapters for container-based (`container/Request`, `container/Response`) and Vert.x-based (`vertx/Request`, `vertx/Response`) HTTP abstractions
- **Async lifecycle management**: Uses `AsyncFinishRequestCallback` pattern to track async request start/finish across thread boundaries
- **Context propagation**: `TracingContextProvider` implements MicroProfile ThreadContextProvider SPI for reactive context management
- **Lazy initialization**: `QuarkusFilterLoader` extends `GenericFilterAsyncLoader` for deferred filter initialization
- **CDI integration**: `PerfmonRouteFilter` is a `@Singleton` that observes `Filters` events to register handlers

## Stack Best Practices
- **Quarkus 2.x conventions**: Uses Quarkus BOM (2.16.7.Final) for dependency management
- **CDI event observers**: Uses `@Observes Filters` pattern for runtime filter registration in `PerfmonRouteFilter.init()`
- **Jandex indexing**: Uses `jandex-maven-plugin` to generate CDI bean index (`META-INF/jandex.idx`) required for Quarkus
- **MicroProfile Context Propagation**: Implements `ThreadContextProvider` SPI registered via `META-INF/services/org.eclipse.microprofile.context.spi.ThreadContextProvider`
- **Reactive context management**: Uses `ThreadLocal<RequestContext>` with proper init/clear lifecycle in `TracingContextProvider`
- **SLF4J logging**: All components use SLF4J for consistent logging (not JUL or Log4j directly)
- **Singleton tracking**: Uses `SingletonTracker.getSingleton().register()` for monitoring filter instantiation
- **Provided scope dependencies**: Quarkus runtime dependencies (`quarkus-vertx-http`, `quarkus-smallrye-opentracing`) marked as `provided` scope

## Anti-Patterns
- **Avoid enabling PerfmonHandler**: The Vert.x handler implementation is explicitly disabled (`enabled = false`) due to incomplete context propagation for synchronous requests
- **Don't rely on session attributes**: Both request adapters return `null` for `getSessionAttribute()` - sessions not supported in this integration
- **Avoid creating new PerfmonRouteFilter instance**: In `PerfmonRouteFilter.init()`, line 17 creates `new PerfmonRouteFilter()` instead of using `this` - potential singleton violation
- **Don't assume single-threaded execution**: Reactive/async execution means request start and finish may occur on different threads
- **Avoid direct PerfMon API calls**: Use the GenericFilter abstraction layer instead of calling PerfMon directly (except in TracingContextProvider)

## Data Models
- **RequestContext**: Internal context object with unique ID (`ctx_<id>`) for tracking requests across reactive thread switches
  - `context: String` - The current context ID
  - `initContext()` - Generates new context ID using `AtomicLong.incrementAndGet()`
  - `clearContext()` - Dissociates reactive context via `PerfMon.dissociateReactiveContextFromCurrentThread()`
  - `getContext()/setContext()` - Moves context to current thread via `PerfMon.moveReactiveContextToCurrentThread()`
- **Request adapters**: Implement `HttpRequest` interface with different backing types
  - `container.Request` - Wraps `ContainerRequestContext` (JAX-RS)
  - `vertx.Request` - Wraps `RoutingContext` (Vert.x)
  - Common methods: `getServletPath()`, `getMethod()`, `getQueryString()`, `getQueryParameter()`, `getCookieValue()`
- **Response adapters**: Implement `HttpResponse` interface
  - `container.Response` - Wraps `ContainerResponseContext` (JAX-RS)
  - `vertx.Response` - Wraps `RoutingContext` (Vert.x)
  - Common methods: `getStatus()`, `getHeader()`
- **AsyncFinishRequestCallback**: Callback interface from GenericFilter for completing async requests
- **FilterParamsVO**: Value object for filter configuration parameters (from genericfilter module)

## Security & Configuration
- **No environment variables**: Filter configuration comes from Perfmon4j system configuration, not local env vars
- **No secrets in code**: No hardcoded credentials or sensitive data
- **Thread context isolation**: Each request gets unique context ID to prevent cross-request data leakage in reactive scenarios
- **Context cleanup**: `clearContext()` ensures proper dissociation when request completes to prevent memory leaks
- **Provided dependencies**: Runtime dependencies are provided by Quarkus, reducing deployment attack surface
- **No authentication logic**: Filter focuses on monitoring; authentication/authorization handled by Quarkus framework

## Commands & Scripts
- **Build this module**: `mvn clean install` (from quarkus2x directory)
- **Build with parent**: `mvn clean install` (from project root includes this module)
- **Run tests**: `mvn test` (uses mockito-core 5.3.0)
- **Generate Jandex index**: Automatically runs during build via `jandex-maven-plugin`
- **Package artifact**: `mvn package` produces `perfmon4j-quarkus2x-2.2.1-SNAPSHOT.jar`
- **Skip this module**: `mvn clean install -pl '!quarkus2x'` (from project root)
- **Verify ThreadContextProvider registration**: Check `target/classes/META-INF/services/org.eclipse.microprofile.context.spi.ThreadContextProvider` exists after build
