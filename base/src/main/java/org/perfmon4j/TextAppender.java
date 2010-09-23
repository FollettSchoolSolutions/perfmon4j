/*
 *	Copyright 2008 Follett Software Company 
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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class TextAppender extends Appender {
    private static final Logger logger;
    public static final long DEFAULT_INTERVAL_MILLIS = 5 * 60 * 1000;
    private boolean includeSQL = SQLTime.isEnabled();

    static {
        logger = LoggerFactory.initLogger(TextAppender.class);
        logger.enableInfo();
    }
    
	public boolean isIncludeSQL() {
			return includeSQL;
	}

	/**
	 * Indicates if interval data should include the SQL attributes...
	 * Note this is conditional on PerfMon4j being loaded with the
	 * -eSQL instrumentation. 
	 * @param includeSQL
	 */
	public void setIncludeSQL(boolean includeSQL) {
		if (includeSQL && !SQLTime.isEnabled()) {
			logger.logWarn("Unabled to include SQL information.  Perfmon4j agent must enable SQL/JDBC instrumentation.");
			includeSQL = false;
		}
		this.includeSQL = includeSQL;
	}

	/*----------------------------------------------------------------------------*/
    public TextAppender(AppenderID id) {
        super(id);
    }
     
/*----------------------------------------------------------------------------*/
    public void outputData(PerfMonData data) {
    	if (data instanceof IntervalData) {
            logger.logInfo(((IntervalData)data).toAppenderString(isIncludeSQL()));
    	} else {
            logger.logInfo(data.toAppenderString());
    	}
    }
    
    
    
}


