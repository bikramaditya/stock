import java.time.LocalDate;
import java.util.Map;

public class Predictor {
	public void PredictAndStore(String MKT, String SYMBOL, int days, int candles) 
	{
		Util util = new Util();
		Map<LocalDate, Double> maDays = util.calcMovingAverage(MKT, SYMBOL, days, candles, "HIGH");

		Map<LocalDate, Double> highData = util.getColumnValues(MKT, SYMBOL, "HIGH", days);

		Map<LocalDate, Double> predictedhighDays = util.getPredictionSeries(MKT, SYMBOL, "HIGH", maDays, days);

		Map<LocalDate, Double> predictedLOWDays = util.getPredictionSeries(MKT, SYMBOL, "LOW", maDays, days);

		Map<LocalDate, Double> lowData = util.getColumnValues(MKT, SYMBOL, "LOW", days);
		
		util.calcAndStoreMidline(MKT, SYMBOL, highData, predictedhighDays, lowData, predictedLOWDays);		
	}
}
