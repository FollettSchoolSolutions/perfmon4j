package org.perfmon4j.reporter.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.perfmon4j.reporter.gui.ToolTipInfo;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.vo.ResponseInfo;

public class IntervalCategory extends Category<IntervalCategory> implements ToolTipInfo {
	private final ReportSQLConnection reportConnection;
	private final Long databaseID;
    
	static public enum DataSeries {
		MAX_THREADS("MaxThread", "Maximum Threads"),
		THROUGH_PUT("Throughput", "Throughput/Minute"),
		HITS("Hits", "Total Hits"),
		COMPLETIONS("Completions", "Total Completions"),
		MAX_DURATION("Max Duration", "Max Duration/Millis"),
		MIN_DURATION("Min Duration", "Min Duration/Millis"),
		AVERAGE_DURATION("Min Duration", "Min Duration/Millis");
		
		private final String name;
		private final String description;
		
		DataSeries(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}
	}
	
	
	public IntervalCategory(String name, Long databaseID, ReportSQLConnection reportConnection) {
		super(name);
		this.databaseID = databaseID;
		this.reportConnection = reportConnection;
	}
	
	public static IntervalCategory getOrCreate(IntervalCategory root, 
		String fullPath, Long databaseID, ReportSQLConnection reportConnection) {
		
		IntervalCategory currentChild = root;
		
		StringTokenizer t = new StringTokenizer(fullPath, ".");
		while (t.hasMoreElements()) {
			String name = (String)t.nextElement();
			IntervalCategory curr = currentChild.getChild(name);
			if (curr == null) {
				curr = new IntervalCategory(name, t.hasMoreElements() ? null : databaseID, reportConnection);
				currentChild.addCategory(curr);
			}
			currentChild = curr;
		}
		
		return currentChild;
	}
	
	public Long getDatabaseID() {
		return databaseID;
	}

	public String getToolTip() {
		if (databaseID != null) {
			return databaseID + "";
		} else {
			return null;
		}
	}
	
	@Override
	public P4JTreeNode.Type getType() {
		return P4JTreeNode.Type.P4JIntervalCategory;
	}	
	
	
	public TimeSeries[] getTimeSeries() throws SQLException {
		TimeSeries throughput = new TimeSeries("Throughput");
		throughput.setDescription("Throughput/Minute");
		
		TimeSeries avgDur = new TimeSeries("AverageDuration");
		avgDur.setDescription("Avg Duration(Millis)");
		
		Connection conn = null;
		try {
			conn =  reportConnection.createJDBCConnection();
			List<ResponseInfo> list = JDBCHelper.queryResponseInfo(conn, reportConnection.getSchema(), this.databaseID);
			Iterator<ResponseInfo> itr = list.iterator();
			while (itr.hasNext()) {
				ResponseInfo r = itr.next();
				Minute m = new Minute(new Date(r.getStartTime()));
				throughput.add(m, r.getThroughput());
				avgDur.add(m, r.getAverageDuration());
			}
		} finally {
			JDBCHelper.closeNoThrow(conn);
		}
		return new TimeSeries[]{throughput, avgDur};
	}
	
	public static IntervalCategory safeCast(P4JTreeNode node) {
		if (node instanceof IntervalCategory) {
			return (IntervalCategory)node;
		} else {
			return null;
		}
	}
}
