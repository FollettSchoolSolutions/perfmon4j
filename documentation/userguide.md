---
layout: default
title: User Guide
permalink: /documentation/userguide/
showInHeader: false
---

# Purpose

Perfmon4j has been successfully deployed in hundreds of production java systems over the last 5 years. It has proven to be a highly successful tool to measure performance while production servers are under load.

>> Perfmon4j has been used to monitor thousands of Java based application servers in production over the last 10 years.

We have found Perfmon4j typically enables us to diagnose performance related issues in hours instead of weeks. This is accomplished by the ability to deploy Perfmon4j in a very low overhead dormant state. If/when performance issues occur it is possible to activate monitoring on targeted portions of the application code to isolate any bottlenecks. The overhead of this targeted monitoring is minimal allowing measurements to be obtained as the problems are occurring. Since monitoring can be configured at runtime there is no need for inconvenience users with an application restart.


##How it works?

At a base level Perfmon4j groups performance measurements into configurable intervals. Based on our experience monitoring production application servers it is not typically useful to know that the response time for method MyClass.doSomething() has averaged 57 milliseconds since the application server was started. The things we are more likely interested in include:

* What is the Performance of the method while it is under its peak load?
* What is its peak load?
	* What time of day does the peak load typically occur?
* If/when the method slows down, why does it slow down? Is it in the java layer or while accessing the database?
* Is this method a bottleneck? How many concurrent processing threads does this method handle?

Perfmon4j attempts to answer these questions by providing a set of tools that enable you to instrument code (and more such as dynamic application behavior (i.e. Web Request Duration)) and apply configurable output based on any meaningful duration you desire. Perfmon4j does this through its Monitor + Appender architecture. The monitors collect data, while the appenders harvest at regularly defined intervals. On top of this Perfmon4j also provides SnapShot monitors, which allow you to monitor internal system variables (i.e. JMX Attributes, static counters, etc) and Thread Trace Sampling which provides a detailed stack trace, including performance measurements of the internal workings of application code.

>> Perfmon4j peak load monitoring provides essential measuresment to use to estimates usage patterns for Performance/Load testing and capacity planning. 
s

# Features

## Interval Monitors

Interval Monitors are a primary data collection feature of Perfmon4j. These monitors provided detailed performance measurements of executing application code. Below is an example of the performance measures that can be gathered (example output based on the Text Appender):

>> For simplicity the output displayed in this document is from the TextAppender.  Perfmon4j also includes a SQL appender to output performance measurements directly to a SQL database.

~~~~
********************************************************************************
 1) WebRequest.dap.rest
 2) 09:26:34:876 -> 09:27:34:877
 3)  Max Active Threads. 2 (2012-09-14 09:27:34:292)
 4)  Throughput......... 20.00 per minute
 5)  Average Duration... 96.00
 6)  Median Duration.... 85.0
 7)  > 100 ms........... 40.00%
 8)  > 500 ms........... 0.00%
 9)  > 1 second......... 0.00%
10)  Standard Deviation. 62.22
11)  Max Duration....... 260 (2012-09-14 09:27:21:785)
12)  Min Duration....... 25 (2012-09-14 09:27:21:785)
13)  Total Hits......... 20
14)  Total Completions.. 20
15) (SQL)Avg. Duration. 48.00
16) (SQL)Std. Dev...... 30.55
17) (SQL)Max Duration.. 100 (2012-09-14 09:27:34:382)
18) (SQL)Min Duration.. 11 (2012-09-14 09:27:34:325)
19) Lifetime (2012-09-14 09:14:33):
20)  Max Active Threads. 6 (2012-09-14 09:22:21:836)
21)  Max Throughput..... 85.00 (2012-09-14 09:17:34 -> 2012-09-14 09:18:34)
22)  Average Duration... 416.00
23)  Standard Deviation. 2585.71
24)  Max Duration....... 30053 (2012-09-14 09:16:46:764)
25)  Min Duration....... 20 (2012-09-14 09:22:12:709)
26)  (SQL)Avg. Duration. 173.00
27)  (SQL)Std. Dev...... 1090.24
28)  (SQL)Max Duration.. 12385 (2012-09-14 09:16:46:764)
29)  (SQL)Min Duration.. 7 (2012-09-14 09:24:35:638)
********************************************************************************
~~~~

