package web.org.perfmon4j.console.app.data;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EMProvider {
	private static final EntityManager em;

	public static EntityManager getEM() {
		return em;
	}
	
	static {
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("default");
		em = factory.createEntityManager();
	}
}
