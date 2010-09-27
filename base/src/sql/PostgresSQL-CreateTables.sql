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
Notes - This script contains the SQL required to build the tables required for 
the "org.perfmon4j.JDBCSQLAppender" and "org.perfmon4j.PooledSQLAppender".
This script has been tested and works with PostgresSQL but should be
easily modified for other databases.
******************************************************************************  */

/* Uncomment the following lines to drop the existing
 tables */

/*

DROP TABLE P4JThreadTrace;

DROP TABLE P4JThreadPoolMonitor;

DROP TABLE P4JGlobalRequestProcessor;

DROP TABLE P4JMemoryPool;

DROP TABLE P4JVMSnapShot;

DROP TABLE P4JGarbageCollection;

DROP VIEW P4JUserAgentView;

DROP TABLE P4JUserAgentOccurance;

DROP TABLE P4JUserAgentOSVersion;

DROP TABLE P4JUserAgentOS;

DROP TABLE P4JUserAgentBrowserVersion;

DROP TABLE P4JUserAgentBrowser;

DROP TABLE P4JIntervalThreshold;

DROP TABLE P4JIntervalData;

DROP TABLE P4JCategory;
*/

CREATE TABLE P4JCategory (
	CategoryID SERIAL PRIMARY KEY,
	CategoryName NCHAR(512) NOT NULL
);

CREATE UNIQUE INDEX P4JCategory_CategoryName_idx
	ON P4JCategory (
		CategoryName
);

CREATE TABLE P4JIntervalData (
	IntervalID SERIAL PRIMARY KEY,
	CategoryID INT NOT NULL,
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
	TotalHits BIGINT NOT NULL,
	TotalCompletions BIGINT NOT NULL,
	MaxActiveThreads INT NOT NULL,
	MaxActiveThreadsSet TIMESTAMP NULL,
	MaxDuration INT NOT NULL,
	MaxDurationSet TIMESTAMP NULL,
	MinDuration INT NOT NULL,
	MinDurationSet TIMESTAMP NULL,
	AverageDuration DECIMAL(18, 2) NOT NULL,
	MedianDuration DECIMAL(18, 2) NULL,
	StandardDeviation DECIMAL(18, 2) NOT NULL,
	NormalizedThroughputPerMinute DECIMAL(18, 2) NOT NULL,
	DurationSum BIGINT NOT NULL,
	DurationSumOfSquares BIGINT NOT NULL,
	/*  START NEW Columns added in Perfmon4j 1,1,0 */
	SQLMaxDuration INT NULL,
	SQLMaxDurationSet TIMESTAMP NULL,
	SQLMinDuration INT NULL,
	SQLMinDurationSet TIMESTAMP NULL,
	SQLAverageDuration DECIMAL(18, 2) NULL,
	SQLStandardDeviation DECIMAL(18, 2) NULL,
	SQLDurationSum BIGINT NULL,
	SQLDurationSumOfSquares BIGINT NULL,
	/*  STOP NEW Columns added in Perfmon4j 1,1,0 */
	CONSTRAINT P4JIntervalData_CategoryID_fk FOREIGN KEY (
		CategoryID
	) REFERENCES P4JCategory (
		CategoryID
	) ON DELETE CASCADE
); 

CREATE INDEX P4JIntervalData_Category_idx
	ON P4JIntervalData (
		CategoryID,
		StartTime,
		EndTime
);

CREATE TABLE P4JIntervalThreshold (
	IntervalID INT NOT NULL,
	ThresholdMillis INT NOT NULL,
	PercentOver DECIMAL(5, 2) NOT NULL,
	CompletionsOver INT NOT NULL,
	PRIMARY KEY (
		IntervalID,
		ThresholdMillis,
		PercentOver
	),
	CONSTRAINT P4JIntervalThreshold_IntervalID_fk FOREIGN KEY (
		IntervalID
	) REFERENCES P4JIntervalData (
		IntervalID
	) ON DELETE CASCADE
); 

