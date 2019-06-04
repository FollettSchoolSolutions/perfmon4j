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

- Significant enhancements for maping IntervalMonitors to Appenders.  In addition to the 
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

