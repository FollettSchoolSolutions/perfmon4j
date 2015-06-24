package web.org.perfmon4j.console.app.data;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class AppConfigService {
	private static final Logger logger = LoggerFactory.initLogger(AppConfigService.class);
	
	private EntityManager em = EMProvider.getEM(); 

	public void updateConfig(AppConfig config) {
		em.getTransaction().begin();
		try {
			em.persist(config);
		} finally {
			em.getTransaction().commit();
		}
	}
	
	public AppConfig getConfig() {
		Query q = em.createQuery("FROM AppConfig");
		
		@SuppressWarnings("unchecked")
		List<AppConfig> results = (List<AppConfig>)q.getResultList();
		if (results.isEmpty()) {
			return new AppConfig();
		} else {
			return results.get(0);
		}
	}
}
