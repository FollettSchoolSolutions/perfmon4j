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

import org.perfmon4j.util.MiscHelper;

public class MaxThroughput {
    final private long startMillis;
    final private long endMillis;
    final private double throughputPerMinute;
    
    MaxThroughput(long startMillis, long endMillis, double throughputPerMinute) {
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.throughputPerMinute = throughputPerMinute;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public double getThroughputPerMinute() {
        return throughputPerMinute;
    }
    
    public String toString() {
        return String.format("%.2f (%s -> %s)",
            new Double(throughputPerMinute), 
            MiscHelper.formatDateTimeAsString(startMillis),
            MiscHelper.formatDateTimeAsString(endMillis));
    }
}
