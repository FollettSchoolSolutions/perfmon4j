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
This script has been tested and works with ORACLE XE 10g  but should be
easily modified for other versions.
******************************************************************************  */

/* Uncomment the following lines to drop the existing
 tables */
 /*
DROP TRIGGER P4JThreadTrace_trg;

DROP SEQUENCE P4JThreadTrace_seq;

DROP TABLE P4JThreadTrace;

DROP TABLE P4JThreadPoolMonitor;

DROP TABLE P4JGlobalRequestProcessor;

DROP TABLE P4JMemoryPool;

DROP TABLE P4JVMSnapShot;

DROP TABLE P4JGarbageCollection;

DROP VIEW P4JUserAgentView; 

DROP TABLE P4JUserAgentOccurance;

DROP TRIGGER P4JUserAgentOSVersion_trg;

DROP SEQUENCE P4JUserAgentOSVersion_seq;

DROP TABLE P4JUserAgentOSVersion;

DROP TRIGGER P4JUserAgentOS_trg;

DROP SEQUENCE P4JUserAgentOS_seq;

DROP TABLE P4JUserAgentOS;

DROP TRIGGER P4JUserAgentBrowserVersion_trg;

DROP SEQUENCE P4JUserAgentBrowserVersion_seq;

DROP TABLE P4JUserAgentBrowserVersion;

DROP TRIGGER P4JUserAgentBrowser_trg;

DROP SEQUENCE P4JUserAgentBrowser_seq;

DROP TABLE P4JUserAgentBrowser;

DROP TABLE P4JIntervalThreshold;

DROP TRIGGER P4JIntervalData_trg;

DROP SEQUENCE P4JIntervalData_seq;

DROP TABLE P4JIntervalData;

DROP TRIGGER P4JCategory_trg;

DROP SEQUENCE P4JCategory_seq;

DROP TABLE P4JCategory;
*/

CREATE TABLE P4JCategory (
	CategoryID NUMBER PRIMARY KEY,
	CategoryName VARCHAR2(512) NOT NULL
);

CREATE  UNIQUE INDEX P4JCategory_CategoryName_idx
	ON P4JCategory (
		CategoryName
);


CREATE SEQUENCE P4JCategory_seq;


CREATE OR REPLACE TRIGGER P4JCategory_trg
BEFORE INSERT ON P4JCategory
FOR EACH ROW
BEGIN
    IF :new.CategoryID IS null
    THEN
        SELECT P4JCategory_seq.nextval INTO :new.CategoryID FROM DUAL;
    END IF;
END;
/

