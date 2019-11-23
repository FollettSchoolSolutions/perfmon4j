package org.perfmon4j.instrument;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class JavassistClassLoaderInjector extends ClassLoaderInjector {

	@Override
	public void InjectClassLoader(ClassLoader target, ClassLoader src) {
		Class<? extends ClassLoader> targetClass = target.getClass();
		
		ClassPool pool = new ClassPool(true);
		pool.appendClassPath(new LoaderClassPath(targetClass.getClassLoader()));
		
		try {
			CtClass ctClass =  pool.getCtClass(targetClass.getName());
			CtClass ctInterface = pool.getCtClass(ClassLoaderInjector.ClassLoaderExtension.class.getName());
			
			ctClass.defrost();
			ctClass.addInterface(ctInterface);
			ctClass.defrost();
			ctClass.toClass(targetClass.getClassLoader());
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CannotCompileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
