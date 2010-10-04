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

package org.perfmon4j.instrument;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.perfmon4j.PerfMon;

/**
 * This class is a utility class to aid in unit testing the PerfMonTimerTransformer.
 * It will spawn a java class including the PerfMon javaagent jar.
 * The output of the command will be returned in a string.
 * 
 * Note: This class will load the perfmon4j.jar from the dist folder.  YOU
 * MUST BUILD WITH ANT to get the latest class versions.
 */
public class LaunchRunnableInVM {
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

	public static String loadClassAndPrintMethods(Class clazzToLoad, String jvmParams, File perfmonJar) throws Exception {
		return run(LoadClassAndPrintMethods.class, jvmParams, clazzToLoad.getName(), perfmonJar);
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
	
	
	public static String run(Class clazz, String javaAgentParams, String args, File perfmonJar) throws Exception {
		StringBuffer output = new StringBuffer();
    	
    	final String javaCmd = System.getProperty("java.home") + "/bin/java";
    	final String runnableName = clazz.getName();

    	final String javaAgentPath = perfmonJar.getCanonicalPath();
    	
    	String cmdString = quoteIfNeeded(javaCmd);
    	
    	String myClassPath = System.getProperty("java.class.path").replaceAll(";/", ";");
    	myClassPath = myClassPath.replaceAll("\\\\", "/");
    	String derbyDriver = System.getProperty("DERBY_EMBEDDED_DRIVER");
    	if (derbyDriver != null) {
    		File driver = new File(derbyDriver);
    		if (!driver.exists()) {
    			throw new RuntimeException("DERBY_EMBEDDED_DRIVER \"" + derbyDriver + "\" - NOT FOUND!");
    		}
    		myClassPath += System.getProperty("path.separator") + derbyDriver;
    	}
    	myClassPath = quoteIfNeeded(myClassPath);
    	
    	System.out.println("CLASSPATH=" + myClassPath); 
		cmdString += " -classpath " + myClassPath;
    	
    	
    	cmdString +=  " -javaagent:" + quoteIfNeeded(javaAgentPath);
    	if (javaAgentParams != null) {
    		cmdString += "=" + javaAgentParams;
    	}

    	String javaAssistProp = System.getProperty("JAVASSIST_JAR");
    	if (javaAssistProp == null) {
    		throw new RuntimeException("JAVASSIST_JAR system property must be set");
    	}
    	
    	
    	
    	
    	
    	File javassistJar = new File(javaAssistProp);
    	final String pathSeparator = System.getProperty("path.separator");
    	
    	
    	cmdString +=  " -DPerfMon4j.preferredLogger=stdout -Djava.endorsed.dirs=" + quoteIfNeeded(perfmonJar.getParentFile().getCanonicalPath() + pathSeparator + javassistJar.getParentFile().getCanonicalPath());
    	
    	cmdString +=  " " + LaunchRunnableInVM.class.getName();
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
			Class clazz = Class.forName(classToRun);
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
	
	public static class LoadClassAndPrintMethods implements ProcessArgs {
		private String classToLoad;
		
		public void processArgs(String[] args) {
			classToLoad = args[1];
		}

		public void run() {
			try {
				Class clazz = PerfMon.getClassLoader().loadClass(classToLoad);
				
				System.out.println("Declared Fields For Class: " + classToLoad);
				Field fields[] = clazz.getDeclaredFields();
				for (int i = 0; i < fields.length; i++) {
					System.out.println("(" + i + ") Field: " +  fields[i].toString());
				}
				
				System.out.println("Declared Methods For Class: " + classToLoad);
				Method methods[] = clazz.getDeclaredMethods();
				for (int i = 0; i < methods.length; i++) {
					System.out.println("(" + i + ") DeclaredMethod: " +  methods[i].toGenericString());
				}

				System.out.println("Monitors Defined For Bootstrap Classes");
				int nullBootstrapMonitors = 0;	
				int insertedBootstrapMonitors = 0;
				
				// RuntimeTimerInjector.monitorsForRedefinedClasses is an array of arrays....
				if (RuntimeTimerInjector.monitorsForRedefinedClasses != null) {
					for (int i = 0; i < RuntimeTimerInjector.monitorsForRedefinedClasses.length; i++) {
						PerfMon monitors[] = RuntimeTimerInjector.monitorsForRedefinedClasses[i];
						for (int j = 0; j < monitors.length; j++) {
							PerfMon mon = monitors[j];
							if (mon == null) {
								nullBootstrapMonitors++;
							} else {
								System.out.println( "(" + insertedBootstrapMonitors +  ") BootStrapMonitor: " + mon.getName());
								insertedBootstrapMonitors++;
							}
						}
					}
				}
				// Having null bootstrap monitors is NOT a problem.  The injector has to reserve
				// space for every potential monitor (reserves a pointer) prior to instrumenting 
				// preloaded bootstrap classes.  Each of the  spaces reserved will likely not be 
				// used since some methods (abstract, noMethodBody, etc) will not have monitors inserted.
				System.out.println("null (unused) bootstrap monitor pointers: " + nullBootstrapMonitors);
				System.out.println("inserted bootstrap monitors: " + insertedBootstrapMonitors);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}

}