Here is a description of the data attributes collected:

* (line 1) The package associated with the interval monitor (See: Interval Monitor Package Structure for details on Perfmon4j package structure.)
* (line 2) The interval start time/end time that was measured.
* (line 3) Max Active Threads - This is the maximum number of threads that were concurrently being executed in within the code segment. The date/time this max threshold was first obtained is also displayed.
* (line 4) Throughput - This is the total number of code executions that completed the code segment within the interval duration, normalized to completions per minute. For a 1 minute interval duration this value is typically the same as the (line 14) Total Completions. However if the interval duration is smaller or larger than 1 minute, the total completions is normalized to a per minute average.
* (line 5) Average Duration - The average completion duration for all completed executions within this code segment.
* (line 6) Median Duration - This is an optional element that represents a rounded median duration. See ?????? for details on how to configure a median calculator.
* (line 7-9) Threshold calculation - These are optional elements that can report % of completions that exceed various threshold durations. See ??? for details on how to configure a threshold calculator.
* (line 10) Standard Deviation - A statistical measure indicating the variance in duration among completed executions during the interval period.
* (line 11) Max Duration - The maximum execution duration during the interval period and the data/time that execution completed.
* (line 12) Min Duration - The minimum execution duration during the interval period and the data/time that execution completed.
* (line 13) Total Hits - The total number of executions that entered the code segment during the interval period.
* (line 14) Total Completions - The total number of executions that exited the code segment during the interval period.
* (line 15) Average SQL Duration - The average number of milli-seconds spent in the JDBC layer across all completed executions. See ???? for how to enable SQL monitoring.
* (line 16) SQL Standard Deviation - The the standard deviation of JDBC access duration across all completed executions.
* (line 17) SQL Max Duration - The maximum JDBC execution duration during the interval period and the data/time that execution completed.
* (line 18) SQL Min Duration - The minimum JDBC execution duration during the interval period and the data/time that execution completed.
* (line 19-29) Lifetime - Statistical summary for the lifetime since this monitor.


Perfmon4j groups timing into configurable intervals. This allows you to not only evaluate the duration of operations, but how those durations respond over time.

Perfmon4j takes snapshots of performance characteristics at specific configurable intervals. For example you could configure a monitor that would record web request/response time every minute throughout the day.

For each interval the following attributes can be recorded:

* Operation duration (Max, Min, Average, Median, Standard Deviation)
* Throughput (Operations started and completed during the duration)
* Configurable thresholds (Percentage of operations exceeding a specified duration)
* Max concurrent threads
* What percentage of my method execution time is in SQL?

Capturing this data at specified intervals allows you to analyze how your system responds over time and under load. For example:

* What is my peak load?
* What time of day is my system under peak load?
* When and how often throughout the day does my system not meet my response time criteria?
* Where are the bottlenecks in my system? (Based on max concurrent threads)

Interval monitors are created through the following methods:

* Extreme (Method) Monitors are created at runtime by the Perfmon4j instrumentation agent.
* Web Request Monitors are created at runtime by the Perfmon4j servlet valve.
* Annotation Monitors are based on annotations declared in the java source code. Monitors ,based on the annotations, are created at runtime by Perfmon4j instrumentation agent.
* Code Monitors can be manually inserted into code by the java developer.

##Extreme (Java Method Level) Monitors

Extreme monitors are a type of Interval Monitor that are automatically attached to methods based on parameters passed to the Perfmon4j instrumentation agent.

