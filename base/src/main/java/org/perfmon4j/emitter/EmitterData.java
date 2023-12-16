package org.perfmon4j.emitter;

public interface EmitterData {
	public void addData(String fieldName, long value);
	public void addData(String fieldName, int value);
	public void addData(String fieldName, double value);
	public void addData(String fieldName, float value);
	public void addData(String fieldName, boolean value);
	public void addData(String fieldName, String value);
	
	public String getInstanceName();
	
	public static final EmitterData NO_OP_DATA = new EmitterData() {
		
		@Override
		public String getInstanceName() {
			return null;
		}
		
		@Override
		public void addData(String fieldName, String value) {
		}
		
		@Override
		public void addData(String fieldName, boolean value) {
		}
		
		@Override
		public void addData(String fieldName, float value) {
		}
		
		@Override
		public void addData(String fieldName, double value) {
		}
		
		@Override
		public void addData(String fieldName, int value) {
		}
		
		@Override
		public void addData(String fieldName, long value) {
		}
	};
	
	
}
