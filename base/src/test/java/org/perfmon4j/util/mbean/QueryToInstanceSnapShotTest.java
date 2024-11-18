package org.perfmon4j.util.mbean;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import org.perfmon4j.util.mbean.QueryToInstanceSnapShot.MBeanInstanceToSnapShotRegistrar;
import org.perfmon4j.util.mbean.QueryToInstanceSnapShot.MBeanQueryRunner;

import junit.framework.TestCase;

public class QueryToInstanceSnapShotTest extends TestCase {
	private MockRegistrar mockRegistrar = new MockRegistrar();
	private MockQueryRunner mockQueryRunner = new MockQueryRunner();
	
	public void setUp() throws Exception {
		super.setUp();

		mockRegistrar.reset();
		mockQueryRunner.reset();
	}
	
	private static class MockRegistrar implements MBeanInstanceToSnapShotRegistrar {
		private final List<MBeanInstance> registrations = new ArrayList<MBeanInstance>();
		private final List<MBeanInstance> deRegistrations = new ArrayList<MBeanInstance>();
		
		@Override
		public void registerSnapShot(MBeanInstance instance) {
			registrations.add(instance);
		}

		@Override
		public void deRegisterSnapShot(MBeanInstance instance) {
			deRegistrations.add(instance);
		}
		
		MBeanInstance[] popRegistrations() {
			MBeanInstance[] result = registrations.toArray(new MBeanInstance[]{});
			registrations.clear();
			return result;
		}
		
		MBeanInstance[] popDeRegistrations() {
			MBeanInstance[] result = deRegistrations.toArray(new MBeanInstance[]{});
			deRegistrations.clear();
			return result;
		}
		
		void reset() {
			popRegistrations();
			popDeRegistrations();
		}
	}
	
	private static class MockQueryRunner implements MBeanQueryRunner {
		private final List<MBeanQuery> queries = new ArrayList<MBeanQuery>();
		private final MBeanQueryResult result;
		
		MockQueryRunner() {
			result = Mockito.mock(MBeanQueryResult.class);
			when(result.getInstances()).thenReturn(new MBeanInstance[] {});
		}
		
		@Override
		public MBeanQueryResult doQuery(MBeanQuery query) throws MBeanQueryException {
			queries.add(query);
			return result;
		}
		
		void setResults(MBeanInstance ...instances) {
			when(result.getInstances()).thenReturn(instances);
		}
		
		MBeanQuery[] popQueries() {
			MBeanQuery[] result = queries.toArray(new MBeanQuery[] {});
			queries.clear();
			
			return result;
		}
		
		void reset() {
			setResults(new MBeanInstance[] {});
			popQueries();
		}
		
	}
	
	private MBeanQuery mockQuery(boolean singleInstance) throws Exception {
		MBeanQuery mock = Mockito.mock(MBeanQuery.class);
		
		if (singleInstance) {
			// A MBeanQuery without an instanceKey means that 0 or 1 MBeanInstances will be returned.
			when(mock.getInstanceKey()).thenReturn(null);
		} else {
			// A MBeanQuery with an instanceKey means that 0 to n MBeanInstances will be returned.
			// For the purposes of the mock the specific value of the instanceKey is irrelevant, it just must be non-null.
			when(mock.getInstanceKey()).thenReturn("name");
		}
		
		return mock;
	}
	
	public void testNewQueryInstance() throws Exception {
		MBeanQuery query = mockQuery(true);

		QueryToInstanceSnapShot snapShot = QueryToInstanceSnapShot.newQueryToInstanceSnapShot(query, mockRegistrar); 
		assertTrue("Expected a 'SingleQueryToInstance' object", snapShot instanceof QueryToInstanceSnapShot.SingleQueryToInstance);
		
		when(query.getInstanceKey()).thenReturn("name");
		query = mockQuery(false);
		
		snapShot = QueryToInstanceSnapShot.newQueryToInstanceSnapShot(query, mockRegistrar); 
		assertTrue("Expected a 'MultiQueryToInstance' object", snapShot instanceof QueryToInstanceSnapShot.MultiQueryToInstance);
	}

	
	public void testSingleInstanceLifecycle() throws Exception {
		MBeanQuery mockQuery = mockQuery(true);
		QueryToInstanceSnapShot snapShot = QueryToInstanceSnapShot.newQueryToInstanceSnapShot(mockQuery, mockRegistrar, mockQueryRunner);		
		
		// First we will simulate that the target MBean has not been Registered with the MBeanServer
		snapShot.refresh();

		// Should not have called to register a MBeanSnapShot
		// Should have run the query
		MBeanQuery[] queries = mockQueryRunner.popQueries();
		assertTrue("Should have run the query", queries.length == 1 && queries[0].equals(mockQuery));
		assertEquals("Should NOT have registered any MBeans", 0, mockRegistrar.popRegistrations().length);
		
		// Now simulate registering an MBeanInstance with the MBeanServer so it will be returned by the query.
		MBeanInstance mBean = Mockito.mock(MBeanInstance.class); 
		mockQueryRunner.setResults(mBean);

		// Now call refresh() and we should find and register the MBean with the snapShotRegistry
		snapShot.refresh();
		queries = mockQueryRunner.popQueries();
		assertTrue("Should have re-run the query", queries.length == 1 && queries[0].equals(mockQuery));
		assertEquals("Should have registered the MBean", 1, mockRegistrar.popRegistrations().length);
		
		// Now call refresh again, since we have already loaded the single instance
		// We no longer need to re-run the query.
		snapShot.refresh();
		queries = mockQueryRunner.popQueries();
		assertEquals("Should NOT have re-run the query", 0, queries.length);
		
		// Now deInitialize the snapShot.  This should call deregister the MBeanInstance from 
		// the list of SnapShot monitors. 
		snapShot.deInit();
		assertEquals("Should have deRegistered snapShot monitor", 1, mockRegistrar.popDeRegistrations().length);

		// Now if we call reset we are starting fresh, we will need to query the MBean
		// and when we find it, re-register it.
		snapShot.refresh();
		queries = mockQueryRunner.popQueries();
		assertTrue("Should have re-run the query", queries.length == 1 && queries[0].equals(mockQuery));
		assertEquals("Should have registered the MBean", 1, mockRegistrar.popRegistrations().length);
	}

