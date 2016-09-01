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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import org.perfmon4j.PerfMonTimer;

public class JavassistTest {
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        CtClass clazz = ClassPool.getDefault().get("org.perfmon4j.instrument.JavassistTest$SimpleClass");       
        
        CtMethod[] theMethods = clazz.getDeclaredMethods();
        
        for (int i = 0; i < theMethods.length; i++) {
            CtMethod theMethod = theMethods[i];
            
            if (theMethod.getName().startsWith("func")) {
                System.err.println(theMethod.getName());
            }
        }
        
        PerfMonTimerTransformer.runtimeTimerInjector.injectPerfMonTimers(clazz, false);
    }
    
    private static class SimpleClass {
        @DeclarePerfMonTimer("com.follett.fsc.destiny.testing.funcnoargsnoreturn")
        public static void funcNoArgsNoReturn() {
            double k = 1.332;
            int x = (int)k + 1;
            System.out.println("result is " + x);
        }
     
        @DeclarePerfMonTimer("com.follett.fsc.destiny.testing.funcnoargswithreturn")
        public static int funcNoArgsWithReturn() {
            double k = 1.332;
            return (int)k + 1;
        }
         
        @DeclarePerfMonTimer("com.follett.fsc.destiny.testing.funcwithargsnoreturn")
        public static void funcWithArgsNoReturn(StringBuffer sb, String s) {
            sb.append(s);
        }
        
        @DeclarePerfMonTimer("com.follett.fsc.destiny.testing.funcwithargsandreturn")
        public static int funcWithArgsAndReturn(int augend, int addend) {
            return augend + addend;
        }
        
        @DeclarePerfMonTimer("com.follett.fsc.destiny.testing.funcnestedperfmon")
        public static int funcNestedPerfmon() {
            PerfMonTimer timer = PerfMonTimer.start("asdfa");
            try {
                double k = 1.332;
                int x = (int)k + 1;
                return x;
            } finally {
                PerfMonTimer.stop(timer);
            }
        }
    }
}    