CREATE TABLE P4JIntervalData (
	IntervalID NUMBER PRIMARY KEY,
	CategoryID NUMBER NOT NULL,
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
	TotalHits NUMBER(18) NOT NULL,
	TotalCompletions NUMBER(18) NOT NULL,
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
	DurationSum NUMBER(18) NOT NULL,
	DurationSumOfSquares NUMBER(18) NOT NULL,
	/*  START NEW Columns added in Perfmon4j 1,1,0 */
	SQLMaxDuration INT NULL,
	SQLMaxDurationSet TIMESTAMP NULL,
	SQLMinDuration INT NULL,
	SQLMinDurationSet TIMESTAMP NULL,
	SQLAverageDuration DECIMAL(18, 2) NULL,
	SQLStandardDeviation DECIMAL(18, 2) NULL,
	SQLDurationSum NUMBER(18) NULL,
	SQLDurationSumOfSquares NUMBER(18) NULL,
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

CREATE SEQUENCE P4JIntervalData_seq;


CREATE OR REPLACE TRIGGER P4JIntervalData_trg
BEFORE INSERT ON P4JIntervalData
FOR EACH ROW
BEGIN
    IF :new.IntervalID IS null
    THEN
        SELECT P4JIntervalData_seq.nextval INTO :new.IntervalID FROM DUAL;
    END IF;
END;
/

CREATE TABLE P4JIntervalThreshold (
	IntervalID NUMBER NOT NULL,
	ThresholdMillis NUMBER NOT NULL,
	PercentOver DECIMAL(5, 2) NOT NULL,
	CompletionsOver NUMBER NOT NULL,
	PRIMARY KEY (
		IntervalID,
		ThresholdMillis,
		PercentOver
	),
	CONSTRAINT P4JIntTh_IntervalID_fk FOREIGN KEY (
		IntervalID
	) REFERENCES P4JIntervalData (
		IntervalID
	) ON DELETE CASCADE
); 


CREATE TABLE P4JUserAgentBrowser(
	BrowserID NUMBER PRIMARY KEY,
	BrowserName VARCHAR2(100) NOT NULL
);


CREATE UNIQUE INDEX P4JUAB_BrowserName_idx
	ON P4JUserAgentBrowser (
		BrowserName
);


CREATE SEQUENCE P4JUserAgentBrowser_seq;


CREATE OR REPLACE TRIGGER P4JUserAgentBrowser_trg
BEFORE INSERT ON P4JUserAgentBrowser
FOR EACH ROW
BEGIN
    IF :new.BrowserID IS null
    THEN
        SELECT P4JUserAgentBrowser_seq.nextval INTO :new.BrowserID FROM DUAL;
    END IF;
END;
/


CREATE TABLE P4JUserAgentBrowserVersion(
	BrowserVersionID NUMBER PRIMARY KEY,
	BrowserVersion VARCHAR2(50) NOT NULL
);


CREATE UNIQUE INDEX P4JUABV_BrowserName_idx
	ON P4JUserAgentBrowserVersion (
		BrowserVersion
);


CREATE SEQUENCE P4JUserAgentBrowserVersion_seq;


CREATE OR REPLACE TRIGGER P4JUserAgentBrowserVersion_trg
BEFORE INSERT ON P4JUserAgentBrowserVersion
FOR EACH ROW
BEGIN
    IF :new.BrowserVersionID IS null
    THEN
        SELECT P4JUserAgentBrowserVersion_seq.nextval INTO :new.BrowserVersionID FROM DUAL;
    END IF;
END;
/


CREATE TABLE P4JUserAgentOS(
	OSID NUMBER PRIMARY KEY,
	OSName VARCHAR2(100) NOT NULL
);

CREATE UNIQUE INDEX P4JUserAgentOS_OSName_idx
	ON P4JUserAgentOS (
		OSName
);


CREATE SEQUENCE P4JUserAgentOS_seq;


CREATE OR REPLACE TRIGGER P4JUserAgentOS_trg
BEFORE INSERT ON P4JUserAgentOS
FOR EACH ROW
BEGIN
    IF :new.OSID IS null
    THEN
        SELECT P4JUserAgentOS_seq.nextval INTO :new.OSID FROM DUAL;
    END IF;
END;
/


CREATE TABLE P4JUserAgentOSVersion(
	OSVersionID NUMBER PRIMARY KEY,
	OSVersion VARCHAR2(50) NOT NULL
);

CREATE UNIQUE INDEX P4JUAOV_idx
	ON P4JUserAgentOSVersion (
		OSVersion
);

CREATE SEQUENCE P4JUserAgentOSVersion_seq;


CREATE OR REPLACE TRIGGER P4JUserAgentOSVersion_trg
BEFORE INSERT ON P4JUserAgentOSVersion
FOR EACH ROW
BEGIN
    IF :new.OSVersionID IS null
    THEN
        SELECT P4JUserAgentOSVersion_seq.nextval INTO :new.OSVersionID FROM DUAL;
    END IF;
END;
/


CREATE TABLE P4JUserAgentOccurance (
	CollectionDate DATE NOT NULL,
	BrowserID NUMBER NOT NULL,
	BrowserVersionID NUMBER NOT NULL,
	OSID NUMBER NOT NULL,
	OSVersionID NUMBER NOT NULL,
	RequestCount NUMBER DEFAULT 0,
	CONSTRAINT P4JUserAgentOccurance_pk PRIMARY KEY (
		CollectionDate,
		BrowserID,
		BrowserVersionID,
		OSID,
		OSVersionID
	),
	CONSTRAINT P4JUAO_BrowserID_fk FOREIGN KEY (
		BrowserID
	) REFERENCES P4JUserAgentBrowser (
		BrowserID
	) ON DELETE CASCADE,
	CONSTRAINT P4UAO_BrowserVersionID_fk FOREIGN KEY (
		BrowserVersionID
	) REFERENCES P4JUserAgentBrowserVersion (
		BrowserVersionID
	) ON DELETE CASCADE,
	CONSTRAINT P4JUAO_OSID_fk FOREIGN KEY (
		OSID
	) REFERENCES P4JUserAgentOS (
		OSID
	) ON DELETE CASCADE,
	CONSTRAINT P4JUAO_OSVersionID_fk FOREIGN KEY (
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
	InstanceName VARCHAR2(200) NOT NULL,
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
	CONSTRAINT P4JVMSnapShot_pk PRIMARY KEY  (
		StartTime,
		EndTime 
	)
);


CREATE TABLE P4JMemoryPool(
	InstanceName VARCHAR2(200) NOT NULL,
	StartTime TIMESTAMP NOT NULL,
	EndTime TIMESTAMP NOT NULL,
	Duration INT NOT NULL,
	InitialMB DECIMAL(18,2) NOT NULL,
	UsedMB DECIMAL(18,2) NOT NULL,
	CommittedMB DECIMAL(18,2) NOT NULL,
	MaxMB DECIMAL(18,2) NOT NULL,
	MemoryType VARCHAR2(50) NULL,
	CONSTRAINT P4JMemoryPool_pk PRIMARY KEY (
		InstanceName,
		StartTime,
		EndTime 
	)
);

CREATE TABLE P4JGlobalRequestProcessor(
	InstanceName VARCHAR2(200) NOT NULL,
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
	ThreadPoolOwner VARCHAR2(50) NOT NULL,
	InstanceName VARCHAR2(200) NOT NULL,
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
	TraceRowID NUMBER PRIMARY KEY,
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

CREATE SEQUENCE P4JThreadTrace_seq;

CREATE OR REPLACE TRIGGER P4JThreadTrace_trg
BEFORE INSERT ON P4JThreadTrace
FOR EACH ROW
BEGIN
    IF :new.TraceRowID IS null
    THEN
        SELECT P4JThreadTrace_seq.nextval INTO :new.TraceRowID FROM DUAL;
    END IF;
END;
/

CREATE INDEX P4JThreadTrace_StartTime_idx
	ON P4JThreadTrace (
		StartTime,
		CategoryID
);

CREATE INDEX P4JThreadTrace_ParentRowID_idx
	ON P4JThreadTrace (
		ParentRowID
);
