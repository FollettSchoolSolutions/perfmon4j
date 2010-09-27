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
DROP TABLE dbo.P4JThreadTrace
GO

DROP TABLE dbo.P4JThreadPoolMonitor
GO

DROP TABLE dbo.P4JGlobalRequestProcessor
GO

DROP TABLE dbo.P4JMemoryPool
GO

DROP TABLE dbo.P4JVMSnapShot
GO

DROP TABLE dbo.P4JGarbageCollection
GO

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
	/*  START NEW Columns added in Perfmon4j 1,1,0 */
	SQLMaxDuration INT NULL,
	SQLMaxDurationSet DATETIME NULL,
	SQLMinDuration INT NULL,
	SQLMinDurationSet DATETIME NULL,
	SQLAverageDuration DECIMAL(18, 2) NULL,
	SQLStandardDeviation DECIMAL(18, 2) NULL,
	SQLDurationSum BIGINT NULL,
	SQLDurationSumOfSquares BIGINT NULL,
	/*  STOP NEW Columns added in Perfmon4j 1,1,0 */
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
	BrowserName VARCHAR(100) NOT NULL,
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
	),
	CONSTRAINT P4JUserAgentOccurance_BrowserID_fk FOREIGN KEY (
		BrowserID
	) REFERENCES dbo.P4JUserAgentBrowser (
		BrowserID
	) ON DELETE CASCADE,
	CONSTRAINT P4JUserAgentOccurance_BrowserVersionID_fk FOREIGN KEY (
		BrowserVersionID
	) REFERENCES dbo.P4JUserAgentBrowserVersion (
		BrowserVersionID
	) ON DELETE CASCADE,
	CONSTRAINT P4JUserAgentOccurance_OSID_fk FOREIGN KEY (
		OSID
	) REFERENCES dbo.P4JUserAgentOS (
		OSID
	) ON DELETE CASCADE,
	CONSTRAINT P4JUserAgentOccurance_OSVersionID_fk FOREIGN KEY (
		OSID
	) REFERENCES dbo.P4JUserAgentOSVersion (
		OSVersionID
	) ON DELETE CASCADE
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
GO

CREATE TABLE dbo.P4JGarbageCollection(
	InstanceName NVARCHAR(200) NOT NULL,
	StartTime DATETIME NOT NULL,
	EndTime DATETIME NOT NULL,
	Duration INT NOT NULL,
	NumCollections INT NOT NULL,
	CollectionMillis INT NOT NULL,
	NumCollectionsPerMinute DECIMAL(18,2) NOT NULL,
	CollectionMillisPerMinute DECIMAL(18,2) NOT NULL,
	CONSTRAINT P4JGarbageCollection_pk PRIMARY KEY CLUSTERED (
		InstanceName,
		StartTime,
		EndTime 
	)
)
GO

CREATE TABLE dbo.P4JVMSnapShot(
	StartTime DATETIME NOT NULL,
	EndTime DATETIME NOT NULL,
	Duration INT NOT NULL,
	CurrentClassLoadCount INT NOT NULL,
	ClassLoadCountInPeriod INT NOT NULL,
	ClassLoadCountPerMinute DECIMAL(18,2) NOT NULL,
	ClassUnloadCountInPeriod INT NOT NULL,
	ClassUnloadCountPerMinute DECIMAL(18,2) NOT NULL,
	PendingClassFinalizationCount INT NOT NULL,
	CurrentThreadCount INT NOT NULL,
	CurrentDaemonThreadCount INT NOT NULL,
	ThreadStartCountInPeriod INT NOT NULL,
	ThreadStartCountPerMinute DECIMAL(18,2) NOT NULL,
	HeapMemUsedMB  DECIMAL(18,2)  NOT NULL,
	HeapMemCommitedMB DECIMAL(18,2) NOT NULL,
	HeapMemMaxMB DECIMAL(18,2) NOT NULL,
	NonHeapMemUsedMB  DECIMAL(18,2)  NOT NULL,
	NonHeapMemCommittedUsedMB  DECIMAL(18,2) NOT NULL,
	NonHeapMemMaxUsedMB  DECIMAL(18,2) NOT NULL,
	SystemLoadAverage DECIMAL(5, 2) NULL,
	CompilationMillisInPeriod  INT NULL,
	CompilationMillisPerMinute DECIMAL(18,2) NULL,
	CONSTRAINT P4JVMSnapShot_pk PRIMARY KEY CLUSTERED (
		StartTime,
		EndTime 
	)
)
GO


