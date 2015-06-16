package web.org.perfmon4j.console.app.model;

import java.util.Map;

import org.zkoss.bind.Property;
import org.zkoss.bind.ValidationContext;
import org.zkoss.bind.validator.AbstractValidator;

public class UserFormValidator extends AbstractValidator {

	@Override
	public void validate(ValidationContext ctx) {
        //all the bean properties
        Map<String,Property> beanProps = ctx.getProperties(ctx.getProperty().getBase());
         
        validatePasswords(ctx, getValidatorArg(ctx, "password"), getValidatorArg(ctx, "retypedPassword"));
        validateUserName(ctx, getValue(beanProps, "userName"));
        validateDisplayName(ctx, getValue(beanProps, "displayName"));
 	}

	private String getValue(Map<String,Property> beanProps, String fieldName) {
		String input = (String)beanProps.get(fieldName).getValue();
		return input == null ? "" : input.trim();
	}

	private String getValidatorArg(ValidationContext ctx, String fieldName) {
		String input = (String)ctx.getValidatorArg(fieldName);
		return input == null ? "" : input.trim();
	}
	
	private void validatePasswords(ValidationContext ctx, String password, String retypedPassword) {
		if (password.isEmpty()) {
			this.addInvalidMessage(ctx, "password", "Password is required");
		} else if (!password.equals(retypedPassword)) {
			this.addInvalidMessage(ctx, "password", "Your passwords do not match");
		}
	}

	private void validateUserName(ValidationContext ctx, String userName) {
		if (userName.isEmpty()) {
			this.addInvalidMessage(ctx, "userName", "User name is required");
		}
		// Also need to check to see is  a duplicate
	}
	
	private void validateDisplayName(ValidationContext ctx, String displayName) {
		if (displayName.isEmpty()) {
			this.addInvalidMessage(ctx, "displayName", "Display name is required");
		}
	}
}
