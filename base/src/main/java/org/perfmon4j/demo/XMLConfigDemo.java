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
package org.perfmon4j.demo;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.XMLConfigurator;

public class XMLConfigDemo {
    
    private XMLConfigDemo() {
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("org.perfmon4j").setLevel(Level.DEBUG);
        Logger logger = Logger.getLogger(XMLConfigDemo.class);
        
        try (XMLConfigurator configurator = new XMLConfigurator(new File("/home/ddeucher/perfmonconfig.xml"), "democonfig.xml", 5)) {
        	configurator.start();
       
	        for (int i = 0; i < 10; i++) {
	            new Thread(new RunnerImpl()).start();
	        }
	        
	        while (true) {
	            PerfMonTimer timer = PerfMonTimer.start("SimpleExample.outer");
	            try {
	                logger.info(".");
	                System.gc();
	                Thread.sleep(1000);
	            } finally {
	                PerfMonTimer.stop(timer);
	            }
	        }
        }
    }
    
    private static class RunnerImpl implements Runnable {
        private void sleep() {
            try {
                Thread.sleep((long)(Math.random() * 10000));
            } catch (InterruptedException ie) {
                // Nothing todo
            }
        }
        
        public void run() {
            while (true) {
                sleep();
                PerfMonTimer timer = PerfMonTimer.start("SimpleExample.inner");
                try {
                    sleep();
                } finally {
                    PerfMonTimer.stop(timer);
                }
            }
        }
    }
    
}
