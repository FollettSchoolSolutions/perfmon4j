/*
 *	Copyright 2008,2011 Follett Software Company 
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

package org.perfmon4j.instrument;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

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

	public static String loadClassAndPrintMethods(Class<?> clazzToLoad, String jvmParams, File perfmonJar) throws Exception {
		return run(LoadClassAndPrintMethods.class, jvmParams, clazzToLoad.getName(), perfmonJar);
	}
	
	public static String loadClassAndPrintMethods(Class<?> clazzToLoad, String jvmParams, Properties systemProperties, File perfmonJar) throws Exception {
		return run(LoadClassAndPrintMethods.class, jvmParams, clazzToLoad.getName(), systemProperties, perfmonJar);
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
	
	public static String run(Class<?> clazz, String javaAgentParams, String args, File perfmonJar) throws Exception {
		return run(clazz, javaAgentParams, args, null, perfmonJar);
	}

	public static String fixupLinuxHomeFolder(String path) {
		if (path != null) {
			// Java does not always deal with the ~ substitution for home folder.
			if (path.startsWith("~")) {
				path = path.replace("~", System.getProperty("user.home"));
			}
		}
		return path;
	}

	public static String runWithoutPerfmon4jJavaAgent(Class<?> clazz, File perfmonJar) throws Exception {
		return runWithoutPerfmon4jJavaAgent(clazz, "", null, perfmonJar);
	}
	
	public static String runWithoutPerfmon4jJavaAgent(Class<?> clazz, String args, Properties systemProperties, File perfmonJar) throws Exception {
		return run(clazz, null, args, systemProperties, perfmonJar, false);
	}
	
	public static String run(Class<?> clazz, String javaAgentParams, String args, Properties systemProperties, File perfmonJar) throws Exception {
		return run(clazz, javaAgentParams, args, systemProperties, perfmonJar, true);
	}
	
	
	private static int getMajorJavaVersion() {
		String version = System.getProperty("java.version");
		return Integer.parseInt(version.substring(0, version.indexOf(".")));
	}
	
	
	private static String run(Class<?> clazz, String javaAgentParams, String args, Properties systemProperties, File perfmonJar, boolean loadJavaAgent) throws Exception {
		StringBuffer output = new StringBuffer();
    	
    	final String javaCmd = System.getProperty("java.home") + "/bin/java";
    	final String runnableName = clazz.getName();

    	final String PATH_SEPERATOR = System.getProperty("path.separator");
    	String cmdString = quoteIfNeeded(javaCmd);
    
    	String myClassPath = "";
    	String originalClasspath = System.getProperty("java.class.path").replaceAll(";/", ";");
    	originalClasspath.split(PATH_SEPERATOR);
    	File javassistFile = null;
    	
    	for (String part : originalClasspath.split(PATH_SEPERATOR)) {
    		if (part.endsWith("test-classes")) {
    			// Add the perfmon4j test classes...
    			myClassPath += quoteIfNeeded(part) + PATH_SEPERATOR; 
    		} else if (part.contains("junit")) {
    			// Add junit classes...
    			myClassPath += quoteIfNeeded(part) + PATH_SEPERATOR; 
    		} else if (part.contains("derby")) {
    			// Add derby classes for SQLAppender tests...
    			myClassPath += quoteIfNeeded(part) + PATH_SEPERATOR; 
    		} else if (part.contains("log4j")) {
    			// Add log4j classes for log4j tests....
    			myClassPath += quoteIfNeeded(part) + PATH_SEPERATOR; 
    		} else if (part.contains("perfmon4j"+ File.separator + "agent-api")) {
    			// Add perfmon4j agent.
    			myClassPath += quoteIfNeeded(part) + PATH_SEPERATOR; 
    		} else if (part.contains("javassist")) {
    			javassistFile = new File(part) ;
			} 
    	}
    	
		if (loadJavaAgent) {
			// For some strange reason it works better to have the classPath before the agent.
			cmdString += " -classpath " + myClassPath;
			
			cmdString +=  " -javaagent:" + quoteIfNeeded(perfmonJar.getCanonicalPath());
	    	if (javaAgentParams != null) {
	    		cmdString += "=" + javaAgentParams;
	    	}
	    	if (getMajorJavaVersion() < 11) {
	    		// Java 11 or better does not support and, at least in this case, does not require
	    		// endoresedDirs.
		    	String endorsedDirs = " -Djava.endorsed.dirs=" + quoteIfNeeded(perfmonJar.getParentFile().getCanonicalPath());
		    	if (javassistFile != null) {
		    		endorsedDirs += PATH_SEPERATOR + quoteIfNeeded(javassistFile.getParentFile().getCanonicalPath());
		    	}
				cmdString +=  endorsedDirs;
	    	}
		} else {
			// Just load the perfmon4j.jar onto the classpath.
			myClassPath += quoteIfNeeded(perfmonJar.getCanonicalPath()) + PATH_SEPERATOR;
			cmdString += " -classpath " + myClassPath;
		}

    	
		if (systemProperties != null) {
			Iterator<Map.Entry<Object,Object>> itr = systemProperties.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry<Object, Object> v = itr.next();
				cmdString += " -D" + v.getKey() + "=" + v.getValue();
			}
		}
		
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

			ClassLoader loader = ClassLoader.getSystemClassLoader();
			Class<?> clazz = loader.loadClass(classToRun);
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
				Class<?> clazz = PerfMon.getClassLoader().loadClass(classToLoad);
				
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
				
				// PerfMonTimerTransformer.monitorsForRedefinedClasses is an array of arrays....
				if (PerfMonTimerTransformer.monitorsForRedefinedClasses != null) {
					for (int i = 0; i < PerfMonTimerTransformer.monitorsForRedefinedClasses.length; i++) {
						PerfMon monitors[] = PerfMonTimerTransformer.monitorsForRedefinedClasses[i];
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