/*
Tables for the UserAgentSnapShotMonitor
*/
CREATE TABLE P4JUserAgentBrowser(
	BrowserID SERIAL PRIMARY KEY,
	BrowserName VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX P4JUserAgentBrowser_BrowserName_idx
	ON P4JUserAgentBrowser (
		BrowserName
);

CREATE TABLE P4JUserAgentBrowserVersion(
	BrowserVersionID SERIAL PRIMARY KEY,
	BrowserVersion VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX P4JUserAgentBrowserVersion_BrowserName_idx
	ON P4JUserAgentBrowserVersion (
		BrowserVersion
);

CREATE TABLE P4JUserAgentOS(
	OSID SERIAL PRIMARY KEY,
	OSName VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX P4JUserAgentOS_OSName_idx
	ON P4JUserAgentOS (
		OSName
);

CREATE TABLE P4JUserAgentOSVersion(
	OSVersionID SERIAL PRIMARY KEY,
	OSVersion VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX P4JUserAgentOSVersion_OSVersion_idx
	ON P4JUserAgentOSVersion (
		OSVersion
);

CREATE TABLE P4JUserAgentOccurance (
	CollectionDate DATE NOT NULL,
	BrowserID INT NOT NULL,
	BrowserVersionID INT NOT NULL,
	OSID INT NOT NULL,
	OSVersionID INT NOT NULL,
	RequestCount INT NOT NULL DEFAULT 0,
	CONSTRAINT P4JUserAgentOccurance_pk PRIMARY KEY (
		CollectionDate,
		BrowserID,
		BrowserVersionID,
		OSID,
		OSVersionID
	),
	CONSTRAINT P4JUserAgentOccurance_BrowserID_fk FOREIGN KEY (
		BrowserID
	) REFERENCES P4JUserAgentBrowser (
		BrowserID
	) ON DELETE CASCADE,
	CONSTRAINT P4JUserAgentOccurance_BrowserVersionID_fk FOREIGN KEY (
		BrowserVersionID
	) REFERENCES P4JUserAgentBrowserVersion (
		BrowserVersionID
	) ON DELETE CASCADE,
	CONSTRAINT P4JUserAgentOccurance_OSID_fk FOREIGN KEY (
		OSID
	) REFERENCES P4JUserAgentOS (
		OSID
	) ON DELETE CASCADE,
	CONSTRAINT P4JUserAgentOccurance_OSVersionID_fk FOREIGN KEY (
		OSID
	) REFERENCES P4JUserAgentOSVersion (
		OSVersionID
	) ON DELETE CASCADE
);

CREATE VIEW P4JUserAgentView AS
SELECT 
	oc.CollectionDate
	,b.BrowserName
	,bv.BrowserVersion
	,os.OSName
	,osv.OSVersion
	,oc.RequestCount
FROM P4JUserAgentOccurance oc
JOIN P4JUserAgentBrowser b ON b.BrowserID = oc.BrowserID
JOIN P4JUserAgentBrowserVersion bv ON bv.BrowserVersionID = oc.BrowserVersionID
JOIN P4JUserAgentOS os ON os.OSID = oc.OSID
JOIN P4JUserAgentOSVersion osv ON osv.OSVersionID = oc.OSVersionID;

CREATE TABLE P4JGarbageCollection(
	InstanceName VARCHAR(200) NOT NULL,
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
	Duration INT NOT NULL,
	NumCollections INT NOT NULL,
	CollectionMillis INT NOT NULL,
	NumCollectionsPerMinute DECIMAL(18,2) NOT NULL,
	CollectionMillisPerMinute DECIMAL(18,2) NOT NULL,
	CONSTRAINT P4JGarbageCollection_pk PRIMARY KEY (
		InstanceName,
		StartTime,
		EndTime 
	)
);

CREATE TABLE P4JVMSnapShot(
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
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
	CONSTRAINT P4JVMSnapShot_pk PRIMARY KEY (
		StartTime,
		EndTime 
	)
);


CREATE TABLE P4JMemoryPool(
	InstanceName VARCHAR(200) NOT NULL,
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
	Duration INT NOT NULL,
	InitialMB DECIMAL(18,2) NOT NULL,
	UsedMB DECIMAL(18,2) NOT NULL,
	CommittedMB DECIMAL(18,2) NOT NULL,
	MaxMB DECIMAL(18,2) NOT NULL,
	MemoryType VARCHAR(50) NULL,
	CONSTRAINT P4JMemoryPool_pk PRIMARY KEY (
		InstanceName,
		StartTime,
		EndTime 
	)
);

CREATE TABLE P4JGlobalRequestProcessor(
	InstanceName VARCHAR(200) NOT NULL,
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
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
	CONSTRAINT P4JGlobalRequestProcessor_pk PRIMARY KEY (
		InstanceName,
		StartTime,
		EndTime 
	)
);

CREATE TABLE P4JThreadPoolMonitor(
	ThreadPoolOwner VARCHAR(50) NOT NULL,
	InstanceName VARCHAR(200) NOT NULL,
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
	Duration INT NOT NULL,
	CurrentThreadsBusy INT NOT NULL,
	CurrentThreadCount INT NOT NULL,
	CONSTRAINT P4JThredPoolMonitor_pk PRIMARY KEY (
		ThreadPoolOwner,
		InstanceName,
		StartTime,
		EndTime 
	)
);

CREATE TABLE P4JThreadTrace(
	TraceRowID SERIAL PRIMARY KEY,
	ParentRowID INT NULL,
	CategoryID INT NOT NULL,
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
	Duration INT NOT NULL,
	/*  START NEW Columns added in Perfmon4j 1,1,0 */
	SQLDuration INT NULL,
	/*  STOP NEW Columns added in Perfmon4j 1,1,0 */
	CONSTRAINT P4JThreadTraceCategoryID_fk FOREIGN KEY (
		CategoryID
	) REFERENCES P4JCategory (
		CategoryID
	) ON DELETE CASCADE,
	CONSTRAINT P4JParentRowID_fk FOREIGN KEY (
		ParentRowID
	) REFERENCES P4JThreadTrace (
		TraceRowID
	) 
);

CREATE INDEX P4JThreadTrace_StartTime_idx
	ON P4JThreadTrace (
		StartTime,
		CategoryID
);

CREATE INDEX P4JThreadTrace_ParentRowID_idx
	ON P4JThreadTrace (
		ParentRowID
);


