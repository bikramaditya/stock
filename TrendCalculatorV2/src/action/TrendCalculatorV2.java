package action;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import entity.Stock;
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class TrendCalculatorV2 {
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
		
		while (isTradingDay && isMarketOpen)
		{	
			System.out.println("Starting all threads - "+new Date());
	        Util.Logger.log(0, "Starting all threads - "+new Date());
	        
			ArrayList<Stock> watchList = dao.getFreshDataWatchList("NSE");
			
			if(watchList.size() > 0)
			{
				ExecutorService executor = Executors.newFixedThreadPool(2);
				
				for (Stock stock : watchList) {
					Runnable worker;
					try {
						//if(stock.SYMBOL.equals("HINDPETRO"))
						{
							worker = new CandleTrendWorker(stock,kite);
							executor.execute(worker);
				            //Thread.sleep(100000000);
						}
					} catch (Exception e) {
						e.printStackTrace();
						Util.Logger.log(1, e.getMessage());
					}
				}
	        
				executor.shutdown();
		        while (!executor.isTerminated()) {
		        	Thread.sleep(200);
		        }
		        
		        dao.updateWatchListAnalysisEnd("NSE");							
			}
	        
	        System.out.print("--Finished all threads"+new Date()+"\n\n");
	        Util.Logger.log(0, "Finished all threads"+new Date());	
	        
	        isMarketOpen = util.isMarketOpen();
	        
	        Thread.sleep(1000);
		}
		
		System.out.println("isTradingDay && isMarketOpen"+isTradingDay +"-"+ isMarketOpen);
        Util.Logger.log(0, "isTradingDay && isMarketOpen"+isTradingDay +"-"+ isMarketOpen);
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