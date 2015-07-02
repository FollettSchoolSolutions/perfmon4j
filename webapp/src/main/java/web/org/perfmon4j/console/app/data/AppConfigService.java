/*
 *	Copyright 2015 Follett School Solutions 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package web.org.perfmon4j.console.app.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import web.org.perfmon4j.restdatasource.DataSourceSecurityInterceptor;
import web.org.perfmon4j.restdatasource.oauth2.OauthTokenHelper;

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
		OauthTokenService tokenService = new OauthTokenService();
		AppConfig config = getConfig();
		
		DataSourceSecurityInterceptor.setSecuritySettings(new SecuritySettings(config, tokenService.getOauthTokens()));
	}
	
	private static class SecuritySettings implements DataSourceSecurityInterceptor.SecuritySettings {
		private final AppConfig config;
		private final Map<String, OauthTokenHelper> oauthHelpers = Collections.synchronizedMap(new HashMap<String, OauthTokenHelper>());
		
		SecuritySettings(AppConfig config, List<OauthToken> tokens) {
			this.config = config;
			
			for (OauthToken t : tokens) {
				oauthHelpers.put(t.getKey(), new OauthTokenHelper(t.getKey(), t.getSecret()));
			}
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
		public OauthTokenHelper getTokenHelper(String oauthKey) {
			return oauthHelpers.get(oauthKey);
		}
	}
	
	
}
