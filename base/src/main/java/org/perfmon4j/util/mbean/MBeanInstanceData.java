package org.perfmon4j.util.mbean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.instrument.SnapShotStringFormatter;
import org.perfmon4j.instrument.snapshot.GeneratedData;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.SnapShotPOJOLifecycle;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;

public class MBeanInstanceData extends SnapShotData implements GeneratedData, PerfMonObservableData, SnapShotPOJOLifecycle  {
	private static final Logger logger = LoggerFactory.initLogger(MBeanInstanceData.class);

	private long startTime = PerfMon.NOT_SET;
	private long endTime = PerfMon.NOT_SET;
	private long durationMillis = PerfMon.NOT_SET;
	
	private String instanceName = null;
	private DatumDefinition[] datumDefinition = null;
	private Map<String,MBeanDatum<?>> initialData = null;
	private Map<String,MBeanDatum<?>> finalData = null;
 	
	public MBeanInstanceData() {
	} 

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public long getEndTime() {
		return endTime;
	}

	@Override
	public long getDuration() {
		return endTime - startTime;
	}

	@Override
	public long getTimestamp() {
		return getEndTime();
	}

	@Override
	public long getDurationMillis() {
		return durationMillis;
	}

	@Override
	public String getDataCategory() {
		return "Snapshot." + getName();
	}

	@Override
	public void init(Object data, long timeStamp) {
		startTime = timeStamp;
		try {
			initialData = buildDatumMap(((MBeanInstance)data).extractAttributes());
		} catch (MBeanQueryException e) {
			logger.logWarn("Unable to initialize SnapShot for: " +  getName(), e);
		}
	}

	@Override
	public void takeSnapShot(Object dataProvider, long timesStamp) {
		endTime = timesStamp;
		durationMillis = endTime - startTime;
		try {
			MBeanInstance mBeanInstance = (MBeanInstance)dataProvider;
			finalData = buildDatumMap(mBeanInstance.extractAttributes());
			datumDefinition = mBeanInstance.getDatumDefinition();
		} catch (MBeanQueryException e) {
			logger.logWarn("Unable to take SnapShot for: " + getName(), e);
		}
	}

	@Override
	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	@Override
	public String getInstanceName() {
		return this.instanceName;
	}

	@Override
	public Map<FieldKey, Object> getFieldData(FieldKey[] fields) {
		logger.logWarn("getFieldData not supported by " + this.getClass().getName());
		return new HashMap<FieldKey, Object>();
	}

	@Override
	public String toAppenderString() {
		SnapShotStringFormatter stringFormatter = new SnapShotStringFormatter();
		org.perfmon4j.instrument.snapshot.Delta delta;		
		
		String result = "\r\n********************************************************************************\r\n";
		result += getName() + "\r\n";
		result += MiscHelper.formatTimeAsString(startTime) + " -> " + MiscHelper.formatTimeAsString(endTime) + "\r\n";
		if (instanceName != null) {
			result += stringFormatter.format(25, "instanceName", instanceName);
		}
		for (PerfMonObservableDatum<?> datum : getObservations(false)) {
			result += stringFormatter.format(25, datum.getFieldName(), datum.getValue());
		}
		result += "********************************************************************************\r\n";
		
		return result;
	}

	@Override
	public Set<PerfMonObservableDatum<?>> getObservations() {
		return getObservations(true);
	}
	
	private Set<PerfMonObservableDatum<?>> getObservations(boolean includeInstanceName) {
		Set<PerfMonObservableDatum<?>> result = new HashSet<>();
		
		if (includeInstanceName && !MiscHelper.isBlankOrNull(instanceName)) {
			result.add(PerfMonObservableDatum.newDatum("instanceName", instanceName));
		}
		
		for (DatumDefinition def : datumDefinition) {
			String attributeName = def.getName();
			switch (def.getOutputType()) {
				case COUNTER: // {
//					MBeanDatum<?> datumBefore = initialData.get(attributeName);
//					MBeanDatum<?> datumAfter = finalData.get(attributeName);
//					result.add(PerfMonObservableDatumn(new Delta(datumAfter.getName(), (Long)datumBefore.getValue(). (Long)datumAfter.getValue()));
//				}
//				break;

				case GAUGE:
				case STRING: 
				default: {
					MBeanDatum<?> datum = finalData.get(attributeName);
					result.add(PerfMonObservableDatum.newDatum(attributeName, datum.getValue()));
				}
				break;
			}
		}
		return result;
	}
	
	private Map<String,MBeanDatum<?>> buildDatumMap(MBeanDatum<?>[] data) {
		Map<String, MBeanDatum<?>> result = new HashMap<>();
		
		for (MBeanDatum<?> datum : data) {
			result.put(datum.getName(), datum);
		}
		
		return result;
	}
}
