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
import org.perfmon4j.util.mbean.GaugeCounterArgumentParser.AttributeSpec;
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
	
	Set<DatumDefinition> buildCompositeDatumDefinitionArray(GaugeCounterArgumentParser parser, Map<String, String> foundRatioComponents) {
		Map<String, CompositeDataWrapper> wrapperMap = new HashMap<String, CompositeDataWrapper>(); 
		Set<DatumDefinition> result = new HashSet<MBeanAttributeExtractor.DatumDefinition>();

		for (AttributeSpec ratioComponent : parser.getRatioComponents()) {
			if (ratioComponent.isCompositeName()) {
				String compositeName[] = splitAtFirstPeriod(ratioComponent.getName());
				if (compositeName.length > 1) {
					try {
						CompositeDataWrapper wrapper = getOrCreateCompositeWrapper(wrapperMap, compositeName[0]);
						if (wrapper != null) {
							DatumDefinition def = wrapper.getDataDefinition(compositeName[1], OutputType.VOID, ratioComponent); 
							if (def != null && def.getAttributeType().isSupportsRatioComponent()) {
								foundRatioComponents.put(ratioComponent.getName(), def.getName());
								result.add(def);
							}
						}
					} catch (MBeanQueryException e) {
						logger.logWarn("Unable to retrieve attribute type for composite attribute for ratio component: " + ratioComponent, e);
					}
				}
			}
		}
		
		for (AttributeSpec counter : parser.getCounters()) {
			if (counter.isCompositeName()) {
				String compositeName[] = splitAtFirstPeriod(counter.getName());
				if (compositeName.length > 1) {
					try {
						CompositeDataWrapper wrapper = getOrCreateCompositeWrapper(wrapperMap, compositeName[0]);
						if (wrapper != null) {
							DatumDefinition def = wrapper.getDataDefinition(compositeName[1], OutputType.COUNTER, counter); 
							if (def != null) {
								result.add(def);
							}
						}
					} catch (MBeanQueryException e) {
						logger.logWarn("Unable to retrieve attribute type for composite attribute: " + counter, e);
					}
				}
			}
		}
		
		for (AttributeSpec gauge : parser.getGauges()) {
			if (gauge.isCompositeName()) {
				String compositeName[] = splitAtFirstPeriod(gauge.getName());
				try {
					CompositeDataWrapper wrapper = getOrCreateCompositeWrapper(wrapperMap, compositeName[0]);
					if (wrapper != null) {
						DatumDefinition def = wrapper.getDataDefinition(compositeName[1], OutputType.GAUGE, gauge); 
						if (def != null) {
							result.add(def);
						}
					}
				} catch (MBeanQueryException e) {
					logger.logWarn("Unable to retrieve attribute type for composite attribute: " + gauge, e);
				}
			}
		}
		
		return result;
	}
	
	Map<String, MBeanDatum<?>> extractCompositeDataAttributes(DatumDefinition[] datumArray) throws MBeanQueryException {
		Map<String, CompositeDataWrapper> wrapperMap = new HashMap<String, CompositeDataWrapper>(); 
		Map<String, MBeanDatum<?>> result = new HashMap<String, MBeanDatum<?>>();
		
		for (DatumDefinition def : datumArray) {
			if (def.isCompositeAttribute()) {
				CompositeDataWrapper wrapper = getOrCreateCompositeWrapper(wrapperMap, def.getParentName());
				if (wrapper != null) {
					result.put(def.getName(), wrapper.getMBeanDatum(def));
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
