package web.org.perfmon4j.console.app.data;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class OauthTokenService {
	private static final Logger logger = LoggerFactory.initLogger(OauthTokenService.class);
	
	private EntityManager em = EMProvider.getEM(); 

	public void update(OauthToken token) {
		em.getTransaction().begin();
		try {
			em.persist(token);
		} finally {
			em.getTransaction().commit();
		}
		new AppConfigService().refreshDataSourceSecurity();
	}

	public void delete(OauthToken token) {
		em.getTransaction().begin();
		try {
			em.remove(token);
		} finally {
			em.getTransaction().commit();
		}
		new AppConfigService().refreshDataSourceSecurity();
	}
	
	
	public List<OauthToken> getOauthTokens() {
		Query q = em.createQuery("FROM OauthToken o order by o.applicationName");
		
		@SuppressWarnings("unchecked")
		List<OauthToken> results = (List<OauthToken>)q.getResultList();
		return results;
	}
}
