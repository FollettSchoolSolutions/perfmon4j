package org.perfmon4j.util.mbean;

import java.util.HashSet;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.mbean.MBeanQueryBuilder.NamedRegExFilter;
import org.perfmon4j.util.mbean.MBeanQueryBuilder.RegExFilter;

public class MBeanQueryEngine {
	private static final Logger logger = LoggerFactory.initLogger(MBeanQueryEngine.class);
	private final MBeanServerFinder mBeanServerFinder;
	
	
	public MBeanQueryEngine(MBeanServerFinder mBeanServerFinder) {
		this.mBeanServerFinder = mBeanServerFinder;
	}
	
	public MBeanQueryResult doQuery(MBeanQuery query) throws MBeanQueryException {
		try {
			String instanceKey = query.getInstanceKey();
			String fullMBeanQuery = query.getBaseJMXName();
			
			if (!MiscHelper.isBlankOrNull(instanceKey)) {
				fullMBeanQuery += "," + instanceKey.trim() + "=*";
			}
			
			Set<ObjectInstance> mBeans = mBeanServerFinder.getMBeanServer().queryMBeans(new ObjectName(fullMBeanQuery), null);
			Set<MBeanInstance> instances = new HashSet<MBeanInstance>();
			RegExFilter filter = query.getInstanceValueFilter();
			if (filter != null && instanceKey == null) {
				logger.logWarn("instanceKey was not specified on query. Ignoring instanceValueFilter: " + filter);
				filter = null;
			}
			
			NamedRegExFilter attributeFilter = query.getAttributeValueFilter();
			
			for (ObjectInstance o : mBeans) {
				boolean addInstance = true;
				
				if (filter != null && !filter.matches(o.getObjectName().getKeyProperty(instanceKey))) {
					addInstance = false;
				}
				
				if (addInstance && attributeFilter != null) {
					addInstance = evaluateAttributeFilter(mBeanServerFinder, o.getObjectName(), attributeFilter);
				}
				
				if (addInstance) {
					instances.add(new MBeanInstanceImpl(mBeanServerFinder, o.getObjectName(), query));
				}
			}
			return new MBeanQueryResultImpl(query, instances);
		} catch (MalformedObjectNameException e) {
			throw new MBeanQueryException(e);
		}
		
	}
	
	private boolean evaluateAttributeFilter(MBeanServerFinder serverFinder, ObjectName objectName, 
			NamedRegExFilter attributeFilter) throws MBeanQueryException {
		Object attributeValue = null;
		String attributeName = attributeFilter.getName();
		
		try {
			try {
				attributeValue = serverFinder.getMBeanServer().getAttribute(objectName, attributeName);
			} catch (AttributeNotFoundException a) {
				attributeValue = serverFinder.getMBeanServer().getAttribute(objectName, MiscHelper.toggleCaseOfFirstLetter(attributeName));
			}
			return attributeFilter.matches(String.valueOf(attributeValue));
		} catch (InstanceNotFoundException | AttributeNotFoundException | ReflectionException | MBeanException
				| MBeanQueryException e) {
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
