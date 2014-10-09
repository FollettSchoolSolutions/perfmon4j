package org.perfmon4j.utils.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseTextAppenderLog {

	public ParseTextAppenderLog() {
	}

	public void doParse(File inputFile, File outputFile, String systemName) throws IOException {
		PrintStream out = new PrintStream(outputFile);
		try {
			doParse(inputFile, out, systemName);	
		} finally {
			out.close();
		}
	}
	
	public void doParse(File inputFile, PrintStream out, String systemName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		try {
			doParse(reader, out, systemName);	
		} finally {
			reader.close();
		}
		out.flush();
	}
	
	void doParse(BufferedReader in, PrintStream out, String systemName) throws IOException {
		Writer writer = new Writer(out, systemName);
		Thread writerThread = new Thread(writer);
		writerThread.start();
		IntervalVO data = null;
	
		while (in.ready()) {
			data = getNextElement(in);
			if (data != null) {
				writer.queueForWrite(data);
			}
		}
//		try {
//			Thread.currentThread().sleep(100);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

		writer.stopWhenEmpty();
		try {
			writerThread.join();
		} catch (InterruptedException e) {
			// Nothing to do...
		}
		if (writer.isAborted()) {
			throw writer.getAbortException();
		}
	}
	
	static IntervalVO getNextElement(BufferedReader in) throws IOException {
		boolean abort = false;
		IntervalVO result = null;
	
		String date = getHeader(in.readLine());
		String category = null;
		String startTime = null;
		String endTime = null;
		String maxActiveThreads = null;
		String throughput = null;
		String averageDuration = null;
		String medianDuration = null;
		String standardDeviation = null;
		String maxDuration = null;
		String minDuration = null;
		String totalHits = null;
		String totalCompletions = null;
		String sqlAverageDuration = null;
		String sqlStandardDeviation = null;
		String sqlMaxDuration = null;
		String sqlMinDuration = null;
		
		abort = (date == null);
		if (!abort) {
			abort = !isSeparator(in.readLine());
		}
		if (!abort) {
			category = getCategory(in.readLine());
			abort = (category == null);
		}
		if (!abort) {
			String[] startEndTime = getStartEndTimes(in.readLine());
			if (startEndTime != null) {
				startTime = date + " " + startEndTime[0];
				endTime = date + " " + startEndTime[1];
			} else {
				abort = true;
			}
		}
		if (!abort) {
			maxActiveThreads = getSingleNumber("Max Active Threads", in.readLine());
			abort = (maxActiveThreads == null);
		}
		if (!abort) {
			throughput = getSingleNumber("Throughput", in.readLine());
			abort = (throughput == null);
		}
		if (!abort) {
			averageDuration = getSingleNumber("Average Duration", in.readLine());
			abort = (averageDuration == null);
		}
		if (!abort) {
			medianDuration = getSingleNumber("Median Duration", in.readLine());
			abort = (medianDuration == null);
		}

		if (!abort) {
			// Skip through any thresholds...
			String nextLine = in.readLine();
			while ((getThreshold(nextLine) != null)) {
				nextLine = in.readLine();
			}
			standardDeviation = getSingleNumber("Standard Deviation", nextLine);
			abort = (standardDeviation == null);
		}
		if (!abort) {
			maxDuration = getSingleNumber("Max Duration", in.readLine());
			abort = (maxDuration == null);
		}
		if (!abort) {
			minDuration = getSingleNumber("Min Duration", in.readLine());
			abort = (minDuration == null);
		}
		if (!abort) {
			totalHits = getSingleNumber("Total Hits", in.readLine());
			abort = (totalHits == null);
		}
		if (!abort) {
			totalCompletions = getSingleNumber("Total Completions", in.readLine());
			abort = (totalCompletions == null);
		}
		
		// Check to see if we have optional SQL measurements.
		if (!abort) {
			sqlAverageDuration = getSingleNumber("(SQL)Avg. Duration", in.readLine());
			if (sqlAverageDuration != null) {
				sqlStandardDeviation = getSingleNumber("(SQL)Std. Dev", in.readLine());
				abort = (sqlStandardDeviation == null);
				
				if (!abort) {
					sqlMaxDuration = getSingleNumber("(SQL)Max Duration", in.readLine());
					abort = (sqlMaxDuration == null);
				}
				if (!abort) {
					sqlMinDuration = getSingleNumber("(SQL)Min Duration", in.readLine());
					abort = (sqlMinDuration == null);
				}
			}
		}

		if (!abort) {
			result = new IntervalVO();
			try {
				result.setStartAndEndTime(startTime, endTime);
				result.setCategory(category);
				result.setMaxActiveThreads(Long.parseLong(maxActiveThreads));
				result.setThroughput(Double.parseDouble(throughput));
				result.setAverageDuration(Double.parseDouble(averageDuration));
				result.setStandardDeviation(Double.parseDouble(standardDeviation));
				result.setMaxDuration(Long.parseLong(maxDuration));
				result.setMinDuration(Long.parseLong(minDuration));
				result.setTotalHits(Long.parseLong(totalHits));
				result.setTotalCompletions(Long.parseLong(totalCompletions));
				
				if (!"NA".equals(medianDuration)) {
					result.setMedianDuration(Double.parseDouble(medianDuration));
				}
				
				if (sqlAverageDuration != null) {
					result.setSqlAverageDuration(Double.parseDouble(sqlAverageDuration));
					result.setSqlStandardDeviation(Double.parseDouble(sqlStandardDeviation));
					result.setSqlMaxDuration(Long.parseLong(sqlMaxDuration));
					result.setSqlMinDuration(Long.parseLong(sqlMinDuration));
				}
				
			} catch (ParseException e) {
				e.printStackTrace();
				result = null;
			}
		}
		
		return result;
	}
	
	private static final Pattern HEADER_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s.+org\\.perfmon4j\\.TextAppender");
	private static final Pattern CATEGORY_PATTERN = Pattern.compile("(\\S*)");
	private static final Pattern SEPARATOR_PATTERN = Pattern.compile("\\*{20,80}");
	private static final Pattern START_END_TIME_PATTERN = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}:\\d{3}) -\\> (\\d{2}:\\d{2}:\\d{2}:\\d{3})");
	private static final Pattern SINGLE_NUMBER_PATTERN = Pattern.compile("(\\d+\\.?\\d*)");
	private static final Pattern THRESHOLD_PATTERN = Pattern.compile(" > (\\d+\\.?\\d* [^\\.]*)\\D*(\\d+\\.?\\d*)\\%");
	
	
	/**
	 * Look for the separator line
	 * @param line
	 * @return
	 */
	static boolean isSeparator(String line) {
		boolean result = false;
		
		if (line != null) {
			Matcher m = SEPARATOR_PATTERN.matcher(line);
			result = m.matches();
		}
		return result;
	}

	
	static String[] getThreshold(String line) {
		String[] result = null;
		
		if (line != null) {
			if (line.trim().startsWith(">")) {
				Matcher m = THRESHOLD_PATTERN.matcher(line);
				if (m.find()) {
					result = new String[2];
					result[0] = m.group(1);
					result[1] = m.group(2);
				} 			
			}
		}
		return result;
	}

	
	static String getSingleNumber(String label, String line) {
		String result = null;
		
		if (line != null) {
			if (line.trim().startsWith(label)) {
				Matcher m = SINGLE_NUMBER_PATTERN.matcher(line);
				if (m.find()) {
					result = m.group(1);
				} else {
					result = "NA";
				}
			}
		}
		return result;
	}

	
	static String getHeader(String line) {
		String result = null;
		
		if (line != null) {
			Matcher m = HEADER_PATTERN.matcher(line);
			if (m.find()) {
				result = m.group(1);
			}
		}
		return result;
	}

	static String getCategory(String line) {
		String result = null;
		
		if (line != null) {
			Matcher m = CATEGORY_PATTERN.matcher(line);
			if (m.matches()) {
				result = m.group(1);
			}
		}
		return result;
	}
	

	static String[] getStartEndTimes(String line) {
		String[] result = null;
		
		if (line != null) {
			Matcher m = START_END_TIME_PATTERN.matcher(line);
			if (m.matches()) {
				result = new String[2];
				result[0] = m.group(1);
				result[1] = m.group(2);
			}
		}
		return result;
	}
	
	private static final String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss:SSS";
	private static final String CSV_DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SSS";
	