Extreme monitors are created at run time based on parameters passed to the Perfmon4j java agent (See: -e agent parameter). An extreme monitor is created for each method, of each class that belongs to a package, or child package, based on the specified package. The extreme monitor, associated with each method, takes on a package structure based on its class/package name. See: Interval Monitor Package Structure for details on method package structure.

##Web Request Monitors

Web request monitoring is available when running an application under the JBoss Application Server or Apache-Tomcat Servlet Engine. To enable WebRequest monitoring include the -eVALVE as a parameter on the Perfmon4j java agent (See: -eVALVE agent parameter)

As with code based monitors Web Request monitoring is based on a package structure. The package is built on a combination of the servlet context and query parameters of the request. See: Web Request Package Structure for details on package structure.
Annotation Monitors

Unlike Extreme monitors, Annotation monitors require modification to the java source code. To use an annotation monitor you must do the following:

* First add the DeclarePerfMonTimer annotation to a method.
* Then specify the class containing the method with the -a parameter on the Perfmon4j java agent (See: -a agent parameter).

###In Code Monitors

Perfmon4j provides an API that allows you to directly insert timer blocks into your java code. Although this method does require access to the source code, it can add great deal of flexibility to your monitoring options.

See: Code Monitor Package Structure for details on adding code monitors.


##Snap Shot Monitors

Snap Shot Monitors are used to capture system metrics at regular intervals. Snap Shot Monitors can be used for the following:

* Built-In Monitors - Perfmon4j comes with several built in monitors that gather various system wide metrics.
* JMX Monitoring - You can configure Perfmon4j to collect attributes from any JMX Object.
* Custom Monitoring - You can create your own custom monitoring class.

###JVM SnapShot Monitor (Built-in monitor)

* Monitor Class: org.perfmon4j.java.management.JVMSnapShot
* Supports SQL Appender Output: true
* Supports Instance Name: false

The JVMSnapShot monitor collects information from the following Java management objects:

* java.lang.management.MemoryMXBean
* java.lang.management.ThreadMXBean
* java.lang.management.ClassLoadingMXBean
* java.lang.management.CompilationMXBean
* java.lang.management.OperatingSystemMXBean

Below is an example of the output from a TextAppender:

~~~~
JVMSnapShot
23:59:54:028 -> 00:00:54:030
 classesLoaded............ 10156
 totalLoadedClassCount.... 0.000/per minute
 unloadedClassCount....... 0.000/per minute
 compilationTime.......... 0.000/per minute
 compilationTimeActive.... true
 heapMemUsed.............. 1235.830 MB
 heapMemCommitted......... 3491.688 MB
 heapMemMax............... 3491.688 MB
 nonHeapMemUsed........... 100.707 MB
 nonHeapMemCommitted...... 322.125 MB
 nonHeapMemMax............ 348.000 MB
 pendingFinalization...... 0
 systemLoadAverage........ 0.540
 threadCount.............. 231
 daemonThreadCount........ 221
 threadsStarted........... 113.996/per minute
 heapMemUsedCommitted..... 35.394%
 heapMemUsedMax........... 35.394%
 nonHeapMemUsedCommitted.. 31.263%
 nonHeapMemUsedMax........ 28.939%
~~~~

### Garbage Collector SnapShot Monitor (Built-in monitor)

* Monitor Class: org.perfmon4j.java.management.GarbageCollectorSnapShot
* Supports SQL Appender Output: true
* Supports Instance Name: True

The GarbageCollectorSnapShot monitor collects information from the following Java management objects:

* java.lang.management.GarbageCollectorMXBean

Below is an example of the output from a TextAppender:

~~~~
Composite Garbage Collector
23:59:54:028 -> 00:00:54:030
 instanceName............. Composite("ParNew", "ConcurrentMarkSweep")
 collectionCount.......... 134.996/per minute
 collectionTime........... 3097.897/per minute
~~~~

### Memory Pool SnapShot Monitor (Built-in monitor)

* Monitor Class: org.perfmon4j.java.management.MemoryPoolSnapShot
* Supports SQL Appender Output: true
* Supports Instance Name: true

