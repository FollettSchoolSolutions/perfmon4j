package org.perfmon4j.utils.parsers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.perfmon4j.utils.parsers.ParseTextAppenderLog.IntervalVO;


public class ParseTextAppenderLogTest extends TestCase {
	
	public static final String EXAMPLE_A = 
			"2014-09-16 02:01:53,454 INFO  [org.perfmon4j.TextAppender] (PerfMon.utilityTimer)\r\n" + 
			"********************************************************************************\r\n" +
			"OutboundWS.wrxxfsctts01_fdr_follett_com\r\n" +
			"02:00:52:360 -> 02:01:52:360\r\n" +
			" Max Active Threads. 1 (2014-09-16 04:12:47:463)\r\n" +
			" Throughput......... 2.22 per minute\r\n" +
			" Average Duration... 3.33\r\n" +
			" Median Duration.... 4.44\r\n" +
			" > 100 ms........... 5.55%\r\n" +
			" > 500 ms........... 6.66%\r\n" +
			" > 1 second......... 7.77%\r\n" +
			" Standard Deviation. 8.88\r\n" +
			" Max Duration....... 9 (2014-09-16 04:12:47:463)\r\n" +
			" Min Duration....... 10 (2014-09-16 04:12:47:463)\r\n" +
			" Total Hits......... 11\r\n" +
			" Total Completions.. 12\r\n" +
			"Lifetime (2014-09-12 10:36:03):\r\n" +
			" Max Active Threads. 1 (2014-09-15 12:18:51:502)\r\n" +
			" Max Throughput..... 4.00 (2014-09-15 10:44:44 -> 2014-09-15 10:45:44)\r\n" +
			" Average Duration... 37.00\r\n" +
			" Standard Deviation. 55.84\r\n" +
			" Max Duration....... 260 (2014-09-15 12:18:51:762)\r\n" +
			" Min Duration....... 3 (2014-09-15 10:46:18:608)\r\n" +
			"********************************************************************************\r\n";

	public static final String EXAMPLE_STARTED_PREVIOUS_DAY = 
			"2014-09-16 02:01:53,454 INFO  [org.perfmon4j.TextAppender] (PerfMon.utilityTimer)\r\n" + 
			"********************************************************************************\r\n" +
			"OutboundWS.wrxxfsctts01_fdr_follett_com\r\n" +
			"23:59:52:360 -> 00:01:52:360\r\n" +
			" Max Active Threads. 1 (2014-09-16 04:12:47:463)\r\n" +
			" Throughput......... 2.22 per minute\r\n" +
			" Average Duration... 3.33\r\n" +
			" Median Duration.... NA\r\n" +
			" > 100 ms........... 5.55%\r\n" +
			" > 500 ms........... 6.66%\r\n" +
			" > 1 second......... 7.77%\r\n" +
			" Standard Deviation. 8.88\r\n" +
			" Max Duration....... 9 (2014-09-16 04:12:47:463)\r\n" +
			" Min Duration....... 10 (2014-09-16 04:12:47:463)\r\n" +
			" Total Hits......... 11\r\n" +
			" Total Completions.. 12\r\n" +
			"(SQL)Avg. Duration. 13.13\r\n" +
			"(SQL)Std. Dev...... 14.14\r\n" +
			"(SQL)Max Duration.. 15 (2014-09-16 04:12:58:321)\r\n" +
			"(SQL)Min Duration.. 16 (2014-09-16 04:12:10:588)\r\n" +
			"Lifetime (2014-09-12 10:36:03):\r\n" +
			" Max Active Threads. 1 (2014-09-15 12:18:51:502)\r\n" +
			" Max Throughput..... 4.00 (2014-09-15 10:44:44 -> 2014-09-15 10:45:44)\r\n" +
			" Average Duration... 37.00\r\n" +
			" Standard Deviation. 55.84\r\n" +
			" Max Duration....... 260 (2014-09-15 12:18:51:762)\r\n" +
			" Min Duration....... 3 (2014-09-15 10:46:18:608)\r\n" +
			"********************************************************************************\r\n";
	
	
	public ParseTextAppenderLogTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}


	public void testGetThreshold() {
		String []times = ParseTextAppenderLog.getThreshold(" > 100 ms........... 8.15%");
		assertNotNull(times);
		assertEquals(2, times.length);;
	
		assertEquals("threshold", "100 ms", times[0]);
		assertEquals("percentage", "8.15", times[1]);
		
		assertNull("Must match expected pattern", ParseTextAppenderLog.getThreshold("X > 100 ms........... 8.15%"));
		assertNull("Should be null safe", ParseTextAppenderLog.getThreshold(null));
	}
	
	
	
	public void testGetSingleNumber_Integer() {
		assertEquals("934", ParseTextAppenderLog.getSingleNumber("Max Active Threads", " Max Active Threads. 934"));
		assertNull("Label must match", ParseTextAppenderLog.getSingleNumber("Max Active Threads", " Max Duration.... 934"));
		assertNull("Should be null safe", ParseTextAppenderLog.getSingleNumber("Max Active Threads", null));
	}
	
	public void testGetSingleNumber_Decimal() {
		assertEquals("934.567", ParseTextAppenderLog.getSingleNumber("Max Active Threads", " Max Active Threads. 934.567"));
	}

	public void testGetSingleNumber_NA() {
		assertEquals("NA", ParseTextAppenderLog.getSingleNumber("Max Active Threads", " Max Active Threads. NA"));
	}
	
	public void testGetStartEndTime() {
		String []times = ParseTextAppenderLog.getStartEndTimes("02:00:52:360 -> 02:01:52:360");
		assertNotNull(times);
		assertEquals(2, times.length);;
	
		assertEquals("start time", "02:00:52:360", times[0]);
		assertEquals("end time", "02:01:52:360", times[1]);
		
		assertNull("Must match expected pattern", ParseTextAppenderLog.getStartEndTimes("X 02:00:52:360 -> 02:01:52:360"));
		assertNull("Should be null safe", ParseTextAppenderLog.getStartEndTimes(null));
	}

	
	
