
package action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zerodhatech.models.Instrument;
import entity.Stock;
import uitls.DAO;
import uitls.DownloadWorker;
import uitls.Kite;
import uitls.Util;

public class TickerDownloaderV1 {
	static DAO dao = null;
	static Util util = null;
	static final long ONE_MINUTE_IN_MILLIS=60000;// milli seconds
	
	public static void main(String[] args) throws InterruptedException {
		util = new Util();

		Util.Logger.log(0, "Starting Downloader");

		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		boolean isMarketOpen = util.isMarketOpen();

		if (isTradingDay)
		{
			try 
			{
				dao = new DAO();
				ArrayList<Stock> watchList = dao.getWatchList();
				
				Kite kite = new Kite();
				
				if(isMarketOpen)
				{
					ArrayList<Long> tokens = new ArrayList<>();
	                
					for (Stock stock : watchList) 
					{
						tokens.add(Long.parseLong(stock.instrument_token));
					}
					Thread t = new Thread(new DownloadWorker(kite,tokens));
				    t.start();
				}
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
		        Util.Logger.log(1, e.getMessage());
			}
			
			while(isMarketOpen)
			{
				isMarketOpen = util.isMarketOpen();
				Thread.sleep(5000);
			}
			
			System.out.println("Day Over"+new Date());
	        Util.Logger.log(0, "Day Over"+new Date());
	        System.exit(0);
		}
	}

	@SuppressWarnings("unused")
	private static void storeInstrumentCodes(String[] stockList, Kite kite) 
	{
		List<Instrument> instruments = kite.getAllInstruments();
		
		for(String stockName : stockList)
		{
			String exc = stockName.split(":")[0];
			String sym = stockName.split(":")[1];
			for(Instrument inst : instruments)
			{
				if(inst.exchange.equals(exc))
				{
					if(inst.tradingsymbol.equals(sym))
					{
						long inst_token = inst.instrument_token;
						dao.updateInstrumentToken(exc,sym,inst_token);
					}
				}
			}
		}
	}
}