The MemoryPoolSnapShot monitor collects information from the following Java management objects:

* java.lang.management.MemoryPoolMXBean

Below is an example of the output from a TextAppender:

~~~~
Composite Memory Pool
23:59:54:028 -> 00:00:54:030
 type..................... (N/A)
 used..................... 1336.537 MB
 committed................ 3813.813 MB
 max...................... 3839.688 MB
 instanceName............. Composite("Code Cache", "Par Eden Space", "Par Survivor Space", "CMS Old Gen", "CMS Perm Gen")
 init..................... 3794.125 MB
 usedCommittedRatio....... 35.045%
 usedMaxRatio............. 34.808%
~~~~

###Thread Pool Monitor (Built-in monitor)

* Monitor Class: org.perfmon4j.extras.tomcat7.ThreadPoolMonitorImpl,org.perfmon4j.extras.tomcat55.ThreadPoolMonitorImpl
* Supports SQL Appender Output: true
* Supports Instance Name: true

The Thread Pool Monitor is available under Apache Tomcat and JBoss (4.x - 6.x) application servers.

Below is an example of the output from a TextAppender:

~~~~
ThreadPool
23:59:54:028 -> 00:00:54:029
 instanceName............. Composite("http-0.0.0.0-8080", "http-0.0.0.0-8443")
 currentThreadCount....... 2
 currentThreadsBusy....... 0
~~~~

###Global Request Monitor (Built-in monitor)

* Monitor Class: org.perfmon4j.extras.tomcat7.GlobalRequestMonitorImpl,org.perfmon4j.extras.tomcat55.GlobalRequestMonitorImpl
* Supports SQL Appender Output: true
* Supports Instance Name: true

The Global Request Monitor is available under Apache Tomcat and JBoss (4.x - 6.x) application servers.

Below is an example of the output from a TextAppender:

~~~~
TomcatRequestProcessor
23:59:47:566 -> 00:00:47:565
 instanceName............. Composite("http-0.0.0.0-8443", "http-0.0.0.0-8080")
 bytesSent................ 8554.326 KB/per minute
 bytesReceived............ 581.408 KB/per minute
 requestCount............. 2909.048/per minute
 errorCount............... 1.000/per minute
 processingTimeMillis..... 124803.080/per minute
~~~~

###JMX Monitoring

* Monitor Class: org.perfmon4j.instrument.jmx.JMXSnapShotProxyFactory
* Supports SQL Appender Output: false
* Supports Instance Name: false

The JMXSnapShotProxyFactory class provides the ability to monitor any JMX accessible attributes. The following example demonstrates monitoring the attributes associated with the jboss.web:name=httpThreadPool,type=Executor JMX object.

~~~~ xml
<snapShotMonitor name='httpsThreadPool.Executor'
 	className='org.perfmon4j.instrument.jmx.JMXSnapShotProxyFactory'>
		<attribute name='jmxXML'><![CDATA[
			<JMXWrapper defaultObjectName='jboss.web:name=httpThreadPool,type=Executor'>
				<attribute name='largestPoolSize'/>
				<attribute name='activeCount'/>
				<attribute name='TotalTaskCount' jmxName='completedTaskCount'/>
				<attribute name='completedTasks'  jmxName='completedTaskCount'>
					<snapShotCounter formatter='org.perfmon4j.util.NumberFormatter'
						display='DELTA_PER_MIN'/>
				</attribute>
				<attribute name='poolSize'/>
			</JMXWrapper>
		]]></attribute>
		<appender name='snapshot-appender'/>
</snapShotMonitor>	
~~~~

Below is an example of the output from a Text Appender:

~~~~
httpThreadPool.Executor
17:46:24:550 -> 17:47:24:550
 largestPoolSize.......... 30
 activeCount.............. 0
 totalTaskCount........... 44
 completedTasks........... 2.340/per minute
 poolSize................. 30
~~~~

