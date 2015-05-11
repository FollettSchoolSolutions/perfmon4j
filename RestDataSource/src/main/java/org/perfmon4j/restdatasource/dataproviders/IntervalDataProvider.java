package org.perfmon4j.restdatasource.dataproviders;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.perfmon4j.restdatasource.DataProvider;
import org.perfmon4j.restdatasource.RestImpl.SystemID;
import org.perfmon4j.restdatasource.data.AggregationMethod;
import org.perfmon4j.restdatasource.data.CategoryTemplate;
import org.perfmon4j.restdatasource.data.Field;
import org.perfmon4j.restdatasource.data.query.advanced.ResultAccumulator;
import org.perfmon4j.restdatasource.util.SeriesField;
import org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import org.perfmon4j.restdatasource.util.aggregators.SumAggregatorFactory;
import org.perfmon4j.restdatasource.util.aggregators.decorator.ColumnValueFilterFactory;
import org.perfmon4j.util.JDBCHelper;

public class IntervalDataProvider extends DataProvider {
	private static final String TEMPLATE_NAME = "Interval";
	private final IntervalTemplate categoryTemplate;
	
	public IntervalDataProvider() {
		super(TEMPLATE_NAME);
		categoryTemplate = new IntervalTemplate(TEMPLATE_NAME);
	}

	private String toSQLSet(Set<String> values, boolean quoteForSQL) {
		boolean firstElement = true;
		StringBuilder builder = new StringBuilder("( ");
		
		for(String s : values) {
			if (!firstElement) {
				builder.append(", ");
			}
			firstElement = false;
			if (quoteForSQL) {
				builder.append("'");
			}
			builder.append(s);
			if (quoteForSQL) {
				builder.append("'");
			}
		}
		builder.append(" )");
		
		return builder.toString();
	}
	
	private String buildSystemIDList(SeriesField fields[]) {
		Set<String> systemIDs = new HashSet<String>();
		
		for (SeriesField field : fields) {
			for (SystemID id : field.getSystems()) {
				systemIDs.add(Long.toString(id.getID()));
			}
		}
		
		return toSQLSet(systemIDs, false);
	}

	private String buildSchemaPrefix(ResultAccumulator accumulator) {
		String result = "";
		String schema = accumulator.getSchema();
		
		if (schema != null && !"".equals(schema)) {
			result = schema + ".";
		}
		
		return result;
	}
	
	private String normalizeCategoryName(String categoryName) {
		return categoryName.replaceFirst("Interval\\.", "");
	}
	
	
	private String buildCategoryNameSet(SeriesField[] fields) {
		Set<String> categories = new HashSet<String>();
		
		for (SeriesField field : fields) {
			String shortenedCategoryName = normalizeCategoryName(field.getCategory().getName());
			categories.add(shortenedCategoryName);
		}
		
		return toSQLSet(categories, true);
	}
	
	private String buildSelectListAndPopulateAccumulators(ResultAccumulator accumulator, SeriesField []fields) {
		String databaseColumn = "AverageDuration";
		String selectList = ", pid." + databaseColumn;

		for (SeriesField field : fields) {
			String shortenedCategoryName = normalizeCategoryName(field.getCategory().getName());
			AggregatorFactory delegate = new SumAggregatorFactory(databaseColumn, true);
			AggregatorFactory factory = new ColumnValueFilterFactory(delegate, "CategoryName", new String[]{shortenedCategoryName});
			
			field.setFactory(factory);
			accumulator.addSeries(field);
		}
		
		return selectList;
	}
	
	
	@Override
	public void processResults(ResultAccumulator accumulator, SeriesField[] fields, long startTime, 
			long endTime) throws SQLException {
		String schemaPrefix = buildSchemaPrefix(accumulator);
		String selectList = buildSelectListAndPopulateAccumulators(accumulator, fields);
		
		String query =
			"SELECT pid.EndTime, cat.categoryName, pid.systemID " + selectList + "\r\n"
			+ "FROM " + schemaPrefix + "P4JIntervalData pid\r\n"
			+ "JOIN " + schemaPrefix + "P4JCategory cat ON cat.categoryID = pid.CategoryID\r\n"
			+ "WHERE pid.systemID IN " + buildSystemIDList(fields) + "\r\n"
			+ "AND cat.categoryName IN "+ buildCategoryNameSet(fields) + "\r\n"
			+ "AND pid.EndTime >= ?\r\n"
			+ "AND pid.EndTime <= ?\r\n";
System.out.println(query);		
		Connection conn = accumulator.getConn();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(query);
			stmt.setTimestamp(1, new Timestamp(startTime));
			stmt.setTimestamp(2, new Timestamp(endTime));
			rs = stmt.executeQuery();
			while (rs.next()) {
				accumulator.accumulateResults(TEMPLATE_NAME, rs);
			}
		} finally {
			JDBCHelper.closeNoThrow(stmt);
			JDBCHelper.closeNoThrow(rs);
		}
	}

	@Override
	public CategoryTemplate getCategoryTemplate() {
		return categoryTemplate;
	}
	
	
	private static class IntervalTemplate extends CategoryTemplate {

		private IntervalTemplate(String templateName) {
			super(templateName, buildFields());
		}
		
		private static final Field[] buildFields() {
			List<Field> fields = new ArrayList<Field>();
			
			fields.add(new Field("maxActiveThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM));
			fields.add(new Field("maxDuration", AggregationMethod.DEFAULT, AggregationMethod.MAX));
			fields.add(new Field("minDuration", AggregationMethod.DEFAULT, AggregationMethod.MIN));
			fields.add(new Field("throughputPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL));
			fields.add(new Field("averageDuration", AggregationMethod.DEFAULT_WITH_NATURAL));
			fields.add(new Field("medianDuration", AggregationMethod.DEFAULT, AggregationMethod.AVERAGE));
			fields.add(new Field("standardDeviation", AggregationMethod.DEFAULT_WITH_NATURAL));
			fields.add(new Field("sqlMaxDuration", AggregationMethod.DEFAULT, AggregationMethod.MAX));
			fields.add(new Field("sqlLMinDuration", AggregationMethod.DEFAULT, AggregationMethod.MIN));
			fields.add(new Field("sqlAverageDuration", AggregationMethod.DEFAULT_WITH_NATURAL));
			fields.add(new Field("sqlStandardDeviation", AggregationMethod.DEFAULT_WITH_NATURAL));
			
			return fields.toArray(new Field[]{});
		}
	}
}
