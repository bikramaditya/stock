package action;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Order;

import entity.Opportunity;
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class TradeExecutorV2 {
	static DAO dao = null;
	static Util util = null;
	static final long ONE_MINUTE_IN_MILLIS=60000;// milli seconds
	
	public static void main(String[] args) throws InterruptedException {
		
		try {
			util = new Util();	
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Util.Logger.log(1, e.getMessage());
			System.exit(0);
		}
		
		System.out.println("Starting TradeExecutorV2");
		Util.Logger.log(0, "Starting TradeExecutorV2");

		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		boolean isMarketOpen = util.isMarketOpen();		
		double sliceCashToday = 0.0;		
		Kite kite = null;
		
		if(isTradingDay && isMarketOpen) //Debug
		{
			try {
				kite = new Kite();
				double totalCashToday =  kite.getMargins();
				sliceCashToday = totalCashToday/10;
			} catch (Exception | KiteException e) {
				e.printStackTrace();
				Util.Logger.log(1,e.getMessage());
			}	
		}
		
		dao = new DAO();
		
		while (isTradingDay && isMarketOpen && kite != null && sliceCashToday > 0) //Debug
		{
			//System.out.print("Starting all threads - "+new Date());
	        Util.Logger.log(0, "Starting all threads - "+new Date());
	        
			ArrayList<Opportunity> watchList = dao.getFreshOpportunityList("NSE");	
			
			if(watchList.size() > 0)
			{
				ExecutorService executor = Executors.newFixedThreadPool(5);
				Runnable worker;
				for (Opportunity opty : watchList) 
				{
					worker = new OrderWorker(kite,opty,sliceCashToday);
					executor.execute(worker);
				}
				
				System.out.print("--orders executed "+new Date()+"\n\n");
		        Util.Logger.log(0, "--orders executed "+new Date());
			}
			else
			{
				//System.out.print("--size 0, no mail sent "+new Date()+"\n\n");
		        Util.Logger.log(0, "--size 0, no order sent "+new Date());	
			}
	        
	        isMarketOpen = util.isMarketOpen();
	        
	        Thread.sleep(1000);
		}
		
		System.out.println("isTradingDay && isMarketOpen "+isTradingDay +"-"+ isMarketOpen);
        Util.Logger.log(0, "isTradingDay && isMarketOpen "+isTradingDay +"-"+ isMarketOpen);
        System.exit(0);
	}
}