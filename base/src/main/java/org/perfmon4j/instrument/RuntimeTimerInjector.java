package org.perfmon4j.instrument;

import java.security.ProtectionDomain;

public abstract class RuntimeTimerInjector {

	public abstract TimerInjectionReturn injectPerfMonTimers(byte[] classfileBuffer, boolean beingRedefined, 
	    		TransformerParams params, ClassLoader loader, ProtectionDomain protectionDomain) throws Exception;
	public abstract byte[] installUndertowOrTomcatSetValveHook(byte[] classfileBuffer, ClassLoader loader) throws Exception;	
    public abstract byte[] wrapTomcatRegistry(byte[] classfileBuffer, ClassLoader loader) throws Exception;
    public abstract byte[] disableSystemGC(byte[] classfileBuffer, ClassLoader loader) throws Exception;
	
    
    private ThreadLocal<Boolean> singnalThreadInTimer = new ThreadLocal<Boolean>() {
        public Boolean initialValue() {
            return Boolean.FALSE;
        }
    }; 

    /* (non-Javadoc)
	 * @see org.perfmon4j.instrument.RuntimeTimerInjectorInterface#signalThreadInTimer()
	 */
    public void signalThreadInTimer() {
        singnalThreadInTimer.set(Boolean.TRUE);
    }

    /* (non-Javadoc)
	 * @see org.perfmon4j.instrument.RuntimeTimerInjectorInterface#releaseThreadInTimer()
	 */
    public void releaseThreadInTimer() {
        singnalThreadInTimer.set(Boolean.FALSE);
    }
    
    /* (non-Javadoc)
	 * @see org.perfmon4j.instrument.RuntimeTimerInjectorInterface#isThreadInTimer()
	 */
    public boolean isThreadInTimer() {
        return singnalThreadInTimer.get().booleanValue();
    }

	
	
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