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

import web.org.perfmon4j.console.app.spring.security.Perfmon4jUserConsoleLoginService;

public class UserService {
	private static final Logger logger = LoggerFactory.initLogger(UserService.class);
	
	private EntityManager em = EMProvider.getEM(); 

	public static final String ADMIN_USER_NAME = "admin";
	public static final String ADMIN_DISPLAY_NAME  = "Administrator";
	public static final String ADMIN_LOCALHOST_DISPLAY_NAME  = ADMIN_DISPLAY_NAME + " (Localhost)";
	public static final String DEFAULT_ADMIN_PASSWORD_MD5 = Perfmon4jUserConsoleLoginService.generateMD5Hash("LocalhostOnly");
	
	public void initializeUsers() {
		Long numUsers =  ((Long)em.createQuery("SELECT COUNT(*) FROM User").getSingleResult()).longValue();
		if (numUsers < 1) {
			logger.logDebug("No users found.  Initializing with the localhost administrator");
			em.getTransaction().begin();
			try {
				// Add our default admin user...
				User u = new User();
				u.setDisplayName(ADMIN_LOCALHOST_DISPLAY_NAME);
				u.setUserName("admin");
				u.setHashedPassword(DEFAULT_ADMIN_PASSWORD_MD5);
		
				em.persist(u);
			} finally {
				em.getTransaction().commit();
			}
		}
	}
	
	public User findByUserName(String userName) {
		Query q = em.createQuery("FROM User WHERE userName = :userName");
		q.setParameter("userName", userName);
		
		@SuppressWarnings("unchecked")
		List<User> results = (List<User>)q.getResultList();
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}
}
