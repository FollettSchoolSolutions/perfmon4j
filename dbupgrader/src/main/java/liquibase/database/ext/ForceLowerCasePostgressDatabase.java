package liquibase.database.ext;

import liquibase.database.core.PostgresDatabase;
import liquibase.structure.DatabaseObject;

public class ForceLowerCasePostgressDatabase extends PostgresDatabase {
	
	public ForceLowerCasePostgressDatabase() {
	}
	
	@Override
	public int getPriority() {
		// Give this a higher priority so Liquibase will use it instead
		// of the builtin PostgresDatabase class.
		return super.getPriority() + 1;
	}

	@Override
	public String escapeObjectName(String objectName,
			Class<? extends DatabaseObject> objectType) {
		// The legacy Perfmon4j object contained unqoted identifers 
		// which Postgress explicitly changed to lowercase.
		// To be compatible make all of our object identifiers lowercase.
		if (objectName != null) {
			objectName = objectName.toLowerCase();
		}
		return super.escapeObjectName(objectName, objectType);
	}
}
