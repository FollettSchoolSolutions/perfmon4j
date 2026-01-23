# quarkus3x

## Overview
- Perfmon4j integration module for Quarkus 3.x framework applications
- Provides performance monitoring filters for REST endpoints using both Jakarta EE Container filters and Vert.x handlers
- Wraps the generic filter (`perfmon4j-generic-filter`) with Quarkus-specific adapters
- Supports reactive/async request processing with context propagation across threads
- Primary integration point: `PerfmonRouteFilter` (CDI Singleton) and `PerfmonFilter` (JAX-RS Provider)
- Integrates with Quarkus OpenTracing and Vert.x HTTP subsystems

## Architecture & Patterns
- **Filter implementations**: Two parallel approaches (Container API vs Vert.x handler) in `impl/` package
  - `PerfmonFilter`: Jakarta EE ContainerRequestFilter/ContainerResponseFilter for JAX-RS endpoints
  - `PerfmonHandler`: Vert.x Handler approach (currently disabled with `enabled = false` flag)
- **Adapter pattern**: `container.Request/Response` and `vertx.Request/Response` wrap framework-specific types to implement `HttpRequest/HttpResponse` interfaces
- **Async loading**: `QuarkusFilterLoader` extends `GenericFilterAsyncLoader` to defer initialization until configuration is available
- **Context propagation**: `TracingContextProvider` implements MicroProfile ThreadContextProvider SPI to propagate reactive context across async boundaries
- **Service loader**: META-INF/services registration for `ThreadContextProvider` enables automatic discovery
- **CDI integration**: `@Singleton` and `@Observes Filters` for lifecycle management

## Stack Best Practices
- Uses Jakarta EE 9+ APIs (jakarta.ws.rs, jakarta.inject, jakarta.enterprise) instead of legacy javax
- Leverages Quarkus Vert.x HTTP runtime for low-level filter registration
- Implements MicroProfile Context Propagation for reactive request tracking across threads
- SLF4J logging with scoped loggers (e.g., `org.perfmon4j.QuarkusFilter`)
- Jandex maven plugin generates bean index for faster CDI discovery
- Quarkus BOM (3.1.0.Final) in dependencyManagement for consistent version management
- All Quarkus dependencies use `provided` scope (container supplies at runtime)
- SingletonTracker pattern for lifecycle monitoring (`PerfmonFilter.singletonTracker`)

## Anti-Patterns
- Avoid mixing Container and Vert.x filter approaches (currently `PerfmonHandler.enabled = false`)
- Do not access request/response context outside of filter lifecycle methods
- Never block in async handlers or context propagation callbacks
- Do not create filter instances manually - let CDI/Quarkus manage lifecycle
- Avoid hardcoding reactive context IDs - use `TracingContextProvider.REQUEST_CONTEXT` ThreadLocal
- Do not catch generic Exception in response handling - log ClassCastException specifically (`Response.getStatus()` workaround)
- Never skip jandex indexing - required for Quarkus bean discovery

## Data Models
- **HttpRequest interface** (from generic-filter): abstraction for request data
  - `getServletPath()`, `getMethod()`, `getQueryString()`, `getQueryParameter()`, `getCookieValue()`
  - Implementations: `container.Request` (ContainerRequestContext) and `vertx.Request` (RoutingContext)
- **HttpResponse interface** (from generic-filter): abstraction for response data
  - `getStatus()`, `getHeader(String name)`
  - Implementations: `container.Response` (ContainerResponseContext) and `vertx.Response` (HttpServerResponse)
- **RequestContext** (inner class in TracingContextProvider): manages reactive context state
  - `context` field: String identifier (e.g., "ctx_123") generated via AtomicLong counter
  - `initContext()`, `clearContext()`, `getContext()`, `setContext()` for lifecycle management
- **AsyncFinishRequestCallback**: callback interface for completing async request monitoring
- No database models - purely request/response tracking

## Security & Configuration
- Configuration loaded via `GenericFilterAsyncLoader` from Perfmon4j system (details in generic-filter module)
- No direct environment variables in this module - delegates to base Perfmon4j configuration
- `FilterParams` passed to `QuarkusFilter` constructor controls monitoring behavior
- Context propagation ensures monitoring spans follow requests across thread boundaries
- Cookie values extracted from requests (`getCookieValue()`) - be cautious with sensitive cookies
- Session attributes intentionally return null (`getSessionAttribute()`) - session tracking not supported
- ClassCastException handlers in `container.Response` protect against incompatible response types

## Commands & Scripts
- **Build**: `mvn clean install` (from quarkus3x directory or parent)
- **Run tests**: `mvn test` (uses mockito-core 5.3.0)
- **Generate Jandex index**: `mvn jandex:jandex` (automatically runs during build)
- **Package**: `mvn package` (creates JAR for inclusion in Quarkus apps)
- **Integration**: Add dependency to Quarkus application's pom.xml:
  ```xml
  <dependency>
    <groupId>org.perfmon4j</groupId>
    <artifactId>perfmon4j-quarkus3x</artifactId>
    <version>2.2.1-SNAPSHOT</version>
  </dependency>
  ```
- **Verify singleton registration**: Check logs for `SingletonTracker` messages on startup
