import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.zerodhatech.models.Quote;

public class DailyDataDownloaderApp {
	DAO dao = null;

	@SuppressWarnings("static-access")
	public static void main(String[] args) 
	{		
		Util util = new Util();
		
		util.Logger.log(0, "Starting Downloader");
		
		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		boolean isMarketOpen = util.isMarketOpen();
		boolean isDownloadHoursOpen = util.isDownloadHoursOpen();

		if (isTradingDay) 
		{
			DAO dao = new DAO(); 

			ArrayList<Stock> watchList = dao.getWatchList();

			String[] stockList = new String[watchList.size()];
			
			for(int i = 0; i < watchList.size();i++)
			{
				Stock stock = watchList.get(i);
				String sym = stock.MKT+":"+stock.SYMBOL;
				stockList[i]=sym;
			}
			
			if(stockList.length>0)
			{
				util.Logger.log(0, "Connecting to kite...");
				Kite kite = new Kite();
				LocalDateTime latestDateTime = LocalDateTime.now();
				Map<String, Quote> dataMap = kite.getAllQuotes(stockList);
				
				Set<String> keys = dataMap.keySet();
				
				for (String key : keys) 
				{
					Quote quote = dataMap.get(key);
					double price = quote.lastPrice;
					long volume = (long)quote.volumeTradedToday;
					String MKT = key.split(":")[0];
					String SYMBOL = key.split(":")[1];
					dao.insertToIntraDayPriceTable(MKT, SYMBOL, latestDateTime, price, volume);
					System.out.println("");
				}
			}
			else
			{
				util.Logger.log(1, "Error in getting watch list...");
			}
		}
		else
		{
			util.Logger.log(0, "Didn't download. isTradingDay="+isTradingDay+" isMarketOpen="+isMarketOpen+" and isDownloadHoursOpen="+isDownloadHoursOpen);
		}
	}
}