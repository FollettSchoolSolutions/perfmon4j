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

package web.org.perfmon4j.console.app;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.security.core.context.SecurityContextHolder;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

import web.org.perfmon4j.console.app.data.EMProvider;
import web.org.perfmon4j.console.app.data.User;
import web.org.perfmon4j.console.app.data.UserService;
import web.org.perfmon4j.console.app.spring.security.Perfmon4jUserConsoleLoginService.Perfmon4jUser;
import web.org.perfmon4j.console.app.zk.RefreshableComposer;


public class UsersController extends RefreshableComposer<Component>  {
	private static final long serialVersionUID = 1L;
	    
	private EntityManager em = EMProvider.getEM();
	
	@Wire
	private Component usersWindow;
	
	@Wire
    private Grid usersGrid;
	
    @Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		usersGrid.setRowRenderer(new UserRowRender());
		usersGrid.setModel(new ListModelList<User>(getAllUsers()));
	    }
	    
	@Listen("onClick = #createUserButton")
	public void createUser() {
		UserAEDController.showDialog(usersWindow, null);
		usersGrid.setModel(new ListModelList<User>(getAllUsers()));
	}

	@SuppressWarnings("unchecked")
	private Collection<User> getAllUsers() {
		Query q = em.createQuery("select u from User u order by u.userName");
		return (Collection<User>)q.getResultList();
	}
	
	
	public void onDeleteUser(Event event) {
		final User user = (User)event.getTarget().getAttribute("user");
		Messagebox.show("Delete user: " + user.getUserName() + "?", "Question", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				int result = ((Integer)event.getData()).intValue();
				if (result == Messagebox.YES) {
					em.getTransaction().begin();
					try {
						em.remove(user);
					} finally {
						em.getTransaction().commit();
					}
					RefreshableComposer.postRefreshEvent(usersWindow);
				}
			}
		}); 
	}

	public void onEditUser(Event event) {
		User user = (User)event.getTarget().getAttribute("user");
		UserAEDController.showDialog(usersWindow, user);
		usersGrid.setModel(new ListModelList<User>(getAllUsers()));
	}
	
	protected void handleRefreshEvent(Event event) {
		usersGrid.setModel(new ListModelList<User>(getAllUsers()));		
	}

	private class UserRowRender implements RowRenderer<User> {
		@Override
		public void render(Row row, User user, int whatIsThis) throws Exception {
			row.appendChild(new Label(user.getUserName()));
			row.appendChild(new Label(user.getDisplayName()));
			Hlayout layout = new Hlayout();
			
			Button edit = new Button();
			edit.setAttribute("user", user);
			edit.setImage("/app/images/pencil-2x.png");
			edit.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
				public void onEvent(Event event) {
					onEditUser(event);
				}
			});
			layout.appendChild(edit);
			
			Button delete = new Button();
			delete.setAttribute("user", user);
			delete.setImage("/app/images/delete-2x.png");
			delete.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
				public void onEvent(Event event) {
					onDeleteUser(event);
				}
			});
			
			
			if (UserService.ADMIN_USER_NAME.equals(user.getUserName())) {
				// Don't allow user to delete admin user.
				delete.setDisabled(true);
			} else {
				// Don't allow the logged in user to delete themselves
				Perfmon4jUser loggedInUser =  (Perfmon4jUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
				if (loggedInUser.getUserID().equals(user.getId())) {
					delete.setDisabled(true);
				}
			}
			layout.appendChild(delete);
			
			row.appendChild(layout);
		}
	}
}
