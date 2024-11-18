package org.perfmon4j.util.mbean;

import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public abstract class QueryToInstanceSnapShot {
	private static final Logger logger = LoggerFactory.initLogger(QueryToInstanceSnapShot.class);
	protected final MBeanQuery query;
	protected final MBeanInstanceToSnapShotRegistrar registrar;
	protected final MBeanQueryRunner queryRunner;
	

	public interface MBeanQueryRunner {
		public MBeanQueryResult doQuery(MBeanQuery query) throws MBeanQueryException;
	}
	
	public interface MBeanInstanceToSnapShotRegistrar {
		public void registerSnapShot(MBeanInstance instance);
		public void deRegisterSnapShot(MBeanInstance instane);
	}	

	protected QueryToInstanceSnapShot(MBeanQuery query, MBeanInstanceToSnapShotRegistrar registrar, MBeanQueryRunner queryRunner) {
		this.query = query;
		this.registrar = registrar;
		this.queryRunner = queryRunner;
	}
	
	public abstract void refresh();
	public abstract void deInit();
	
	
	protected MBeanQueryResult doQuery() throws MBeanQueryException {
		return queryRunner.doQuery(query);
	}

	/* package */ static QueryToInstanceSnapShot newQueryToInstanceSnapShot(MBeanQuery query, MBeanInstanceToSnapShotRegistrar registrar) {
		return newQueryToInstanceSnapShot(query, registrar, new MBeanQueryRunnerImpl());
	}
	/* package */ static QueryToInstanceSnapShot newQueryToInstanceSnapShot(MBeanQuery query, MBeanInstanceToSnapShotRegistrar registrar, MBeanQueryRunner queryRunner) {
		if (query.getInstanceKey() == null) {
			return new SingleQueryToInstance(query, registrar, queryRunner);
		} else {
			return new MultiQueryToInstance(query, registrar, queryRunner); 
		}
	}
	
	/* package */ static class SingleQueryToInstance extends QueryToInstanceSnapShot {
		private MBeanInstance instance = null;

		private SingleQueryToInstance(MBeanQuery query, MBeanInstanceToSnapShotRegistrar registrar, MBeanQueryRunner queryRunner) {
			super(query, registrar, queryRunner);
		}
		
		public synchronized void refresh() {
			if (instance == null) {
				try {
					MBeanQueryResult result = doQuery();
					if (result.getInstances().length > 0) {
						instance = result.getInstances()[0];
						registrar.registerSnapShot(instance);
					}
				} catch (MBeanQueryException e) {
					logger.logDebug("Error execting query: " + query, e);
				}
			}
		}
		
		public synchronized void deInit() {
			if (instance != null) {
				registrar.deRegisterSnapShot(instance);
				instance = null;
			}
		}
	}
	
	/* package */ static class MultiQueryToInstance extends QueryToInstanceSnapShot {
		private final Map<String, MBeanInstance> instances = new HashMap<String, MBeanInstance>();

		private MultiQueryToInstance(MBeanQuery query, MBeanInstanceToSnapShotRegistrar registrar, MBeanQueryRunner queryRunner) {
			super(query, registrar, queryRunner);
		}
		
		public synchronized void refresh() {
			try {
				MBeanQueryResult result = doQuery();
				for (MBeanInstance instance : result.getInstances()) {
					String instanceName = instance.getInstanceName();
					if (!instances.containsKey(instanceName)) {
						instances.put(instanceName, instance);
						registrar.registerSnapShot(instance);
					}
				}
			} catch (MBeanQueryException e) {
				logger.logDebug("Error execting query: " + query, e);
			}
		}
		
		public synchronized void deInit() {
			for (MBeanInstance instance : instances.values()) {
				registrar.deRegisterSnapShot(instance);
			}
			instances.clear();
		}
	}
	
	private static class MBeanQueryRunnerImpl implements MBeanQueryRunner {
		@Override
		public MBeanQueryResult doQuery(MBeanQuery query) throws MBeanQueryException {
			MBeanQueryEngine queryEngine = new MBeanQueryEngine(new MBeanServerFinderImpl(query.getDomain()));
			return queryEngine.doQuery(query);
		}
	}
}
