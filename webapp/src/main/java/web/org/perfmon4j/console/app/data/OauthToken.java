package web.org.perfmon4j.console.app.data;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.perfmon4j.util.MiscHelper;


@Entity
@Table(name = "OauthToken")
public class OauthToken {
	private Integer id;
	private String applicationName;
	private String key = MiscHelper.generateOauthKey();
	private String secret = MiscHelper.generateOauthSecret();
	
	@Basic
	@Column(nullable=false, unique=false)
	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	@Basic
	@Column(name="OauthKey", nullable=false, unique=true)
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Basic
	@Column(name="OauthSecret", nullable=false, unique=false)
	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
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
