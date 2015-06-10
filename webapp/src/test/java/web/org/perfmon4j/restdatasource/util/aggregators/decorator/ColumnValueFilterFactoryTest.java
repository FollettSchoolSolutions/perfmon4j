package web.org.perfmon4j.restdatasource.util.aggregators.decorator;

import java.sql.ResultSet;

import junit.framework.TestCase;

import org.mockito.Mockito;

import web.org.perfmon4j.restdatasource.util.aggregators.Aggregator;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.SumAggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.decorator.ColumnValueFilterFactory;

public class ColumnValueFilterFactoryTest extends TestCase {

	public ColumnValueFilterFactoryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFilter() throws Exception {
		ResultSet rs = Mockito.mock(ResultSet.class);
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(3));
		Mockito.when(rs.getString("Category")).thenReturn("WebRequest");
		
		// Create sum aggregator factory.
		AggregatorFactory delegate = new SumAggregatorFactory("myColumn", false);
		
		// Wrap it with a factory that will filter calls to the SumAggregator based on the result set containing a specific category.
		AggregatorFactory factory = new ColumnValueFilterFactory(delegate, "Category", new String[]{"WebRequest"});
		
		Aggregator ag = factory.newAggregator();
		
		ag.aggreagate(rs);
		
		
		Mockito.when(rs.getLong("myColumn")).thenReturn(Long.valueOf(5));
		Mockito.when(rs.getString("Category")).thenReturn("SomeOtherCategory");
		ag.aggreagate(rs);
		
		Number result = ag.getResult();
		assertTrue("Should be a Long value", result instanceof Long);
		assertEquals("should have filtered out the request for SomeOtherCategory", 3, result.intValue());
	}
	
}
