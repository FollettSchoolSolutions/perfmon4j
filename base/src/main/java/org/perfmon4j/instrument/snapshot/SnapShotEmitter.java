package org.perfmon4j.instrument.snapshot;

import org.perfmon4j.util.MiscHelper;

public class SnapShotEmitter {
	
	public final void emit(EmitterData data) {
	}
	
	public final EmitterData initData() {
		return initData(MiscHelper.currentTimeWithMilliResolution());
	}
	
	public final EmitterData initData(long timestamp) {
		return new EmitterData("", "", timestamp);
	}
	
	public boolean isAppenderAttached() {
		return false;
	}
}
