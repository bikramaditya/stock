package actions;
import java.util.Set;
import java.util.TreeMap;

import entities.Stock;
import utils.Util;

public class Trader 
{
	Util util = null;
	public void placeTrades() throws Exception
	{
		util = new Util();
		util.Logger.log(0, "Starting trade placing");
		
		TreeMap<String, Stock> StocksWatchList = util.StocksWatchList;
		Set<String> keys = StocksWatchList.keySet();
		
		
		for (String key : keys) 
		{
			Stock stock = StocksWatchList.get(key);
			boolean isValid = util.isTradePredictionValid(stock);
			
			placeTrade(stock.MKT, stock.SYMBOL, isValid);
		}
		
		util.Logger.log(0, "Trades placed");
	}

	private void placeTrade(String MKT, String SYMBOL, boolean isValid) 
	{
		util.Logger.log(0, "Placing trade for "+MKT+"--"+ SYMBOL+"-- Is Valid="+isValid);
	}
}