	/**
	 * MultiInstance Lifecycle apply to MBeans that are being monitored through a wild card -
	 * For instance all of the GarbageCollection instances in a JVM
	 * 
	 * @throws Exception
	 */
	public void testMultiInstanceLifeCyle() throws Exception {
		MBeanQuery mockQuery = mockQuery(false);
		QueryToInstanceSnapShot snapShot = QueryToInstanceSnapShot.newQueryToInstanceSnapShot(mockQuery, mockRegistrar, mockQueryRunner);		
		
		// First we will simulate that the no match MBeans have not been Registered with the MBeanServer
		snapShot.refresh();

		// Should not have called to register a MBeanSnapShot
		// Should have run the query
		MBeanQuery[] queries = mockQueryRunner.popQueries();
		assertTrue("Should have run the query", queries.length == 1 && queries[0].equals(mockQuery));
		assertEquals("Should NOT have registered any MBeans", 0, mockRegistrar.popRegistrations().length);
				
		// Now register one Mock Garbage Collector.  This should be found and registered
		// on a call to refresh.
		MBeanInstance mBeanGC1 = Mockito.mock(MBeanInstance.class);
		Mockito.when(mBeanGC1.getInstanceName()).thenReturn("GC1");
		mockQueryRunner.setResults(mBeanGC1);
		
		snapShot.refresh();
		queries = mockQueryRunner.popQueries();
		assertTrue("Should have re-run the query", queries.length == 1 && queries[0].equals(mockQuery));
		assertEquals("Should have registered the MBean", mBeanGC1, mockRegistrar.popRegistrations()[0]);
		
		// Now we are going to refresh again. We will re-run the MBeanQuery because
		// we need to be sure additional garbage collectors were not loaded.  However,
		// any that have already been registered should not be reregistered.
		snapShot.refresh();
		queries = mockQueryRunner.popQueries();
		assertTrue("Should have re-run the query", queries.length == 1 && queries[0].equals(mockQuery));
		assertEquals("Should NOT have re-registered the MBean", 0, mockRegistrar.popRegistrations().length);
		
		// Now we will add another "garbage collector" and refresh.  We should find and register
		// the new, and only the new garbage collector
		MBeanInstance mBeanGC2 = Mockito.mock(MBeanInstance.class); 
		Mockito.when(mBeanGC2.getInstanceName()).thenReturn("GC2");
		mockQueryRunner.setResults(mBeanGC1, mBeanGC2);
		
		snapShot.refresh();
		queries = mockQueryRunner.popQueries();
		assertTrue("Should have re-run the query", queries.length == 1 && queries[0].equals(mockQuery));
		MBeanInstance[] registrations = mockRegistrar.popRegistrations();
		
		assertEquals("Should have registered the new GC MBean", mBeanGC2, registrations[0]);
		assertEquals("Should have ONLY registered the new GC MBean", 1, registrations.length);
		
		// Now deInitialize the snapShot.  This should call deregister both MBeanInstances from 
		// the list of SnapShot monitors. 
		snapShot.deInit();
		assertEquals("Should have deRegistered both snapShot monitors", 2, mockRegistrar.popDeRegistrations().length);

		// Now if we call refresh we are starting over
		snapShot.refresh();
		queries = mockQueryRunner.popQueries();
		assertTrue("Should have re-run the query", queries.length == 1 && queries[0].equals(mockQuery));
		assertEquals("Should have registered both MBeans", 2, mockRegistrar.popRegistrations().length);
	}
}
