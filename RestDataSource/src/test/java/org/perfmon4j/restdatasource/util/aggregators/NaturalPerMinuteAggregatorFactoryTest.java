package org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;
import java.sql.Timestamp;

import junit.framework.TestCase;

import org.mockito.Mockito;

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
	
	public void testSimple() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		long now = System.currentTimeMillis();
		long minuteAndHalfAgo = now - 90000;
		
		Mockito.when(rs.getTimestamp("startTime")).thenReturn(new Timestamp(minuteAndHalfAgo));
		Mockito.when(rs.getTimestamp("endTime")).thenReturn(new Timestamp(now));
		
		Aggregator ag = new NaturalPerMinuteAggregatorFactory("startTime", "endTime", "counter").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(800));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(900));
		ag.aggreagate(rs);

		Mockito.when(rs.getLong("counter")).thenReturn(Long.valueOf(1000));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 600, result.longValue());
	}
}