//	"OutboundWS.wrxxfsctts01_fdr_follett_com\r\n" 

	public void testGetDateCategory() {
		assertEquals("OutboundWS.wrxxfsctts01_fdr_follett_com", ParseTextAppenderLog.getCategory("OutboundWS.wrxxfsctts01_fdr_follett_com"));
		assertNull("Must be a single line of alpha-numeric characters, no whitespace", ParseTextAppenderLog.getCategory("X OutboundWS.wrxxfsctts01_fdr_follett_com"));
		assertNull("Should be null safe", ParseTextAppenderLog.getCategory(null));
	}

	
	public void testIsSeparator() {
		assertTrue(ParseTextAppenderLog.isSeparator("********************************************************************************"));
		assertFalse(ParseTextAppenderLog.isSeparator("X********************************************************************************"));
		assertFalse(ParseTextAppenderLog.isSeparator(null));
	}
	
	public void testGetDateFromHeader() {
		assertEquals("2014-09-16", ParseTextAppenderLog.getHeader("2014-09-16 02:01:53,454 INFO  [org.perfmon4j.TextAppender] (PerfMon.utilityTimer)"));
		assertNull("Need to see org.perfmon4j.TextAppender in line", ParseTextAppenderLog.getHeader("2014-09-16 02:01:53,454 INFO  [org.perfmon4j.NotTextAppender] (PerfMon.utilityTimer)"));
		assertNull("Should be null safe", ParseTextAppenderLog.getHeader(null));
	}
	
	
	public void testTransformStringToDate() throws Exception {
		final String TEST_DATE = "2014-09-16 02:01:53:454";
		
		long timestamp = ParseTextAppenderLog.toTimestamp(TEST_DATE);
		
		assertEquals("Date should round trip between a timestamp", TEST_DATE, ParseTextAppenderLog.fromTimestamp(timestamp));
	}

	public void testDecrementTimestampBy1Day() throws Exception {
		final String TEST_DATE = "2014-09-15 02:01:53:454";
		
		long timestamp = ParseTextAppenderLog.toTimestamp(TEST_DATE);
		timestamp = ParseTextAppenderLog.decrementBy1Day(timestamp);
		
		assertEquals("Should have decremented by 1 day", "2014-09-14 02:01:53:454", ParseTextAppenderLog.fromTimestamp(timestamp));
	}
	
	
	
