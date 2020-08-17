package org.perfmon4j.azure;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.perfmon4j.PerfMonData;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.SystemNameAndGroupsAppender;
import org.perfmon4j.util.HttpHelper;
import org.perfmon4j.util.HttpHelper.Response;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


public class LogAnalyticsAppender extends SystemNameAndGroupsAppender {
	private static final Logger logger = LoggerFactory.initLogger(LogAnalyticsAppender.class);
	private static final String API_VERSION = "2016-04-01";
	
	private String customerID = null;
	private String sharedKey = null;
	private String resourceID = null;
	private boolean numericOnly = false;
	
	private int batchSeconds = 5; // How long to delay before sending a batch of measurements out.
	private int maxMeasurementsPerBatch = 1000;  // Max number of measurements to send per post.
	private final HttpHelper helper = new HttpHelper();
	private final AtomicInteger batchWritersPending = new AtomicInteger(0);
	
	private final Object writeQueueLockTokan = new Object();
	
	final List<String> writeQueue = new ArrayList<String>();
	
	private static final Object dedicatedTimerThreadLockTocken = new Object();
	private static Timer dedicatedTimerThread = null;
	
	// This is a fail safe to prevent failure of writing to influxDb to allow measurements
	// to continue to accumulate and run out of memory.
	private final int maxWriteQueueSize = Integer.getInteger(LogAnalyticsAppender.class.getName() + ".maxQueueWriteSize" ,1000000).intValue();
	
	public LogAnalyticsAppender(AppenderID id) {
		super(id, false);
		this.setExcludeCWDHashFromSystemName(true);
	}
	
	/* package level for testing */ String buildPostURL() {
		StringBuilder url = new StringBuilder();
		
		url.append("https://")
			.append(getCustomerID())
			.append(".ods.opinsights.azure.com/api/logs?api-version=")
			.append(API_VERSION);
		
		return url.toString();
	}
	
	private String addValueNoQuotes(String key, String value, boolean isLastValue) {
		return "\"" + MiscHelper.escapeJSONString(key) + "\" : " + value + (isLastValue ? "" : ",");
	}

	private String addValue(String key, String value, boolean isLastValue) {
		return "\"" + MiscHelper.escapeJSONString(key) + "\" : \"" + MiscHelper.escapeJSONString(value) + "\"" + (isLastValue ? "" : ",");
	}
	
	private String addDatumValue(PerfMonObservableDatum<?> datum, boolean isLastValue) {
		String result = null;
		String key = datum.getDefaultDisplayName();
		
		if (datum.isNumeric()) {
			Number value = datum.getValue();
			Object complexValue = datum.getComplexObject();
			if ((complexValue != null) && (complexValue instanceof Boolean)) {
				result = addValueNoQuotes(key, (((Boolean)complexValue).booleanValue() ? "true" : "false"), isLastValue);
			} else { 
				result = addValueNoQuotes(key, datum.toString(), isLastValue);;
			}
		} else {
			result = addValue(key, datum.toString(), isLastValue);
		}
		
		return result;
	}
	
	
	
	/* package level for testing */ String buildJSONElement(PerfMonObservableData data) {
		StringBuilder json = new StringBuilder();
		
		json.append("{");
		json.append(addValue("category", data.getDataCategory(), false));
		json.append(addValue("systemName", getSystemName(), false));
		
		String[] groups = getGroupsAsArray();
		if (groups.length > 0) {
			json.append(addValue("group", groups[0], false));
		}
		
		int numDataElements = 0;
		for(PerfMonObservableDatum<?> datum : ((PerfMonObservableData) data).getObservations()) {
			if (!datum.getInputValueWasNull() && (!numericOnly || datum.isNumeric())) {
				numDataElements++;
				json.append(addDatumValue(datum, false));
			}
		}
		
		json.append(addValueNoQuotes("timestamp", Long.toString(data.getTimestamp() / 1000), true));
		json.append("}");
		
		
		return numDataElements > 0 ? json.toString() : null;
	}
	
