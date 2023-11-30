package api.org.perfmon4j.agent.impl;

import java.util.Objects;

import api.org.perfmon4j.agent.Emitter;
import api.org.perfmon4j.agent.EmitterController;
import api.org.perfmon4j.agent.EmitterData;

public class EmitterInstrumentationHelper {
	
	public static Object wrapAPIEmitterWithAgentEmitter(Emitter emitter) {
		return new APIEmitterWrapper(emitter);
	}
	
	public static EmitterController wrapAgentEmitterControllerWithAPIEmitterController(Object agentEmitterController) {
		return new EmitterControllerWrapper(agentEmitterController);
	}
	
	public static EmitterData wrapAgentEmitterDataWithAPIEmitterData(Object agentEmitterData) {
		return new EmitterDataWrapper(agentEmitterData);
	}

	public static Object getDelegateFromAPIEmitterData(EmitterData emitterData) {
		Object result = null;
		
		if (emitterData instanceof EmitterDataWrapper) {
			result = ((EmitterDataWrapper)emitterData).getDelegate();
		}
		
		return result;
	}
	
	
	/**
	 * The Perfmon4j instrumentation agent will 
	 * modify this class to implement the org.perfmon4j.emitter.Emitter agent
	 * interface
	 */
	public static class APIEmitterWrapper /* implements org.perfmon4j.emitter.Emitter */ {
		private final Emitter delegate;
		
		public APIEmitterWrapper(Emitter delegate) {
			this.delegate = delegate;
		}
		
		public Emitter getDelegate() {
			return delegate;
		}
		
		/* The perfmon4j instrumentation agent will add this method */
		/*
		public void acceptController(org.perfmon4j.emitter.EmitterController controller) {
			getDelegate().acceptController(api.org.perfmon4j.agent.impl.EmitterInstrumentationHelper.wrapAgentEmitterControllerWithAPIEmitterController(controller));
		}
		*/

		@Override
		public int hashCode() {
			return Objects.hashCode(delegate);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			APIEmitterWrapper other = (APIEmitterWrapper) obj;
			return Objects.equals(delegate, other.delegate);
		}
		
	    /**
	     * If true this class has been rewritten by the Perfmon4j agent.
	     * @return
	     */
	    public static boolean isAttachedToAgent() {
	    	return false;
	    }		
	}
	
	public static class EmitterDataWrapper implements EmitterData {
		protected final Object delegate;
		
		public EmitterDataWrapper(Object delegate) {
			this.delegate = delegate;
		}
		
		public Object getDelegate() {
			return delegate;
		}

		@Override
		public void addData(String fieldName, long value) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				((org.perfmon4j.emitter.EmitterData)getDelegate()).addData($1, $2);
			*/
		}

		@Override
		public void addData(String fieldName, int value) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				((org.perfmon4j.emitter.EmitterData)getDelegate()).addData($1, $2);
			*/
		}

		@Override
		public void addData(String fieldName, double value) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				((org.perfmon4j.emitter.EmitterData)getDelegate()).addData($1, $2);
			*/
		}

		@Override
		public void addData(String fieldName, float value) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				((org.perfmon4j.emitter.EmitterData)getDelegate()).addData($1, $2);
			*/
		}

		@Override
		public void addData(String fieldName, boolean value) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				((org.perfmon4j.emitter.EmitterData)getDelegate()).addData($1, $2);
			*/
		}

		@Override
		public void addData(String fieldName, String value) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				((org.perfmon4j.emitter.EmitterData)getDelegate()).addData($1, $2);
			*/
		}

	    /**
	     * If true this class has been rewritten by the Perfmon4j agent.
	     * @return
	     */
	    public static boolean isAttachedToAgent() {
	    	return false;
	    }		
	}
	
	public static class EmitterControllerWrapper implements EmitterController {
		private final Object delegate;
		
		public EmitterControllerWrapper(Object delegate) {
//System.out.println("Building " + EmitterControllerWrapper.class.getName() + " delegate: " + delegate);
			this.delegate = delegate;
		}
		
		public Object getDelegate() {
			return delegate;
		}
		
		@Override
		public void emit(EmitterData data) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
			 * 
				org.perfmon4j.emitter.EmitterData delegateData = (org.perfmon4j.emitter.EmitterData)api.org.perfmon4j.agent.impl.EmitterInstrumentationHelper.getDelegateFromAPIEmitterData($1);
				((org.perfmon4j.emitter.EmitterController)getDelegate()).emit(delegateData);
			*/
		
		}

		@Override
		public EmitterData initData() {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				return api.org.perfmon4j.agent.impl.EmitterInstrumentationHelper.wrapAgentEmitterDataWithAPIEmitterData(((org.perfmon4j.emitter.EmitterController)getDelegate()).initData());
			*/
			return null;
		}

		@Override
		public EmitterData initData(String instanceName) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				return api.org.perfmon4j.agent.impl.EmitterInstrumentationHelper.wrapAgentEmitterDataWithAPIEmitterData(((org.perfmon4j.emitter.EmitterController)getDelegate()).initData($1));
			*/
			return null;
		}

		@Override
		public EmitterData initData(long timestamp) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				return api.org.perfmon4j.agent.impl.EmitterInstrumentationHelper.wrapAgentEmitterDataWithAPIEmitterData(((org.perfmon4j.emitter.EmitterController)getDelegate()).initData($1));
			*/
			return null;
		}

		@Override
		public EmitterData initData(String instanceName, long timeStamp) {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				return api.org.perfmon4j.agent.impl.EmitterInstrumentationHelper.wrapAgentEmitterDataWithAPIEmitterData(((org.perfmon4j.emitter.EmitterController)getDelegate()).initData($1, $2));
			*/
			return null;
		}

		@Override
		public boolean isActive() {
			/* The code in the comment below will be added by the Perfmon4j instrumentation agent */ 
			/*
				return ((org.perfmon4j.emitter.EmitterController)getDelegate()).isActive();
			*/
			return false;
		}
		
	    /**
	     * If true this class has been rewritten by the Perfmon4j agent.
	     * @return
	     */
	    public static boolean isAttachedToAgent() {
	    	return false;
	    }		
		
	}
}
