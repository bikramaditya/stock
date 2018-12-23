import java.util.ArrayList;

public class Trader {
	public void placeTrades(ArrayList<Stock> watchList)
	{
		Util util = new Util();
		util.Logger.log(0, "Starting trade placing");
		
		for (Stock stock : watchList) {
			boolean isValid = util.isTradePredictionValid(stock);
			
			util.Logger.log(0, "Placing trade for "+stock.MKT+"--"+ stock.SYMBOL+"-- Is Valid=");
			placeTrade(stock.MKT, stock.SYMBOL);
		}
		
		util.Logger.log(0, "Trades placed");
	}

	private void placeTrade(String mKT, String sYMBOL) 
	{
		
	}
}