package web.org.perfmon4j.console.app;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Window;

import web.org.perfmon4j.console.app.data.EMProvider;
import web.org.perfmon4j.console.app.data.User;


public class UsersController  extends SelectorComposer<Component> {
	   private static final long serialVersionUID = 1L;
	     
	   private EntityManager em = EMProvider.getEM();
	   
		@Wire
	    Grid usersGrid;

	    @Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		Query q = em.createQuery("select u from User u");
		Collection<User> users = (Collection<User>)q.getResultList();

		for (User u : users) {
			Row row = new Row();
			row.appendChild(new Label("" + u.getId()));
			row.appendChild(new Label(u.getDisplayName()));
			usersGrid.getRows().appendChild(row);
		}
	}
	    
	@Listen("onClick = #createUserButton")
	public void about() {
		  Window window = (Window)Executions.createComponents(
				  "/app/aedUser.zul", null, null);
		  window.doModal();
		
//		em.getTransaction().begin();
//		User user = new User();
//		user.setDisplayName("Dave");
//		user.setUserName("ddeucher");
//		user.setHashedPassword("81dc9bdb52d04dc20036dbd8313ed055");
//		
//		em.persist(user);
//		em.getTransaction().commit();
//		em.flush();
//		
//System.out.println(user.getId());		
	}
	    
}
