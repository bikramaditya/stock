package action;

import java.util.ArrayList;
import java.util.Date;

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
				sliceCashToday = totalCashToday/20;
			} catch (Exception | KiteException e) {
				e.printStackTrace();
				Util.Logger.log(1,e.getMessage());
			}	
		}
		
		while (isTradingDay && isMarketOpen && kite != null && sliceCashToday > 0) //Debug
		{
			dao = new DAO();
			
			//System.out.print("Starting all threads - "+new Date());
	        Util.Logger.log(0, "Starting all threads - "+new Date());
	        
			ArrayList<Opportunity> watchList = dao.getFreshOpportunityList("NSE");	
			
			if(watchList.size() > 0)
			{
				for (Opportunity opty : watchList) {
					MyOrder myOrder = new MyOrder(kite,opty,sliceCashToday);
					Order order = myOrder.execute();
					
					dao.updateOptyPicked(opty,order);
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
	}
}