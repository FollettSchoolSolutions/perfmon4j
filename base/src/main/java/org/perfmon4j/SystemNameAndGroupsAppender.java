/*
 *	Copyright 2008, 2009, 2010, 2018 Follett School Solutions 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.perfmon4j.util.MiscHelper;

public abstract class SystemNameAndGroupsAppender extends Appender {
	private static final TagField[] DEFAULT_TAG_FIELDS = new TagField[] {new TagField("instanceName")}; 
	
	private String systemNamePrefix = null;
	private String systemNameBody = null;
	private String systemNameSuffix = null;
	private boolean excludeCWDHashFromSystemName = false;
	private String[] groups = new String[]{};
	private TagField[] tagFields = new TagField[] {};
	private boolean useDefaultTagFields = true;
	
	public SystemNameAndGroupsAppender(AppenderID id) {
		super(id);
	}

	/**
	 * 
	 * @param id
	 * @param useAsyncWriter - This should almost always be true.  
	 * See comment in Appender(AppenderID id, boolean useAsyncWriter) 
	 * for more info.
	 */
	public SystemNameAndGroupsAppender(AppenderID id, boolean useAsyncWriter) {
		super(id, useAsyncWriter);
	}
	
	public String getSystemName() {
		return (systemNamePrefix == null ? "" : systemNamePrefix)
			+ getSystemNameBody()
			+ (systemNameSuffix == null ? "" : systemNameSuffix);
	}
	
	public void setSystemNameBody(String systemName) {
		this.systemNameBody = systemName;
	}
	
	public String getSystemNameBody() {
		if (systemNameBody == null) {
			boolean includeCWDHash = !excludeCWDHashFromSystemName;
			return MiscHelper.getDefaultSystemName(includeCWDHash);
		} else {
			return systemNameBody;
		}
	}

	public String getSystemNamePrefix() {
		return systemNamePrefix;
	}

	public void setSystemNamePrefix(String systemNamePrefix) {
		this.systemNamePrefix = systemNamePrefix;
	}

	public String getSystemNameSuffix() {
		return systemNameSuffix;
	}

	public void setSystemNameSuffix(String systemNameSuffix) {
		this.systemNameSuffix = systemNameSuffix;
	}
	
	public boolean isExcludeCWDHashFromSystemName() {
		return excludeCWDHashFromSystemName;
	}

	/**
	 * If this is set to true the cwdHash will NOT be appended to the system name.
	 * This is very useful when you do not want the system name to change if the 
	 * path of the application changes.
	 * 
	 * By default the CWD has is included.
	 *   
	 * @param excludeCWDHashFromSystemName
	 */
	public void setExcludeCWDHashFromSystemName(boolean excludeCWDHashFromSystemName) {
		this.excludeCWDHashFromSystemName = excludeCWDHashFromSystemName;
	}

	public String[] getGroupsAsArray() {
		return groups;
	}

	public void setGroups(String csvGroups) {
		this.groups = MiscHelper.tokenizeCSVString(csvGroups);
	}

	public TagField[] getTagFields() {
		if (useDefaultTagFields) {
			List<TagField> allTagFields = new ArrayList<TagField>();
			
			allTagFields.addAll(Arrays.asList(DEFAULT_TAG_FIELDS));
			allTagFields.addAll(Arrays.asList(tagFields));
			
			return allTagFields.toArray(new TagField[]{});
		} else {
			return tagFields;
		}
	}

	public void setTagFields(String tagFields) {
		this.tagFields = TagField.parseTagFields(tagFields);
	}
	
	public boolean isUseDefaultTagFields() {
		return useDefaultTagFields;
	}

	public void setUseDefaultTagFields(String useDefaultTagFields) {
		setUseDefaultTagFields(Boolean.parseBoolean(useDefaultTagFields));
	}
	
	public void setUseDefaultTagFields(boolean useDefaultTagFields) {
		this.useDefaultTagFields = useDefaultTagFields;
	}

	/**
	 * TagFields are a way to inform an appender which collected data elements 
	 * should be considered a tag (used to select measurements to report)
	 * vs which values are fields.
	 * 
	 * See: https://github.com/FollettSchoolSolutions/perfmon4j/wiki/appenderTagFields
	 * for further information and usage. 
	 * 
	 * @author ddeucher
	 */
	public static class TagField {
		private final String fieldName;
		private final String monitorName; // If null it applies to any monitor.
		
		TagField(String fieldName) {
			this(fieldName, null);
		}
		
		TagField(String fieldName, String monitorName) {
			this.fieldName = fieldName;
			this.monitorName = monitorName;
		}
		
		static TagField[] parseTagFields(String tagFields) {
			List<TagField> result = new ArrayList<SystemNameAndGroupsAppender.TagField>();
			
			if (tagFields != null) {
				for (String element : tagFields.split("\\,")) {
					String[] elementAndMonitor = element.split("\\|", 2);
					if (elementAndMonitor.length == 2) {
						result.add(new TagField(elementAndMonitor[1].trim(), elementAndMonitor[0].trim()));
					} else {
						result.add(new TagField(element.trim()));
					}
				}
			}
			
			return result.toArray(new TagField[] {});
		}
		
		public boolean matches(String monitorName, String fieldName) {
			return fieldName.equals(this.fieldName) && (
					this.monitorName == null || this.monitorName.equals(monitorName));
		}
		
		public static boolean isTagField(String monitorName, String fieldName, TagField... tagFields) {
			for (TagField field : tagFields) {
				if (field.matches(monitorName, fieldName)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "TagField [fieldName=" + fieldName + ", monitorName=" + monitorName + "]";
		}
	}
}
