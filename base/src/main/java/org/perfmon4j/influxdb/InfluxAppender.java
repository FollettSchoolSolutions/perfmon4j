package org.perfmon4j.influxdb;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

/**
 * This appender uses the InfluxDb write endpoint.  For details
 * on the write endpoint and an explanation of the parameters
 * see https://docs.influxdata.com/influxdb/v1.6/tools/api/#write-http-endpoint 
 * 
 * @author perfmon
 *
 */
public class InfluxAppender extends SystemNameAndGroupsAppender {
	private static final Logger logger = LoggerFactory.initLogger(InfluxAppender.class);
	private String baseURL = null;
	private String database = null;
	private String userName = null;
	private String password = null;
	private String retentionPolicy = null;
	private boolean numericOnly = false;
	private int batchSeconds = 5; // How long to delay before sending a batch of measurements out.
	private int maxMeasurementsPerBatch = 1000;  // Max number of measurements to send per post.
	private final HttpHelper helper = new HttpHelper();
	private final AtomicInteger batchWritersPending = new AtomicInteger(0);
	
	private static String ESCAPED_DOUBLE_QUOTE = "\\\\\"";
	private static final Pattern DOUBLE_QUOTE_PATTERN = Pattern.compile("\"");
	private static String ESCAPED_SPACE = "\\\\ ";
	private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
	private static String ESCAPED_COMMA = "\\\\,";
	private static final Pattern COMMA_PATTERN = Pattern.compile("\\,");
	private static String ESCAPED_BACKSLASH = "\\\\\\\\";
	private static final Pattern BACKSLASH_PATTERN = Pattern.compile("\\\\");
	private static String ESCAPED_EQUALS = "\\\\=";
	private static final Pattern EQUALS_PATTERN = Pattern.compile("\\=");
	
	private final Object writeQueueLockTokan = new Object();
	private final List<String> writeQueue = new ArrayList<String>();
	
	private static final Object dedicatedTimerThreadLockTocken = new Object();
	private static Timer dedicatedTimerThread = null;
	
	private final String precision = "s";

	// This is a fail safe to prevent failure of writing to influxDb to allow measurements
	// to continue to accumulate and run out of memory.
	private final int maxWriteQueueSize = Integer.getInteger(InfluxAppender.class.getName() + ".maxQueueWriteSize" ,1000000).intValue();
	
	public InfluxAppender(AppenderID id) {
		super(id, false);
		this.setExcludeCWDHashFromSystemName(true);
	}
	
	/* package level for testing */ 
	/**
	 * Review https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_tutorial/ for information
	 * on escaping characters.
	 */
	String decorateDatumForInflux(PerfMonObservableDatum<?> datum) {
		String result = datum.toString();
		
		if (datum.isNumeric()) {
			Number value = datum.getValue();
			Object complexValue = datum.getComplexObject();
			if ((complexValue != null) && (complexValue instanceof Boolean)) {
				result = ((Boolean)complexValue).booleanValue() ? "true" : "false";
			} else if (value instanceof Short || value instanceof Integer || value instanceof Long) {
				// Signed 64-bit integers (Which includes java Long) are suffixed with an i
				result = result + "i";
			}
		} else {
			// Escape any quotes in the string
			result = DOUBLE_QUOTE_PATTERN.matcher(result).replaceAll(ESCAPED_DOUBLE_QUOTE);
			result = "\"" + result + "\"";
		}
		
		return result;
	}
	
	/* package level for testing  */ 
	/**
	 * Review https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_tutorial/ for information
	 * on escaping characters.
	 */
	String decorateMeasurementForInflux(String measurement) {
		measurement = BACKSLASH_PATTERN.matcher(measurement).replaceAll(ESCAPED_BACKSLASH);
		measurement = SPACE_PATTERN.matcher(measurement).replaceAll(ESCAPED_SPACE);
		measurement = COMMA_PATTERN.matcher(measurement).replaceAll(ESCAPED_COMMA);
		
		return measurement;
	}