###Custom Snapshot Monitors

* Monitor Class: N/A
* Supports SQL Appender Output: true
* Supports Instance Name: false

You can also create you own snap shot monitors to capture application specific metrics at a regular interval. The following example demonstrates how to annotate a simple class to create a custom snap shot monitor:

~~~~ java
package org.mystuff.mymonitor;

... // Import statements omitted

@SnapShotProvider(type = SnapShotProvider.Type.STATIC)
public class JVMMemory {
   
	@SnapShotGauge
	public static long getFreeMemory() {
		return Runtime.getRuntime().freeMemory();
	}
	
	@SnapShotGauge
	public static long getTotalMemory() {
		return Runtime.getRuntime().totalMemory();
	}
}
~~~~ 

You would configure this monitor as follows:

~~~~ xml
<snapShotMonitor name='httpsThreadPool.Executor'
 	className='org.mystuff.mymonitor.JVMMemory'>
		<appender name='snapshot-appender'/>
</snapShotMonitor>	
~~~~

The following is an example of the monitor output in a Text Appender:

~~~~
JVM Memory
16:36:12:645 -> 16:36:13:645
 freeMemory............... 118838272
 totalMemory.............. 127533056
~~~~

<!--
Thread Traces
VisualVM Plugin
Text appender
SQL appender
-->

<a name="configuration"/>

#Configuration 

##VM Boot time configuration

The boot time configuration is done through the following two methods:

* Parameters to the perfmon4j javaagent on the java command line.
* The boot section of the perfmonconfig.xml file.

<a name="javaagent-config"/>

##Javaagent configuration

Installing the perfmon4j java agent on the command line is configured as follows: `-javaagent:<<path to perfmon4j.jar>>perfmon4j.jar=<<parameter 1>>,<<parameter n>>`


###Java agent parameters:

| Parameter | Description | Example(s) |
 ------------ | :----------- | :----------- |
| -f | This option specifies the location of the perfmonconfig.xml configuration file. This file will be checked for modification every 60 seconds. If the file does not exist perfmon4j will start in an inactive mode. | -f../config/perfmonconfig.xml |
|-e | (repeatable)This option is used to indicate a package or class name for instrumentation. Every method of any class within the specified package hierarchy will have a timer inserted. By default the extreme instrumentation will not create a monitor for methods that match the bean setter/getter pattern. You can enable instrumentation on getter and setters by prefixing the class/package name with (+getter,+setter) |-eorg.jboss,-ecom.follett, -e(+getter,+setter)com.follet.bean |
|-eSQL|(Repeatable)Perfmon4j can instrument the classes associated with a JDBC driver. This will create a monitor on each method associated with the drive implementation in the format SQL.<method name>. By default all classes loaded will be examined to find the JDBC classes. You can limit which classes are instrumented by specifying the following options: * JTDS - Instrument JTDS driver for Microsoft SQL Server. * MYSQL - Instrument the MYSQL driver * DERBY - Instrument the DERBY driver * POSTGRESQL - Instrument the postgresql driver. * ORACLE - Instrument the oracle driver * <package> - Only instrument classes in the specified package. |-eSQL, -eSQL(JTDS), -eSQL(MYSQL), -eSQL(POSTGRESQL), -eSQL(ORACLE),-eSQL(org.my.jdbc.impl)|
|-eVALVE|Used in JBoss, Appache-Tomcat or Wildfly servers. This option will install the perfmon4j valve which will enable timings around each web request. See servletValve for parameters to customize the behavior of the servlet valve.|-eVALVE|
|-a|(repeatable) This option is used to indicate a package or class name for instrumentation. Any methods, containing a DeclarePerfMonTimer annotation will, have a timer inserted|-aorg.jboss,-acom.follett|
|-i|(repeatable) This option is used to override the –e and –a options. Any class or packages within the specified package hierarchy will be ignored and NOT instrumented.|-eorg.jboss,-iorg.jboss.system * This will instrument all classes within org.jboss, except any classes in org.jboss.system|
|-p| This option specifies a port should be opened to monitor perfmon4j instrumentation through the perfmon4j VisualVM plugin. You must specify AUTO, where perfmon4j will attempt to find an avialable port, or the specific port to use.|-pAUTO,-p5400|
|-d|Enables debug level logging in perfmon4j. Defaults to false.|-dtrue|
|-v|Enables verbose instrumentation output. This will output each method that is instrumented and the name of the monitor created.|-vtrue|
|-b|When this option is true perfmon4j will instrument ALL classes loaded by the bootstrap loader. When this option is false (default) classes that are loaded before the javaagent is loaded are NOT instrumented.|-btrue|
|-r|Used with the -f option. Will override the duration in seconds that the perfmonconfig.xml file will be checked for modification. The default reload duration is every 60 seconds|-fc:/perfmonconfig.xml,-r360|

