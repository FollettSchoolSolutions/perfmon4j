Copyright 2008,2009 Follett Software Company 
 
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
1391 Corparate Drive
McHenry, IL 60050


Changes

*** Version 1.0.0.GA (released 2009-11-07) ***

- Feature Request: #2879256 - Provide option to instrument getters and setters.  Now methods that match the bean getter 
and setter specification can be included in extreme instrumentation.  
	
- Fixed severe defect: #2889034 - Global classloader incorrectly maintains reference to classes that have been unloaded by classloader.	
- Fixed minor defect: #2888151 - Null Pointer Exception when named appender does not exist in configuration of SnapShotMonitor.	
- Fixed minor defect: #2888552 - Failure loading snapshot monitor containing non-public annotated method.

*** Version 1.00 RC 4 ***




For more information see: http://www.perfmon4j.org