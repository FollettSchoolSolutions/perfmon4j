package war.org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;

import junit.framework.TestCase;

import org.mockito.Mockito;

import war.org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import war.org.perfmon4j.restdatasource.util.aggregators.AverageAggregatorFactory;

public class AverageAggregatorFactoryTest extends TestCase {


	public AverageAggregatorFactoryTest(String name) {
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
		Mockito.when(rs.getDouble("myColumn")).thenReturn(Double.valueOf("1.5"));
		
		Aggregator ag = new AverageAggregatorFactory("myColumn", true).newAggregator();
		
		assertNull("We dont have any data should return null", ag.getResult());
		
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		
		Number result = ag.getResult();
		assertTrue("Should be a double value", result instanceof Double);
		assertEquals("sum", 1.5, result.doubleValue());
	}

	public void testFixedPoint() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf("2"));
		
		Aggregator ag = new AverageAggregatorFactory("myColumn", false).newAggregator();
		
		assertNull("We dont have any data should return null", ag.getResult());
		
		ag.aggreagate(rs);
		ag.aggreagate(rs);
		
		Number result = ag.getResult();
		assertTrue("Should be a Long value", result instanceof Double);
		assertEquals("sum", 2.0, result.doubleValue());
	}
}
