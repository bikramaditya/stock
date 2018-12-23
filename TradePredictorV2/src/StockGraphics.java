import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;

public class StockGraphics extends ApplicationFrame {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("deprecation")
	public StockGraphics(String applicationTitle, String chartTitle, String MKT, String SYMBOL, int days, int candles,
			String column) {
		super(applicationTitle);

		TimeSeriesCollection dataset = createDataset(MKT, SYMBOL, days, candles, column);

		JFreeChart chart = ChartFactory.createTimeSeriesChart(SYMBOL+" Stock Prediction - High", 
				"Date", 
				"MA", 
				dataset,true,true,true);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.darkGray);
		plot.setDomainGridlinePaint(Color.darkGray);
		plot.setOutlineVisible(false);
		
		/*
		XYItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0, Color.blue);
		renderer.setSeriesPaint(1, Color.red);
		renderer.setSeriesPaint(2, Color.green);
		renderer.setSeriesPaint(3, Color.pink);
		 */
		
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setTickUnit(new DateTickUnit(DateTickUnit.DAY, 1, new SimpleDateFormat("MM-dd")));
		axis.setVerticalTickLabels(true);
		axis.setTickLabelFont(new Font("Dialog", Font.PLAIN, 8));

		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setBackground(Color.white);
		chartPanel.setPreferredSize(new java.awt.Dimension(1800, 700));
		setContentPane(chartPanel);
	}

	private TimeSeriesCollection createDataset(String MKT, String SYMBOL, int days, int candles, String column) {
		
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		Util util = new Util();
		Map<LocalDate, Double> maDays = util.calcMovingAverage(MKT, SYMBOL, days, candles, column);
		TimeSeries seriesMA = new TimeSeries(SYMBOL+"MA");
		maDays.forEach((key, value) -> {
			seriesMA.add(new Day(key.getDayOfMonth(), key.getMonthValue(), key.getYear()), value);
		});
		dataset.addSeries(seriesMA);

		Map<LocalDate, Double> highData = util.getColumnValues(MKT, SYMBOL, column, days);
		TimeSeries seriesHI = new TimeSeries("High");
		highData.forEach((key, value) -> {
			if (value > 0)
				seriesHI.add(new Day(key.getDayOfMonth(), key.getMonthValue(), key.getYear()), value);
		});
		dataset.addSeries(seriesHI);

		/*
		
		Map<LocalDate, Double> predictedhighDays = util.getPredictionSeries(MKT, SYMBOL, "HIGH", maDays, days);
		TimeSeries seriesPredictedHI = new TimeSeries("HI Predicted");
		predictedhighDays.forEach((key, value) -> {
			if (value > 0)
				seriesPredictedHI.add(new Day(key.getDayOfMonth(), key.getMonthValue(), key.getYear()), value);
		});
		dataset.addSeries(seriesPredictedHI);

		Map<LocalDate, Double> predictedLOWDays = util.getPredictionSeries(MKT, SYMBOL, "LOW", maDays, days);
		TimeSeries seriesPredictedLOW = new TimeSeries("LOW Predicted");
		predictedLOWDays.forEach((key, value) -> {
			if (value > 0)
				seriesPredictedLOW.add(new Day(key.getDayOfMonth(), key.getMonthValue(), key.getYear()), value);
		});
		dataset.addSeries(seriesPredictedLOW);

		
		Map<LocalDate, Double> predictedMidLine = util.getMidline(MKT, SYMBOL, predictedhighDays, predictedLOWDays);
		TimeSeries seriesPredictedMidLine = new TimeSeries("Midline");
		predictedMidLine.forEach((key, value) -> {
			if (value > 0)
				seriesPredictedMidLine.add(new Day(key.getDayOfMonth(), key.getMonthValue(), key.getYear()), value);
		});
		dataset.addSeries(seriesPredictedMidLine);
		*/
		
		
		Map<LocalDate, Double> lowDays = util.getColumnValues(MKT, SYMBOL, "LOW", days);
		TimeSeries seriesLO = new TimeSeries("Low");
		lowDays.forEach((key, value) -> {
			if (value > 0)
				seriesLO.add(new Day(key.getDayOfMonth(), key.getMonthValue(), key.getYear()), value);
		});
		dataset.addSeries(seriesLO);
		
		return dataset;
	}
}
