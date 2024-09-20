package org.perfmon4j.util.mbean;

import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.perfmon4j.util.MiscHelper;

public class MBeanQueryEngine {
	private final MBeanServer mBeanServer;
	
	public MBeanQueryEngine() {
		this((String)null);
	}
	
	public MBeanQueryEngine(String serverDomain) {
		this(MiscHelper.findMBeanServer(serverDomain));
	}
	
	public MBeanQueryEngine(MBeanServer mBeanServer) {
		this.mBeanServer = mBeanServer;
	}
	
	public MBeanQueryResult doQuery(MBeanQuery query) throws MBeanQueryException {
		try {
			String instanceName = query.getInstancePropertyKey();
			String fullMBeanQuery = query.getBaseJMXName();
			
			if (instanceName != null && !instanceName.isBlank()) {
				fullMBeanQuery += "," + instanceName.trim() + "=*";
			}
			
			Set<ObjectInstance> mBeans = mBeanServer.queryMBeans(new ObjectName(fullMBeanQuery), null);
			Set<MBeanInstance> instances = new HashSet<MBeanInstance>();
			
			for (ObjectInstance o : mBeans) {
				instances.add(new MBeanInstanceImpl(mBeanServer, o.getObjectName(), query));
			}
			
			return new MBeanQueryResultImpl(query, instances);
		} catch (MalformedObjectNameException e) {
			throw new MBeanQueryException(e);
		}
		
	}

	private static final class MBeanQueryResultImpl implements MBeanQueryResult {
		private final MBeanQuery query;
		private final MBeanInstance[] instances;
		
		public MBeanQueryResultImpl(MBeanQuery query, Set<MBeanInstance> instances) {
			this.query = query;
			this.instances = instances.toArray(new MBeanInstance[] {});
		}

		@Override
		public MBeanQuery getQuery() {
			return query;
		}

		@Override
		public MBeanInstance[] getInstances() {
			return instances;
		}
	}
	
	
//	private static final class MBeanMonitorResultImpl implements MBeanMonitorResult {
//		private final MBeanMonitor
//		
//	}
	
}
