/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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

/*  ******************************************************************************
Notes - This script contains the SQL required to build the tables required for the
"org.perfmon4j.JDBCSQLAppender" and "org.perfmon4j.PooledSQLAppender".
This script has been tested and works with Microsoft SQL 2005 but should be
easily modified for other databases.
******************************************************************************  */

/** Uncomment the following to drop existing tables **/
-- DROP Tables
/*
DROP VIEW dbo.P4JUserAgentView
GO

DROP TABLE dbo.P4JUserAgentOccurance
GO

DROP TABLE dbo.P4JUserAgentOSVersion
GO

DROP TABLE dbo.P4JUserAgentOS
GO

DROP TABLE dbo.P4JUserAgentBrowserVersion
GO

DROP TABLE dbo.P4JUserAgentBrowser
GO

DROP TABLE dbo.P4JIntervalThreshold
GO

DROP TABLE dbo.P4JIntervalData
GO

DROP TABLE dbo.P4JCategory
GO
*/

CREATE TABLE dbo.P4JCategory (
	CategoryID INT IDENTITY(1,1) NOT NULL,
	CategoryName NCHAR(450) NOT NULL,
	CONSTRAINT P4JCategory_pk PRIMARY KEY CLUSTERED (
		CategoryID
	)
)
GO

CREATE  UNIQUE INDEX P4JCategory_CategoryName_idx
	ON dbo.P4JCategory (
		CategoryName
	)
GO

CREATE TABLE dbo.P4JIntervalData (
	IntervalID INT IDENTITY(1,1) NOT NULL,
	CategoryID INT NOT NULL,
	StartTime DATETIME NOT NULL,
	EndTime DATETIME NOT NULL,
	TotalHits BIGINT NOT NULL,
	TotalCompletions BIGINT NOT NULL,
	MaxActiveThreads INT NOT NULL,
	MaxActiveThreadsSet DATETIME NULL,
	MaxDuration INT NOT NULL,
	MaxDurationSet DATETIME NULL,
	MinDuration INT NOT NULL,
	MinDurationSet DATETIME NULL,
	AverageDuration DECIMAL(18, 2) NOT NULL,
	MedianDuration DECIMAL(18, 2) NULL,
	StandardDeviation DECIMAL(18, 2) NOT NULL,
	NormalizedThroughputPerMinute DECIMAL(18, 2) NOT NULL,
	DurationSum BIGINT NOT NULL,
	DurationSumOfSquares BIGINT NOT NULL,
	CONSTRAINT P4JIntervalData_pk PRIMARY KEY CLUSTERED (
		IntervalID 
	),
	CONSTRAINT P4JIntervalData_CategoryID_fk FOREIGN KEY (
		CategoryID
	) REFERENCES dbo.P4JCategory (
		CategoryID
	) ON DELETE CASCADE
) 
GO

CREATE INDEX P4JIntervalData_Category_idx
	ON dbo.P4JIntervalData (
		CategoryID,
		StartTime,
		EndTime
	)
GO

CREATE TABLE dbo.P4JIntervalThreshold (
	IntervalID INT NOT NULL,
	ThresholdMillis INT NOT NULL,
	PercentOver DECIMAL(5, 2) NOT NULL,
	CompletionsOver INT NOT NULL,
	CONSTRAINT P4JIntervalThreshold_pk PRIMARY KEY CLUSTERED (
		IntervalID,
		ThresholdMillis,
		PercentOver
	),
	CONSTRAINT P4JIntervalThreshold_IntervalID_fk FOREIGN KEY (
		IntervalID
	) REFERENCES dbo.P4JIntervalData (
		IntervalID
	) ON DELETE CASCADE
) 
GO

/*
Tables for the UserAgentSnapShotMonitor
*/
CREATE TABLE dbo.P4JUserAgentBrowser(
	BrowserID INT IDENTITY NOT NULL ,
	BrowserName VARCHAR(100),
	CONSTRAINT P4JUserAgentBrowser_pk PRIMARY KEY CLUSTERED (
		BrowserID
	)
  )
GO

CREATE UNIQUE INDEX P4JUserAgentBrowser_BrowserName_idx
	ON dbo.P4JUserAgentBrowser (
		BrowserName
	)
GO
  
CREATE TABLE dbo.P4JUserAgentBrowserVersion(
	BrowserVersionID INT IDENTITY NOT NULL,
	BrowserVersion VARCHAR(50) NOT NULL,
	CONSTRAINT P4JUserAgentBrowserVersion_pk PRIMARY KEY CLUSTERED (
		BrowserVersionID
	)
)
GO

CREATE UNIQUE INDEX P4JUserAgentBrowserVersion_BrowserName_idx
	ON dbo.P4JUserAgentBrowserVersion (
		BrowserVersion
	)
GO

CREATE TABLE dbo.P4JUserAgentOS(
	OSID INT IDENTITY NOT NULL,
	OSName VARCHAR(100) NOT NULL,
	CONSTRAINT P4JUserAgentOS_pk PRIMARY KEY CLUSTERED (
		OSID
	)
)
GO

CREATE UNIQUE INDEX P4JUserAgentOS_OSName_idx
	ON dbo.P4JUserAgentOS (
		OSName
	)
GO

CREATE TABLE dbo.P4JUserAgentOSVersion(
	OSVersionID INT IDENTITY NOT NULL,
	OSVersion VARCHAR(50) NOT NULL,
	CONSTRAINT P4JUserAgentOSVersoin_pk PRIMARY KEY CLUSTERED (
		OSVersionID
	)
)
GO

CREATE UNIQUE INDEX P4JUserAgentOSVersion_OSVersion_idx
	ON dbo.P4JUserAgentOSVersion (
		OSVersion
	)
GO

CREATE TABLE dbo.P4JUserAgentOccurance (
	CollectionDate DATETIME NOT NULL,
	BrowserID INT NOT NULL,
	BrowserVersionID INT NOT NULL,
	OSID INT NOT NULL,
	OSVersionID INT NOT NULL,
	RequestCount INT NOT NULL DEFAULT 0,
	CONSTRAINT P4JUserAgentOccurance_pk PRIMARY KEY CLUSTERED (
		CollectionDate,
		BrowserID,
		BrowserVersionID,
		OSID,
		OSVersionID
	) /*,
	CONSTRAINT P4JIntervalThreshold_IntervalID_fk FOREIGN KEY (
		IntervalID
	) REFERENCES dbo.P4JIntervalData (
		IntervalID
	) ON DELETE CASCADE
*/
)
GO

CREATE VIEW dbo.P4JUserAgentView AS
SELECT 
	oc.CollectionDate
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
