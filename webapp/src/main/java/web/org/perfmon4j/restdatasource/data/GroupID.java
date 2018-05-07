package web.org.perfmon4j.restdatasource.data;

public class GroupID extends ID {
	private final long groupID;
	
	public GroupID(String databaseID, long groupID) {
		super(databaseID, buildSortable(databaseID, groupID), buildDisplayable(databaseID, groupID));
		this.groupID = groupID;
	}
	
	@Override
	public boolean isGroup() {
		return true;
	}

	public long getSystemID() {
		return groupID;
	}

	private static String buildSortable(String databaseID, long groupID) {
		// No need to validate databaseID format here, it will 
		// be validated in the ID constructor.
		return String.format("%s.GROUP.%020d", databaseID, Long.valueOf(groupID)); 
	}
	
	private static String buildDisplayable(String databaseID, long groupID) {
		// No need to validate databaseID format here, it will 
		// be validated in the ID constructor.
		return databaseID + ".GROUP." + groupID;
	}
}
