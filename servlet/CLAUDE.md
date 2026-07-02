# servlet

## Overview
- Provides Java Servlet Filter implementations for performance monitoring of web requests in servlet containers
- Core components: `PerfMonFilter` (base filter) and `PerfMonNDCFilter` (NDC-enabled filter) for tracking HTTP request performance
- Integrates with PerfMon4j monitoring framework to time and categorize web requests
- Can be deployed as a standard servlet filter in web.xml or via Tomcat Valve
- Primary consumers: Java web applications running on servlet containers (Tomcat, Jetty, etc.)
- Integration points: Depends on `perfmon4j` core module for timing/monitoring and servlet-api for filter lifecycle

## Architecture & Patterns
- Dual package structure: `org.perfmon4j.servlet` (deprecated, backward compatibility) and `web.org.perfmon4j.servlet` (current)
- Filter chain pattern: Implements `javax.servlet.Filter` to intercept HTTP requests/responses
- Validator pattern: Uses `TriggerValidator` implementations (HttpRequestValidator, HttpSessionValidator, HttpCookieValidator) for conditional thread tracing
- Wrapper pattern: `ResponseWrapper` extends `HttpServletResponseWrapper` to detect redirects
- Builder pattern: `buildRequestDescription()` constructs request URLs with sanitized parameters
- ServletPathTransformer: Applies regex-based transformations to normalize dynamic URLs into monitoring categories

## Stack Best Practices
- Filter initialization: All configuration loaded via `FilterConfig.getInitParameter()` in `init()` method
- Timer lifecycle: Always use try-finally blocks to ensure `PerfMonTimer.stop()` or `PerfMonTimer.abort()` is called
- Request categorization: Override `buildMonitorCategory()` to customize how requests map to monitor categories
- Password protection: Parameter names containing "password" are automatically masked with "*******" in logs
- URL encoding: Uses `URLEncoder.encode()` with UTF-8 for parameter values in request descriptions
- Lazy monitor creation: Monitors only created when appenders are configured (prevents memory bloat from dynamic URLs)
- Thread-local validator cleanup: Validators pushed onto ThreadTraceConfig stack must be popped in finally blocks

## Anti-Patterns
- Avoid using deprecated classes in `org.perfmon4j.servlet` package (use `web.org.perfmon4j.servlet` instead due to classloading issues)
- Do not create monitors dynamically for every unique URL path (use ServletPathTransformer to normalize paths)
- Never skip popping validators from ThreadTraceConfig stack (causes validator stack leaks)
- Avoid hardcoding context paths in URL patterns when filter is used in Tomcat Valve mode
- Do not rely on monitor existence without checking appender configuration (monitors are lazily created)
- Never log sensitive request parameters without sanitization (check for "password" in param names)

## Data Models
- `PerfMonTimer`: Represents timing for a single request, created by `PerfMonTimer.start()`, stopped or aborted
- `HttpRequestValidator`: Validates HTTP request parameters against ThreadTraceConfig triggers
- `HttpSessionValidator`: Validates HTTP session attributes against ThreadTraceConfig triggers
- `HttpCookieValidator`: Validates HTTP cookies against ThreadTraceConfig triggers
- `ResponseWrapper`: Wraps HttpServletResponse to track redirect state
- Monitor categories: Hierarchical dot-separated paths like "WebRequest.circulation.checkout"
- ServletPathTransformer: Configurable regex-based transformation rules for URL normalization

## Security & Configuration
- Filter configuration parameters (set via web.xml `<init-param>`):
  - `BASE_FILTER_CATEGORY` (default: "WebRequest") - Root category for all web request monitors
  - `ABORT_TIMER_ON_REDIRECT` (boolean) - Skip timing on HTTP redirects
  - `ABORT_TIMER_ON_IMAGE_RESPONSE` (boolean) - Skip timing on image responses
  - `ABORT_TIMER_ON_URL_PATTERN` (regex) - Skip timing for URLs matching pattern
  - `SKIP_TIMER_ON_URL_PATTERN` (regex) - Skip filter entirely for URLs matching pattern
  - `OUTPUT_REQUEST_AND_DURATION` (boolean) - Log request details and duration
  - `SERVLET_PATH_TRANSFORMATION_PATTERN` - Regex pattern to normalize dynamic URLs
- Sensitive data handling: Password parameters automatically masked in logs and request descriptions
- X-Forwarded-For header captured in NDC for client IP tracking (PerfMonNDCFilter)
- No environment variables used; configuration via FilterConfig only
- PerfMonNDCFilter additional configuration:
  - `PUSH_URL_ON_NDC` (boolean) - Push request URL to Log4j NDC
  - `NDC_PUSH_CLIENT_INFO` (boolean) - Push client IP/X-Forwarded-For to NDC
  - `NDC_PUSH_COOKIES` (CSV or "*") - Push specified cookie values to NDC
  - `NDC_PUSH_SESSION_ATTRIBUTES` (CSV or "*") - Push session attributes to NDC

## Commands & Scripts
- Build: `mvn clean package` (from servlet directory or parent pom)
- Test: `mvn test` or `mvn surefire:test`
- Install locally: `mvn install`
- Test single class: `mvn test -Dtest=PerfMonFilterTest`
- Parent project build includes this module: `mvn clean package` (from root)
- Eclipse project files: `.project` and `.classpath` available for IDE import
