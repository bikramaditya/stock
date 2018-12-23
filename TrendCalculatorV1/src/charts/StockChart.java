package charts;

import java.awt.Color;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import entity.HistoricalDataEx;

public class StockChart extends ApplicationFrame implements Runnable{
	
	private static final long serialVersionUID = 1L;
	private String title = "";
	private ArrayList<HistoricalDataEx> historicalData;
	private String[] columns;
	
	public StockChart(final String title, ArrayList<HistoricalDataEx> historicalData , String[] columns) throws ParseException {
		super(title);
		
		this.title = title;
		this.historicalData=historicalData;
		this.columns = columns;
		
		final TimeSeriesCollection dataset = createDataset();
		final JFreeChart chart = createChart(dataset);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(1200, 600));
		setContentPane(chartPanel);
	}

	@SuppressWarnings("deprecation")
	private TimeSeriesCollection createDataset() throws ParseException {
		SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		final TimeSeries  series1 = new TimeSeries (columns[0]);
		final TimeSeries  series2 = new TimeSeries (columns[1]);
		
		
		for (HistoricalDataEx candle : historicalData) {
			Date time = formatter.parse(candle.timeStamp);
			int year = 1900+time.getYear();
			Minute minute = new Minute(time.getMinutes(),time.getHours(),time.getDate(),1+time.getMonth(),year);
			
			if(minute.getHourValue() >= 9 && minute.getHourValue() <= 15 && time.getDate()==30)
			{
				try {
					Class<? extends HistoricalDataEx> clazz = candle.getClass();
				    
					Field field1 = clazz.getField(columns[0]); 
					double val1 = (double) field1.get(candle);

					Field field2 = clazz.getField(columns[1]); 
					double val2 = (double) field2.get(candle);
					
					//if(val1 > 0 && val2 >0)
					{
						series1.addOrUpdate(minute, val1);
						series2.addOrUpdate(minute, val2);	
					}
				}catch(Exception e) {
					e.printStackTrace();
				}	
			}					
		}
		
		final TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(series1);
		dataset.addSeries(series2);

		return dataset;
	}

	/**
	 * Creates a chart.
	 * 
	 * @param dataset the data for the chart.
	 * 
	 * @return a chart.
	 */
	private JFreeChart createChart(final XYDataset dataset) {

		final JFreeChart chart = ChartFactory.createTimeSeriesChart
				(
				this.title, // chart title
				"Time", // x axis label
				"Price", // y axis label
				dataset
		);

		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
		chart.setBackgroundPaint(Color.white);
		
		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);
		
		plot.setBackgroundPaint(Color.lightGray);
		// plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);

		String dateFormat = "dd/MM HH:mm";
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat(dateFormat));
		
		//plot.getDomainAxis().set setDateFormatOverride(new SimpleDateFormat("HH:mm"));
		
		// change the auto tick unit selection to integer units only...
		//final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		//rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// OPTIONAL CUSTOMISATION COMPLETED.

		return chart;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
