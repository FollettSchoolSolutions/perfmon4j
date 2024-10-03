package org.perfmon4j.util.mbean;

import java.util.Set;

import org.mockito.Mockito;
import org.perfmon4j.util.mbean.GaugeCounterArgumentParser.AttributeSpec;

import junit.framework.TestCase;

public class GaugeCounterArgumentParserTest extends TestCase {
	public void testOverrideDisplayName() {
		MBeanQuery query = Mockito.mock(MBeanQuery.class);
		Mockito.when(query.getCounters()).thenReturn(new String[] {"threadCount(displayName='threadsStarted')"});
		Mockito.when(query.getGauges()).thenReturn(new String[] {"activeThreads(displayName=\"currentActiveThreads\")"});

		
		GaugeCounterArgumentParser parser = new GaugeCounterArgumentParser(query);
		Set<AttributeSpec> counters = parser.getCounters();
		assertEquals("expected number of counters", 1, counters.size());
		AttributeSpec counter =	counters.toArray(new AttributeSpec[]{})[0];
		assertEquals("expected name", "threadCount", counter.getName());
		assertEquals("expected display name", "threadsStarted", counter.getPreferredDisplayName());
		
		Set<AttributeSpec> gauges = parser.getGauges(); 
		assertEquals("expected number of gauges", 1, gauges.size());
		AttributeSpec gauge = gauges.toArray(new AttributeSpec[]{})[0];
		assertEquals("expected name", "activeThreads", gauge.getName());
		assertEquals("expected display name", "currentActiveThreads", gauge.getPreferredDisplayName());
	}

	public void testAllowNonWordCharactersInDisplayName() {
		MBeanQuery query = Mockito.mock(MBeanQuery.class);
		Mockito.when(query.getCounters()).thenReturn(new String[] {"threadCount(displayName='#threads@Started(this is a test!)')"});
		Mockito.when(query.getGauges()).thenReturn(new String[] {});
		
		GaugeCounterArgumentParser parser = new GaugeCounterArgumentParser(query);
		Set<AttributeSpec> counters = parser.getCounters();
		assertEquals("expected number of counters", 1, counters.size());
		AttributeSpec counter =	counters.toArray(new AttributeSpec[]{})[0];
		assertEquals("expected name", "threadCount", counter.getName());
		assertEquals("expected display name", "#threads@Started(this is a test!)", counter.getPreferredDisplayName());
	}

}
