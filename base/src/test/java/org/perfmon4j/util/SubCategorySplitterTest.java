/*
 *	Copyright 2022 Follett School Solutions, LLC 
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
 *  Follett School Solutions, LLC
 *  1340 Ridgeview Drive
 *  McHenry, IL 60050
 * 
*/

package org.perfmon4j.util;

import org.perfmon4j.util.SubCategorySplitter.Split;

import junit.framework.TestCase;

public class SubCategorySplitterTest extends TestCase {

	public void testCategoryMatch() throws Exception {
    	String category = "DistrictRequest.blue";
    	SubCategorySplitter splitter = new SubCategorySplitter("DistrictRequest\\.(.*)");

    	Split split = splitter.split(category);
    	
    	assertNotNull("Should always get back a split object", split);
    	
    	assertTrue("Should have a subCategory", split.hasSubCategory());
    	assertEquals("Expected subCategory after the split", "blue", split.getSubCategory());
    	assertEquals("Expected category after the split", "DistrictRequest", split.getCategory());
    }

    public void testNoMatch() throws Exception {
    	String category = "WebRequest.blue";
    	SubCategorySplitter splitter = new SubCategorySplitter("DistrictRequest\\.(.*)");

    	Split split = splitter.split(category);
    	
    	assertNotNull("Should always get back a split object", split);
    	
    	assertFalse("Should NOT have a subCategory", split.hasSubCategory());
    	assertNull("subCategory should be null", split.getSubCategory());
    	assertEquals("Expect category to be unchanged", category, split.getCategory());
    }
 
    public void testInnerMatch() throws Exception {
    	String category = "DistrictRequest.dist_0001.circulation";
    	SubCategorySplitter splitter = new SubCategorySplitter("DistrictRequest\\.([^\\.]*).*");

    	Split split = splitter.split(category);
    	
    	assertNotNull("Should always get back a split object", split);
    	
    	assertTrue("Should have a subCategory", split.hasSubCategory());
    	assertEquals("Expected subCategory after the split", "dist_0001", split.getSubCategory());
    	assertEquals("Expected category after the split", "DistrictRequest.circulation", split.getCategory());
    }
    
    public void testInvalidRegEx() throws Exception {
    	// If you create a splitter with an invalid regex it should 
    	// perform as a no-op function.  
    	
    	String category = "DistrictRequest.blue";
    	String badRegEx = "[DistrictRequest\\.(.*)";
    	SubCategorySplitter splitter = new SubCategorySplitter(badRegEx);

    	Split split = splitter.split(category);
    	
    	assertNotNull("Should always get back a split object", split);
    	
    	assertFalse("Should NOT have a subCategory since splitter was invalid", split.hasSubCategory() || (split.getCategory() == null));
    	assertEquals("Expected category since splitter was invalid", "DistrictRequest.blue", split.getCategory());
    }
    
    public void fixupPeriodsOnSubCategory() throws Exception {
    	String category = "DistrictRequest.dist_0001.circulation";
    	
    	// This is not a great pattern, because it will leave a subcategory that starts with a
    	// period.
    	String notAGreatPattern = "DistrictRequest(.*)";
    	SubCategorySplitter splitter = new SubCategorySplitter(notAGreatPattern);

    	Split split = splitter.split(category);
    	
    	assertEquals("Expected subCategory after the split", "dist_0001.circulation", split.getSubCategory());
    	assertEquals("Expected category after the split", "DistrictRequest", split.getCategory());
    }
    
    /**
     * For dynamic Appenders (like the InfluxAppender) Perfmon4j
     * prepends the pattern with "Interval." (for Interval monitors)
     * or "Snapshot." (for Snapshot monitors).  The user
     * does not have to account for this prefix in their pattern.
     * @throws Exception
     */
    public void testPatternAutoHandlesIntervalPrefix() throws Exception {
    	String category = "DistrictRequest.dist_0001";
    	String finalInfluxDbCategory = "Interval." + category; 
    	
    	String pattern = "DistrictRequest\\.(.*)";
    	SubCategorySplitter splitter = new SubCategorySplitter(pattern);

    	Split split = splitter.split(finalInfluxDbCategory);
    	
    	assertEquals("Expected subCategory after the split", "dist_0001", split.getSubCategory());
    	assertEquals("Expected category after the split", "Interval.DistrictRequest", split.getCategory());
   
    
    	// Now make sure the "autoHandle" function does not get in the way if the
    	// user specifies a full pattern that includes the "Interval." prefix
    	String patternWithPrefixIncluded = "Interval.DistrictRequest\\.(.*)";
    	splitter = new SubCategorySplitter(patternWithPrefixIncluded);
    	
    	split = splitter.split(finalInfluxDbCategory);
    	
    	assertEquals("Expected subCategory after the split", "dist_0001", split.getSubCategory());
    	assertEquals("Expected category after the split", "Interval.DistrictRequest", split.getCategory());
    }

    /**
     * For dynamic Appenders (like the InfluxAppender) Perfmon4j
     * prepends the pattern with "Interval." (for Interval monitors)
     * or "Snapshot." (for Snapshot monitors).  The user
     * does not have to account for this prefix in their pattern.
     * @throws Exception
     */
    public void testPatternAutoHandlesSnapshotPrefix() throws Exception {
    	String category = "DistrictRequest.dist_0001";
    	String finalInfluxDbCategory = "Snapshot." + category; 
    	
    	String pattern = "DistrictRequest\\.(.*)";
    	SubCategorySplitter splitter = new SubCategorySplitter(pattern);

    	Split split = splitter.split(finalInfluxDbCategory);
    	
    	assertEquals("Expected subCategory after the split", "dist_0001", split.getSubCategory());
    	assertEquals("Expected category after the split", "Snapshot.DistrictRequest", split.getCategory());
   
    
    	// Now make sure the "autoHandle" function does not get in the way if the
    	// user specifies a full pattern that includes the "Snapshot." prefix
    	String patternWithPrefixIncluded = "Snapshot.DistrictRequest\\.(.*)";
    	splitter = new SubCategorySplitter(patternWithPrefixIncluded);
    	
    	split = splitter.split(finalInfluxDbCategory);
    	
    	assertEquals("Expected subCategory after the split", "dist_0001", split.getSubCategory());
    	assertEquals("Expected category after the split", "Snapshot.DistrictRequest", split.getCategory());
    }

    public void fixupPeriodsOnConcatBeforeAfterSubCategory() throws Exception {
    	String category = "DistrictRequest.dist_0001.circulation";
    	
    	// This is NOT a great pattern so we will help.
    	// It will result in multiple periods when the two
    	// sections before and after the match are concatenated.
    	// What the user probably wants out of this is:
    	// 		category = DistrictRequest.circulation
    	// 		subCategory =  dist001
    	// But what they would get, if we didn't fix up the result
    	// after the regex was applied, would be:
    	// 		category = DistrictRequest..circulation
    	// 		subCategory =  dist001
    	
    	String notAGreatPattern = "DistrictRequest\\.(dist_\\d{4}).*";
    	SubCategorySplitter splitter = new SubCategorySplitter(notAGreatPattern);

    	Split split = splitter.split(category);
    	
    	assertEquals("Expected subCategory after the split", "dist_0001", split.getSubCategory());
    	assertEquals("Expected category after the split", "DistrictRequest.circulation", split.getCategory());
    }
}
