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
	static String[] APIKeys = {"XHVEY8A7PXUZJO1P","MU3KTHXK0M6U10SM","RZYBYTRVUOK29JRM","5SIWA2DDI2ZPTICC","NVGT5VEOUD4H4KB5"
			,"NO9KC4GHXGHQEXIY","C1ES9KDVE4UZNLO6","F8QOIE8Y5J2F9EWF","61PLRJXIS5Y2M9QC","WBUV5FIE2374KADH"};

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

		if (isTradingDay && isMarketOpen)
		{
			ExecutorService executor = Executors.newFixedThreadPool(10);
			dao = new DAO();
			
	    	ArrayList<Stock> watchList = util.GetVolumeToper("NSE");
	    	
			System.out.println("Starting all threads - "+new Date());
	        Util.Logger.log(0, "Starting all threads - "+new Date());
	        
			if(watchList.size() > 0)
			{
				//for (int i = 0 ; i < 10 ; i++) 
				{
					//Stock stock = watchList.get(i);
					try {
						//if(stock.SYMBOL.equals("RELIANCE"))
						{
							Stock stock = new Stock();
							stock.SYMBOL="SUNPHARMA";
							stock.MKT="NSE";
							
							Runnable worker = new CandleTrendWorker(stock,APIKeys[0]);
							executor.execute(worker);
							//Thread.sleep(10000);
							//System.out.println();
						}
					} catch (Exception e) {
						e.printStackTrace();
						Util.Logger.log(1, e.getMessage());
					}
				}
	        
		        //dao.updateWatchListAnalysisEnd("NSE");							
			}
	        
			executor.shutdown();
	        while (!executor.isTerminated()) {
	        	Thread.sleep(100);
	        }
			
	        System.out.print("--running all threads"+new Date()+"\n\n");
	        Util.Logger.log(0, "running all threads"+new Date());	
	        
		}
		while(isTradingDay && isMarketOpen)
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