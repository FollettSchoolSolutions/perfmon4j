Copyright 2008, 2009, 2010 Follett Software Company 
 
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

****** Please Help ********************************************************************************************
Perfmon4j is an internal tool in use and under active development at Follett Software Company. 
We have found it extremly usefull for tracking down production based performance bottlenecks in 
minutes/hours instead days/weeks.  Creating the open source releases is time consuming and will 
only be continued if we have your feedback.

* If you like Perfmon4j, please let us know.  

* If you would like to try Perfmon4j but dont know where to start, let us know.

* If you try Perfmon4j and have question or problems REALLY let us know!  

* Your feedback is essential to ensure future open source releases of Perfmon4j.

Please direct emails to perfmon4j@fsc.follett.com

OR

Contact us through the SourceForge forums
	- Open Discussion: https://sourceforge.net/projects/perfmon4j/forums/forum/809789
	- Help: https://sourceforge.net/projects/perfmon4j/forums/forum/809790

Dave
****** Please Help ********************************************************************************************

Changes
*** Version 1.0.2.GA (released 2010-06-10) ***
- Feature Request: Added a powerfull new way to force ThreadTraceMonitoring on a specific thread (or a specific request).  
	You can now force a thread trace based on one of the following: HTTPRequest parameter, HTTPSession parameter,
	HTTPCookie value, thread name or thread property.
	See: "Example 10 – Setup a ThreadTrace for detailed peformance logging", 
		"Example 11 –Trigger ThreadTrace by request" and "Example 12 –Trigger ThreadTrace by cookie" in
		Perfmon4j-ConfigSamples.pdf for configuration examples.

- Feature Request: #2972831 - Added a SQLAppender for UserAgentData.  This enables you to configure the
	UserAgent logging to a SQL database.  When combined with the Perfmon4j Servlet filter or the 
	Perfmon4j Apache Tomcat Valve this enables tracking the total number of request broken out by browser, 
	browserVersion, clientOS amd clientOS version.
	
	See: "Example 9 – Output UserAgent (Browser Summary) data to a SQL Database' in 
	Perfmon4j-ConfigSamples.pdf for configuration examples.

- Feature Request: Added support for SQLAppender to the following SnapShot monitors: Tomcat GlobalRequestProcessorMonitor,
	Tomcat ThreadPoolMonitor, ThreadTraceData, GarbageCollectorSnapShot, MemoryPoolSnapShot,
	and JVMSnapShot. 

- Fixed moderate defect: In some very rare cases the GlobalClassLoader could enter into an infinite loop.  
	Now a thread local is used to ensure recursion can not happen.

- Feature Request: Allow targeted disable of calls to System.gc().  This is a bit of a misplaced feature but we
	found it was an easy addition to the PerfMon4j instrumentation agent.  The use case is a desire
	to disable all calls to System.gc(), similar to the Sun jvm parameter -XX:-DisableExplicitGC.  However,
	unlike that parameter we still want the ability to force a complete GC, including a memory compaction,
	when the system is NOT under extreme load.  Using the perfmon4j agent parameter of "-g=true" you can
	disable calls to System.gc() while still allowing calls to Runtime.getRuntime().gc().

- Feature Request: Added SQL create scripts for Oracle and PostgreSQL.

*** Version 1.0.1.GA (released 2010-03-15) ***
- Feature Request: #2964325  - Add SQL Based appender for interval data. 
	Now you can configure timing data to be written to an SQL database.
	
	See "Output Interval data to a SQL Database" in Perfmon4j-ConfigSamples.pdf 
	for configuration examples.

- Fixed severe defect: #2953547  - Release jars not compiled for Java 1.5
- Fixed moderate defect: #2964274 - Log4j logger does not include Throwable
- Fixed defect: #2964290 - Reloading appender fails to propagate attributes

*** Version 1.0.0.GA (released 2009-11-07) ***

- Feature Request: #2879256 - Provide option to instrument getters and setters.  Now methods that match the bean getter 
and setter specification can be included in extreme instrumentation.  
	
- Fixed severe defect: #2889034 - Global classloader incorrectly maintains reference to classes that have been unloaded by classloader.	
- Fixed minor defect: #2888151 - Null Pointer Exception when named appender does not exist in configuration of SnapShotMonitor.	
- Fixed minor defect: #2888552 - Failure loading snapshot monitor containing non-public annotated method.

*** Version 1.00 RC 4 ***


For more information see: http://www.perfmon4j.org