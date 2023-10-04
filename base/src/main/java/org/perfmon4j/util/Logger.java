/*
 *	Copyright 2008,2009 Follett Software Company 
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
 
*/

package org.perfmon4j.util;

public interface Logger {
	public final static long DYNAMIC_LOAD_RETRY_MILLIS = 10000;
	
    public boolean isDebugEnabled();
    public boolean isInfoEnabled();
    
    public void enableInfo();
    public void enableDebug();
    
    public void logError(String msg);
    public void logWarn(String msg);
    public void logInfo(String msg);
    public void logDebug(String msg);
    public void logVerbose(String msg);
    
    public void logError(String msg, Throwable th);
    public void logWarn(String msg, Throwable th);
    public void logInfo(String msg, Throwable th);
    public void logDebug(String msg, Throwable th);
    public void logVerbose(String msg, Throwable th);
}
