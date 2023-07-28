package org.perfmon4j;

import org.perfmon4j.SystemNameAndGroupsAppender.TagField;

import junit.framework.TestCase;

public class SystemNameAndGroupsAppenderTest extends TestCase {

	public void testParseTagFields_SingleField() {
		String example = "instanceName";
	
		TagField[] tagFields = TagField.parseTagFields(example);
		assertNotNull(tagFields);
		assertEquals("Expected number of tag fields", 1, tagFields.length);
		assertTrue("instanceName for any Monitor should be considered a tag field",
			TagField.isTagField("xyz", "instanceName", tagFields)
			&& TagField.isTagField("abc", "instanceName", tagFields));

		assertFalse("fields not matching instanceName are not tag fields",
				TagField.isTagField("xyz", "region", tagFields));
	}

	public void testParseTagFields_MultipleFields() {
		String example = "instanceName,region,dataCenter";
	
		TagField[] tagFields = TagField.parseTagFields(example);
		assertNotNull(tagFields);
		assertEquals("Expected number of tag fields", 3, tagFields.length);
		assertTrue("instanceName or region, in any Monitor, should be considered a tag field",
			TagField.isTagField("xyz", "instanceName", tagFields)
			&& TagField.isTagField("abc", "region", tagFields)
			&& TagField.isTagField("efg", "dataCenter", tagFields));
	}
	
	public void testParseTagFields_LimitToSingleMonitor() {
		String example = "Memory Monitor|instanceName";
		
		TagField[] tagFields = TagField.parseTagFields(example);
		assertNotNull(tagFields);

		assertTrue("Should match if monitor matches", TagField.isTagField("Memory Monitor", "instanceName", tagFields));
		assertFalse("Should NOT match if monitor matches", TagField.isTagField("GC Monitor", "instanceName", tagFields));
	}
}