//	"2014-09-16 02:01:53,454 INFO  [org.perfmon4j.TextAppender] (PerfMon.utilityTimer)\r\n" + 
//	"********************************************************************************\r\n" +
//	"OutboundWS.wrxxfsctts01_fdr_follett_com\r\n" +
//	"02:00:52:360 -> 02:01:52:360\r\n" +
//	" Max Active Threads. 1\r\n" +
//	" Throughput......... 2.22 per minute\r\n" +
//	" Average Duration... 3.33\r\n" +
//	" Median Duration.... 444\r\n" +
//	" > 100 ms........... 5.55%\r\n" +
//	" > 500 ms........... 6.66%\r\n" +
//	" > 1 second......... 7.77%\r\n" +
//	" Standard Deviation. 8.88\r\n" +
//	" Max Duration....... 9\r\n" +
//	" Min Duration....... 10\r\n" +
//	" Total Hits......... 11\r\n" +
//	" Total Completions.. 12\r\n" +
//	"Lifetime (2014-09-12 10:36:03):\r\n" +
//	" Max Active Threads. 1 (2014-09-15 12:18:51:502)\r\n" +
//	" Max Throughput..... 4.00 (2014-09-15 10:44:44 -> 2014-09-15 10:45:44)\r\n" +
//	" Average Duration... 37.00\r\n" +
//	" Standard Deviation. 55.84\r\n" +
//	" Max Duration....... 260 (2014-09-15 12:18:51:762)\r\n" +
//	" Min Duration....... 3 (2014-09-15 10:46:18:608)\r\n" +
//	"********************************************************************************\r\n";	
	public void testSimpleParse() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(EXAMPLE_A.getBytes())));
		try {
			IntervalVO d = ParseTextAppenderLog.getNextElement(in);
			assertNotNull("Should have returned interval data", d);
			
			assertEquals("StartTime",  "2014-09-16 02:00:52:360", ParseTextAppenderLog.fromTimestamp(d.getStartTime()));
			assertEquals("EndTime",  "2014-09-16 02:01:52:360", ParseTextAppenderLog.fromTimestamp(d.getEndTime()));
			assertEquals("Category", "OutboundWS.wrxxfsctts01_fdr_follett_com", d.getCategory());
			assertEquals("Max Active Threads", 1, d.getMaxActiveThreads());
			assertEquals("Throughput", 2.22, d.getThroughput());
			assertEquals("Average Duration", 3.33, d.getAverageDuration());
			assertEquals("Median Duration", 4.44, d.getMedianDuration().doubleValue());
//			assertEquals("", 1, d.getMedianCalculator().getMedian().getResult());
//			" > 100 ms........... 5.55%\r\n" +
//			" > 500 ms........... 6.66%\r\n" +
//			" > 1 second......... 7.77%\r\n" +
			assertEquals("StandardDeviation", 8.88, d.getStandardDeviation());
			assertEquals("Max Duration", 9, d.getMaxDuration());
			assertEquals("Min Duration", 10, d.getMinDuration());
			assertEquals("Total Hits", 11, d.getTotalHits());
			assertEquals("Total Completions", 12, d.getTotalCompletions());
		} finally {
			in.close();
		}
	}

	public void testParseNeedToAdjustStartDate() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(EXAMPLE_STARTED_PREVIOUS_DAY.getBytes())));
		try {
			IntervalVO d = ParseTextAppenderLog.getNextElement(in);
			assertNotNull("Should have returned interval data", d);
			
			assertEquals("StartTime",  "2014-09-15 23:59:52:360", ParseTextAppenderLog.fromTimestamp(d.getStartTime()));
			assertEquals("EndTime",  "2014-09-16 00:01:52:360", ParseTextAppenderLog.fromTimestamp(d.getEndTime()));
		} finally {
			in.close();
		}
	}
	
