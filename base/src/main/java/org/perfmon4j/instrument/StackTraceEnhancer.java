package org.perfmon4j.instrument;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class StackTraceEnhancer implements ClassFileTransformer {
    private final static Logger logger = LoggerFactory.initLogger(StackTraceEnhancer.class);
    static ExposeNativeMethodsOnThrowable wrappedThrowable = null;
	
    
    public StackTraceEnhancer() {
//    	if (wrappedThrowable == null) {
//			try {
//	    		ClassPool pool = new ClassPool(true);
//	    		CtClass newClass = pool.makeClass("java.lang.SubThrowable", pool.get("java.lang.Throwable"));
//	    		String src = 
//	    				"public int getStackTraceDepthFromNative() {\r\n" +
//	    				"\treturn super.getStackTraceDepth();\r\n" +
//	    				"}";
//	    		CtMethod method = CtMethod.make(src, newClass);
//	    		newClass.addMethod(method);
//	    		newClass.addInterface(pool.get(ExposeNativeMethodsOnThrowable.class.getName()));
//	    		
//	    		wrappedThrowable = (ExposeNativeMethodsOnThrowable)newClass.toClass().newInstance();
//	    		
//	    		System.err.println("~~~~~~~~~~~~~~~~~~~~~YES~~~~~~~~~~~~~~~~~~~~~~~~~~");
//	    		
//			} catch (Exception e) {
//				e.printStackTrace();
//			} 
//    	}
    }
    
    
    public byte[] transform(ClassLoader loader, String className, 
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
            byte[] classfileBuffer) {
        byte[] result = null;
        
        final boolean inThrowable = "java/lang/Throwable".equals(className);
        final boolean inStackTraceElement = "java/lang/StackTraceElement".equals(className);
        
        if (inThrowable || inStackTraceElement) {
            try {
            	ClassPool classPool;
	            
	            if (loader == null) {
	                classPool = new ClassPool(true);
	            } else {
	                classPool = new ClassPool(false);
	                classPool.appendClassPath(new LoaderClassPath(loader));
	            }
	            
	            ByteArrayInputStream inStream = new ByteArrayInputStream(classfileBuffer);
	            CtClass clazz = classPool.makeClass(inStream);
	            if (clazz.isFrozen()) {
	                clazz.defrost();
	            }
	            if (inThrowable) {
	            	enhanceThrowable(clazz, classPool);
	            } else {
	            	enhanceStackTraceElement(clazz);
	            }
	            result = clazz.toBytecode();
	            logger.logInfo("Perfmon4j installed StackTraceEnhancer in: " + clazz.getName());
            } catch (Exception ex) {
            	logger.logError("Problem installing StackTraceEnhancer", ex);
            }
        }
		
		return result;
	}
   
    public static int getStackTraceDepth() {
    	int result = -1;
    	
    	if (wrappedThrowable != null) {
    		result = wrappedThrowable.getStackTraceDepthFromNative();
    	}
    	
    	return result;
    }

    private void enhanceThrowable(CtClass clazz, ClassPool pool) throws Exception {
//    	final String source =
//    			"public int getCurrentThreadDepth()  {\r\n"
//    			+ "\treturn getStackTraceDepth();\r\n"
//    			+ "}"; 
//    	CtMethod methodGetCurrentThreadDepth = CtMethod.make(source, clazz);
//    	
//    	clazz.addMethod(methodGetCurrentThreadDepth);
//    	CtClass intf = pool.get(GetThreadDepthThrowable.class.getName());
//    	clazz.addInterface(intf);
    	
        CtMethod m = clazz.getDeclaredMethod("getOurStackTrace");
        m.instrument(new WrapMethodCall("getStackTraceElement", "", "System.out.println(\"************* Im here:\" + $_);"));
    }

    public static interface GetThreadDepthThrowable {
    	
    }
    
    
    private void enhanceStackTraceElement(CtClass clazz) throws Exception {
    	System.err.println("###HERE");

    	CtMethod m = clazz.getDeclaredMethod("toString");
    	
        m.instrument(new WrapMethodCall("getClassName", "", "$_ = \"(100 ms) \" + $_;"));
    
    	
    	
//    	CtField field = CtField.make("private Long timestamp;", clazz);
//    	clazz.addField(field);
//    	
//    	final String setter = 
//    			"public void setTimestamp(long timestamp) {"
//    			+ "\tthis.timestamp = Long.valueOf(timestamp);"
//    			+ "}";
//    	
//    	CtMethod method = CtMethod.make(setter, clazz);
//    	clazz.addMethod(method);
//    	
//        CtMethod m = clazz.getDeclaredMethod("toString");
//        m.instrument(new WrapMethodCall("getStackTraceElement", "", "System.out.println(\"************* Im here:\" + $_);"));
    	
//        CtMethod m = clazz.getDeclaredMethod("getOurStackTrace");
//        m.instrument(new WrapMethodCall("getStackTraceElement", "", "System.out.println(\"************* Im here:\" + $_);"));
    }
   
    
    
    private static class WrapMethodCall extends ExprEditor {
    	private final String methodName;
    	private final String before;
    	private final String after;
    	
    	WrapMethodCall(String methodName, String before, String after) {
    		this.methodName = methodName;
    		this.before = before;
    		this.after = after;
    	}
    	
    	public void edit(FieldAccess fa) throws CannotCompileException  {
   		 	System.out.println("FieldAccess: " + fa.getFieldName());
    	}
    	
    	
    	 public void edit(MethodCall m) throws CannotCompileException  {
    		 if (methodName.equals(m.getMethodName())) {
	        	 m.replace(before + " $_ = $proceed($$);  " + after);
        	 }  
    	 }
    }
    
    
    public static interface ExposeNativeMethodsOnThrowable {
    	public int getStackTraceDepthFromNative();
    }
}
