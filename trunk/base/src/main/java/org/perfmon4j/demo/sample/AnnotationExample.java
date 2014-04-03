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
package org.perfmon4j.demo.sample;

import java.util.Arrays;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.TextAppender;
import org.perfmon4j.instrument.DeclarePerfMonTimer;


public class AnnotationExample extends SampleRunner {

    @DeclarePerfMonTimer("SortingTest")
    public void sampleMethod() {
        bruteForceSort(cloneArray());
        
        bubbleSort(cloneArray());

        arraySort(cloneArray());
    }

    @DeclarePerfMonTimer("SortingTest.bubbleSort")
    private void bubbleSort(int values[]) {
        int len = values.length - 1;

        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len - i; j++) {
                if (values[j] > values[j + 1]) {
                    int tmp = values[j];
                    values[j] = values[j+1];
                    values[j+1] = tmp;
                }
            }
        }
    }

    @DeclarePerfMonTimer("SortingTest.arraySort")
    private void arraySort(int values[]) {
        Arrays.sort(values);
    }
    
    @DeclarePerfMonTimer("SortingTest.bruteForceSort")
    private void bruteForceSort(int values[]) {
        int len = values.length;
        for (int i = len; i > 1; i--) {
            int v = values[len - 1];
            
            // Find the spot to insert
            int insertionPoint = 0;
            for (insertionPoint = 0; insertionPoint < values.length - i; insertionPoint++) {
                if (v <= values[insertionPoint]) {
                    break;
                }
            }
            int numToMove = len - insertionPoint - 1;
            System.arraycopy(values, insertionPoint, values, insertionPoint+1, numToMove);
            values[insertionPoint] = v;
        }
    }


    public static void main(String args[]) throws Exception {
//        "-javaagent:./dist/perfmon4j.jar="a=org.perfmon4j"
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineAppender("Basic", TextAppender.class.getName(), "10 seconds");
        config.defineMonitor("SortingTest");
        config.attachAppenderToMonitor("SortingTest", "Basic", PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS);
        
        PerfMon.configure(config);
        launchSamplers(AnnotationExample.class);
    }
}
