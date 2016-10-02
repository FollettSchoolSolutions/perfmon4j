package org.perfmon4j.instrument;

import java.util.Stack;

import org.perfmon4j.PerfMonTimer;

// Unfortunately Javassist does not currently allow an inserted finally to 
// access local variables in the method.  This is OK when we are able to add methods to the
// class since we simply create a wrapper around the method.  This is not acceptable 
// for boot classes (classes that are being redefined) since the javaagent instrumentation
// api does not allow you to add methods when redefining a class.  Too get around these
// limitations we stick the timers on a thread local.  This does have some disadvantages,
// namely a modest performance penalty, we only do this if we have to.
// Oh and in fairness, javassist is a great tool and I am pretty confident it can do
// what I want, however, you have to use it to directly manipulate java byte code, ick.

// Note: 1/16/15 
// There was a thought that you could implement this without thread locals.  By using an ExprEditor class
// as follows:  
//
// method.instrument(  
// new ExprEditor() {  
//	 public void edit(MethodCall m)  
//	 throws CannotCompileException  
//	 {  
//	 m.replace(beforeMethod + " try {$_ = $proceed($$); } " + afterMethod);  
//	 }  
//	 });  
//
//  This DOES NOT WORK as you might expect.  It is not replacing the body of the method you
//  are instrumenting, RATHER it modifies the call to each method that is called within the
// body of the method.
public class PushTimerForBootClass {
    private static ThreadLocal<Stack<PerfMonTimer>> bootClassTimers = new ThreadLocal<Stack<PerfMonTimer>>() {
         protected synchronized Stack<PerfMonTimer> initialValue() {
             return new Stack<PerfMonTimer>();
         }
    };
    
    public static void pushTimer(PerfMonTimer timer) {
        bootClassTimers.get().push(timer);
    }
    
    public static PerfMonTimer popTimer() {
        return bootClassTimers.get().pop();
    }
}
