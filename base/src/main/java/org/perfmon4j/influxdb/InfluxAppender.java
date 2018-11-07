package org.perfmon4j.influxdb;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.perfmon4j.PerfMon;
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
	private String username = null;
	private String password = null;
	private String retentionPolicy = null;
	private int batchSeconds = 5; // How long to delay before sending a batch out.
	private final HttpHelper helper = new HttpHelper();
	private final AtomicInteger batchWritersPending = new AtomicInteger(0);
	
	private Object lockToken = new Object();
	private List<String> writeQueue = new ArrayList<String>();
	
	private final String precision = "s";
	
	public InfluxAppender(AppenderID id) {
		super(id);
		this.setExcludeCWDHashFromSystemName(true);
	}
	
	private String quoteIfNeeded(String value) {
		if (value != null && value.contains(" ")) {
			value = "\"" + value + "\"";
		}
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
		
		if (username != null) {
			url.append("&u=")
				.append(username);
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
		postLine.append(quoteIfNeeded(data.getDataCategory()))
			.append(",system=")
			.append(quoteIfNeeded(this.getSystemName()));
		
		String[] groups = getGroupsAsArray();
		if (groups.length > 0) {
			postLine.append(",group=")
				.append(quoteIfNeeded(groups[0]));
		}
		postLine.append(" ");
		
		boolean first = true;
		for(Map.Entry<String, PerfMonObservableDatum<?>> entry : ((PerfMonObservableData) data).getObservations().entrySet()) {
			if (first) {
				first = false;
			} else {
				postLine.append(",");
			}
			postLine.append(quoteIfNeeded(entry.getKey()))
				.append("=")
				.append(entry.getValue().toString());
		}
		postLine.append(" ");
		postLine.append(Long.toString(data.getTimestamp() / 1000));
		
		return postLine.toString();
	}
	
	private void appendDataLine(String line) {
		synchronized (lockToken) {
			writeQueue.add(line);
		}
	}
	
	private List<String> getBatch() {
		List<String> result = null;
		synchronized (lockToken) {
			if (!writeQueue.isEmpty()) {
				result = new ArrayList<String>();
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
			appendDataLine(dataLine);
			if (batchWritersPending.intValue() <= 0) {
				PerfMon.utilityTimer.schedule(new BatchWriter(), getBatchSeconds() * 1000);
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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
	
	public HttpHelper getHelper() {
		return helper;
	}

	private class BatchWriter extends TimerTask {
		BatchWriter() {
			batchWritersPending.incrementAndGet();
		}
		
		@Override
		public void run() {
			batchWritersPending.decrementAndGet();
			
			List<String> batch = getBatch();
			if (batch != null) {
				StringBuilder postBody = null;
				for (String line : batch) {
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
