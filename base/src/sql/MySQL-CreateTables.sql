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
This script has been tested and works with MySQL but should be
easily modified for other databases.
******************************************************************************  */

/* Uncomment the following lines to drop the existing
 tables */
 /*
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
	CategoryID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	CategoryName NCHAR(255) NOT NULL
);

CREATE  UNIQUE INDEX P4JCategory_CategoryName_idx
	ON P4JCategory (
		CategoryName
);

CREATE TABLE P4JIntervalData (
	IntervalID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
	BrowserID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	BrowserName NCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX P4JUserAgentBrowser_BrowserName_idx
	ON P4JUserAgentBrowser (
		BrowserName
);

  
CREATE TABLE P4JUserAgentBrowserVersion(
	BrowserVersionID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	BrowserVersion NCHAR(50) NOT NULL
);


CREATE UNIQUE INDEX P4JUserAgentBrowserVersion_BrowserName_idx
	ON P4JUserAgentBrowserVersion (
		BrowserVersion
);

CREATE TABLE P4JUserAgentOS(
	OSID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	OSName NCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX P4JUserAgentOS_OSName_idx
	ON P4JUserAgentOS (
		OSName
);

CREATE TABLE P4JUserAgentOSVersion(
	OSVersionID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	OSVersion NCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX P4JUserAgentOSVersion_OSVersion_idx
	ON P4JUserAgentOSVersion (
		OSVersion
);

CREATE TABLE P4JUserAgentOccurance (
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
JOIN P4JUserAgentOSVersion osv ON osv.OSVersionID = oc.OSVersionID
