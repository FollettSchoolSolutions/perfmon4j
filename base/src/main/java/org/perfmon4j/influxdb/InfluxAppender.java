/*
 *	Copyright 2022 Follett School Solutions, LLC 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 *  Follett School Solutions, LLC
 *  1340 Ridgeview Drive
 *  McHenry, IL 60050
 *
 */
package org.perfmon4j.influxdb;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
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
import org.perfmon4j.util.SubCategorySplitter;

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
	private boolean numericOnly = false;
	private SubCategorySplitter subCategorySplitter = null;

	// InfluxDb 1.x attributes
	private String database = null;
	private String userName = null;
	private String password = null;
	private String retentionPolicy = null;
	
	// InfluxDb 2.x attributes
	private String bucket = null;
	private String org = null;
	private String token = null;
	
	private int batchSeconds = 5; // How long to delay before sending a batch of measurements out.
	private int maxMeasurementsPerBatch = 1000;  // Max number of measurements to send per post.
	private final HttpHelper helper;
	private final AtomicInteger batchWritersPending = new AtomicInteger(0);
	private boolean resubmitMeasurementsOnFailedPost = true;
	private int maxRetrysPerMeasurement = 5;
	
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
	private final List<DataLine> writeQueue = new ArrayList<DataLine>();
	
	private static final Object dedicatedTimerThreadLockTocken = new Object();
	private static Timer dedicatedTimerThread = null;
	private boolean unitTestMode = false;
	
	private final String precision = "s";

	// This is a fail safe to prevent failure of writing to influxDb to allow measurements
	// to continue to accumulate and run out of memory.
	private final int maxWriteQueueSize = Integer.getInteger(InfluxAppender.class.getName() + ".maxQueueWriteSize" ,1000000).intValue();
	
	public InfluxAppender(AppenderID id) {
		this(id, new HttpHelper());
	}

	/* package level for testing */ InfluxAppender(AppenderID id, HttpHelper helper) {
		super(id, false);
		this.setExcludeCWDHashFromSystemName(true);
		this.helper = helper;
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
	
	
	
	/* package level for testing */ PostUrlAndHeaders buildPostURL() {
		StringBuilder url = new StringBuilder();
		url.append(baseURL);
		Map<String, String> headers = null;
		final boolean api2Output = isInfluxDb2Output();

		if (!baseURL.endsWith("/")) {
			url.append("/");
		}
		
		if (api2Output) {
			url.append("api/v2/write?org=")
				.append(helper.urlEncodeUTF_8(org))
				.append("&bucket=")
				.append(helper.urlEncodeUTF_8(bucket));
			
			headers = new HashMap<String, String>();
			headers.put("Authorization", "Token " + token);
			headers.put("Content-Type", "text/plain; charset=utf-8");
			headers.put("Accept", "application/json");
		} else {
			url.append("write?db=")
				.append(helper.urlEncodeUTF_8(database));
			if (userName != null) {
				url.append("&u=")
					.append(helper.urlEncodeUTF_8(userName));
			}
			if (password != null) {
				url.append("&p=")
					.append(helper.urlEncodeUTF_8(password));
			}
			if (retentionPolicy != null) {
				url.append("&rp=")
					.append(helper.urlEncodeUTF_8(retentionPolicy));
			}
		}
		url.append("&precision=")
		.append(precision);

		return new PostUrlAndHeaders(url.toString(), headers);
	}
	
	/* package level for testing */ String buildPostDataLine(PerfMonObservableData data) {
		StringBuilder tags = new StringBuilder();
		StringBuilder fields = new StringBuilder();
		String category = data.getDataCategory();
		String subCategory = null;
		
		if (subCategorySplitter != null) {
			SubCategorySplitter.Split split = subCategorySplitter.split(category);
			category = split.getCategory();
			subCategory = split.getSubCategory();
		}
		
		tags.append(decorateMeasurementForInflux(category))
			.append(",system=")
			.append(decorateTagKeyTagValueFieldKeyForInflux(this.getSystemName()));
		
		String[] groups = getGroupsAsArray();
		if (groups.length > 0) {
			tags.append(",group=")
				.append(decorateTagKeyTagValueFieldKeyForInflux(groups[0]));
		}
		
		if (subCategory != null) {
			tags.append(",subCategory=")
				.append(decorateTagKeyTagValueFieldKeyForInflux(subCategory));
		}
		
		boolean first = true;
		int numDataElements = 0;
		for(PerfMonObservableDatum<?> datum : ((PerfMonObservableData) data).getObservations()) {
			String fieldName = datum.getFieldName();
			if (TagField.isTagField(category, fieldName, getTagFields())) {
				tags.append(",")
					.append(decorateMeasurementForInflux(fieldName))
					.append("=")
					.append(decorateTagKeyTagValueFieldKeyForInflux(datum.toString()));
			} else 	if (!datum.getInputValueWasNull() && (!numericOnly || datum.isNumeric())) {
				if (first) {
					first = false;
				} else {
					fields.append(",");
				}
				numDataElements++;
				fields.append(decorateTagKeyTagValueFieldKeyForInflux(datum.getDefaultDisplayName()))
					.append("=")
					.append(decorateDatumForInflux(datum));
			}
		}
		
		StringBuilder postLine = new StringBuilder();
		postLine.append(tags)
			.append(" ")
			.append(fields)
			.append(" ")
			.append(Long.toString(data.getTimestamp() / 1000));
		
		return numDataElements > 0 ? postLine.toString() : null;
	}
	
	private void appendDataLines(DataLine... lines) {
		synchronized (writeQueueLockTokan) {
			for (DataLine line : lines) {
				if (writeQueue.size() < maxWriteQueueSize) {
					if (line.getRetryCount() <= maxRetrysPerMeasurement) {
						writeQueue.add(line);
					}
				} else {
					logger.logWarn("InfluxAppender execeeded maxWriteQueueSize.  Measurement(s) are being dropped");
					break;
				}
			}
		}
		if (!unitTestMode && (batchWritersPending.intValue() <= 0)) {
			synchronized (dedicatedTimerThreadLockTocken) {
				if (dedicatedTimerThread == null) {
					dedicatedTimerThread = new Timer("PerfMon4J.InfluxAppenderHttpWriteThread", true);
				}
			}
			dedicatedTimerThread.schedule(new BatchWriter(), getBatchSeconds() * 1000);
		}
	}
	
	
	void directRunBatchWriterForTest() {
		new BatchWriter().run();
	}
	
	private Deque<DataLine> getBatch() {
		Deque<DataLine> result = null;
		synchronized (writeQueueLockTokan) {
			if (!writeQueue.isEmpty()) {
				result = new LinkedList<DataLine>();
				result.addAll(writeQueue);
				writeQueue.clear();
			}
		}
		return result;
	}
	
	@Override
	public void outputData(PerfMonData data) {
		if (data instanceof PerfMonObservableData) {
			String line = buildPostDataLine((PerfMonObservableData)data);
			if (line != null) {
				appendDataLines(new DataLine(line));
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

	public SubCategorySplitter getSubCategorySplitter() {
		return subCategorySplitter;
	}
	
	public void setSubCategorySplitter(SubCategorySplitter subCategorySplitter) {
		this.subCategorySplitter = subCategorySplitter;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public HttpHelper getHelper() {
		return helper;
	}
	
	public boolean isNumericOnly() {
		return numericOnly;
	}
	
	/* package level for testing */ boolean isInfluxDb2Output() {
		return bucket != null;
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

	public boolean isResubmitMeasurementsOnFailedPost() {
		return resubmitMeasurementsOnFailedPost;
	}

	public void setResubmitMeasurementsOnFailedPost(boolean resubmitMeasurementsOnFailedPost) {
		this.resubmitMeasurementsOnFailedPost = resubmitMeasurementsOnFailedPost;
	}
	
	public int getMaxRetrysPerMeasurement() {
		return maxRetrysPerMeasurement;
	}

	public void setMaxRetrysPerMeasurement(int maxRetrysPerMeasurement) {
		this.maxRetrysPerMeasurement = maxRetrysPerMeasurement;
	}
	
	boolean isUnitTestMode() {
		return unitTestMode;
	}

	void setUnitTestMode(boolean unitTestMode) {
		this.unitTestMode = unitTestMode;
	}

	private class BatchWriter extends TimerTask {
		BatchWriter() {
			batchWritersPending.incrementAndGet();
		}
		
		@Override
		public void run() {
			batchWritersPending.decrementAndGet();
			List<DataLine> resubmitMeasurementsOnFail = null; 
			Deque<DataLine> batch = getBatch();
			
			while (batch != null && !batch.isEmpty()) {
				if (isResubmitMeasurementsOnFailedPost()) {
					// Store off the batch, if the post fails we will
					// resubmit the measurements for inclusion in 
					// the next batch.
					resubmitMeasurementsOnFail = new ArrayList<DataLine>();
				}
				
				int batchSize = 0;
				StringBuilder postBody = null;
				for (;batchSize < maxMeasurementsPerBatch && !batch.isEmpty(); batchSize++) {
					DataLine dataLine = batch.remove();
					if (resubmitMeasurementsOnFail != null) {
						resubmitMeasurementsOnFail.add(dataLine.incRetryCount());
					} 
					if (postBody == null) {
						postBody = new StringBuilder();
					} else {
						postBody.append("\n");
					}
					postBody.append(dataLine.getLine());
				}
				HttpHelper helper = getHelper();
				PostUrlAndHeaders postURL = buildPostURL();
				String debugOutput = "URL(" + postURL + ") batchSize(" + batchSize + ")";
				try {
					Response response = helper.doPost(postURL.getUrl(), postBody.toString(), postURL.getHeaders());
					if (!response.isSuccess()) {
						String message = "Http error writing to InfluxDb: \"" + debugOutput + 
							"\" Response: " + response.toString();
						logger.logWarn(message);
					} else if (logger.isDebugEnabled()) {
						logger.logDebug("Measurements written to influxDb: " + debugOutput);
					}
				} catch (IOException e) {
					String message = "Exception writing to InfluxDb"+ (resubmitMeasurementsOnFail != null ? " (Measurements will be retried on next post)" : "") + ": " + debugOutput;
					if (logger.isDebugEnabled()) {
						logger.logWarn(message, e);
					} else {
						message += " Exception(" + e.getClass().getName() + "): " + e.getMessage();
						logger.logWarn(message);
					}
					if (resubmitMeasurementsOnFail != null) {
						appendDataLines(resubmitMeasurementsOnFail.toArray(new DataLine[]{}));
					}
				}
			}
		}
	}
	
	static class PostUrlAndHeaders {
		private final String url;
		private final Map<String, String> headers;
		
		private PostUrlAndHeaders(String url, Map<String, String> headers) {
			this.url = url;
			this.headers = (headers != null) ? Collections.unmodifiableMap(headers) : null;
		}

		public String getUrl() {
			return url;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}
		
		private boolean hasHeaders() {
			return headers != null;
		}

		@Override
		public String toString() {
			if (headers == null) {
				return url;
			} else {
				return "[url=" + url + ", headers=" + headers + "]";
			}
		}
	}
	
	
	private final class DataLine {
		private final String line;
		private int retryCount = 0;
		
		DataLine(String line) {
			this.line = line;
		}
		
		DataLine incRetryCount() {
			retryCount++;
			return this;
		}

		public String getLine() {
			return line;
		}
		
		public int getRetryCount() {
			return retryCount;
		}

		@Override
		public String toString() {
			return line;
		}
		
		
		
	}
	
}
