package web.org.perfmon4j.console.app;


import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Window;

public class MainWindowController extends SelectorComposer<Component> {
	private static final long serialVersionUID = -7098664190564412475L;

	@Wire
	private Tabbox mainTabbox;
	
	@Listen("onClick = #aboutMenuItem")
	public void about() {
		  Window window = (Window)Executions.createComponents(
				  "/about.zul", null, null);
		  window.doModal();
	}
	
	@Listen("onClick = #systemInfoMenuItem")
	public void systemInfo() {
		Tabpanel panel = new Tabpanel();
	
		Tab tab = new Tab("System Info");
		tab.setClosable(true);
		
		
		mainTabbox.getTabpanels().appendChild(panel);
		mainTabbox.getTabs().appendChild(tab);
		
		Executions.createComponents("/systeminfo.zul", panel, null);
	}

	
		
	
}


