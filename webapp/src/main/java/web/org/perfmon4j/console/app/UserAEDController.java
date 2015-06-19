package web.org.perfmon4j.console.app;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import web.org.perfmon4j.console.app.data.EMProvider;
import web.org.perfmon4j.console.app.data.User;
import web.org.perfmon4j.console.app.zk.RefreshableComposer;

public class UserAEDController extends SelectorComposer<Component> {
	private static final long serialVersionUID = -4455643804067121875L;
	private static final String PASSWORD_UNMODIFIED =  "4455643804067121875";

	private final EntityManager em = EMProvider.getEM();
	private User user;

	@Wire
	Window userAEDDialog;
	@Wire
	Textbox userNameTextbox;
	@Wire
	Textbox displayNameTextbox;
	@Wire
	Textbox passwordTextbox;
	@Wire
	Textbox retypePasswordTextbox;
	@Wire
	Label errorMessagesLabel;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		User tmpUser = (User)Executions.getCurrent().getArg().get("user");
		if (tmpUser != null) {
			userAEDDialog.setTitle("Edit User");
			user = tmpUser;
			
			userNameTextbox.setValue(user.getUserName());
			displayNameTextbox.setValue(user.getDisplayName());
			passwordTextbox.setValue(PASSWORD_UNMODIFIED);
			retypePasswordTextbox.setValue(PASSWORD_UNMODIFIED);
		} else {
			user = new User();
		}
	}

	@Listen("onClick = #submitButton")
	public void saveAction() {
		if (validate()) {
			em.getTransaction().begin();
			try {
				user.setDisplayName(getDisplayName());
				if (user.getId() == null || !getPassword().equals(PASSWORD_UNMODIFIED)) {
					user.setHashedPassword(getPassword()); // Must create MD5
				}
				user.setUserName(getUserName());
				em.persist(user);
			} finally {
				em.getTransaction().commit();
			}
			RefreshableComposer.postRefreshEvent(userAEDDialog.getParent());
			userAEDDialog.detach();
		}
	}

	@Listen("onClick = #cancelButton")
	public void cancelAction() {
		userAEDDialog.detach();
	}

	public static void showDialog(Component parent, User user) {
		Map<String, Object> arguments = null;
		if (user != null) {
			arguments =	new HashMap<String, Object>();
			arguments.put("user", user);
		}	
		
		Window window = (Window) Executions.createComponents(
				"/app/userAED.zul", parent, arguments);
		window.doModal();
	}

	private boolean validate() {
		boolean result = false;

		if ("".equals(getUserName())) {
			setError("You must supply a username");
		} else if ("".equals(getDisplayName())) {
			setError("You must supply a display name");
		} else if ("".equals(getPassword())) {
			setError("You must supply a password");
		} else if (!getPassword().equals(getRetypePassword())) {
			setError("Passwords must match");
		} else {
			result = true;
		}

		return result;
	}

	private void setError(String error) {
		errorMessagesLabel.setVisible(true);
		errorMessagesLabel.setValue(error);
	}

	private String getUserName() {
		return normalizeValue(userNameTextbox);
	}

	private String getDisplayName() {
		return normalizeValue(displayNameTextbox);
	}

	private String getPassword() {
		return normalizeValue(passwordTextbox);
	}

	private String getRetypePassword() {
		return normalizeValue(retypePasswordTextbox);
	}

	private String normalizeValue(Textbox textbox) {
		
		String result = textbox.getValue();
		return result == null ? "" : result.trim();
	}
}