CREATE TABLE dbo.P4JMemoryPool(
	InstanceName NVARCHAR(200) NOT NULL,
	StartTime DATETIME NOT NULL,
	EndTime DATETIME NOT NULL,
	Duration INT NOT NULL,
	InitialMB DECIMAL(18,2) NOT NULL,
	UsedMB DECIMAL(18,2) NOT NULL,
	CommittedMB DECIMAL(18,2) NOT NULL,
	MaxMB DECIMAL(18,2) NOT NULL,
	MemoryType NVARCHAR(50) NULL,
	CONSTRAINT P4JMemoryPool_pk PRIMARY KEY CLUSTERED (
		InstanceName,
		StartTime,
		EndTime 
	)
)
GO


CREATE TABLE dbo.P4JGlobalRequestProcessor(
	InstanceName NVARCHAR(200) NOT NULL,
	StartTime DATETIME NOT NULL,
	EndTime DATETIME NOT NULL,
	Duration INT NOT NULL,
	RequestCountInPeriod INT NOT NULL,
	RequestCountPerMinute DECIMAL(18,2) NOT NULL,
	KBytesSentInPeriod DECIMAL(18, 2) NOT NULL,
	KBytesSentPerMinute DECIMAL(18, 2) NOT NULL,
	KBytesReceivedInPeriod DECIMAL(18, 2) NOT NULL,
	KBytesReceivedPerMinute DECIMAL(18, 2) NOT NULL,
	ProcessingMillisInPeriod INT NOT NULL,
	ProcessingMillisPerMinute DECIMAL(18, 2) NOT NULL,
	ErrorCountInPeriod INT NOT NULL,
	ErrorCountPerMinute DECIMAL(18, 2) NOT NULL,
	CONSTRAINT P4JGlobalRequestProcessor_pk PRIMARY KEY CLUSTERED (
		InstanceName,
		StartTime,
		EndTime 
	)
)
GO

CREATE TABLE dbo.P4JThreadPoolMonitor(
	ThreadPoolOwner NVARCHAR(50) NOT NULL,
	InstanceName NVARCHAR(200) NOT NULL,
	StartTime DATETIME NOT NULL,
	EndTime DATETIME NOT NULL,
	Duration INT NOT NULL,
	CurrentThreadsBusy INT NOT NULL,
	CurrentThreadCount INT NOT NULL,
	CONSTRAINT P4JThredPoolMonitor_pk PRIMARY KEY CLUSTERED (
		ThreadPoolOwner,
		InstanceName,
		StartTime,
		EndTime 
	)
)
GO

CREATE TABLE dbo.P4JThreadTrace(
	TraceRowID INT IDENTITY NOT NULL,
	ParentRowID INT NULL,
	CategoryID INT NOT NULL,
	StartTime DATETIME NOT NULL,
	EndTime DATETIME NOT NULL,
	Duration INT NOT NULL,
	/*  START NEW Columns added in Perfmon4j 1,1,0 */
	SQLDuration INT NULL,
	/*  STOP NEW Columns added in Perfmon4j 1,1,0 */
	CONSTRAINT P4JThreadTrace_pk PRIMARY KEY CLUSTERED (
		TraceRowID 
	),
	CONSTRAINT P4JThreadTraceCategoryID_fk FOREIGN KEY (
		CategoryID
	) REFERENCES dbo.P4JCategory (
		CategoryID
	) ON DELETE CASCADE,
	CONSTRAINT P4JParentRowID_fk FOREIGN KEY (
		ParentRowID
	) REFERENCES dbo.P4JThreadTrace (
		TraceRowID
	) 
)
GO

CREATE INDEX P4JThreadTrace_StartTime_idx
	ON dbo.P4JThreadTrace (
		StartTime,
		CategoryID
	)
GO

CREATE INDEX P4JThreadTrace_ParentRowID_idx
	ON dbo.P4JThreadTrace (
		ParentRowID
	)
GO