//	"(SQL)Avg. Duration. 13.13\r\n" +
//	"(SQL)Std. Dev...... 14.14\r\n" +
//	"(SQL)Max Duration.. 15 (2014-09-16 04:12:58:321)\r\n" +
//	"(SQL)Min Duration.. 16 (2014-09-16 04:12:10:588)\r\n" +
	public void testParseGetSQLStatsIfExists() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(EXAMPLE_STARTED_PREVIOUS_DAY.getBytes())));
		try {
			IntervalVO d = ParseTextAppenderLog.getNextElement(in);
			assertNotNull("Should have returned interval data", d);
			
			assertEquals("SQL Avg Duration",  13.13, d.getSqlAverageDuration().doubleValue());
			assertEquals("SQL Std Dev",  14.14, d.getSqlStandardDeviation().doubleValue());
			assertEquals("SQL Max",  15, d.getSqlMaxDuration().longValue());
			assertEquals("SQL Min",  16, d.getSqlMinDuration().longValue());
		} finally {
			in.close();
		}
	}
	

	public void testNAForMedian() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(EXAMPLE_STARTED_PREVIOUS_DAY.getBytes())));
		try {
			IntervalVO d = ParseTextAppenderLog.getNextElement(in);
			assertNotNull("Should have returned interval data", d);
			
			assertNull("Median Duration was NA", d.getMedianDuration());
		} finally {
			in.close();
		}
	}
	
	public void testToCSV() throws Exception {
		IntervalVO vo = new IntervalVO();
		vo.setStartAndEndTime("2014-10-08 10:57:01:433", "2014-10-08 10:58:01:433");
		vo.setCategory("mycategory");
		vo.setMaxActiveThreads(1);
		vo.setThroughput(2.22);
		vo.setAverageDuration(3.33);
		vo.setMedianDuration(new Double(4.44));
		vo.setStandardDeviation(8.88);
		vo.setMaxDuration(9);
		vo.setMinDuration(10);
		vo.setTotalHits(11);
		vo.setTotalCompletions(12);
		vo.setSqlAverageDuration(Double.valueOf(13.13));
		vo.setSqlStandardDeviation(Double.valueOf(14.14));
		vo.setSqlMaxDuration(Long.valueOf(15));
		vo.setSqlMinDuration(Long.valueOf(16));
		
		final String expectedCSV = "DAP,2014-10-08 10:57:01.433,2014-10-08 10:58:01.433,mycategory,1,2.22,3.33,4.44,8.88,9,10,11,12,13.13,14.14,15,16";
		assertEquals("Expected CSV", expectedCSV, vo.toCSV("DAP"));
	}

	public void testToCSVOmitsNullMedian() throws Exception {
		IntervalVO vo = new IntervalVO();
		vo.setStartAndEndTime("2014-10-08 10:57:01:433", "2014-10-08 10:58:01:433");
		vo.setCategory("mycategory");
		vo.setMaxActiveThreads(1);
		vo.setThroughput(2.22);
		vo.setAverageDuration(3.33);
		vo.setMedianDuration(null);
		vo.setStandardDeviation(8.88);
		vo.setMaxDuration(9);
		vo.setMinDuration(10);
		vo.setTotalHits(11);
		vo.setTotalCompletions(12);
		
		final String expectedCSV = "DAP,2014-10-08 10:57:01.433,2014-10-08 10:58:01.433,mycategory,1,2.22,3.33,-1,8.88,9,10,11,12,-1,-1,-1,-1";
		assertEquals("Expected CSV", expectedCSV, vo.toCSV("DAP"));
	}
	
	public void testToCSVOmitsNullSQLMeasurs() throws Exception {
		IntervalVO vo = new IntervalVO();
		vo.setStartAndEndTime("2014-10-08 10:57:01:433", "2014-10-08 10:58:01:433");
		vo.setCategory("mycategory");
		vo.setMaxActiveThreads(1);
		vo.setThroughput(2.22);
		vo.setAverageDuration(3.33);
		vo.setMedianDuration(null);
		vo.setStandardDeviation(8.88);
		vo.setMaxDuration(9);
		vo.setMinDuration(10);
		vo.setTotalHits(11);
		vo.setTotalCompletions(12);
		
		final String expectedCSV = "DAP,2014-10-08 10:57:01.433,2014-10-08 10:58:01.433,mycategory,1,2.22,3.33,-1,8.88,9,10,11,12,-1,-1,-1,-1";
		assertEquals("Expected CSV", expectedCSV, vo.toCSV("DAP"));
	}
	

}
