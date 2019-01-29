package action;

import java.util.ArrayList;
import java.util.Date;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import entity.Stock;
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class TickFollowerV1 {
	static DAO dao = null;
	static Util util = null;
	static final long ONE_MINUTE_IN_MILLIS=60000;// milli seconds
	
	public static void main(String[] args) throws Exception 
	{
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
    	
		if (isTradingDay && isMarketOpen)
		{	
			System.out.println("starting followers - "+new Date());
	        Util.Logger.log(0, "starting followers - "+new Date());
	     
			if(watchList.size() > 0)
			{
				for (Stock stock : watchList) {
					Thread worker = new Thread(new CandleTrendWorker(channel,stock,kite));
					worker.start();
					break;
				}							
			}			
		}
		
		while(isMarketOpen)
		{
			isMarketOpen = util.isMarketOpen();			
	        Thread.sleep(5000);
		}
		
		System.out.println("isTradingDay && isMarketOpen"+isTradingDay +"-"+ isMarketOpen);
        Util.Logger.log(0, "isTradingDay && isMarketOpen"+isTradingDay +"-"+ isMarketOpen);
        Thread.sleep(1000);
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