package org.perfmon4j.instrument;

import java.security.ProtectionDomain;

public interface RuntimeTimerInjector {

	public TimerInjectionReturn injectPerfMonTimers(byte[] classfileBuffer, boolean beingRedefined, 
	    		TransformerParams params, ClassLoader loader, ProtectionDomain protectionDomain) throws Exception;
	public byte[] installUndertowOrTomcatSetValveHook(byte[] classfileBuffer, ClassLoader loader) throws Exception;	
    public byte[] wrapTomcatRegistry(byte[] classfileBuffer, ClassLoader loader) throws Exception;
    public byte[] disableSystemGC(byte[] classfileBuffer, ClassLoader loader) throws Exception;
	

	public abstract void signalThreadInTimer();
	public abstract void releaseThreadInTimer();
	public abstract boolean isThreadInTimer();
	
	
	public static class TimerInjectionReturn {
		private final int numTimersAdded;
		private final byte[] classBytes;
		
		public TimerInjectionReturn(int numTimersAdded, byte[] classBytes) {
			super();
			this.numTimersAdded = numTimersAdded;
			this.classBytes = classBytes;
		}

		public int getNumTimersAdded() {
			return numTimersAdded;
		}

		public byte[] getClassBytes() {
			return classBytes;
		}
	}

}