	private void appendDataLine(String line) {
		synchronized (writeQueueLockTokan) {
			if (writeQueue.size() < maxWriteQueueSize) {
				writeQueue.add(line);
			} else {
				logger.logWarn("LogAnalyticsAppender execeeded maxWriteQueueSize.  Measurement is being dropped");
			}
		}
		if (batchWritersPending.intValue() <= 0) {
			synchronized (dedicatedTimerThreadLockTocken) {
				if (dedicatedTimerThread == null) {
					dedicatedTimerThread = new Timer("PerfMon4J.LogAnalyticsAppenderHttpWriteThread", true);
				}
			}
			dedicatedTimerThread.schedule(new BatchWriter(), getBatchSeconds() * 1000);
		}
	}
	
	private Deque<String> getBatch() {
		Deque<String> result = null;
		synchronized (writeQueueLockTokan) {
			if (!writeQueue.isEmpty()) {
				result = new LinkedList<String>();
				result.addAll(writeQueue);
				writeQueue.clear();
			}
		}
		return result;
	}
	
	@Override
	public void outputData(PerfMonData data) {
		if (data instanceof PerfMonObservableData) {
			String dataLine = buildJSONElement((PerfMonObservableData)data);
			if (dataLine != null) {
				appendDataLine(dataLine);
			} else {
				logger.logWarn("No observable data elements found.  Skipping output of data: " + 
						((PerfMonObservableData)data).getDataCategory());
			}
		} else {
			logger.logWarn("Unable to write data to Azure. Data class does not implement PerfMonObservableData. "
					+ "Data class = " + data.getClass());
		}
	}

	public int getBatchSeconds() {
		return batchSeconds;
	}
	
	public void setBatchSeconds(int batchSeconds) {
		this.batchSeconds = batchSeconds;
	}
	
	public int getMaxMeasurementsPerBatch() {
		return maxMeasurementsPerBatch;
	}

	public void setMaxMeasurementsPerBatch(int maxMeasurementsPerBatch) {
		this.maxMeasurementsPerBatch = maxMeasurementsPerBatch;
	}

	public HttpHelper getHelper() {
		return helper;
	}
	
	public void setConnectTimeoutMillis(int connectTimeoutMillis) {
		helper.setConnectTimeoutMillis(connectTimeoutMillis);
	}
	
	public int getConnectTimeoutMillis() {
		return helper.getConnectTimeoutMillis();
	}
	
	public void setReadTimeoutMillis(int readTimeoutMillis) {
		helper.setReadTimeoutMillis(readTimeoutMillis);
	}

	public int getReadTimeoutMillis() {
		return helper.getReadTimeoutMillis();
	}
	
	public String getCustomerID() {
		return customerID;
	}

	public void setCustomerID(String customerID) {
		this.customerID = customerID;
	}

	public String getSharedKey() {
		return sharedKey;
	}

	public void setSharedKey(String sharedKey) {
		this.sharedKey = sharedKey;
	}
	
	public boolean isNumericOnly() {
		return numericOnly;
	}

	public void setNumericOnly(boolean numericOnly) {
		this.numericOnly = numericOnly;
	}

	private class BatchWriter extends TimerTask {
		BatchWriter() {
			batchWritersPending.incrementAndGet();
		}
		
		@Override
		public void run() {
			batchWritersPending.decrementAndGet();
			
			Deque<String> batch = getBatch();
			while (batch != null && !batch.isEmpty()) {
				int batchSize = 0;
				StringBuilder postBody = null;
				for (;batchSize < maxMeasurementsPerBatch && !batch.isEmpty(); batchSize++) {
					String line = batch.remove();
					if (postBody == null) {
						postBody = new StringBuilder();
					} else {
						postBody.append("\n");
					}
					postBody.append(line);
				}
				HttpHelper helper = getHelper();
				String postURL = buildPostURL();
				try {
					Response response = helper.doPost(buildPostURL(), postBody.toString());
					if (!response.isSuccess()) {
						String message = "Http error writing to Azure using postURL: \"" + postURL + 
							"\" Response: " + response.toString();
						logger.logWarn(message);
					} else if (logger.isDebugEnabled()) {
						logger.logDebug("Measurements written to azure. BatchSize: " + batchSize);
					}
				} catch (IOException e) {
					String message = "Exception writing to Azure using postURL: \"" + postURL + "\"";
					if (logger.isDebugEnabled()) {
						logger.logWarn(message, e);
					} else {
						message += " Exception(" + e.getClass().getName() + "): " + e.getMessage();
						logger.logWarn(message);
					}
				}
			}
		}
	}
}
