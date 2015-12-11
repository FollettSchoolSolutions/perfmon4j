package web.org.perfmon4j.console.app.util.openmbean;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

public class TabularDataHelper {
	
	public List<String> extractTabularData(Object obj) {
		List<String> result = null;
		
		if (obj instanceof TabularData) {
			TabularData tabData = (TabularData)obj;
			result = new ArrayList<String>();
			
			for (Object v: tabData.values()) {
			   CompositeData row = (CompositeData)v;
			   String key = row.getCompositeType().getDescription();
			   StringBuilder rowString = new StringBuilder();
			   for (Object rv: row.values()) {
			       if (rowString.length()!=0)
			            rowString.append(";");
			       rowString.append(rv);
			   }
			   result.add(key + "=" + rowString.toString());
			}
		}
		return result;
	}
}
