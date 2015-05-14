package org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;

import junit.framework.TestCase;

import org.mockito.Mockito;

public class NaturalStdDevAverageAggregatorFactoryTest extends TestCase {


	public NaturalStdDevAverageAggregatorFactoryTest(String name) {
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
		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf(3));
		
		Aggregator ag = new NaturalStdDevAggregatorFactory("numerator", "sumOfSquares", "denominator").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf(15));
		Mockito.when(rs.getDouble("sumOfSquares")).thenReturn(Double.valueOf(75));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf(18));
		Mockito.when(rs.getDouble("sumOfSquares")).thenReturn(Double.valueOf(108));
		ag.aggreagate(rs);

		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf(21));
		Mockito.when(rs.getDouble("sumOfSquares")).thenReturn(Double.valueOf(147));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 0.866, result.doubleValue());
	}

//	public void testHandleVERYLargeNumerator() throws Exception {
//		ResultSet rs = Mockito.mock(ResultSet.class);
//		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf("1"));
//		
//		Aggregator ag = new NaturalAverageAggregatorFactory("numerator", "denominator").newAggregator();
//		assertNull("We dont have any data should return null", ag.getResult());
//		
//		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf(Long.MAX_VALUE));
//		// We are going to create a numerator of (5 * Long.MAX_VALUE).  The average (divide by 5) should equal Long.MAX_VALUE;  
//		ag.aggreagate(rs);
//		ag.aggreagate(rs);
//		ag.aggreagate(rs);
//		ag.aggreagate(rs);
//		ag.aggreagate(rs);
//		
//		
//		Number result = ag.getResult();
//		assertTrue("Should be a double value", result instanceof Double);
//		assertEquals("sum", Long.MAX_VALUE, result.longValue());
//	}
	
	
}
