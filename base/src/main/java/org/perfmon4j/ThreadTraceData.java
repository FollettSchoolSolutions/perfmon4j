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

import java.util.List;
import java.util.Vector;

import org.perfmon4j.util.MiscHelper;


public class ThreadTraceData implements PerfMonData {
    private ThreadTraceData parent;
    private final String name;
    private final List<ThreadTraceData> children = new Vector<ThreadTraceData>();
    private final int depth;
    private final long startTime;
    private long endTime = -1;
    
    ThreadTraceData(String name, long startTime) {
        this(name, null, startTime);
    }

    ThreadTraceData(String name, ThreadTraceData parent, long startTime) {
        this.name = name;
        this.parent = parent;
        this.startTime = startTime;
        if (parent != null) {
            parent.children.add(this);
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }
    }
    
    public ThreadTraceData[] getChildren() {
        return children.toArray(new ThreadTraceData[]{});
    }
    

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    void stop() {
        endTime = MiscHelper.currentTimeWithMilliResolution();
    }
    
    ThreadTraceData getParent() {
        return parent;
    }

    public String toAppenderString() {
        String result = String.format(
            "\r\n********************************************************************************\r\n" +
            "%s" +
            "********************************************************************************",
            buildAppenderStringBody(""));
        return result;
    }
    
    
    public String buildAppenderStringBody(String indent) {
        StringBuilder childAppenderString = new StringBuilder();
        ThreadTraceData children[] = getChildren();
        String childIndent = indent + "|\t";
        for (int i = 0; i < children.length; i++) {
            childAppenderString.append(children[i].buildAppenderStringBody(childIndent));
        }
        String result = String.format(
            "%s+-%s (%d) %s\r\n" +
            "%s" +
            "%s+-%s %s\r\n",
            indent,
            MiscHelper.formatTimeAsString(getStartTime()),
            getEndTime() - getStartTime(),
            name,
            childAppenderString.toString(),
            indent,
            MiscHelper.formatTimeAsString(getEndTime()),
            name);
        return result;
        
    }
        

    public String getName() {
        return name;
    }

    void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getDepth() {
        return depth;
    }

    void seperateFromParent() {
        if (parent != null) {
            parent.children.remove(this);
            parent = null;
        }
    }
}
