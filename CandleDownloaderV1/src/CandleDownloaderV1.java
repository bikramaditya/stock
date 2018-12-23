import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.zerodhatech.models.Instrument;
import entity.Stock;
import uitls.DAO;
import uitls.DownloadWorker;
import uitls.Kite;
import uitls.Util;

public class CandleDownloaderV1 {
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
			dao = new DAO();
			ArrayList<Stock> watchList = dao.getWatchList();
			
			Kite kite = new Kite();
			
			while(isMarketOpen)
			{
				int secs = LocalDateTime.now().getSecond();
				
				if(secs <= 2)
				{
					ExecutorService executor = Executors.newFixedThreadPool(2);

					Calendar date = Calendar.getInstance();				
					long t= date.getTimeInMillis();			
					Date to = new Date(t);
					
					for (Stock stock : watchList) {
						Runnable worker = new DownloadWorker(kite,stock,to);
			            executor.execute(worker);
			            Thread.sleep(501);
					}
					
					executor.shutdown();
			        while (!executor.isTerminated()) {
			        	Thread.sleep(100);
			        }
			        
			        isMarketOpen = util.isMarketOpen();
				}
				Thread.sleep(1000);
			}
			
			System.out.println("Day Over"+new Date());
	        Util.Logger.log(0, "Day Over"+new Date());
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