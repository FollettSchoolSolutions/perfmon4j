package org.perfmon4j.influxdb;

import org.perfmon4j.PerfMonData;
import org.perfmon4j.SystemNameAndGroupsAppender;

public class InfluxAppender extends SystemNameAndGroupsAppender {

	public InfluxAppender(AppenderID id) {
		super(id);
		this.setExcludeCWDHashFromSystemName(true);
	}

	@Override
	public void outputData(PerfMonData data) {
		
	}

}
