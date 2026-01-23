# reportconsole

## Overview
- Desktop Swing application for visualizing Perfmon4j performance monitoring data from SQL databases
- Primary user: developers and operations teams analyzing Java application performance metrics
- Provides tree-based navigation of performance categories and time-series charting capabilities
- Connects to Perfmon4j databases via JDBC to query interval timer data and throughput metrics
- Uses JFreeChart library for rendering time-series graphs of performance data
- Integrates with core perfmon4j library (dependency) for data access utilities and value objects
- Standalone JAR-packaged application with MigLayout for UI and JFreeChart for visualization

## Architecture & Patterns
- Classic MVC pattern with clear separation: `model/`, `gui/`, `controller/` packages
- Singleton pattern used for Model and Controller classes (static instance access)
- Tree-based hierarchical data model: `P4JConnectionList` -> `P4JConnection` -> `Category` -> `IntervalCategory`
- Observer pattern: `NodeChangeListenerImpl` listens to tree node selection changes
- Event-driven Swing architecture with action listeners and tree selection listeners
- `src/main/java/org/perfmon4j/reporter/` contains all application code organized by layer
- `src/main/resources/org/perfmon4j/images/` stores UI icons (exit, new-connection, close-connection)
- Generic tree node structure (`P4JTreeNode<P,T>`) provides type-safe parent-child relationships
- JDBC connection management with custom driver loading from JAR files

## Stack Best Practices
- Java Swing for desktop GUI with Metal look-and-feel (cross-platform)
- MigLayout library for flexible grid-based UI layout (version 3.7.3)
- JFreeChart library (1.0.13) for time-series chart rendering and data visualization
- Singleton pattern for global Model and Controller access: `Model.getModel()`, `Controller.getController()`
- Resource loading via classpath: icons loaded as `/org/perfmon4j/images/*.png`
- TreeNode interface implementation for Swing JTree integration
- JDBC connection pooling avoided; connections created on-demand and closed via JDBCHelper
- Action classes extend `ActionWithIcon` for consistent toolbar/menu button creation
- Exception handling via `ExceptionDialog.showDialog()` for user-friendly error display

## Anti-Patterns
- Avoid hardcoded database connection defaults (currently in `AEDConnectionDialog`: localhost, sa user, c:/data paths)
- Do not bypass connection validation in refresh operations (SQLException handling is present but minimal)
- Avoid mixing business logic in GUI classes (some chart creation logic exists in `App.java`)
- Do not use `System.out.println()` for logging (found extensively in `TreeListener.java`)
- Avoid static mutable state beyond singletons (result field in `AEDConnectionDialog` is static and mutable)
- Do not suppress exceptions without user notification (some catches print to console only)
- Avoid exposing passwords in plain text fields (uses JPasswordField but stores as String)

## Data Models
- `P4JTreeNode<P,T>`: Abstract generic tree node with parent type P and child type T, implements Swing TreeNode
- `P4JConnectionList`: Root tree node containing list of database connections
- `P4JConnection`: Represents a database connection URL, contains categories as children
- `ReportSQLConnection`: Extends P4JConnection with JDBC credentials (userName, password, schema, driverClass, jarFileName)
- `Category<T>`: Generic category node for organizing hierarchical performance data
- `IntervalCategory`: Leaf node with databaseID, provides time-series data via `getTimeSeries()`
- `ResponseInfo` (from perfmon4j-utils): Value object with startTime, throughput, averageDuration
- `DataSeries` enum: MAX_THREADS, THROUGH_PUT, HITS, COMPLETIONS, MAX_DURATION, MIN_DURATION, AVERAGE_DURATION
- Tree structure validated via `Type` enum: P4JConnectionList, P4JConnection, P4JCategory, P4JIntervalCategory

## Security & Configuration
- JDBC credentials stored in-memory only, entered via dialog (no configuration file)
- Default connection values hardcoded in `AEDConnectionDialog` (localhost SQL Server with sa user)
- Dynamic JDBC driver loading from user-specified JAR file using custom ClassLoader caching
- No encrypted storage of passwords; password field is JPasswordField but value stored as plain String
- Connection validation performed on creation via `ReportSQLConnection.refresh()` method
- SQL injection risk mitigated via parameterized queries in JDBCHelper utility methods
- No authentication/authorization layer; relies on database-level access controls
- File system access required for JDBC driver JAR selection (uses JFileChooser)

## Commands & Scripts
- Build: `mvn clean install` (standard Maven JAR packaging)
- Test: `mvn test` (uses maven-surefire-plugin 2.17)
- Run: `java -jar target/perfmon4j-reportconsole-2.2.1-SNAPSHOT.jar`
- Package: `mvn package` (creates executable JAR via maven-jar-plugin 2.5)
- Main class: `org.perfmon4j.reporter.App`
- Dependencies: Requires perfmon4j-base artifact, miglayout 3.7.3, jfreechart 1.0.13
- No deployment scripts; desktop application distributed as standalone JAR
