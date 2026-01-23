# utils

## Overview
- Standalone utility module providing helper tools for PerfMon4j log analysis and visualization
- Contains command-line utilities for parsing TextAppender logs and displaying thread traces
- Provides JFreeChart-based charting capabilities for performance data visualization
- Primary consumers are developers and operations teams analyzing PerfMon4j output files
- Minimal dependencies on core perfmon4j (provided scope), making utilities easily portable
- Three main functional areas: log parsing (parsers), SQL trace visualization (sql), and chart generation (chart)

## Architecture & Patterns
- Package structure: `org.perfmon4j.utils.{parsers|sql|chart}`
  - `parsers/` - ParseTextAppenderLog for converting text logs to CSV format
  - `sql/` - PrintThreadTrace and TraceRow for database thread trace visualization
  - `chart/jfree/` - ChartBuilderImpl for JFreeChart-based performance charts
- Each utility is self-contained with main() methods for standalone execution
- Asynchronous processing pattern in ParseTextAppenderLog using ArrayBlockingQueue for I/O efficiency
- Immutable data objects (TraceRow) with static factory methods for JDBC ResultSet mapping
- Chart configuration stub (ChartConfig.java) exists but is not yet implemented

## Stack Best Practices
- Java standard I/O patterns: BufferedReader, PrintStream, try-with-resources equivalent
- Regex-based parsing with compiled Pattern constants for performance (HEADER_PATTERN, CATEGORY_PATTERN, etc.)
- DateFormat with SimpleDateFormat for timestamp conversion (DATE_FORMAT_STRING, CSV_DATE_FORMAT_STRING)
- JUnit 3.x style testing (extends TestCase) with comprehensive unit tests for parsers
- Maven jar plugin packaging for standalone utilities
- JFreeChart integration for time-series visualization (TimeSeries, TimeSeriesCollection, XYPlot)
- JDBC Helper pattern from org.perfmon4j.util.JDBCHelper for database connections
- Producer-consumer pattern with dedicated Writer thread in ParseTextAppenderLog

## Anti-Patterns
- Hardcoded file paths in main() methods (e.g., "/media/sf_shared/NoBackup/tmp.txt") - examples only, remove before production use
- Hardcoded traceID value (line 54 in PrintThreadTrace: `targetTraceID = 356043;`) overrides command-line argument
- Missing resource cleanup in some cases - ensure JDBC connections and file handles are properly closed
- No logging framework - uses System.out and printStackTrace() for error reporting
- Thread.currentThread().sleep() commented out rather than removed (line 56-60 in ParseTextAppenderLog)
- ChartConfig.java is essentially empty - incomplete implementation

## Data Models
- **IntervalVO** (ParseTextAppenderLog.IntervalVO)
  - Represents a single monitoring interval from TextAppender output
  - Fields: startTime, endTime, category, maxActiveThreads, throughput, averageDuration, medianDuration, standardDeviation, maxDuration, minDuration, totalHits, totalCompletions
  - Optional SQL metrics: sqlAverageDuration, sqlStandardDeviation, sqlMaxDuration, sqlMinDuration
  - Validation: Automatically adjusts startTime if endTime < startTime (crosses midnight boundary)
  - CSV serialization: buildCSVHeader() and toCSV(systemName) for data export
- **TraceRow** (org.perfmon4j.utils.sql.TraceRow)
  - Immutable representation of a thread trace database row
  - Fields: systemName, categoryName, rowID, parentRowID, duration, sqlDuration, startTime, endTime, children list
  - Factory method: fromResultSet() for JDBC mapping
  - Tree structure: parent-child relationships via parentRowID for hierarchical trace display
- **ChartConfig** (org.perfmon4j.utils.chart.ChartConfig)
  - Stub class with only title field - not yet implemented

## Security & Configuration
- **Database Credentials**: PrintThreadTrace accepts username/password as command-line args (args[3], args[4]) - avoid hardcoding
- **JDBC Driver Loading**: Uses JDBCHelper.createJDBCConnection() with external driver jar path for database connectivity
- **File Paths**: All utilities accept file paths as parameters - validate and sanitize input paths
- **No Environment Variables**: Current implementation uses command-line args only
- **SQL Injection**: Uses parameterized SQL queries with literal values (safe in current implementation)
- **Chart Output**: ChartBuilderImpl writes PNG files to specified paths - ensure write permissions
- N/A: No authentication/authorization mechanisms (standalone CLI tools)

## Commands & Scripts
- **Build**: `mvn clean package` (from utils directory)
- **Test**: `mvn test` or `mvn surefire:test`
- **Parse TextAppender Log**:
  ```bash
  java -cp perfmon4j-utils-2.2.1-SNAPSHOT.jar org.perfmon4j.utils.parsers.ParseTextAppenderLog <inputFile> <outputFile> <systemName>
  ```
- **Print Thread Trace**:
  ```bash
  java -cp perfmon4j-utils-2.2.1-SNAPSHOT.jar org.perfmon4j.utils.sql.PrintThreadTrace <driverJarFile> <jdbcURL> <driverClassName> <userName> <password> <targetTraceID>
  ```
- **Generate Chart** (example only):
  ```bash
  java -cp perfmon4j-utils-2.2.1-SNAPSHOT.jar:jfreechart-1.0.19.jar org.perfmon4j.utils.chart.jfree.ChartBuilderImpl
  ```
- **Run Single Test**: `mvn test -Dtest=ParseTextAppenderLogTest`
- **Skip Tests**: `mvn package -DskipTests`
