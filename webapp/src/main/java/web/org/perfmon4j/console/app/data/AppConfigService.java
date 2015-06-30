package web.org.perfmon4j.console.app.data;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import web.org.perfmon4j.restdatasource.DataSourceSecurityInterceptor;

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
		refreshDataSourceSecurity();
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
	
	public void refreshDataSourceSecurity() {
		AppConfig config = getConfig();
		DataSourceSecurityInterceptor.setSecuritySettings(new SecuritySettings(config));
	}
	
	private static class SecuritySettings implements DataSourceSecurityInterceptor.SecuritySettings {
		private final AppConfig config;
		
		SecuritySettings(AppConfig config) {
			this.config = config;
		}
		
		@Override
		public boolean isEnabled() {
			return config.isAccessEnabled();
		}

		@Override
		public boolean isAnonymousAllowed() {
			return config.isAnonymousAccessEnabled();
		}

		@Override
		public String getSecret(String oauthKey) {
			return null;
		}
	}
	
	
}
