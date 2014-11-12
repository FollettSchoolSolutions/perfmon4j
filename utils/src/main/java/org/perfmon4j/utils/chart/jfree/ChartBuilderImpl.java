package org.perfmon4j.utils.chart.jfree;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class ChartBuilderImpl {
	private final Random random = new Random();
	
	private Date addRandomMinute(TimeSeries througputSeries, TimeSeries maxThreadsSeries, Date date) {
		througputSeries.add(new Minute(date), random.nextDouble() * 12000);
		maxThreadsSeries.add(new Minute(date), random.nextDouble() * 600);

		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, -1);
		return cal.getTime();
	}
	
	public void buildChart() throws Exception {
		TimeSeries throughputSeries = new TimeSeries("Throughput", Minute.class);
		TimeSeries maxThreadsSeries = new TimeSeries("MaxThreads", Minute.class);
		
		Date date = new Date();
		for (int i = 0; i < 480; i++) {
			date = addRandomMinute(throughputSeries, maxThreadsSeries, date);
		}
		
		TimeSeriesCollection througputDataSet = new TimeSeriesCollection();
		througputDataSet.addSeries(throughputSeries);
		
		
		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				"FollettShelf:WebRequest", // title
				"Time", // x-axis label
				"Throughput", // y-axis label
				througputDataSet, // data
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
				);
		XYPlot plot = chart.getXYPlot();
		
		TimeSeriesCollection maxThreadsDataSet = new TimeSeriesCollection();
		maxThreadsDataSet.addSeries(maxThreadsSeries);
		
		ValueAxis axis2 = new NumberAxis("Max Theads");
		plot.setRangeAxis(1, axis2);

		plot.setDataset(1, maxThreadsDataSet);
		plot.mapDatasetToRangeAxis(1, 1);
		
		XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
		renderer2.setSeriesShapesVisible(0, Boolean.FALSE);
		
		plot.setRenderer(1, renderer2);
		
		ChartUtilities.saveChartAsPNG(new File("/media/sf_shared/t/test.png"), chart, 600, 400);
	}
	
	public static void main(String args[]) throws Exception {
		new ChartBuilderImpl().buildChart();
	}
}
