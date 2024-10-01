package org.perfmon4j.util.mbean;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenType;

import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;
import org.perfmon4j.util.mbean.MBeanDatum.AttributeType;
import org.perfmon4j.util.mbean.MBeanDatum.OutputType;

public class CompositeDataWrapper {
	private final CompositeData data;
	private final String baseName;
	
	private static final Object[][] SUPPORTED_TYPE_MAPPING =
		{
			{Boolean.class.getName(), MBeanDatum.AttributeType.BOOLEAN},
			{Byte.class.getName(), MBeanDatum.AttributeType.BYTE},
			{Character.class.getName(), MBeanDatum.AttributeType.CHARACTER},
			{Double.class.getName(), MBeanDatum.AttributeType.DOUBLE},
			{Float.class.getName(), MBeanDatum.AttributeType.FLOAT},
			{Integer.class.getName(), MBeanDatum.AttributeType.INTEGER},
			{Long.class.getName(), MBeanDatum.AttributeType.LONG},
			{Short.class.getName(), MBeanDatum.AttributeType.SHORT},
			{String.class.getName(), MBeanDatum.AttributeType.STRING}
		};
	
	/*
	 * baseName is the attribute name of the composite data object.
	 * 
	 * For example if we have an mBean with the "java.lang:name=G1 Old Gen,type=MemoryPool"
	 * contains an attribute called "Usage"
	 * 
	 *  The individual data attributes retrieved will be prefixed with the composite data object
	 *  name.  For example if we retrieve the "used" memory object of the "Usage" composite data
	 *  object the attribute name will be "Usage.used".
	 */
	public CompositeDataWrapper(CompositeData data, String baseName) {
		this.data = data;
		this.baseName = baseName;
	}
	
	/**
	 * 
	 * @param attributeName - This name can be passed in prefixed or not prefixed 
	 * with the Composite data base name.  For example "Usage.used" or "used"
	 * will be allowed.
	 * 
	 * @param outputType
	 * 
	 * @return DataDefinition - The name of the returned definition will always
	 *  be prefixed with the baseName  (i.e. Usage.used") regardless of if the
	 *  base name was passed in or not.
	 */
	public DatumDefinition getDataDefinition(String attributeName, OutputType outputType) {
		DatumDefinition result = null; 
		attributeName = filterBaseNameFromAttributeName(attributeName);
		AttributeType attributeType = AttributeType.STRING; //  
		OpenType<?> openType = data.getCompositeType().getType(attributeName);
		if (openType == null) {
			// Try again after toggling case of firstLetter
			attributeName = MiscHelper.toggleCaseOfFirstLetter(attributeName);
			openType = data.getCompositeType().getType(attributeName);
		}
		
		if (openType != null) {
			for (Object[] row : SUPPORTED_TYPE_MAPPING) {
				if (row[0].equals(openType.getClassName())) {
					attributeType = (AttributeType)row[1];
					break;
				}
			}
			result = new DatumDefinition(baseName + "." + attributeName, attributeType, outputType);
		}
		return result;
	}
	
	public MBeanDatum<?> getMBeanDatum(DatumDefinition dd) {
		String attributeName = filterBaseNameFromAttributeName(dd.getName());
		Object obj = data.get(attributeName);
		
		return new MBeanAttributeExtractor.MBeanDatumImpl<>(dd, obj);
	}
	
	private String filterBaseNameFromAttributeName(String attributeName) {
		String result = attributeName;
		String prefix = baseName + ".";
		
		if (attributeName.startsWith(prefix) || attributeName.startsWith(MiscHelper.toggleCaseOfFirstLetter(prefix))) {
			result = attributeName.substring(prefix.length());
		} 
		return result;
	}
}