//	2014-09-16 02:01:53,454
	
	static long toTimestamp(String dateTime) throws ParseException {
		DateFormat format = new SimpleDateFormat(DATE_FORMAT_STRING);
		return format.parse(dateTime).getTime();
	}
	
	static String fromTimestamp(long timestamp) {
		DateFormat format = new SimpleDateFormat(DATE_FORMAT_STRING);
		
		return format.format(new Date(timestamp));
	}

	static String fromTimestampForCSV(long timestamp) {
		DateFormat format = new SimpleDateFormat(CSV_DATE_FORMAT_STRING);
		
		return format.format(new Date(timestamp));
	}
	
	static long decrementBy1Day(long timestamp) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(new Date(timestamp));
		cal.add(Calendar.DATE, -1);
		
		return cal.getTime().getTime();
	}
	
	static void write(IntervalVO data, PrintStream out, String systemName) throws IOException {
		out.println(data.toCSV(systemName));
	}
	
	private class Writer implements Runnable {
		private final PrintStream out;
		private final String systemName;
		private volatile boolean stopWhenEmpty = false;
		private volatile IOException abortException = null;
		private final ArrayBlockingQueue<IntervalVO> output = new ArrayBlockingQueue<IntervalVO>(100);
		
		Writer(PrintStream out, String systemName) {
			this.out  = out;
			this.systemName = systemName;
		}
		
		public void run() {
			boolean done = false;
			out.println(IntervalVO.buildCSVHeader());
			while (!done && abortException == null) {
				try {
					IntervalVO d = output.poll(100, TimeUnit.MILLISECONDS);
					if (d != null) {
						try {
							write(d, out, systemName);
						} catch (IOException e) {
							abortException = e;
						}
					} else {
						done = stopWhenEmpty;
					}
				} catch (InterruptedException e) {
					// Do nothing.
				}
			}
		}
		
		public void stopWhenEmpty() {
			stopWhenEmpty = true;
		}
		
		public boolean isAborted() {
			return abortException != null;
		}
		
		public IOException getAbortException() {
			return abortException;
		}
		
		public void queueForWrite(IntervalVO data) throws IOException {
			if (abortException == null) {
				try {
					output.offer(data, 999, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			} else {
				throw abortException;
			}
		}
	}
	
	public static class IntervalVO {
		private long startTime;
		private long endTime;
		private String category;
		private long maxActiveThreads;
		private double throughput;
		private double averageDuration;
		private Double medianDuration;
		private double standardDeviation;
		private long maxDuration;
		private long minDuration;
		private long totalHits;
		private long totalCompletions;
		private Double sqlAverageDuration;
		private Double sqlStandardDeviation;
		private Long sqlMaxDuration;
		private Long sqlMinDuration;
		
		
		public long getStartTime() {
			return startTime;
		}
		public void setStartAndEndTime(String startTime, String endTime) throws ParseException {
			this.startTime = toTimestamp(startTime);
			this.endTime = toTimestamp(endTime);
			
			if (this.endTime < this.startTime) {
				this.startTime = decrementBy1Day(this.startTime);
			}
		}
		public long getEndTime() {
			return endTime;
		}
		public long getMaxActiveThreads() {
			return maxActiveThreads;
		}
		public void setMaxActiveThreads(long maxActiveThreads) {
			this.maxActiveThreads = maxActiveThreads;
		}
		public double getThroughput() {
			return throughput;
		}
		public void setThroughput(double throughput) {
			this.throughput = throughput;
		}
		public double getAverageDuration() {
			return averageDuration;
		}
		public void setAverageDuration(double averageDuration) {
			this.averageDuration = averageDuration;
		}
		public Double getMedianDuration() {
			return medianDuration;
		}
		public void setMedianDuration(Double medianDuration) {
			this.medianDuration = medianDuration;
		}
		public double getStandardDeviation() {
			return standardDeviation;
		}
		public void setStandardDeviation(double standardDeviation) {
			this.standardDeviation = standardDeviation;
		}
		public long getMaxDuration() {
			return maxDuration;
		}
		public void setMaxDuration(long maxDuration) {
			this.maxDuration = maxDuration;
		}
		public long getMinDuration() {
			return minDuration;
		}
		public void setMinDuration(long minDuration) {
			this.minDuration = minDuration;
		}
		public long getTotalHits() {
			return totalHits;
		}
		public void setTotalHits(long totalHits) {
			this.totalHits = totalHits;
		}
		public long getTotalCompletions() {
			return totalCompletions;
		}
		public void setTotalCompletions(long totalCompletions) {
			this.totalCompletions = totalCompletions;
		}
		public String getCategory() {
			return category;
		}
		public void setCategory(String category) {
			this.category = category;
		}
		public Double getSqlAverageDuration() {
			return sqlAverageDuration;
		}
		public void setSqlAverageDuration(Double sqlAverageDuration) {
			this.sqlAverageDuration = sqlAverageDuration;
		}
		public Double getSqlStandardDeviation() {
			return sqlStandardDeviation;
		}
		public void setSqlStandardDeviation(Double sqlStandardDeviation) {
			this.sqlStandardDeviation = sqlStandardDeviation;
		}
		public Long getSqlMaxDuration() {
			return sqlMaxDuration;
		}
		public void setSqlMaxDuration(Long sqlMaxDuration) {
			this.sqlMaxDuration = sqlMaxDuration;
		}
		public Long getSqlMinDuration() {
			return sqlMinDuration;
		}
		public void setSqlMinDuration(Long sqlMinDuration) {
			this.sqlMinDuration = sqlMinDuration;
		}
		public static String buildCSVHeader() {
			return "SystemName,StartTime,EndTime,Category,MaxActiveThreads,Throughput,Average,Median,StandardDeviation,MaxDuration,MinDuration,TotalHits,TotalCompetions,SQLAverage,SQLStandardDeviation,SQLMaxDuration,SQLMinDuration";
		}
		public String toCSV(String systemName) {
			StringBuilder builder = new StringBuilder();
			
			builder
				.append(systemName).append(",")
				.append(fromTimestampForCSV(startTime)).append(",")
				.append(fromTimestampForCSV(endTime)).append(",")
				.append(category).append(",")
				.append(maxActiveThreads).append(",")
				.append(throughput).append(",")
				.append(averageDuration).append(",");
			
			if (medianDuration != null) {
				builder.append(medianDuration);
			} else {
				builder.append("-1");
			}
			builder.append(",");
				
			builder.append(standardDeviation).append(",")
				.append(maxDuration).append(",")
				.append(minDuration).append(",")
				.append(totalHits).append(",")
				.append(totalCompletions).append(",");
			
			if (sqlAverageDuration != null) {
				builder
					.append(sqlAverageDuration).append(",")
					.append(sqlStandardDeviation).append(",")
					.append(sqlMaxDuration).append(",")
					.append(sqlMinDuration);
			} else {
				// Sql measure not included..
				builder.append("-1,-1,-1,-1");
			}
			
			return builder.toString();
		}
	}
	
	public static void main(String args[]) throws Exception {
		long start = System.currentTimeMillis();
		
		ParseTextAppenderLog parser = new ParseTextAppenderLog();
//		parser.doParse(new File("/media/sf_shared/NoBackup/tmp.txt"), new File("/media/sf_shared/NoBackup/output.csv"));
		parser.doParse(new File("/media/sf_shared/NoBackup/FollettShelf/dap/pre_09-20-14.DAPX06"), 
				new File("/media/sf_shared/NoBackup/pre_09-20-14_DAP6.csv"),
				"DAPNode6");
		System.out.println("DONE: " + ((System.currentTimeMillis() - start)/1000) + " seconds");
	}
}
