package org.perfmon4j.util.mbean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;
import org.perfmon4j.util.mbean.MBeanDatum.OutputType;

public class CompositeDataManager {
	private static final Logger logger = LoggerFactory.initLogger(CompositeDataManager.class); 
	private final MBeanServerFinder serverFinder;
	private final ObjectName objectName;
	
	CompositeDataManager(MBeanServerFinder serverFinder, ObjectName objectName) {
		this.serverFinder = serverFinder;
		this.objectName = objectName;
	}
	
	Set<DatumDefinition> buildCompositeDatumDefinitionArray(MBeanQuery query) {
		Map<String, CompositeDataWrapper> wrapperMap = new HashMap<String, CompositeDataWrapper>(); 
		Set<DatumDefinition> result = new HashSet<MBeanAttributeExtractor.DatumDefinition>();
		
		for (String name : query.getCounters()) {
			String compositeName[] = splitAtFirstPeriod(name);
			if (compositeName.length > 1) {
				try {
					CompositeDataWrapper wrapper = getOrCreateCompositeWrapper(wrapperMap, compositeName[0]);
					if (wrapper != null) {
						DatumDefinition def = wrapper.getDataDefinition(compositeName[1], OutputType.COUNTER); 
						if (def != null) {
							result.add(def);
						}
					}
				} catch (MBeanQueryException e) {
					logger.logWarn("Unable to retrieve attribute type for composite attribute: " + name, e);
				}
			}
		}
		
		for (String name : query.getGauges()) {
			String compositeName[] = splitAtFirstPeriod(name);
			if (compositeName.length > 1) {
				try {
					CompositeDataWrapper wrapper = getOrCreateCompositeWrapper(wrapperMap, compositeName[0]);
					if (wrapper != null) {
						DatumDefinition def = wrapper.getDataDefinition(compositeName[1], OutputType.GAUGE); 
						if (def != null) {
							result.add(def);
						}
					}
				} catch (MBeanQueryException e) {
					logger.logWarn("Unable to retrieve attribute type for composite attribute: " + name, e);
				}
			}
		}
		
		return result;
	}
	
	private CompositeDataWrapper getOrCreateCompositeWrapper(Map<String, CompositeDataWrapper> wrapperMap, String attributeName) 
		throws MBeanQueryException {
		CompositeDataWrapper wrapper = wrapperMap.get(attributeName);
		if (wrapper == null) {
			wrapper = wrapperMap.get(MiscHelper.toggleCaseOfFirstLetter(attributeName));
		}
		
		if (wrapper == null) {
			Object obj = serverFinder.getAttribute(objectName, attributeName);
			if (obj == null) {
				attributeName = MiscHelper.toggleCaseOfFirstLetter(attributeName);
				obj = serverFinder.getAttribute(objectName, attributeName);
			}
			
			if (obj != null && (obj instanceof CompositeData)) {
				wrapper = new CompositeDataWrapper((CompositeData)obj, attributeName);
				wrapperMap.put(attributeName, wrapper);
			}
		}
		
		return wrapper;
	}
	
	
	
//	for (String gauge)
	
	
	
	
	
	
	
	static boolean isCompositeAttributeName(String attributeName) {
		return splitAtFirstPeriod(attributeName).length > 1;
	}
	
    static String[] splitAtFirstPeriod(String input) {
        // Check if the string is null or too short to contain a period that isn't at the start or end
        if (input == null || input.length() < 3) {
            return new String[]{input};
        }
        
        // Find the index of the first period
        int firstPeriodIndex = input.indexOf('.');
        
        // If no period is found or it's at the start or end, return the original string
        if (firstPeriodIndex == -1 || firstPeriodIndex == 0 || firstPeriodIndex == input.length() - 1) {
            return new String[]{input};
        }
        
        // Split the string at the first period
        return new String[]{input.substring(0, firstPeriodIndex), input.substring(firstPeriodIndex + 1)};
    }	

}
