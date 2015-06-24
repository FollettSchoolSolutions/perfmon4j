package web.org.perfmon4j.console.app;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Window;

public class AboutController  extends SelectorComposer<Component> {
	   private static final long serialVersionUID = 1L;
	     
	    @Wire
	    Window aboutDialog;
	     
	    @Listen("onClick = #okButton")
	    public void showModal() {
	    	aboutDialog.detach();
	    }
}
