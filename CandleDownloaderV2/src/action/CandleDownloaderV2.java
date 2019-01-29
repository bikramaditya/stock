
package action;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import com.zerodhatech.models.Instrument;
import entity.Stock;
import uitls.DAO;
import uitls.DownloadWorker;
import uitls.Kite;
import uitls.Util;

public class CandleDownloaderV2 {
	static DAO dao = null;
	static Util util = null;
	static final long ONE_MINUTE_IN_MILLIS=60000;// milli seconds
	//static ArrayList<Thread> threads = new ArrayList<Thread>();
	
	public static void main(String[] args) throws InterruptedException {
		util = new Util();

		Util.Logger.log(0, "Starting Downloader");

		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		boolean isMarketOpen = util.isMarketOpen();
		
		if (isTradingDay && isMarketOpen)
		{
			try 
			{
				dao = new DAO();
				ArrayList<Stock> watchList = dao.getWatchList();
				
				Kite kite = new Kite();
				
				System.out.println("Starting new dw ");
		        Util.Logger.log(0, "Starting new dw ");
		        
				Calendar date = Calendar.getInstance();
				long t= date.getTimeInMillis();			
				Date to = new Date(t);
				
				for (Stock stock : watchList) {
					Runnable worker = new DownloadWorker(kite,stock,to);
		            
					Thread thread = new Thread(worker);
					thread.start();
		            Thread.sleep(501);
				}
				Thread.sleep(2000);
				System.out.println("End new dw ");
		        Util.Logger.log(0, "End new dw ");
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
		        Util.Logger.log(1, e.getMessage());
			}
			
			System.out.println("Complete dw"+new Date());
	        Util.Logger.log(0, "Complete dw"+new Date());
	        System.exit(0);
		}
	}
//
//	private static void killThreads() 
//	{
//		for(Thread thread : threads)
//		{
//			//((Object) thread).getDispatcher().getExecutor().shutdown();
//		}
//	}

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