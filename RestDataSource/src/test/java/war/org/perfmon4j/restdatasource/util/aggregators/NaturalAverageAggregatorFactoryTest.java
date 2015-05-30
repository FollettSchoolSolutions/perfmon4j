package war.org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;

import junit.framework.TestCase;

import org.mockito.Mockito;

import war.org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import war.org.perfmon4j.restdatasource.util.aggregators.NaturalAverageAggregatorFactory;

public class NaturalAverageAggregatorFactoryTest extends TestCase {


	public NaturalAverageAggregatorFactoryTest(String name) {
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
		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf("1"));
		
		Aggregator ag = new NaturalAverageAggregatorFactory("numerator", "denominator").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf("1.5"));
		ag.aggreagate(rs);
		
		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf("2.0"));
		ag.aggreagate(rs);

		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf("2.5"));
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 2, result.intValue());
	}

	public void testHandleVERYLargeNumerator() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		Mockito.when(rs.getLong("denominator")).thenReturn(Long.valueOf("1"));
		
		Aggregator ag = new NaturalAverageAggregatorFactory("numerator", "denominator").newAggregator();
		assertNull("We dont have any data should return null", ag.getResult());
		
		Mockito.when(rs.getDouble("numerator")).thenReturn(Double.valueOf(Long.MAX_VALUE));
		// We are going to create a numerator of (5 * Long.MAX_VALUE).  The average (divide by 5) should equal Long.MAX_VALUE;  
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", Long.MAX_VALUE, result.longValue());
	}
	
	
}
