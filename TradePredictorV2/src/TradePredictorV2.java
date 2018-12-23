import java.util.ArrayList;

public class TradePredictorV2 
{
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception 
	{
		Util util = new Util();
		util.Logger.log(0, "Starting predicting");
		
		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		
		if (isTradingDay) 
		{
			util.getNifty();
			util.getGlobalIndices();
			util.getGSXNifty();
			ArrayList<Stock> watchList = util.getWatchList();
			watchList = util.populateSectorIndices(watchList, "NSE");
			
			util.loadAllStockData(watchList);
			
			for (Stock stock : watchList) 
			{
				util.Logger.log(0, "Analyzing..."+stock.SYMBOL);
			    
			    try
			    {
			    	Predictor predictor = new Predictor();
			    	predictor.PredictAndStore(stock,60,5);
			    }
			    catch(Exception e)
			    {
			    	e.printStackTrace();
			    }
			}
			
			util.Logger.log(0, "Analysis complete");
			Trader trader = new Trader(); 
			trader.placeTrades(watchList);
		}
		else
		{
			util.Logger.log(0, "Not a trading day...exiting");			
		}
	}
}