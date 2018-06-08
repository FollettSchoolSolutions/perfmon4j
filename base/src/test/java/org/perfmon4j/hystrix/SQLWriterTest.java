package org.perfmon4j.hystrix;

import java.sql.Connection;

import org.mockito.Mockito;
import org.perfmon4j.SQLTest;
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
		
		HystrixCommandData data = Mockito.mock(HystrixCommandData.class);
		Mockito.when(data.getInstanceName()).thenReturn("TestKey");

		writer.writeToSQL(conn, schema, data, 1L);
		
		long keyRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixKey WHERE KeyID = 1 AND KeyName = 'TestKey'");
		long dataRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixCommand WHERE KeyID = 1");
		
		assertEquals("Should have inserted key row", 1 ,keyRows);
		assertEquals("Should have inserted data row", 1 ,dataRows);
//System.out.println(JDBCHelper.dumpQuery(conn, "SELECT * FROM " + schema + ".P4JHystrixCommand"));
	}
	
	public void testWriteThreadPoolData() throws Exception {
		SQLWriter writer = new SQLWriter();
		
		HystrixThreadPoolData data = Mockito.mock(HystrixThreadPoolData.class);
		Mockito.when(data.getInstanceName()).thenReturn("TestKey");

		writer.writeToSQL(conn, schema, data, 1L);
		
		long keyRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixKey WHERE KeyID = 1 AND KeyName = 'TestKey'");
		long dataRows = JDBCHelper.getQueryCount(conn, "SELECT * FROM " + schema + ".P4JHystrixThreadPool WHERE KeyID = 1");
		
		assertEquals("Should have inserted key row", 1 ,keyRows);
		assertEquals("Should have inserted data row", 1 ,dataRows);
//System.out.println(JDBCHelper.dumpQuery(conn, "SELECT * FROM " + schema + ".P4JHystrixThreadPool"));
	}
}
