package web.org.perfmon4j.console.app.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;


@Entity
@Table(name = "AppConfig")
public class AppConfig {
	private Integer id = null;
	private boolean accessEnabled = true;
	private boolean anonymousAccessEnabled = true;
	
	@Type(type = "numeric_boolean")
	@Column(nullable=false)
	public boolean isAccessEnabled() {
		return accessEnabled;
	}

	public void setAccessEnabled(boolean accessEnabled) {
		this.accessEnabled = accessEnabled;
	}
	
	@Type(type = "numeric_boolean")
	@Column(nullable=false)
	public boolean isAnonymousAccessEnabled() {
		return anonymousAccessEnabled;
	}

	public void setAnonymousAccessEnabled(boolean anonymousAccessEnabled) {
		this.anonymousAccessEnabled = anonymousAccessEnabled;
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(nullable=false, unique=true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}	
}
