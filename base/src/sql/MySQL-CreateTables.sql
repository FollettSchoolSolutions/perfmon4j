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
	) REFERENCES dbo.P4JCategory (
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
