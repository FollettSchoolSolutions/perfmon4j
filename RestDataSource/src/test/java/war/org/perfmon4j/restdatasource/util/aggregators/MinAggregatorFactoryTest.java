package war.org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;

import junit.framework.TestCase;

import org.mockito.Mockito;

import war.org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import war.org.perfmon4j.restdatasource.util.aggregators.MinAggregatorFactory;

public class MinAggregatorFactoryTest extends TestCase {


	public MinAggregatorFactoryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testFloatingPoint() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		
		Aggregator ag = new MinAggregatorFactory("myColumn", true).newAggregator();
		
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getDouble("myColumn")).thenReturn(Double.valueOf(-1.5));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getDouble("myColumn")).thenReturn(Double.valueOf(-3.5));
		ag.aggreagate(rs);

		Mockito.when(rs.getDouble("myColumn")).thenReturn(Double.valueOf(-2.5));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("min", -3.5, result.doubleValue());
	}

	public void testFixedPoint() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		
		Aggregator ag = new MinAggregatorFactory("myColumn", false).newAggregator();
		
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(-2));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(-4));
		ag.aggreagate(rs);

		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(-3));
		ag.aggreagate(rs);
		
		Number result = ag.getResult();
		assertTrue("Should be a Long value", result instanceof Long);
		assertEquals("min", -4, result.intValue());
	}
}
