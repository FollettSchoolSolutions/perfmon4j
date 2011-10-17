/*
 *	Copyright 2011 Follett Software Company 
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

package org.perfmon4j.remotemanagement.intf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This will execute a test class that will exercise the remote management
 * interface.  It is important that the remote management jar file
 * does NOT require ANY perfmon4j dependencies.
 */
public class ThinRunnableInVM {
	private final static String EOL = System.getProperty("line.separator", "\r\n");
	
	private static class StreamReaderThread extends Thread {
		private final BufferedReader reader;
		private final String type;
		private final StringBuffer output;
		
		StreamReaderThread(InputStream inStream, String type, StringBuffer output) {
			reader = new BufferedReader(new InputStreamReader(inStream));
			this.type = type;
			this.output = output;
		}
		
		public void run() {
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					output.append("[" + type + "] " + line + EOL);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	private static String quoteIfNeeded(String src) {
		String result;
		
		if (src.indexOf(' ') > 0) {
			result = "\"" + src + "\"";
		} else {
			result = src;
		}
		
		return result;
	}
	
	
	public static String run(Class<?> clazz, String args, File perfmon4jManagementInterface) throws Exception {
		StringBuffer output = new StringBuffer();
    	
    	final String javaCmd = System.getProperty("java.home") + "/bin/java";
    	final String runnableName = clazz.getName();
    	
    	String cmdString = quoteIfNeeded(javaCmd);
    	
    	String myClassPath = perfmon4jManagementInterface.getCanonicalPath();
    	System.out.println("CLASSPATH=" + myClassPath); 
    	
		cmdString += " -classpath " + myClassPath;
    	
    	cmdString +=  " " + ThinRunnableInVM.class.getName();
    	cmdString +=  " " + runnableName;
    	if (args != null) {
    		cmdString += " " + args;
    	}
		System.out.println(cmdString);    	
    	
		Process p = Runtime.getRuntime().exec(cmdString);
		new StreamReaderThread(p.getErrorStream(), "STDERR", output).start();
		new StreamReaderThread(p.getInputStream(), "STDOUT", output).start();

		int result = p.waitFor();
    	return output.toString() + "[RESULT] " + result;
	}
	
	public static void main(String[] args) {
		try {
			String classToRun = args[0];
			System.out.println("Running class: " + classToRun);
			Class<?> clazz = Class.forName(classToRun);
			System.out.println("Loaded Class: " + clazz.getName());
			Runnable runnable = (Runnable)clazz.newInstance();
			if (runnable instanceof ProcessArgs) {
				((ProcessArgs)runnable).processArgs(args);
			}
			runnable.run();
			System.out.println("Exiting main");
		} catch (Exception ex) {
			System.err.println("Caught Exception");
			ex.printStackTrace();
		}
	}
	
	public static interface ProcessArgs extends Runnable {
		public void processArgs(String[] args);
	}
}
