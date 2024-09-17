package org.perfmon4j.util.mbean;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Objects;
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
	
	public MBeanQueryResult doQuery(MBeanQuery query) throws MalformedObjectNameException {
		Set<ObjectInstance> mBeans = mBeanServer.queryMBeans(new ObjectName(query.getBaseJMXName()), null);
		Set<MBeanInstance> instances = new HashSet<MBeanInstance>();
		
		for (ObjectInstance o : mBeans) {
			instances.add(new MBeanInstanceImpl(mBeanServer, query.getBaseJMXName()));
		}
		
		return new MBeanQueryResultImpl(query, instances);
	}
	
	
	private static final class MBeanInstanceImpl implements MBeanInstance {
		private final WeakReference<MBeanServer> mBeanServer;
		private final String name;
		private final String instanceName;
		
		public MBeanInstanceImpl(MBeanServer mBeanServer, String name) {
			this(mBeanServer, name, null);
		}
		
		public MBeanInstanceImpl(MBeanServer mBeanServer, String name, String instanceName) {
			this.mBeanServer = new WeakReference<MBeanServer>(mBeanServer);
			this.name = name;
			this.instanceName = instanceName;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getInstanceName() {
			return instanceName;
		}

		@Override
		public <T> T getAttribute(String attributeName) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, instanceName);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MBeanInstanceImpl other = (MBeanInstanceImpl) obj;
			return Objects.equals(instanceName, other.instanceName) && Objects.equals(name, other.name);
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
