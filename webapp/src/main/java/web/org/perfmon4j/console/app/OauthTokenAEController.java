package web.org.perfmon4j.console.app;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import web.org.perfmon4j.console.app.data.OauthToken;
import web.org.perfmon4j.console.app.data.OauthTokenService;
import web.org.perfmon4j.console.app.zk.RefreshableComposer;

public class OauthTokenAEController extends SelectorComposer<Component> {
	private static final long serialVersionUID = 4357848143335756061L;
	
	private final OauthTokenService oauthTokenService = new OauthTokenService();
	private OauthToken token;

	@Wire
	Window oauthAEDialog;
	@Wire
	Textbox applicationNameTextbox;
	@Wire
	Textbox oauthKeyTextbox;
	@Wire
	Textbox oauthSecretTextbox;
	@Wire
	Label errorMessagesLabel;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		OauthToken tmpToken = (OauthToken)Executions.getCurrent().getArg().get("oauthToken");
		if (tmpToken != null) {
			token = tmpToken;
		} else {
			token = new OauthToken();
		}
		oauthKeyTextbox.setReadonly(true);
		oauthSecretTextbox.setReadonly(true);
		
		applicationNameTextbox.setValue(token.getApplicationName());
		oauthKeyTextbox.setValue(token.getKey());
		oauthSecretTextbox.setValue(token.getSecret());
	}

	@Listen("onClick = #submitButton")
	public void saveAction() {
		if (validate()) {
			token.setApplicationName(getApplicationName());
			oauthTokenService.update(token);
			RefreshableComposer.postRefreshEvent(oauthAEDialog.getParent());
			oauthAEDialog.detach();
		}
	}

	@Listen("onClick = #cancelButton")
	public void cancelAction() {
		oauthAEDialog.detach();
	}

	public static void showDialog(Component parent, OauthToken token) {
		Map<String, Object> arguments = null;
		if (token != null) {
			arguments =	new HashMap<String, Object>();
			arguments.put("oauthToken", token);
		}	
		
		Window window = (Window) Executions.createComponents(
				"/app/oauthTokenAE.zul", parent, arguments);
		window.doModal();
	}

	private boolean validate() {
		boolean result = false;

		String applicationName = getApplicationName();
		
		if ("".equals(applicationName)) {
			setError("You must supply an application name");
		} else  {
			result = true;
//			// Check to see that we are not creating a duplicate an existing user...
//			User matchingUser = userService.findByUserName(userName);
//			if (matchingUser != null && !matchingUser.getId().equals(user.getId())) {
//				setError("Duplicate userName found");
//			} else {
//				result = true;
//			}
		} 

		return result;
	}

	private void setError(String error) {
		errorMessagesLabel.setVisible(true);
		errorMessagesLabel.setValue(error);
	}

	private String getApplicationName() {
		return normalizeValue(applicationNameTextbox);
	}


	private String normalizeValue(Textbox textbox) {
		
		String result = textbox.getValue();
		return result == null ? "" : result.trim();
	}
}
