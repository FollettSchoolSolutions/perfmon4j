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
