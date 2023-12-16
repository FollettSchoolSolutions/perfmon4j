package org.perfmon4j.emitter;

public interface EmitterController {
	public void emit(EmitterData data);
	
	public EmitterData initData();
	public EmitterData initData(String instanceName);
	public EmitterData initData(long timestamp);
	public EmitterData initData(String instanceName, long timeStamp);
	
	public boolean isActive();
	
	public static final EmitterController NO_OP_CONTROLLER = new EmitterController() {
		
		@Override
		public boolean isActive() {
			return false;
		}
		
		@Override
		public EmitterData initData(String instanceName, long timeStamp) {
			return EmitterData.NO_OP_DATA;
		}
		
		@Override
		public EmitterData initData(long timestamp) {
			return EmitterData.NO_OP_DATA;
		}
		
		@Override
		public EmitterData initData(String instanceName) {
			return EmitterData.NO_OP_DATA;
		}
		
		@Override
		public EmitterData initData() {
			return EmitterData.NO_OP_DATA;
		}
		
		@Override
		public void emit(EmitterData data) {
		}
	};
	
}


