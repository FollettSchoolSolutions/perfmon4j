package web.org.perfmon4j.console.app;

import org.perfmon4j.PerfMon;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

public class SystemInfoController  extends SelectorComposer<Component> {
	   private static final long serialVersionUID = 1L;
	     
	   
	   

		@Wire
	    Grid systemInfoGrid;

	    @Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Row row = new Row();
		row.appendChild(new Label("Java Version"));
		row.appendChild(new Label(System.getProperty("java.version")));
		
		
		systemInfoGrid.getRows().appendChild(row);

		row = new Row();
		row.appendChild(new Label("Perfmon4j enabled"));
		row.appendChild(new Label(Boolean.toString(PerfMon.isConfigured())));
		
		
		systemInfoGrid.getRows().appendChild(row);
		
		
	}
}
