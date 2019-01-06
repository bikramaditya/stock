
package action;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
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
	
	public static void main(String[] args) throws InterruptedException {
		util = new Util();

		Util.Logger.log(0, "Starting Downloader");

		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		boolean isMarketOpen = util.isMarketOpen();

		if (isTradingDay || true)
		{
			try 
			{
				dao = new DAO();
				ArrayList<Stock> watchList = dao.getWatchList();
				
				Kite kite = new Kite();
				
		    	ConnectionFactory factory = new ConnectionFactory();
		    	factory.setHost("localhost");
		    	Connection connection = factory.newConnection();
		    	Channel channel = connection.createChannel();
				
				while(isMarketOpen || true)
				{
					int secs = LocalDateTime.now().getSecond();
					
					if(secs <= 2)
					{
						ExecutorService executor = Executors.newFixedThreadPool(2);

						Calendar date = Calendar.getInstance();
						long t= date.getTimeInMillis();			
						Date to = new Date(t);
						
						for (Stock stock : watchList) {
							Runnable worker = new DownloadWorker(channel,kite,stock,to);
				            executor.execute(worker);
				            Thread.sleep(500001);
						}
						
						executor.shutdown();
				        while (!executor.isTerminated()) {
				        	Thread.sleep(100);
				        }
				        
				        isMarketOpen = util.isMarketOpen();
					}
					Thread.sleep(1000);
				}
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
		        Util.Logger.log(1, e.getMessage());
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