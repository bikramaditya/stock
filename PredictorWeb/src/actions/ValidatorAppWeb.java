package actions;
import java.util.ArrayList;
import java.util.Iterator;

import entities.Stock;
import utils.DAO;
import utils.Util;

public class ValidatorAppWeb
{
	DAO dao = null;
	public void validateAll() throws Exception 
	{
		
		Util util = new Util();
		util.Logger.log(0, "Starting Validator");
		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		
		if(isTradingDay)
		{
			ArrayList<Stock> watchList = util.getWatchList();
			
			final Iterator<Stock> itr3 = watchList.iterator();
			while(itr3.hasNext())
			{
				Stock stock = itr3.next();
				
			    Validator validator = new Validator();
			    validator.validate(stock);
			    util.Logger.log(0, "Analysis complete for "+stock.SYMBOL);
			}
		}
		else
		{
			util.Logger.log(0, "Not a trading day");
		}
		
	}
}