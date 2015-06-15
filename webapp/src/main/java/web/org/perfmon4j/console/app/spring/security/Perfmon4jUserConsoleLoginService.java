package web.org.perfmon4j.console.app.spring.security;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class Perfmon4jUserConsoleLoginService implements UserDetailsService {

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		
		if ("admin".equalsIgnoreCase(username)) {
			// password = 1234
			return new Perfmon4jUser("Administrator", username, "81dc9bdb52d04dc20036dbd8313ed055", "ROLE_ADMIN");
		} else {
			return null;
		}
	}
	
	public static final class Perfmon4jUser implements UserDetails {
		private static final long serialVersionUID = 1L;
		private final String displayName;
		private final String username;
		private final String hashedPassword;
		private final Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>(); 
		

		private Perfmon4jUser(String displayName, String username, String hashedPasswords, String... authorities) {
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
