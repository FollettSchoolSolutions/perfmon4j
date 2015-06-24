package web.org.perfmon4j.console.app;

import java.util.Arrays;

import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.RegisteredDatabaseConnections.Database;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

import web.org.perfmon4j.console.app.data.AppConfig;
import web.org.perfmon4j.console.app.data.AppConfigService;
import web.org.perfmon4j.console.app.data.OauthToken;
import web.org.perfmon4j.console.app.data.OauthTokenService;
import web.org.perfmon4j.console.app.zk.RefreshableComposer;


public class DataSourcesController extends RefreshableComposer<Component>  {
	private static final long serialVersionUID = 1L;

	private AppConfigService configService = new AppConfigService();
	private OauthTokenService oauthTokenService = new OauthTokenService();
	
	@Wire
	private Component dataSourcesWindow;
	
	@Wire
    private Grid databaseGrid;

	@Wire
    private Grid oauthGrid;
	
	@Wire
	private Checkbox enableDataSourcesCheckBox;

	@Wire
	private Checkbox allowAnonymousCheckBox;
	
	@Wire
	private Button createOauthTokenButton;
	
    @Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		AppConfig config = configService.getConfig();
		enableDataSourcesCheckBox.setChecked(config.isAccessEnabled());
		allowAnonymousCheckBox.setChecked(config.isAnonymousAccessEnabled());
		
		enableDataSourcesCheckBox.addEventListener(Events.ON_CHECK, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				onEnabledChecked();
			}
		});
		allowAnonymousCheckBox.addEventListener(Events.ON_CHECK, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				onAnonymousChecked();
			}
		});
		
		onEnabledChecked(); // This will line up the GUI to the current config state.
		
		databaseGrid.setModel(new ListModelList<Database>(
				Arrays.asList(RegisteredDatabaseConnections.getAllDatabases())
				));
		databaseGrid.setRowRenderer(new DatabaseRowRender());
		
		oauthGrid.setRowRenderer(new OauthTokenRowRender());
		oauthGrid.setModel(new ListModelList<OauthToken>(oauthTokenService.getOauthTokens()));
	    }
	    
    public void onEnabledChecked() {
    	AppConfig config = configService.getConfig();
    	boolean isEnabled = enableDataSourcesCheckBox.isChecked();
    	boolean isAnonEnabled = allowAnonymousCheckBox.isChecked();
    	
    	if (isEnabled) {
    		allowAnonymousCheckBox.setDisabled(false);
    		createOauthTokenButton.setDisabled(false);
    		
    		if (!config.isAccessEnabled()) {
    			config.setAccessEnabled(true);
    			config.setAnonymousAccessEnabled(isAnonEnabled);
    			configService.updateConfig(config);
    		}
    	} else {
    		isAnonEnabled = false;
    		
    		allowAnonymousCheckBox.setDisabled(true);
    		allowAnonymousCheckBox.setChecked(false);
    		createOauthTokenButton.setDisabled(true);
    		
    		if (config.isAccessEnabled()) {
    			config.setAccessEnabled(false);
    			config.setAnonymousAccessEnabled(false);
    			configService.updateConfig(config);
    		}
    	}
    }

    public void onAnonymousChecked() {
    	AppConfig config = configService.getConfig();
    	boolean isAnonEnabled = allowAnonymousCheckBox.isChecked();
    	
    	if (isAnonEnabled) {
    		if (!config.isAnonymousAccessEnabled()) {
    			config.setAnonymousAccessEnabled(true);
    			configService.updateConfig(config);
    		}
    	} else {
    		if (config.isAnonymousAccessEnabled()) {
    			config.setAnonymousAccessEnabled(false);
    			configService.updateConfig(config);
    		}
    	}
    }
   
    
	@Listen("onClick = #createOauthTokenButton")
	public void createOauthToken() {
		OauthTokenAEController.showDialog(dataSourcesWindow, null);
	}


	
	public void onDeleteToken(Event event) {
		final OauthToken token = (OauthToken)event.getTarget().getAttribute("oauthToken");
		Messagebox.show("Delete application: " + token.getApplicationName() + "?", "Question", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				int result = ((Integer)event.getData()).intValue();
				if (result == Messagebox.YES) {
					oauthTokenService.delete(token);
					RefreshableComposer.postRefreshEvent(dataSourcesWindow);
				}
			}
		}); 
	}

	public void onEditToken(Event event) {
		OauthToken token = (OauthToken)event.getTarget().getAttribute("oauthToken");
		OauthTokenAEController.showDialog(dataSourcesWindow, token);
	}
	
	protected void handleRefreshEvent(Event event) {
		oauthGrid.setModel(new ListModelList<OauthToken>(oauthTokenService.getOauthTokens()));
	}

	private class DatabaseRowRender implements RowRenderer<Database> {
		@Override
		public void render(Row row, Database database, int whatIsThis) throws Exception {
			row.appendChild(new Label(database.getID()));
			row.appendChild(new Label(database.getName()));
			row.appendChild(new Label(Double.toString(database.getDatabaseVersion())));
		}
	}
	
	private class OauthTokenRowRender implements RowRenderer<OauthToken> {
		@Override
		public void render(Row row, OauthToken token, int whatIsThis) throws Exception {
			row.appendChild(new Label(token.getApplicationName()));
			row.appendChild(new Label(token.getKey()));
			Hlayout layout = new Hlayout();
			
			Button edit = new Button();
			edit.setAttribute("oauthToken", token);
			edit.setImage("/app/images/pencil-2x.png");
			edit.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
				public void onEvent(Event event) {
					onEditToken(event);
				}
			});
			layout.appendChild(edit);
			
			Button delete = new Button();
			delete.setAttribute("oauthToken", token);
			delete.setImage("/app/images/delete-2x.png");
			delete.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
				public void onEvent(Event event) {
					onDeleteToken(event);
				}
			});
			layout.appendChild(delete);
			row.appendChild(layout);
		}
	}
}
