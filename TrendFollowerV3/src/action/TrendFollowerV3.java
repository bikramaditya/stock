package action;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import entity.Stock;
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class TrendFollowerV3 {
	static DAO dao = null;
	static Util util = null;
	static final long ONE_MINUTE_IN_MILLIS=60000;// milli seconds
	
	public static void main(String[] args) throws Exception {
		
		try {
			util = new Util();	
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Util.Logger.log(1, e.getMessage());
			System.exit(0);
		}
		
		
		System.out.println("Starting candle runner");
		Util.Logger.log(0, "Starting candle runner");

		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		boolean isMarketOpen = util.isMarketOpen();

		dao = new DAO();
		
		Kite kite = new Kite();
		
		ConnectionFactory factory = new ConnectionFactory();
    	factory.setHost("localhost");
    	Connection connection = factory.newConnection();
    	Channel channel = connection.createChannel();
		
    	ArrayList<Stock> watchList = dao.getWatchList();
    	
		if (isTradingDay && isMarketOpen || true)
		{	
			System.out.println("Starting all threads - "+new Date());
	        Util.Logger.log(0, "Starting all threads - "+new Date());
	        
			if(watchList.size() > 0)
			{
				for (Stock stock : watchList) {
					
					try {
						//if(stock.SYMBOL.equals("ONGC"))
						{
							Runnable worker = new CandleTrendWorker(channel,stock,kite);
							worker.run();
						}
					} catch (Exception e) {
						e.printStackTrace();
						Util.Logger.log(1, e.getMessage());
					}
				}
	        
		        //dao.updateWatchListAnalysisEnd("NSE");							
			}
	        
	        System.out.print("--running all threads"+new Date()+"\n\n");
	        Util.Logger.log(0, "running all threads"+new Date());	
	        
		}
		while(isMarketOpen)
        {
        	isMarketOpen = util.isMarketOpen();
	        
	        Thread.sleep(1000);	
        }
		System.out.println("isTradingDay && isMarketOpen"+isTradingDay +"-"+ isMarketOpen);
        Util.Logger.log(0, "isTradingDay && isMarketOpen"+isTradingDay +"-"+ isMarketOpen);
     
        System.exit(0);
	}

	/*
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
	*/
}