	/* package level for testing  */ 
	/**
	 * Review https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_tutorial/ for information
	 * on escaping characters.
	 */
	String decorateTagKeyTagValueFieldKeyForInflux(String value) {
		// Escape backslash, space and comma just like measurement.
		value = decorateMeasurementForInflux(value);
		value = EQUALS_PATTERN.matcher(value).replaceAll(ESCAPED_EQUALS);
		
		return value;
	}
	
	
	/* package level for testing */ String buildPostURL() {
		StringBuilder url = new StringBuilder();
		url.append(baseURL);

		if (!baseURL.endsWith("/")) {
			url.append("/");
		}
		
		url.append("write?db=")
			.append(database)
			.append("&precision=")
			.append(precision);
		
		if (userName != null) {
			url.append("&u=")
				.append(userName);
		}
		if (password != null) {
			url.append("&p=")
				.append(password);
		}
		
		if (retentionPolicy != null) {
			url.append("&rp=")
				.append(retentionPolicy);
		}
		
		return url.toString();
	}
	
	/* package level for testing */ String buildPostDataLine(PerfMonObservableData data) {
		StringBuilder postLine = new StringBuilder();
		
		postLine.append(decorateMeasurementForInflux(data.getDataCategory()))
			.append(",system=")
			.append(decorateTagKeyTagValueFieldKeyForInflux(this.getSystemName()));
		
		String[] groups = getGroupsAsArray();
		if (groups.length > 0) {
			postLine.append(",group=")
				.append(decorateTagKeyTagValueFieldKeyForInflux(groups[0]));
		}
		postLine.append(" ");
		
		boolean first = true;
		int numDataElements = 0;
		for(Map.Entry<String, PerfMonObservableDatum<?>> entry : ((PerfMonObservableData) data).getObservations().entrySet()) {
			PerfMonObservableDatum<?> datum = entry.getValue();
			if (!datum.getInputValueWasNull() && (!numericOnly || datum.isNumeric())) {
				if (first) {
					first = false;
				} else {
					postLine.append(",");
				}
				numDataElements++;
				postLine.append(decorateTagKeyTagValueFieldKeyForInflux(entry.getKey()))
					.append("=")
					.append(decorateDatumForInflux(datum));
			}
		}
		postLine.append(" ");
		postLine.append(Long.toString(data.getTimestamp() / 1000));
		
		return numDataElements > 0 ? postLine.toString() : null;
	}
	
	private void appendDataLine(String line) {
		synchronized (writeQueueLockTokan) {
			if (writeQueue.size() < maxWriteQueueSize) {
				writeQueue.add(line);
			} else {
				logger.logWarn("InfluxAppender execeeded maxWriteQueueSize.  Measurement is being dropped");
			}
		}
		if (batchWritersPending.intValue() <= 0) {
			synchronized (dedicatedTimerThreadLockTocken) {
				if (dedicatedTimerThread == null) {
					dedicatedTimerThread = new Timer("PerfMon4J.InfluxAppenderHttpWriteThread", true);
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
			String dataLine = buildPostDataLine((PerfMonObservableData)data);
			if (dataLine != null) {
				appendDataLine(dataLine);
			} else {
				String numeric = numericOnly ? "numeric " : "";
				logger.logWarn("No " + numeric + "observable data elements found.  Skipping output of data: " + 
						((PerfMonObservableData)data).getDataCategory());
			}
		} else {
			logger.logWarn("Unable to write data to influxdb. Data class does not implement PerfMonObservableData. "
					+ "Data class = " + data.getClass());
		}
	}

	public String getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPrecision() {
		return precision;
	}

	public String getRetentionPolicy() {
		return retentionPolicy;
	}

	public void setRetentionPolicy(String retentionPolicy) {
		this.retentionPolicy = retentionPolicy;
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
	
	public boolean isNumericOnly() {
		return numericOnly;
	}

	public void setNumericOnly(boolean numericOnly) {
		this.numericOnly = numericOnly;
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
						String message = "Http error writing to InfluxDb using postURL: \"" + postURL + 
							"\" Response: " + response.toString();
						logger.logWarn(message);
					} else if (logger.isDebugEnabled()) {
						logger.logDebug("Measurements written to influxDb. BatchSize: " + batchSize);
					}
				} catch (IOException e) {
					String message = "Exception writing to InfluxDb using postURL: \"" + postURL + "\"";
					if (logger.isDebugEnabled()) {
						logger.logWarn(message, e);
					} else {
						message += " Exception: " + e.getMessage();
						logger.logWarn(message);
					}
				}
			}
		}
	}
}
