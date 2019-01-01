package action;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.Position;

import entity.Opportunity;
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class TradeStatusV2 {
	static DAO dao = null;
	static Util util = null;
	static final long ONE_MINUTE_IN_MILLIS=60000;// milli seconds
	
	@SuppressWarnings("unused")
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
		
		System.out.println("Starting TradeStatusV2");
		Util.Logger.log(0, "Starting TradeStatusV2");

		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		boolean isMarketOpen = util.isMarketOpen();		

		Kite kite = null;
		
		if(isTradingDay && isMarketOpen) //Debug
		{
			try {
				kite = new Kite();
				dao = new DAO();
			} catch (Exception e) {
				e.printStackTrace();
				Util.Logger.log(1,e.getMessage());
			}	
		}
		
		while (isTradingDay && isMarketOpen && kite != null) //Debug
		{
			int secs = LocalDateTime.now().getSecond();
			System.out.print("Starting all threads - "+new Date());
	        Util.Logger.log(0, "Starting all threads - "+new Date());
	        
			ArrayList<Opportunity> watchList = dao.getFreshTradesList("NSE");	
			
			if(watchList.size() > 0 && secs >=40 && secs <= 45)
			{
				try {
					Map<String, List<Position>> positions = kite.getPositions();
					
					List<Position> positionList = positions.get("day");
					
					for (Opportunity opty : watchList) 
					{
						for (Position pos : positionList)
						{
							if(opty.Symbol.equals(pos.tradingSymbol))
							{
								if(pos.netQuantity==0)
								{
									dao.updateOrderExecuted(opty);
									System.out.println("order updated");
								}
							}
						}
					}
				} catch (IOException | KiteException e) {
					System.out.print("err in order status"+e.getMessage());
			        Util.Logger.log(0, "err in order status"+e.getMessage());				
				}
				
				System.out.print("--orders executed "+new Date()+"\n\n");
		        Util.Logger.log(0, "--orders executed "+new Date());
		        Thread.sleep(5000);
			}
			else
			{
				System.out.print(" waitng for condition "+new Date()+"\n\n");
		        Util.Logger.log(0, "waitng for condition"+new Date());	
			}
	        
	        isMarketOpen = util.isMarketOpen();
	        Thread.sleep(1000);
		}
		
		System.out.println("isTradingDay && isMarketOpen "+isTradingDay +"-"+ isMarketOpen);
        Util.Logger.log(0, "isTradingDay && isMarketOpen "+isTradingDay +"-"+ isMarketOpen);
	}
}