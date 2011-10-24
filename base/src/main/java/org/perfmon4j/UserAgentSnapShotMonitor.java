/*
 *	Copyright 2008-2011 Follett Software Company 
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.UserAgentParser;
import org.perfmon4j.util.UserAgentVO;


public class UserAgentSnapShotMonitor extends SnapShotMonitor {
    private static final int MAX_CONCURRENT_MONITORS = 10;
    
    private static final Object DATA_LOCK_TOKEN = new Object();
    private static final UserAgentData data[] = new UserAgentData[MAX_CONCURRENT_MONITORS];
   
    
    public static void insertUserAgent(String userAgentString) {
        UserAgentVO vo = null;
    
        for (int i = 0; i < data.length; i++) {
            if (data[i] != null) {
                if (vo == null) {
                    // Lazy parsing of the user-agent string... No point doing it
                    // if we don't have any monitors listening.
                    vo = UserAgentParser.parseUserAgentString(userAgentString);
                }
                data[i].logUserAgent(vo);
            }
        }
    }
    
    public UserAgentSnapShotMonitor(String name) {
        super(name);
    }

    public SnapShotData initSnapShot(long currentTimeMillis) {
        return new UserAgentData(currentTimeMillis);
    }
    
    public SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis) {
        ((UserAgentData)data).stop(currentTimeMillis);
        return data;
    }
    
    public void deInit() {
        synchronized (DATA_LOCK_TOKEN) {
            for (int i = 0; i < data.length; i++) {
                data[i] = null;
            }
        }
        super.deInit();
    }
    
    public static class Counter {
        private int count = 0;
        
        public int getCount() {
            return count;
        }
        
        public void setCount(int count) {
            this.count = count;
        }
        
        public void inc() {
            count++;
        }
        
        public String toString() {
            return Integer.toString(count);
        }
    }
    
    
    
    
    public static class UserAgentData extends SnapShotData implements SQLWriteable {
        private final Integer offset;
        private final long startTime;
        private long stopTime = -1;
        
        private final Object mapToken = new Object();
        private final Map<UserAgentVO, Counter> userAgentMap = new HashMap();
     
        
        public static UserAgentData newUserAgentData_TEST_ONLY(long startTime, long endTime, String userAgentStrings[]) {
            UserAgentData result = new UserAgentData(startTime);
            
            for (int i = 0; i < userAgentStrings.length; i++) {
                result.logUserAgent(UserAgentParser.parseUserAgentString(userAgentStrings[i]));
            }
            result.stop(endTime);
            
            return result;
        }
        
        
        UserAgentData(long startTime) {
            this.startTime = startTime;
            Integer offsetToUse = null;
            synchronized (DATA_LOCK_TOKEN) {
                for (int i = 0; i < data.length && (offsetToUse == null); i++) {
                    if (data[i] == null) {
                        data[i] = this;
                        offsetToUse = new Integer(i);
                    }
                } 
                offset = offsetToUse;
            }
        }
        
        private void logUserAgent(UserAgentVO vo) {
            if (stopTime < 0) {
                Counter counter = null;
                synchronized(mapToken) {
                    counter = userAgentMap.get(vo);
                    if (counter == null) {                
                        counter = new Counter();
                        userAgentMap.put(vo, counter);            
                    } 
                }
                counter.inc();
            }
        }
        
        private void stop(long stopTime) {
            this.stopTime = stopTime;
            
            // Take me out of the array...
            if (offset != null) {
                data[offset.intValue()] = null;
            }
        }
        
        
        public String toAppenderString() {
            StringBuilder result = new StringBuilder(String.format(
                "\r\n********************************************************************************\r\n" +
                "%s\r\n" +
                "%s -> %s\r\n", 
                getName(),
                MiscHelper.formatTimeAsString(new Long(startTime)),
                MiscHelper.formatTimeAsString(new Long(stopTime))));
    
            Iterator itr = userAgentMap.entrySet().iterator();    
            while (itr.hasNext()) {
                Map.Entry entry = (Map.Entry)itr.next();
                result.append(entry.getKey())
                    .append(" = ")
                    .append(entry.getValue())
                    .append("\r\n");
            }
            result.append("********************************************************************************");
            
            return result.toString();
        }

        public Map<UserAgentVO, Counter> getUserAgentMap() {
            return userAgentMap;
        }

        private void addOrUpdateOccuranceRow(Connection conn, String schema, Timestamp myTime, 
        	Long browserID, Long browserVersionID, Long osID, Long osVersionID, 
        	int count) throws SQLException {
        	
        	final String tableName = schema + "P4JUserAgentOccurance";
        	
        	final String sqlCount = "SELECT * FROM " + tableName +
        			" WHERE CollectionDate=? " +
        			" AND BrowserID=? " +
        			" AND BrowserVersionID=? " +
        			" AND OSID=? " +
        			" AND OSVersionID=?";
        	
        	PreparedStatement ps = null;
        	ResultSet rs = null;
        	try {
        		ps = conn.prepareStatement(sqlCount);
        		ps.setTimestamp(1, myTime);
        		ps.setLong(2, browserID);
        		ps.setLong(3, browserVersionID);
        		ps.setLong(4, osID);
        		ps.setLong(5, osVersionID);
        		
        		rs = ps.executeQuery();
        		if (!rs.next()) {
        			JDBCHelper.closeNoThrow(rs);
        			rs = null;
        			JDBCHelper.closeNoThrow(ps);
        			// Record does not exist... need to insert it.
        			
        			final String sqlInsert = "INSERT INTO " + tableName +
        				" (CollectionDate, BrowserID, BrowserVersionID, OSID, OSVersionID) " +
        				" VALUES(?, ?, ?, ?, ?)";
        			ps = conn.prepareStatement(sqlInsert);
        			ps.setTimestamp(1, myTime);
        			ps.setLong(2, browserID);
        			ps.setLong(3, browserVersionID);
        			ps.setLong(4, osID);
        			ps.setLong(5, osVersionID);
        			ps.executeUpdate();
        		}
    			JDBCHelper.closeNoThrow(rs);
    			rs = null;
        		JDBCHelper.closeNoThrow(ps);
        		// Now we know the record exists... 
        		// Update it;
        		final String sqlUpdate = "UPDATE " + tableName +
					" SET RequestCount=RequestCount+"  + count +
					" WHERE CollectionDate=? " +
        			" AND BrowserID=? " +
        			" AND BrowserVersionID=? " +
        			" AND OSID=? " +
        			" AND OSVersionID=?";
        		
        		ps = conn.prepareStatement(sqlUpdate);
        		ps.setTimestamp(1, myTime);
        		ps.setLong(2, browserID);
        		ps.setLong(3, browserVersionID);
        		ps.setLong(4, osID);
        		ps.setLong(5, osVersionID);
        		ps.executeUpdate();
        	} finally {
        		JDBCHelper.closeNoThrow(rs);
        		JDBCHelper.closeNoThrow(ps);
        	}
        }
        
        
        private String nullToUnknown(String v) {
        	return v != null ? v : "[UNKNOWN]";
        }
        
		public void writeToSQL(Connection conn, String schema) throws SQLException {
			String s = (schema == null) ? "" : (schema + ".");
			
			
			for (Map.Entry<UserAgentVO, Counter> entry: userAgentMap.entrySet()) {
				UserAgentVO vo = entry.getKey();
				String browserName = nullToUnknown(vo.getBrowserName());
				String browserVersion = nullToUnknown(vo.getBrowserVersion());
				String osName = nullToUnknown(vo.getOsName());
				String osVersion = nullToUnknown(vo.getOsVersion());
				int count = entry.getValue().count;
			
				
				Timestamp now = new Timestamp(MiscHelper.calcDateOnlyFromMillis(stopTime > 0 ? stopTime : startTime));

				Long browserID = JDBCHelper.simpleGetOrCreate(conn, s + "P4JUserAgentBrowser", "BrowserID", 
						"BrowserName", browserName);
				Long browserVersionID = JDBCHelper.simpleGetOrCreate(conn, s + "P4JUserAgentBrowserVersion", "BrowserVersionID", 
						"BrowserVersion", browserVersion);
				Long osID = JDBCHelper.simpleGetOrCreate(conn, s + "P4JUserAgentOS", "OSID", 
						"OSName", osName);
				Long osVersionID = JDBCHelper.simpleGetOrCreate(conn, s + "P4JUserAgentOSVersion", "OSVersionID", 
						"OSVersion", osVersion);
				addOrUpdateOccuranceRow(conn, s, now, browserID, browserVersionID, osID, 
						osVersionID, count);
			}
		}
    }
    
   
}
