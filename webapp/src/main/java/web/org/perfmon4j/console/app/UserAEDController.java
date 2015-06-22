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
import web.org.perfmon4j.console.app.data.UserService;
import web.org.perfmon4j.console.app.spring.security.Perfmon4jUserConsoleLoginService;
import web.org.perfmon4j.console.app.zk.RefreshableComposer;

public class UserAEDController extends SelectorComposer<Component> {
	private static final long serialVersionUID = -4455643804067121875L;
	private static final String PASSWORD_UNMODIFIED =  Long.toString(serialVersionUID);

	private final EntityManager em = EMProvider.getEM();
	private final UserService userService = new UserService();
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
			// For the admin user only password can be changed.
			if (UserService.ADMIN_USER_NAME.equals(user.getUserName())) {
				userNameTextbox.setDisabled(true);
				displayNameTextbox.setDisabled(true);
			}
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
					user.setHashedPassword(Perfmon4jUserConsoleLoginService.generateMD5Hash(getPassword()));
				}
				user.setUserName(getUserName());
				if (UserService.ADMIN_USER_NAME.equals(user.getUserName())) {
					if (UserService.DEFAULT_ADMIN_PASSWORD_MD5.equals(user.getHashedPassword())) {
						// Admin user is using the default password... Access is only allowed
						// from localhost.
						user.setDisplayName(UserService.ADMIN_LOCALHOST_DISPLAY_NAME);
					} else {
						user.setDisplayName(UserService.ADMIN_DISPLAY_NAME);
					}
				}
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

		String userName = getUserName();
		
		
		if ("".equals(userName)) {
			setError("You must supply a username");
		} else if ("".equals(getDisplayName())) {
			setError("You must supply a display name");
		} else if ("".equals(getPassword())) {
			setError("You must supply a password");
		} else if (!getPassword().equals(getRetypePassword())) {
			setError("Passwords must match");
		} else  {
			// Check to see that we are not creating a duplicate an existing user...
			User matchingUser = userService.findByUserName(userName);
			if (matchingUser != null && !matchingUser.getId().equals(user.getId())) {
				setError("Duplicate userName found");
			} else {
				result = true;
			}
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
