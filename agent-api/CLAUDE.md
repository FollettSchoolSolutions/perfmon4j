# agent-api

## Overview
- Provides the public API for Perfmon4j Java instrumentation agent integration
- Contains stub implementations that are bytecode-instrumented at runtime by the Perfmon4j JavaAgent
- Operates in two modes: "Unattached" (no-op stubs) and "Attached" (agent rewrites classes with actual implementations)
- Primary consumers: application developers who want to instrument their Java code for performance monitoring
- Key domain concepts: monitors, timers, snapshots, emitters, reactive contexts
- Integration point: Classes in this module are rewritten by JavassistRuntimeTimerInjector in the main agent module

## Architecture & Patterns
- Package structure:
  - `api.org.perfmon4j.agent` - Core API classes (PerfMon, PerfMonTimer, registries)
  - `api.org.perfmon4j.agent.instrument` - Annotation-based instrumentation markers
  - `api.org.perfmon4j.agent.impl` - Helper classes for bridging API and agent implementations
  - `api.org.perfmon4j.agent.util` - Utility classes for singleton tracking
- Dual-mode pattern: All classes check `isAttachedToAgent()` to determine if bytecode rewriting occurred
- Wrapper pattern: EmitterInstrumentationHelper wraps API objects with agent implementations using delegate pattern
- Zero-dependency design: No external dependencies, ensuring minimal classpath pollution
- Registration pattern: EmitterRegistry and POJOSnapShotRegistry provide centralized registration with weak reference support

## Stack Best Practices
- All public API classes contain stub implementations that return default/no-op values
- Methods are designed to be replaced via bytecode manipulation (Javassist) at class load time
- Annotations use RUNTIME retention for agent discovery during instrumentation
- Static factory methods (getMonitor, start) prevent direct instantiation and enable agent interception
- Reactive context support with explicit contextID parameters for cross-thread timing in reactive frameworks
- isDynamicKey parameters prevent unbounded monitor creation from dynamic values
- Comments document which JavassistRuntimeTimerInjector methods perform bytecode rewriting

## Anti-Patterns
- Do NOT add instance state to stub classes - they will be replaced by agent
- Do NOT add dependencies to this module - it must remain zero-dependency
- Do NOT implement actual functionality in stub methods - agent replaces them entirely
- Do NOT use constructors directly - use static factory methods (PerfMon.getMonitor, PerfMonTimer.start)
- Do NOT create monitors/timers with user input as keys without isDynamicKey=true (prevents memory leaks)
- Do NOT modify signature of public methods - instrumented code depends on exact signatures
- Do NOT rely on method implementations - they are placeholders until agent attaches

## Data Models
- PerfMon: Represents a performance monitor identified by String key (monitor name)
- PerfMonTimer: Token representing an active timing session, supports both traditional and reactive contexts
- EmitterData: Builder for adding typed data fields (long, int, double, float, boolean, String)
- EmitterController: Controls emission lifecycle with initData/emit operations
- SimpleTriggerValidator: Validation interface for thread trace triggers (HTTP request/session/cookie based)
- Reactive context: Supports explicit (with contextID) and implicit (auto-created) reactive contexts
- Registration modes: weak vs strong references, optional instance naming

## Security & Configuration
- No environment variables used in this API module
- No secrets or sensitive data handling - purely a monitoring instrumentation API
- No authentication/authorization - security is application responsibility
- No configuration files - all configuration happens in the main agent module
- Thread safety: Designed for concurrent access (static methods, weak references)
- Weak reference pattern prevents memory leaks when objects are de-registered

## Commands & Scripts
- Build this module:
  ```
  mvn clean package
  ```
- Install to local Maven repository:
  ```
  mvn clean install
  ```
- This module has no tests (stub implementations only)
- Packaging: Creates perfmon4j-agent-api-{version}.jar
- Deployment: This JAR is added to application classpath, agent JAR is added via -javaagent
