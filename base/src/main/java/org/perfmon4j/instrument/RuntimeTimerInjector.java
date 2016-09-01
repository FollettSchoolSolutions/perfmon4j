package org.perfmon4j.instrument;

import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

public interface RuntimeTimerInjector {

	public abstract int injectPerfMonTimers(CtClass clazz,
			boolean beingRedefined) throws ClassNotFoundException,
			NotFoundException, CannotCompileException;

	public abstract void disableSystemGC(CtClass clazz)
			throws ClassNotFoundException, NotFoundException,
			CannotCompileException;

	public abstract int injectPerfMonTimers(CtClass clazz,
			boolean beingRedefined, TransformerParams params)
			throws ClassNotFoundException, NotFoundException,
			CannotCompileException;

	public abstract int injectPerfMonTimers(CtClass clazz,
			boolean beingRedefined, TransformerParams params,
			ClassLoader loader, ProtectionDomain protectionDomain)
			throws ClassNotFoundException, NotFoundException,
			CannotCompileException;

	public abstract void signalThreadInTimer();

	public abstract void releaseThreadInTimer();

	public abstract boolean isThreadInTimer();

}