<a name="perfmonconfig-xml-config"/>

## Perfmonconfig.xml configuration
Runtime configuration is done through the perfmonconfig xml file.  This file will be checked for updates as the application is running. 

### Boot specific configuration

Although perfmonconfig.xml is largely used for dynamic configuration (changes to the file are dynamically reloaded and do not require a JVM restart) the boot section is used to specify boot time configuration which do require a JVM restart. Currently the boot section is used to configure the servlet valve properties (for JBoss and Apache-Tomcat Application servers).

The following an example of the perfmonconfig.xml file with boot configuration:

~~~~ xml
<Perfmon4JConfig enabled='true'>
  <boot>
     <servletValve outputRequestAndDuration='true' />
  </boot>
</Perfmon4JConfig>
~~~~

The parameters, which can be set as attributes on the servletValve tag are:

|Parameter|Description|Default Value|
| ------------ | :----------- | :----------- |
|outputRequestAndDuration | If this is set to true each servlet request will be logged to the console and the server log. The request will also contain the duration, in milliseconds, of processing time required to fulfill the request. If SQL monitoring was enabled the duration of milliseconds spent in the JDBC layer will also be displayed | false |
 | baseFilterCategory | This specifies the perfmon4j monitor catogory that is used as the root category for all web request. | "WebRequest" |
 | abortTimerOnRedirect | Determines if the duration for a request that results in a redirect is included in load/average response time calculations  | false |
 | abortTimerOnImageResponse | Determines if the duration for a request that results in an image response is included in load/average response time calculations | false | 
 | skipTimerOnURLPattern | Specify a Regular expression pattern to match against each incoming URL. If the pattern matches the URL the request will NOT be included in load/average response time calculation. | "" | 
 | pushClientInfoOnNDC | If you are using log4j for your logging implementation the client IP will be pushed onto NDC (Nested Diagnostic Context). |  	false | 
 | pushCookiesOnNDC | If you are using log4j for your logging implementation you can specify one or more cookie values to be pushed onto the NDC (Nested Diagnostic Context). You can use a wild card character "*" to specify all cookies or a comma seperated list of cookies (e.g. "siteID, userName") | "" | 
 | pushSessionAttributesOnNDC | If you are using log4j for your logging implementation you can specify one or more session attributes to be pushed onto the NDC (Nested Diagnostic Context). You can use a wild card character "*" to specify all session attributes or a comma seperated list of session attributes (e.g. "siteID, userName") | "" | 

###Dynamic configuration

The dynamic configuration of perfmon4j is done through the perfmonconfig.xml file. This file is monitored for changes every 60 seconds (by default - go here for information on how to configure this duration).

The perfmonconfig.xml file is used primarily to configure the following 3 elements:

* Appenders collect performance data and write it to an output device.
* Monitors are object that passively collect performance data.
* Thread Traces are objects that collect a detailed trace of threads that pass through a specific interval monitor.

The following sections go into the configuration of these elements in more detail:

###Appender Configuration

