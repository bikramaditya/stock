import java.util.ArrayList;
import java.util.Iterator;

public class ValidatorApp 
{
	DAO dao = null;
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws InterruptedException 
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