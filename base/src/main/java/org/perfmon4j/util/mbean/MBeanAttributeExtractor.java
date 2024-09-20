package org.perfmon4j.util.mbean;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.mbean.MBeanDatum.Type;

class MBeanAttributeExtractor {
	private final WeakReference<MBeanServer> mBeanServer;
	private final ObjectName objectName;
	private final MBeanQuery query;
	private final DatumDefinition dataDefinition[];
	private static final Logger logger = LoggerFactory.initLogger(MBeanAttributeExtractor.class);
	
	MBeanAttributeExtractor(MBeanServer mBeanServer, ObjectName objectName,
			MBeanQuery query) throws MBeanQueryException {
		super();
		this.mBeanServer = new WeakReference<>(mBeanServer);
		this.objectName = objectName;
		this.query = query;
		try {
			dataDefinition = buildDataDefinitionArray(mBeanServer.getMBeanInfo(objectName), query);
		} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
			throw new MBeanQueryException("Unabled to retrieve data from mbean: " + objectName.getCanonicalName(), e);
		}
	}
	
	// A counter is a never increasing value.  These are almost always Long/long, however we will also accept 
	// Integer/int and Short/short.
	private static final Set<String>  VALID_COUNTER_TYPES
		= new HashSet<String>(Arrays.asList(new String[] {"short", "Short", "int", "Integer", "long", "Long"}));
	
    private static final Function<String, String> toggleCaseOfFirstLetter = s -> {
        if (s == null || s.isEmpty()) {
            return s;
        }
        
        // Get the first character
        char firstChar = s.charAt(0);
        
        // Change the case of the first character
        char newFirstChar = Character.isUpperCase(firstChar) 
            ? Character.toLowerCase(firstChar) 
            : Character.toUpperCase(firstChar);
        
        // Combine the new first character with the rest of the string
        return newFirstChar + s.substring(1);
    };
	
	static DatumDefinition[] buildDataDefinitionArray(MBeanInfo mBeanInfo, MBeanQuery query) {
		Set<DatumDefinition> result = new HashSet<>();
		Map<String, MBeanAttributeInfo> attributes = new HashMap<String, MBeanAttributeInfo>();
		Set<String> gaugeNames = new HashSet<String>(Arrays.asList(query.getGauges()));
		Set<String> counterNames = new HashSet<String>(Arrays.asList(query.getCounters()));
		
		for (MBeanAttributeInfo aInfo : mBeanInfo.getAttributes()) {
			attributes.put(aInfo.getName(), aInfo);
		}
		
		for (int i = 0; i < 2; i++) {
			Function<String, String> transform;
			switch (i) {
			case 1:
				transform = toggleCaseOfFirstLetter;
				break;
			default:
				transform = s -> s; // do nothing.
				break;
			}
			
			// First get Counters
			for (String counter : counterNames.toArray(new String[] {})) {
				MBeanAttributeInfo info = attributes.get(transform.apply(counter));
				if (info != null) {
					if (!VALID_COUNTER_TYPES.contains(info.getType())) {
						logger.logWarn("Skipping potential match for defined gauage (" + counter + ") because MBeanAttribute contains incompatable type: " + info.getType());
					} else {
						counterNames.remove(counter);
						attributes.remove(info.getName());
						result.add(new DatumDefinition(Type.COUNTER, info.getName(), info));
					}
				}
			}
			
			// Then try gauges
			for (String gauge : gaugeNames.toArray(new String[] {})) {
				MBeanAttributeInfo info = attributes.get(transform.apply(gauge));
				if (info != null) {
					counterNames.remove(gauge);
					attributes.remove(info.getName());
					result.add(new DatumDefinition(Type.GAUGE, info.getName(), info));
				}
			}
			
		}
		return result.toArray(new DatumDefinition[] {});
	}
	
	
	static final class DatumDefinition {
		private final MBeanDatum.Type type;
		private final String name;
		MBeanAttributeInfo attributeInfo;
		
		public DatumDefinition(Type type, String name, MBeanAttributeInfo attributeInfo) {
			this.type = type;
			this.name = name;
			this.attributeInfo = attributeInfo;
		}

		public MBeanDatum.Type getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, type);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DatumDefinition other = (DatumDefinition) obj;
			return Objects.equals(name, other.name) && type == other.type;
		}
	}
	
	
	
	
	MBeanDatum<?>[] extractAttributes() {
		List<MBeanDatum<?>> result = new ArrayList<MBeanDatum<?>>();
		MBeanServer mbs = mBeanServer.get();
		if (mbs != null) {
			try {
				MBeanInfo info = mbs.getMBeanInfo(objectName);
			
				
//				info.get
//				info.getAttributes()[0].
				
				
				
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstanceNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ReflectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		} else {
			logger.logDebug("MBean server has been garbage collected.  Unable to return attributes for JMX Object: " + objectName.getCanonicalName());
		}
		
		
		
		return null;
	}
}
