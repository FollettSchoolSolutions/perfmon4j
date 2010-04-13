Copyright 2008,2009,2010 Follett Software Company 
 
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
*** Version 1.0.2.GA (released 2010-??????) ***
- Feature Request: #2972831 - Added a SQLAppender for UserAgentData.  This enables you to configure the
	UserAgent logging to a SQL database.  When combined with the Perfmon4j Servlet filter or the 
	Perfmon4j Apache Tomcat Valve this enables tracking the total number of request broken out by browser, 
	browserVersion, clientOS amd clientOS version.
	
	See: "Example 9 – Output UserAgent (Browser Summary) data to a SQL Database' in 
	Perfmon4j-ConfigSamples.pdf for configuration examples.


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