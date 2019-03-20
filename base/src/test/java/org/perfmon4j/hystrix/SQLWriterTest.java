package org.perfmon4j.hystrix;

import java.sql.Connection;

import org.mockito.Mockito;
import org.perfmon4j.SQLTest;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.util.JDBCHelper;

public class SQLWriterTest extends SQLTest {
	private Connection conn;
	private String schema;
	
	public SQLWriterTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		conn = appender.getConnection();
		schema = appender.getDbSchema();
	}

	protected void tearDown() throws Exception {
		conn = null;
		super.tearDown();
	}

	public void testWriteCommandData() throws Exception {
		SQLWriter writer = new SQLWriter();
		
		HystrixCommandData data = buildCommandDataMock("TestKey");

		writer.writeToSQL(conn, schema, data, 1L);
		
		long keyRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixKey WHERE KeyID = 1 AND KeyName = 'TestKey'");
		long dataRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixCommand WHERE KeyID = 1");
		
		assertEquals("Should have inserted key row", 1 ,keyRows);
		assertEquals("Should have inserted data row", 1 ,dataRows);
//System.out.println(JDBCHelper.dumpQuery(conn, "SELECT * FROM " + schema + ".P4JHystrixCommand"));
	}


	
	/**
	 * If you create a monitor without an instance name it will display in the 
	 * text appender as Composite(<key1>, <key2>, <etc...>).   The key that
	 * should be displayed in the SQL database is "ALL"
	 * @throws Exception
	 */
	public void testWriteCommandDataCompositeDataCreatesKeyNameOfALL() throws Exception {
		SQLWriter writer = new SQLWriter();
		
		HystrixCommandData data = buildCommandDataMock("Composite(\"StockQuoteCommand\")");
		
		
		writer.writeToSQL(conn, schema, data, 1L);
		
		long keyRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixKey WHERE KeyID = 1 AND KeyName = 'Composite'");
		long dataRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixCommand WHERE KeyID = 1");
		
		assertEquals("Should have inserted key row", 1 ,keyRows);
		assertEquals("Should have inserted data row", 1 ,dataRows);
//System.out.println(JDBCHelper.dumpQuery(conn, "SELECT * FROM " + schema + ".P4JHystrixCommand"));
	}
	
	
	public void testWriteThreadPoolData() throws Exception {
		SQLWriter writer = new SQLWriter();
		
		HystrixThreadPoolData data = buildThreadPoolDataMock("TestKey");

		writer.writeToSQL(conn, schema, data, 1L);
		
		long keyRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixKey WHERE KeyID = 1 AND KeyName = 'TestKey'");
		long dataRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixThreadPool WHERE KeyID = 1");
		
		assertEquals("Should have inserted key row", 1 ,keyRows);
		assertEquals("Should have inserted data row", 1 ,dataRows);
//System.out.println(JDBCHelper.dumpQuery(conn, "SELECT * FROM " + schema + ".P4JHystrixThreadPool"));
	}
	
	private HystrixCommandData buildCommandDataMock(String instanceName) {
		HystrixCommandData mock = Mockito.mock(HystrixCommandData.class);
		Mockito.when(mock.getInstanceName()).thenReturn(instanceName);
		Mockito.when(mock.getSuccessCount()).thenReturn(new Delta(0, 0, 0));
		Mockito.when(mock.getFailureCount()).thenReturn(new Delta(0, 0, 0));
		Mockito.when(mock.getTimeoutCount()).thenReturn(new Delta(0, 0, 0));
		Mockito.when(mock.getShortCircuitedCount()).thenReturn(new Delta(0, 0, 0));
		Mockito.when(mock.getThreadPoolRejectedCount()).thenReturn(new Delta(0, 0, 0));
		Mockito.when(mock.getSemaphoreRejectedCount()).thenReturn(new Delta(0, 0, 0));
		
		return mock;
	}

	private HystrixThreadPoolData buildThreadPoolDataMock(String instanceName) {
		HystrixThreadPoolData mock = Mockito.mock(HystrixThreadPoolData.class);
		Mockito.when(mock.getInstanceName()).thenReturn(instanceName);
		
		Mockito.when(mock.getExecutedThreadCount()).thenReturn(new Delta(0, 0, 0));
		Mockito.when(mock.getRejectedThreadCount()).thenReturn(new Delta(0, 0, 0));
		Mockito.when(mock.getCompletedTaskCount()).thenReturn(new Delta(0, 0, 0));
		Mockito.when(mock.getScheduledTaskCount()).thenReturn(new Delta(0, 0, 0));
		
		return mock;
	}
	
}
