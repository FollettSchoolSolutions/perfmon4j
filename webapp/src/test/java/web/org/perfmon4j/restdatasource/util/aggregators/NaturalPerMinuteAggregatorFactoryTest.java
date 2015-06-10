package web.org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;
import java.sql.Timestamp;

import junit.framework.TestCase;

import org.mockito.Mockito;

import web.org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import web.org.perfmon4j.restdatasource.util.aggregators.NaturalPerMinuteAggregatorFactory;

public class NaturalPerMinuteAggregatorFactoryTest extends TestCase {


	public NaturalPerMinuteAggregatorFactoryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testSingleSystem() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		long now = System.currentTimeMillis();
		long minuteAgo = now - 60000;
		long twoMinutesAgo = minuteAgo - 60000;
		long threeMinutesAgo = twoMinutesAgo - 60000;
		
		Mockito.when(rs.getString("systemID")).thenReturn("1");
		
		Aggregator ag = new NaturalPerMinuteAggregatorFactory("startTime", "endTime", "counter").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(threeMinutesAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(twoMinutesAgo));
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(800));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(twoMinutesAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(minuteAgo));
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(900));
		ag.aggreagate(rs);

		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(minuteAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(now));
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(1000));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 900, result.longValue());
	}


	
	/**
	 * For multiple systems the throughput per minute for each system
	 * should be summed together.  
	 * For example if you have 4 systems and each processed 10 requests per/minute should
	 * return 40 request per minute.	
	 * 
	 * @throws Exception
	 */
	public void testMultipleSystem() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		long now = System.currentTimeMillis();
		long minuteAgo = now - 60000;
		
		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(minuteAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(now));
		
		Aggregator ag = new NaturalPerMinuteAggregatorFactory("startTime", "endTime", "counter").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		// System1
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(800));
		ag.aggreagate(rs);
		
		// System2
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(900));
		ag.aggreagate(rs);

		// System3
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(1000));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 2700, result.longValue());
	}


}
