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
*** Version 1.1.1.GA (released 2011-9-30) ***  
	(SVN Release Branch: https://perfmon4j.svn.sourceforge.net/svnroot/perfmon4j/branches/BRANCH1.1.x)
	(SVN Release Tag: https://perfmon4j.svn.sourceforge.net/svnroot/perfmon4j/tags/version1.1.1.GA)

- Enhancement - XML Configuration has been made more forgiving.  Now if a monitor
	is defined without an Appender attached, or with attached to an undefined appender,
	it will by attached to a default text appender.

- Change - By default the perfmon4j javaagent will no longer check each class loaded to determine if
	it contans methods annotated with a perfmon4j timer annotation. You are now required to explicitly specify 
	packages that contain timer annotations as a parameter to the java agent.  
	Example: -javaagent:../lib/endorsed/perfmon4j.jar=-acom.follett.fsc
	
- Fix - Perfmon4j would only support sending it's log output to log4j when running within the JBoss
	application server.  Now by default perfmon4j will switch to log4j when log4j is configured.

- Fix - Now if you feed an invalid configuration XML file to perfmon4j the default
	configuration will be limited to all base base level monitors only.
	
	
*** Version 1.1.0.GA (released 2010-10-06) ***
!! IMPORTANT !!:  If you previously created a perfmon4j database for logging you must upgrade the database with
the associated update scripts:
	- MSSQL-UpdateTables-1.0.2_to_1.1.0.sql
	- MySQL-UpdateTables-1.0.2_to_1.1.0.sql
	- Oracle-UpdateTables-1.0.2_to_1.1.0.sql
	- PostgresSQL-UpdateTables-1.0.2_to_1.1.0.sql
	
- Feature Request: Now you can monitor JDBC/SQL request durations.  This option will enable you to 
	monitor the percentage of time your method or process spends in java vs SQL.
	See:  Perfmon4j-ConfigSamples.pdf - Example 14 – Evaluate SQL/JDBC Duration (Version 1.1.0+) a
	configuration example.
	
- Feature Enhancement:  Reduced memory overhead associated with servlet request monitoring. This is particularly
	important for web services that generate a large amount of distinct URL paths (i.e. restful services)
	
- New Perfmon4j now supports JBoss 6.x.  The Perfmon4j-JBossConfigGuide.pdf now includes instructions for installation 
	into JBoss (4x, 5x and 6x).
	
- New Perfmon4j now supports for Apache Tomcat 7.x.  The Perfmon4j-Tomcat-ConfigGuide.pdf now includes instructions
	for installation into Apache Tomcat (5.5x, 6.x, and 7.x).

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