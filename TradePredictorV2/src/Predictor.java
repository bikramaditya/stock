import java.time.LocalDate;
import java.util.Map;

public class Predictor 
{
	static Util util = null;
	public Predictor()
	{
		if(util==null)
		{
			util = new Util();	
		}
	}
	public void PredictAndStore(Stock stock, int days, int candles) 
	{
		String MKT = stock.MKT;
		String SYMBOL = stock.SYMBOL;
		
		Map<LocalDate, Double> maDays = util.calcMovingAverage(MKT, SYMBOL, days, candles, "HIGH");

		Map<LocalDate, Double> highData = util.getColumnValues(MKT, SYMBOL, "HIGH", days);

		Map<LocalDate, Double> predictedhighDays = util.getPredictionSeries(stock, "HIGH", maDays, days);

		Map<LocalDate, Double> predictedLOWDays = util.getPredictionSeries(stock, "LOW", maDays, days);

		Map<LocalDate, Double> lowData = util.getColumnValues(MKT, SYMBOL, "LOW", days);
		
		if(predictedhighDays!=null && predictedLOWDays!=null)
		{
			util.calcAndStoreMidline(MKT, SYMBOL, highData, predictedhighDays, lowData, predictedLOWDays);	
		}			
	}
}
