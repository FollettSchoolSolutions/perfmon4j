package web.org.perfmon4j.restdatasource.data;

public class SystemID extends ID {
	private final long systemID;
	
	public SystemID(String databaseID, long systemID) {
		super(databaseID, buildSortable(databaseID, systemID), buildDisplayable(databaseID, systemID));
		this.systemID = systemID;
	}
	
	@Override
	public boolean isSystem() {
		return true;
	}

	public long getSystemID() {
		return systemID;
	}

	@Deprecated
	public long getID() {
		return getSystemID();
	}
	
	
	private static String buildSortable(String databaseID, long systemID) {
		// No need to validate databaseID format here, it will 
		// be validated in the ID constructor.
		return String.format("%s.%020d", databaseID, Long.valueOf(systemID)); 
	}
	
	private static String buildDisplayable(String databaseID, long systemID) {
		// No need to validate databaseID format here, it will 
		// be validated in the ID constructor.
		return databaseID + "." + systemID;
	}
}
