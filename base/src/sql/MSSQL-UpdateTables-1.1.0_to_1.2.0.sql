/*
 *	Copyright 2008-2012 Follett Software Company 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

/* ******************************************************************************
Notes - This script contains the SQL required to update the Perfmon4j SQL
tables to 1.2.0 FROM 1.1.0
****************************************************************************** */

CREATE TABLE dbo.P4JSystem (
	SystemID INT IDENTITY(1,1) NOT NULL,
	SystemName NCHAR(200) NOT NULL,
	CONSTRAINT P4JSystem_pk PRIMARY KEY CLUSTERED (
		SystemID
	)
)
GO

CREATE UNIQUE INDEX P4JSystem_SystemName_idx 
	ON dbo.P4JSystem (
		SystemName
)
GO

INSERT INTO dbo.P4JSystem (SystemName) VALUES ('Default');
GO

ALTER TABLE dbo.P4JIntervalData 
	ADD SystemID INT NOT NULL DEFAULT 1
GO

ALTER TABLE dbo.P4JIntervalData 
	ADD CONSTRAINT P4JIntervalData_SystemID_fk FOREIGN KEY (
		SystemID
	) REFERENCES dbo.P4JSystem (
		SystemID
	) ON DELETE CASCADE
GO	

ALTER TABLE dbo.P4JUserAgentOccurance 
	ADD SystemID INT NOT NULL DEFAULT 1
GO

ALTER TABLE dbo.P4JUserAgentOccurance 
	ADD CONSTRAINT P4JUserAgentOccurance_SystemID_fk FOREIGN KEY (
		SystemID
	) REFERENCES dbo.P4JSystem (
		SystemID
	) ON DELETE CASCADE
GO	

DROP VIEW dbo.P4JUserAgentView
GO

CREATE VIEW dbo.P4JUserAgentView AS
SELECT 
	oc.SystemID
	,oc.CollectionDate
	,b.BrowserName
	,bv.BrowserVersion
	,os.OSName
	,osv.OSVersion
	,oc.RequestCount
FROM dbo.P4JUserAgentOccurance oc
JOIN dbo.P4JUserAgentBrowser b ON b.BrowserID = oc.BrowserID
JOIN dbo.P4JUserAgentBrowserVersion bv ON bv.BrowserVersionID = oc.BrowserVersionID
JOIN dbo.P4JUserAgentOS os ON os.OSID = oc.OSID
JOIN dbo.P4JUserAgentOSVersion osv ON osv.OSVersionID = oc.OSVersionID
GO

ALTER TABLE dbo.P4JGarbageCollection 
	ADD SystemID INT NOT NULL DEFAULT 1
GO

ALTER TABLE dbo.P4JGarbageCollection 
	ADD CONSTRAINT P4JGarbageCollection_SystemID_fk FOREIGN KEY (
		SystemID
	) REFERENCES dbo.P4JSystem (
		SystemID
	) ON DELETE CASCADE
GO	

ALTER TABLE dbo.P4JVMSnapShot 
	ADD SystemID INT NOT NULL DEFAULT 1
GO

ALTER TABLE dbo.P4JVMSnapShot 
	ADD CONSTRAINT P4JVMSnapShot_SystemID_fk FOREIGN KEY (
		SystemID
	) REFERENCES dbo.P4JSystem (
		SystemID
	) ON DELETE CASCADE
GO	

ALTER TABLE dbo.P4JMemoryPool 
	ADD SystemID INT NOT NULL DEFAULT 1
GO

ALTER TABLE dbo.P4JMemoryPool 
	ADD CONSTRAINT P4JMemoryPool_SystemID_fk FOREIGN KEY (
		SystemID
	) REFERENCES dbo.P4JSystem (
		SystemID
	) ON DELETE CASCADE
GO	

ALTER TABLE dbo.P4JGlobalRequestProcessor 
	ADD SystemID INT NOT NULL DEFAULT 1
GO

ALTER TABLE dbo.P4JGlobalRequestProcessor 
	ADD CONSTRAINT P4JGlobalRequestProcessor_SystemID_fk FOREIGN KEY (
		SystemID
	) REFERENCES dbo.P4JSystem (
		SystemID
	) ON DELETE CASCADE
GO	

ALTER TABLE dbo.P4JThreadPoolMonitor 
	ADD SystemID INT NOT NULL DEFAULT 1
GO

ALTER TABLE dbo.P4JThreadPoolMonitor 
	ADD CONSTRAINT P4JThreadPoolMonitor_SystemID_fk FOREIGN KEY (
		SystemID
	) REFERENCES dbo.P4JSystem (
		SystemID
	) ON DELETE CASCADE
GO	

ALTER TABLE dbo.P4JThreadTrace 
	ADD SystemID INT NOT NULL DEFAULT 1
GO

ALTER TABLE dbo.P4JThreadTrace 
	ADD CONSTRAINT P4JThreadTrace_SystemID_fk FOREIGN KEY (
		SystemID
	) REFERENCES dbo.P4JSystem (
		SystemID
	) ON DELETE CASCADE
GO	
