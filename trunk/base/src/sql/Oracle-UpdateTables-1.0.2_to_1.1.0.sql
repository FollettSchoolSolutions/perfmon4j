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

/* ******************************************************************************
Notes - This script contains the SQL required to update the Perfmon4j MySQL
tables to 1.1.0 FROM 1.0.2
****************************************************************************** */
ALTER TABLE P4JIntervalData
	ADD (SQLMaxDuration INT NULL,
	SQLMaxDurationSet TIMESTAMP NULL,
	SQLMinDuration INT NULL,
	SQLMinDurationSet TIMESTAMP NULL,
	SQLAverageDuration DECIMAL(18, 2) NULL,
	SQLStandardDeviation DECIMAL(18, 2) NULL,
	SQLDurationSum NUMBER(18) NULL,
	SQLDurationSumOfSquares NUMBER(18) NULL);

ALTER TABLE P4JThreadTrace
	ADD SQLDuration INT NULL;
