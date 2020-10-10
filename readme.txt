Copyright 2008-2019 Follett Software Company 
 
Perfmon4j(tm) is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License, version 3,
as published by the Free Software Foundation.  This program is distributed
WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
http://www.gnu.org/licenses/

perfmon4j@fsc.follett.com
David Deuchert
Follett Software Company
1391 Corporate Drive
McHenry, IL 60050


Changes

** Next Version

- Now the LogFilter will include the HTTP method in the log output for each web request.  
  To disable this new behavior you can set the system property 
  web.org.perfmon4j.servlet.PerfMonFilter.SKIP_HTTP_METHOD_ON_LOG_OUTPUT=true
  
- The LogFilter will now mask any parameter that includes password in the parameter
  name regardless of case.

** 1.5.1 - 2020-08-21

- Added a new Appender (org.perfmon4j.azure.LogAnalyticsAppender). This appender will 
  write perfmon4j observations to the Log Analytics workspace in Azure Monitor. Run 
  perfmon4j in debug mode (-dtrue on javaagent command line) for details
  regarding successful request to Azure Log Analytics
- For information on the Microsoft Rest API used by the LogAnalyticsAppender 
  see: https://docs.microsoft.com/en-us/azure/azure-monitor/platform/data-collector-api
- For configuration information see: 
  https://github.com/FollettSchoolSolutions/perfmon4j/wiki/Azure-LogAnalytics-Appender

- Now the observations returned by IntervalData.getObservations() include Date/Time fields
  (timeStart, timeStop, maxActiveThreadCountSet, maxDurationSet, minDurationSet,
  maxSQLDurationSet and minSQLDurationSet).
  
- Logging cleanups under Log4j and Java Logging.  Now -vtrue (verbose mode) will
  also include the -dtrue (debug mode option).  But -dtrue no longer includes
  verbose mode.  -dtrue is now good for watching the messages between appenders
  and their remote repositories. 
- Improved logging in the InfluxAppender.  

- Upgraded javassist from version 3.24.1-GA to 3.27.0-GA

** 1.5.0 - 2020-06-12
- DO NOT MOVE UP TO THIS VERSION UNLESS YOUR PROJECT IS JAVA 11 OR BETTER  
Changed compiler version to java 11.

** 1.4.3 - 2019-06-27
- Renamed the package of the api jar from "org.perfmon4j.agent.api" to 
"api.org.perfmon4j.agent".  This is required because when running under JBoss/Wildfly
classes can only be loaded from the "org.perfmon4j.*" package when loaded
by the perfmon4j javaagent.

- Added SnapShot annotations to the perfmon4j-agent-api.jar. These annotations allow you to
define SnapShotCounters using only the api jar.  Most of the functionality contained in the 
existing perfmon4j jar is available using these annotation.  Some missing functionality
includes the ability to define SQLAppenders and add custom formatting options for the 
text appender.  Annotations added: 
  	- api.org.perfmon4j.agent.instrument.SnapShotProvider
	- api.org.perfmon4j.agent.instrument.SnapShotCounter
	- api.org.perfmon4j.agent.instrument.SnapShotGauge
	- api.org.perfmon4j.agent.instrument.SnapShotRatio
	- api.org.perfmon4j.agent.instrument.SnapShotRatios
	- api.org.perfmon4j.agent.instrument.SnapShotString
	- api.org.perfmon4j.agent.instrument.SnapShotInstanceDefinition

** 1.4.2
- Added the perfmon4j-agent-api.jar.  Using this jar you can optionally add Perfmon4j
timers and annotations to classes (bundled in jar,war or ear files). If the
Perfmon4j java agent is loaded at boot time the functionality represented by these
classes will become activated.  When the Perfmon4j agent is not loaded, these classes
will be inactive.

- The agent jar includes the following files (which can be used in place of their same
named counterparts in the Perfmon4j full implementation jar):
	- org.perfmon4j.agent.api.PerfMon
	- org.perfmon4j.agent.api.PerfMonTimer
	- org.perfmon4j.agent.api.SQLTime
	- org.perfmon4j.agent.api.instrument.DeclarePerfMonTimer (Annotation)


** 1.4.1
- Added support for writing PerfMon output data to InfluxDb 
(https://www.influxdata.com/time-series-platform/influxdb/) with the InfluxAppender class.

- Added support for monitoring Hystrix (https://github.com/Netflix/Hystrix) circuit breakers in
production code using SnapShotMonitors. The HystrixCommandMonitorImpl collects data from
com.netflix.hystrix.HystrixCommandMetrics and the HystrixThreadPoolMonitorImpl from
com.netflix.hystrix.HystrixThreadPoolMetrics.

- Hystrix data can be written to a full variety of Appenders including the SQLAppender and 
the new InfluxApender. 

- Hystrix monitoring is enabled using the –eHYSTRIX java agent parameter.

- Significant refactor to the Appender class to support “no-schema” style derived Appenders.

- Added the PerfMonObservableData interface and PerfMonObservableDataum class. These 
structures provide the foundation for PerfMonData derived classes to map elements 
for “no-schema” Appenders

- Extracted shared functionality for system names and group out of the SQLAppender class 
and into the base class SytemNamesAndGroupsAppender.

- Significant enhancements for mapping IntervalMonitors to Appenders.  In addition to the 
existing monitor patterns: ('.' or './') for parent only; (./*) for parent and children;
(/*) for children only; (./**) for parents, children and all descendents; and (/**) for
children and all descendents you can now specify a wild card pattern.

- The wildcard mapping pattern uses '#' to replace any character and '#*' to replace one or more 
characters. 

- Added DynamicStressTester class.  Although this class is early it will provide a mechanisim 
to compare the Timer performance across different versions of Perfmon4j.

- Significant performance enhancements to the PerfMonTimer.start and PerfMonTimer.stop methods.
Particularly reduced locking contention by replacing the existing ReentrantLock around the
monitor map with a Read/Write Reentrant lock.

** 1.3.4-HotFixB 
- Version 7.0 of the perfmon4j database contains contains a field, that 
when set informs active SQLAppenders to pause writing to the database. The column,
pauseAppenderMinutes, is in the new P4JAppenderControl table.

- Significant performance improvements for the Perfmon4J Rest Datasource. 
You can set a flag in the perfmon4j database that informs SQLAppenders to pause writes.  
	

** 1.3.4-HotfixA 
- Addressed an overhead issue when running perfmon4j in a disabled state.

