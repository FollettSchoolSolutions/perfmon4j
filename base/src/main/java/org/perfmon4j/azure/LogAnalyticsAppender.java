package org.perfmon4j.azure;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.SystemNameAndGroupsAppender;
import org.perfmon4j.util.HttpHelper;
import org.perfmon4j.util.HttpHelper.Response;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


public class LogAnalyticsAppender extends SystemNameAndGroupsAppender {
	private static final Logger logger = LoggerFactory.initLogger(LogAnalyticsAppender.class);
	private static final String API_VERSION = "2016-04-01";
	private static final String CONTENT_TYPE = "application/json";
	
	private String customerID = null;
	private String sharedKey = null;
	private String azureResourceID = null;
	private boolean numericOnly = false;
	
	private int batchSeconds = 5; // How long to delay before sending a batch of measurements out.
	private int maxMeasurementsPerBatch = 1000;  // Max number of measurements to send per post.
	private final HttpHelper helper = new HttpHelper();
	private final AtomicInteger batchWritersPending = new AtomicInteger(0);
	
	private final Object writeQueueLockTokan = new Object();
	
	private final List<QueueElement> writeQueue = new ArrayList<QueueElement>();
	
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

	/* package level for testing */ String createSignature(String message) throws IOException {
		final String signingAlg = "HmacSHA256";
		
		if (sharedKey == null) {
			throw new IOException("Unable to sign message, sharedKey==null");
		}
		
		try {
			Mac mac = Mac.getInstance(signingAlg);
			SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(sharedKey), signingAlg);
			
			mac.init(key);
			byte[] bytes = mac.doFinal(message.getBytes("UTF-8"));
			return Base64.getEncoder().encodeToString(bytes);
		} catch (Exception e) {
			throw new IOException("Unable to create signature", e);
		}
	}
	
	
	/* package level for testing */ String buildStringToSign(int contentLength, String rfc1123DateTime) {
		StringBuilder result = new StringBuilder();
		
		result
			.append("POST\n")
			.append(contentLength)
			.append("\n")
			.append(CONTENT_TYPE)
			.append("\n")
			.append("x-ms-date:")
			.append(rfc1123DateTime)
			.append("\n/api/logs");
		
		return result.toString();
	}
	
	
	/* package level for testing */ Map<String, String> buildRequestHeaders(String logType, int contentLength) throws IOException {
		Map<String, String> result = new HashMap<String, String>();

		final String now = MiscHelper.formatTimeAsRFC1123(System.currentTimeMillis());
		
		result.put("Content-Type", CONTENT_TYPE);
		result.put("Log-Type", logType);
		result.put("time-generated-field", "timestamp");
		result.put("x-ms-date", now);
		
		String resID = getAzureResourceID();
		if (resID != null) {
			result.put("x-ms-AzureResourceId", resID);
		}
		String stringToSign = buildStringToSign(contentLength, now);
		String signature =  createSignature(stringToSign);

		result.put("Authorization", "SharedKey " + customerID + ":" + signature);
		
		return result;
	}
	
	
	static String trimPrefixOffCategory(String category) {
		return category.replaceFirst("^(Interval|Snapshot)\\.", "");
	}
	
	/* package level for testing */ String buildJSONElement(PerfMonObservableData data) {
		StringBuilder json = new StringBuilder();
		
		
		json.append("{");
		json.append(addValue("category", trimPrefixOffCategory(data.getDataCategory()), false));
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
		
		json.append(addValue("timestamp", MiscHelper.formatTimeAsISO8601(data.getTimestamp()), true));
		json.append("}");
		
		
		return numDataElements > 0 ? json.toString() : null;
	}
	
	private void appendDataLine(QueueElement element) {
		synchronized (writeQueueLockTokan) {
			if (writeQueue.size() < maxWriteQueueSize) {
				writeQueue.add(element);
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
	
	private List<QueueElement> getBatch() {
		List<QueueElement> result = null;
		synchronized (writeQueueLockTokan) {
			if (!writeQueue.isEmpty()) {
				result = new LinkedList<QueueElement>();
				result.addAll(writeQueue);
				writeQueue.clear();
			}
		}
		return result;
	}
	
	@Override
	public void outputData(PerfMonData data) {
		if (data instanceof PerfMonObservableData) {
			String json = buildJSONElement((PerfMonObservableData)data);
			if (json != null) {
				appendDataLine(new QueueElement(((PerfMonObservableData)data), json));
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

	public String getAzureResourceID() {
		return azureResourceID;
	}

	public void setAzureResourceID(String azureResourceID) {
		this.azureResourceID = azureResourceID;
	}
	
	static class QueueElement {
		private static final String INTERVAL_PREFIX = "P4J_Interval";
		private static final String MISC_PREFIX = "P4J_Misc";
		private static final String SNAPSHOT_PREFIX = "P4J_SnapShot";
		
		private final String key;
		private final String json; 
		
		QueueElement(PerfMonObservableData data, String json) {
			if (data instanceof SnapShotData) {
				key = snapShotSimpleClassNameToPrefix(data.getClass().getSimpleName());
			} else if (data instanceof IntervalData){
				key = INTERVAL_PREFIX;
			} else {
				key = MISC_PREFIX;
			}
			this.json = json;
		}
		
		/**
		 * TEST_ONLY
		 * @param key
		 * @param json
		 */
		QueueElement(String key, String json) {
			this.key = key;
			this.json = json;
		}
		
		static public Map<String, Deque<String>> sortIntoBatches(List<QueueElement> queue) {
			Map<String, Deque<String>> result = new HashMap<String, Deque<String>>();
			
			for(QueueElement element : queue) {
				String key = element.getKey();
				Deque<String> batch = result.get(key);
				if (batch == null) {
					batch = new LinkedList<String>();
					result.put(key, batch);
				}
				batch.add(element.getJson());
			}
			
			return result;
		}
		
		static String snapShotSimpleClassNameToPrefix(String simpleClassName) {
			String suffix = simpleClassName
				.replaceAll("\\d+$", "")
				.replaceAll("(SnapShot)+$", ""); 
			
			if (suffix.isEmpty()) {
				return SNAPSHOT_PREFIX;
			} else {
				return SNAPSHOT_PREFIX + "_" + suffix;
			}
		}

		public String getKey() {
			return key;
		}

		public String getJson() {
			return json;
		}
	}
	
	
	private class BatchWriter extends TimerTask {
		BatchWriter() {
			batchWritersPending.incrementAndGet();
		}
		
		@Override
		public void run() {
			batchWritersPending.decrementAndGet();
			
		List<QueueElement> batches = getBatch();
		for (Map.Entry<String, Deque<String>> entry : QueueElement.sortIntoBatches(batches).entrySet()) {
				String logType = entry.getKey();
				Deque<String> batch = entry.getValue();
				while (batch != null && !batch.isEmpty()) {
					int batchSize = 0;
					StringBuilder postBody = null;
					for (;batchSize < maxMeasurementsPerBatch && !batch.isEmpty(); batchSize++) {
						String line = batch.remove();
						if (postBody == null) {
							postBody = new StringBuilder();
							postBody.append("[");
						} else {
							postBody.append(",");
						}
						postBody.append(line);
					}
					postBody.append("]");
					HttpHelper helper = getHelper();
					String postURL = buildPostURL();
					try {
						Map<String, String> headers = buildRequestHeaders(logType, postBody.length());
						Response response = helper.doPost(buildPostURL(), postBody.toString(), headers);
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
}
