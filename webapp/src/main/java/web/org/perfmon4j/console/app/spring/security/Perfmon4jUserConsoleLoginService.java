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

package web.org.perfmon4j.console.app.spring.security;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import web.org.perfmon4j.console.app.data.User;
import web.org.perfmon4j.console.app.data.UserService;



@Service
public class Perfmon4jUserConsoleLoginService implements UserDetailsService {
	
	private final UserService userService = new UserService();
	private @Autowired HttpServletRequest request;
	
	private static final String LOCAL_IP = "127.0.0.1";
	private static final String LOCAL_IPV6 = "0:0:0:0:0:0:0:1";
	
	
	@Override
	public UserDetails loadUserByUsername(String userName)
			throws UsernameNotFoundException {

		// In case all users were deleted... We will reinstall the default local host admin.
		userService.initializeUsers();
		
		User user = userService.findByUserName(userName);
		if (user != null && UserService.ADMIN_USER_NAME.equals(user.getUserName())  
				&& UserService.DEFAULT_ADMIN_PASSWORD_MD5.equals(user.getHashedPassword())) {
			// Trying to login with the default admin user, and the password has not been 
			// changed.  Only allow this login if the request is coming from local host.
			if (!isClientOnLocalhost()) {
				user = null;
			}
		}
		
		if (user != null) {
			return new Perfmon4jUser(user.getId(), user.getDisplayName(), user.getUserName(), user.getHashedPassword(), "ROLE_ADMIN");
		} else {
			throw new UsernameNotFoundException("User not found");
		}
	}

	private boolean isClientOnLocalhost() {
		String clientAddr = request.getRemoteAddr();
		return LOCAL_IP.equals(clientAddr) || LOCAL_IPV6.equals(clientAddr);
		
	}
	
	public static String generateMD5Hash(String str) {
		try {
			MessageDigest d = MessageDigest.getInstance("MD5");
			d.update(str.getBytes("UTF-8"));
			return Hex.encodeHexString(d.digest());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 not supported");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unable to create MD5 hash", e);
		}
	}
	
	public static final class Perfmon4jUser implements UserDetails {
		private static final long serialVersionUID = 1L;
		private final String displayName;
		private final String username;
		private final String hashedPassword;
		private final Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
		private final Integer userID;
		

		private Perfmon4jUser(Integer userID, String displayName, String username, String hashedPasswords, String... authorities) {
			this.userID = userID;
			this.displayName = displayName;
			this.username = username;
			this.hashedPassword = hashedPasswords;
			for (String a : authorities) {
				this.authorities.add(new Perfmon4jGrantedAuthority(a));
			}
		}

		@Override
		public Collection<? extends GrantedAuthority> getAuthorities() {
			return new HashSet<GrantedAuthority>(authorities);
		}

		@Override
		public String getPassword() {
			return hashedPassword;
		}

		@Override
		public String getUsername() {
			return username;
		}

		@Override
		public boolean isAccountNonExpired() {
			return true;
		}

		@Override
		public boolean isAccountNonLocked() {
			return true;
		}

		@Override
		public boolean isCredentialsNonExpired() {
			return true;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		public String getDisplayName() {
			return displayName;
		}

		public Integer getUserID() {
			return userID;
		}
	}
	
	private static final class Perfmon4jGrantedAuthority implements GrantedAuthority {
		private static final long serialVersionUID = 1L;
		private final String authority;
		
		public Perfmon4jGrantedAuthority(String authority) {
			this.authority = authority;
		}

		@Override
		public String getAuthority() {
			return authority;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((authority == null) ? 0 : authority.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Perfmon4jGrantedAuthority other = (Perfmon4jGrantedAuthority) obj;
			if (authority == null) {
				if (other.authority != null)
					return false;
			} else if (!authority.equals(other.authority))
				return false;
			return true;
		}
	}
}