Appenders are the primary worker of any Perfmon4j instance. Monitors passively collect data, with extremely minimal overhead placed on the application thread. Appenders are responsible for collecting this data and writing it to various output devices.

All appenders are run within a single, dedicated Perfmon4j thread. This ensures that the application threads are never interrupted to perform heavy time consuming tasks such as data collection and output.

Perfmon4j is shipped with the following appender types. You are also free to extend or create new appender types to output data to any custom devices/formats:

* Text Appender - The text appender will output data to the server log and/or console. This appender respects the logging configuration (log4j, commons logging, java logging) specified within your application. The appender writes, at an info level, to the category org.perfmon4j.TextAppender.
* SQL Appender (2 types a JDBCSQLAppender and PooledSQLAppender) - The SQL appender will output data to a SQL database. Perfmon4j includes scripts to create a database to accept logging information for the following databases: Microsoft SQL server, MySQL, Postgress and Oracle. Additionally the schema is relatively simple so it should be easily addapted to other SQL compatible databases.
* Anything else - Perfmon4j's appender architecture is very flexible. It would be relativly simple to provide your own appender to output data in any format you require.

####Text Appender Configuration:

The following shows an example of a text appender, a JDBC SQL appender, and a Pooled SQL appender.

~~~~ xml
<appender name='webrequest-appender' className='org.perfmon4j.TextAppender' interval='10 minutes'>
   <attribute name='medianCalculator'>factor=10</attribute>		
</appender>
~~~~

~~~~ xml
<appender name='jdbc' className='org.perfmon4j.JDBCSQLAppender' interval='1 minutes'>
  <attribute name='driverClass'>com.mysql.jdbc.Driver</attribute>		
  <attribute name='driverPath'>c:/drivers/mysql-connector-java-5.1.12-bin.jar</attribute>		
  <attribute name='jdbcURL'>jdbc:mysql://192.168.42.100/perfmon4j</attribute>		
  <attribute name='dbSchema'>dbo</attribute>		
  <attribute name='userName'>perfmon4j</attribute>		
  <attribute name='password'>password</attribute>		
  <attribute name='medianCalculator'>factor=10</attribute>		
  <attribute name='thresholdCalculator'>2 seconds, 5 seconds, 10 seconds</attribute>
</appender>
~~~~

~~~~ xml
<appender name='pool' className='org.perfmon4j.PooledSQLAppender' interval='1 minute'>
  <attribute name='poolName'>java:/MSSQLDS</attribute>		
  <attribute name='dbSchema'>dbo</attribute>		
  <attribute name='medianCalculator'>factor=10</attribute>		
  <attribute name='thresholdCalculator'>2 seconds, 5 seconds, 10 seconds</attribute>
</appender>
~~~~
Every appender contains the following parameters, which are declared as XML attributes on the appender tag:


|Parameter | Description | Default value | 
| ------------ | :----------- | :----------- |
 | name | The name is a required parameter, it can be any string value, but it must be uniquely assigned to only one appender per config file | This is a required parameter | 
 | className  | This is a required parameter and represents the fully formed class name of the Appender implementation. Perfmon4j provides the following 3 appender classes: org.perfmon4j.TextAppender, org.perfmon4j.JDBCSQLAppender, org.perfmon4j.PooledSQLAppender. You can, of course, create your own appender classes. | This is a required parameter | 
 | interval | The interval specifies how often the appender will poll it's monitors and write the output. Perfmon4j can take a string argument for this parameter. Any of the following values are acceptable: "1 minute", "45 seconds", "5 minutes", "2 hours". If you simply include a number the unit will be assumed to be in minutes. | "5 minutes" | 

Additional attributes can be attached to the Appender through an attribute tag, within the body of the appender tag. If you create your own appender class these attributes must correspond to a setter (following java bean specification) on your Appender object. Here are the additional attributes for the appender classes defined by Perfmon4j:


Following parameters available for org.perfmon4j.TextAppender,org.perfmon4j.JDBCSQLAppender and org.perfmon4j.PooledSQLAppender:

| Parameter  | Description  | Example(s) | Default | 
| ------------ | :----------- | :----------- |:----------- |
| medianCalculator | The median calculator will attempt to calculate a median duration for all threads passing through an interval monitor. | `<attribute name='medianCalculator'>factor=10</attribute>`, `<attribute name='medianCalculator'>factor=100, maxElements=1000</attribute>` | Median calculation is disabled |
|thresholdCalculator|This calculator allows you to define one or more threshold durations. | `<attribute name='thresholdCalculator'>2 seconds, 5 seconds, 10 seconds</attribute>`, `<attribute name='thresholdCalculator'>50 ms, 100 ms</attribute>`|Threshold calculations is disabled`

Following parameters available for org.perfmon4j.TextAppender:

| Parameter  | Description  | Example(s) | Default | 
| includeSQL | When this parameter is set to false the org.perfmon4j.TextAppender will not include SQL duration information in output to the server log. | `<attribute name='includeSQL'>false</attribute>` | true |

Following parameters available for: org.perfmon4j.JDBCSQLAppender, org.perfmon4j.PooledSQLAppender:

| Parameter  | Description  | Example(s) | Default | 
| dbSchema| This value indicates the dbschema to be used by SQL appender.| `<attribute name='dbSchema'>dbo</attribute>` | The default schema is used.| 
| systemNameBody | To support multiple systems logging to the same SQL database, each log entry is associated with a specific system. | `<attribute name='systemNameBody'>CircSystem-A</attribute>`| The default systemNameBody is calculated based on the machine name and the current working directory of the java application.| 
| systemNamePrefix| To prepend a prefix to the default system name | `<attribute name='systemNamePrefix'>Cluster1.</attribute>`| No prefix is used.| 
| systemNameSuffix| To append a suffix to the default system name| `<attribute name='systemNameSuffix'>-Cluster1</attribute>`| No suffix is used.| 

Following parameters available for org.perfmon4j.JDBCSQLAppender:

| Parameter  | Description  | Example(s) | Default | 
| driverPath | The system path to the jar file containing the JDBC driver. | `<attribute name='driverPath'>c:/drivers/mysql-connector-java-5.1.12-bin.jar</attribute>` | Not required if the driver is loaded in the application's classpath  |
| driverClass|The JDBC driver class.|`<attribute name='driverClass'>com.mysql.jdbc.Driver</attribute>`| None. This is a required parameter|
|jdbcURL|The jdbc URL.|`<attribute name='jdbcURL'>jdbc:mysql://192.168.42.100/perfmon4j</attribute>`| None. This is a required parameter|
|userName| The userName to use for the SQL connection.| `<attribute name='userName'>perfmon4j</attribute>`| None. This is a required parameter|
|password|The password to use for the SQL connection.|`<attribute name='password'>topsecret</attribute>`| None. This is a required parameter|


Following parameters available for: org.perfmon4j.PooledSQLAppender

| Parameter  | Description  | Example(s) | Default | 
| poolName| The JNDI name indicating the javax.sql.DataSource that should be used | `<attribute name='poolName'>java:/MSSQLDS</attribute>`| None. This is a required parameter| 
| contextFactory| The value for the system property java.naming.factory.initial used when obtaining the javax.naming.InitialContext| In most cases the default value does not need to be overridden.|  Uses the default value configured in the JVM| 
| urlPkgs| The value for the system property java.naming.factory.url.pkgs used when obtaining the javax.naming.InitialContext | In most cases the default value does not need to be overridden.| Uses the default value configured in the JVM.| 

#License

Perfmon4j is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License, version 3, as published by the Free Software Foundation. This program is distributed WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,OR FITNESS FOR A PARTICULAR PURPOSE. You should have received a copy of the GNU Lesser General Public License, Version 3, along with this program. If not, you can obtain the LGPL v.s at http://www.gnu.org/licenses/ 
