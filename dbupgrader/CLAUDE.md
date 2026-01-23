# dbupgrader

## Overview
- Database schema management tool for Perfmon4j using Liquibase
- Creates or upgrades Perfmon4j database schemas across multiple database vendors (Derby, PostgreSQL, MySQL, Oracle, SQL Server)
- Packages as standalone executable JAR (`perfmon4j-dbupgrader-{version}.run.jar`) with all dependencies shaded
- Main entry point: `org.perfmon4j.dbupgrader.UpdateOrCreateDb`
- Supports versioned schema migrations from legacy SQL scripts through Liquibase change logs
- Handles backward compatibility by detecting and syncing pre-Liquibase databases (versions 1.x-3.x)
- Consumed by: Database administrators and deployment automation for Perfmon4j installations

## Architecture & Patterns
- Entry point: `src/main/java/org/perfmon4j/dbupgrader/UpdateOrCreateDb.java` - CLI application for database management
- Utility class: `src/main/java/org/perfmon4j/dbupgrader/UpdaterUtil.java` - JDBC operations, driver loading, schema introspection
- Database customization: `src/main/java/liquibase/database/ext/ForceLowerCasePostgressDatabase.java` - PostgreSQL compatibility for legacy schemas
- Liquibase change logs in `src/main/resources/org/perfmon4j/`:
  - `update-change-master-log.xml` - Master change log that includes all versioned change logs
  - `initial-change-log.xml` - Base schema (tables: P4JCategory, P4JIntervalData, etc.)
  - `version-2-change-log.xml` through `version-7-change-log.xml` - Incremental schema updates
  - `thirdParty/` subdirectory for vendor-specific extensions (e.g., `FSS/change-log.xml`)
- Dynamic JDBC driver loading via URLClassLoader for external JAR files
- Detection logic to identify legacy databases and sync them with Liquibase change logs

## Stack Best Practices
- Use Liquibase XML change logs for all schema modifications
- Leverage database-agnostic Liquibase syntax with `<modifySql>` for vendor-specific tweaks (e.g., VARCHAR length for MySQL)
- Each major version gets a separate change log file (`version-N-change-log.xml`)
- Include database version labels (`databaseLabel` author with ID like `0007.0`) to mark releases
- Dynamic driver loading allows external JDBC JARs without classpath pollution
- Use `clearChecksums` parameter to force Liquibase to recalculate checksums after manual changes
- Close resources explicitly with `UpdaterUtil.closeNoThrow()` pattern to avoid leaks
- Support both direct execution and SQL script generation (`sqlOutputScript` parameter)

## Anti-Patterns
- DO NOT hardcode database credentials or connection strings
- NEVER alter schema directly without Liquibase change logs (breaks upgrade path)
- AVOID breaking Liquibase checksums - use `clearChecksums` if necessary after manual fixes
- DO NOT assume schema names - always use `setDefaultSchemaName()` and respect user-provided schema parameter
- NEVER skip backward compatibility checks for legacy databases (pre-Liquibase versions)
- AVOID using database-specific SQL without Liquibase vendor conditionals (`dbms="..."`)
- DO NOT forget to close Database objects properly (use `closeDatabaseNoThrow()`)

## Data Models
- Perfmon4j core tables created by `initial-change-log.xml`:
  - `P4JCategory` - Performance monitoring categories
  - `P4JIntervalData` - Time-series interval performance data (primary key: `IntervalID` as BIGINT)
  - `P4JIntervalThreshold` - Threshold configurations for intervals
  - `P4JGarbageCollection` - JVM garbage collection metrics
  - `P4JSystem` - System identifiers (default system row with SystemID=1)
  - `P4JVMSnapshot` - JVM snapshot metrics (added columns: `systemCpuLoad`, `processCpuLoad` in version 4)
  - `P4JDatabaseIdentity` - Unique database identifier (version 5)
  - `P4JGroup`, `P4JGroupSystemJoin` - System grouping (version 6)
  - `P4JHystrixKey`, `P4JHystrixCommand`, `P4JHystrixThreadPool` - Hystrix metrics (version 7)
  - `P4JAppenderControl` - Appender control configuration (version 7)
- Liquibase tracking tables: `DATABASECHANGELOG`, `DATABASECHANGELOGLOCK`
- Schema detection uses `doesTableExist()`, `doesColumnExist()`, `doesIndexExist()` utility methods
- Unique database identity generated as format: `XXXX-XXXX` (8 random consonants with hyphen)

## Security & Configuration
- Command-line parameters (all key=value format):
  - `driverClass` (required) - JDBC driver class name (e.g., `org.postgresql.Driver`)
  - `jdbcURL` (required) - JDBC connection URL
  - `driverJarFile` (required) - Path to JDBC driver JAR, or `EMBEDDED` for classpath drivers
  - `userName` (optional) - Database username
  - `password` (optional) - Database password (requires `userName` if provided)
  - `schema` (optional) - Database schema name (uses default if not specified)
  - `clearChecksums` (optional) - Boolean to clear Liquibase checksums (default: true)
  - `sqlOutputScript` (optional) - Path to generate SQL script instead of executing
  - `thirdPartyExtensions` (optional) - Comma-separated list of third-party extensions (e.g., `FSS`)
- Credentials passed via command line only (no config files)
- Supports embedded Derby database for testing without external dependencies
- Default schema detection handles SQL Server `dbo` schema automatically
- System property `P4J_UPDATER_DEFAULT_SCHEMA` can override default schema detection

## Commands & Scripts
- Build shaded JAR:
  ```bash
  mvn clean package
  # Creates: target/perfmon4j-dbupgrader-{version}.run.jar
  ```
- Run database upgrade:
  ```bash
  java -jar perfmon4j-dbupgrader-{version}.run.jar \
    driverClass=org.postgresql.Driver \
    jdbcURL=jdbc:postgresql://localhost:5432/perfmon4j \
    driverJarFile=/path/to/postgresql.jar \
    userName=dbuser \
    password=dbpass \
    schema=perfmon4j
  ```
- Generate SQL upgrade script (without executing):
  ```bash
  java -jar perfmon4j-dbupgrader-{version}.run.jar \
    driverClass=org.postgresql.Driver \
    jdbcURL=jdbc:postgresql://localhost:5432/perfmon4j \
    driverJarFile=/path/to/postgresql.jar \
    userName=dbuser \
    password=dbpass \
    sqlOutputScript=./upgrade.sql
  ```
- Run tests:
  ```bash
  mvn test
  # Uses embedded Apache Derby database (derby-{version}.jar in test scope)
  ```
- Apply third-party extensions:
  ```bash
  java -jar perfmon4j-dbupgrader-{version}.run.jar \
    [standard parameters] \
    thirdPartyExtensions=FSS,OtherExtension
  ```
