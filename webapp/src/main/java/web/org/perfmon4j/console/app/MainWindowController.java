package web.org.perfmon4j.console.app;


import org.springframework.security.core.context.SecurityContextHolder;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Menu;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Window;

import web.org.perfmon4j.console.app.spring.security.Perfmon4jUserConsoleLoginService.Perfmon4jUser;

public class MainWindowController extends SelectorComposer<Component> {
	private static final long serialVersionUID = -7098664190564412475L;

	@Wire
	private Tabbox mainTabbox;
	
	@Wire Menu currentUserMenu;
	
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Perfmon4jUser user =  (Perfmon4jUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String welcome = "Welcome " + user.getDisplayName();
		currentUserMenu.setLabel(welcome);
	}

	@Listen("onClick = #aboutMenuItem")
	public void about() {
		  Window window = (Window)Executions.createComponents(
				  "/app/about.zul", null, null);
		  window.doModal();
	}

	@Listen("onClick = #logoutMenuItem")
	public void logout() {
		Executions.sendRedirect("../j_spring_security_logout");
	}
	
	@Listen("onClick = #systemInfoMenuItem")
	public void systemInfo() {
		Tabpanel panel = new Tabpanel();
	
		Tab tab = new Tab("System Info");
		tab.setClosable(true);
		
		
		mainTabbox.getTabpanels().appendChild(panel);
		mainTabbox.getTabs().appendChild(tab);
		
		Executions.createComponents("/app/systeminfo.zul", panel, null);
	}

	@Listen("onClick = #aedUsers")
	public void aedUsers() {
		Tabpanel panel = new Tabpanel();
	
		Tab tab = new Tab("Users");
		tab.setClosable(true);
		
		
		mainTabbox.getTabpanels().appendChild(panel);
		mainTabbox.getTabs().appendChild(tab);
		
		Executions.createComponents("/app/users.zul", panel, null);
	}

	
		
	
}


