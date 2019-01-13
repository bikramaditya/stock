package action;

import java.util.ArrayList;
import java.util.Date;
import entity.Opportunity;
import uitls.DAO;
import uitls.Util;

public class CandleMailer {
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
		
		System.out.println("Starting candle mailer");
		Util.Logger.log(0, "Starting candle mailer");

		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		boolean isMarketOpen = util.isMarketOpen();

		while (isTradingDay && isMarketOpen)
		{
			dao = new DAO();
			
			System.out.print("Starting all threads - "+new Date());
	        Util.Logger.log(0, "Starting all threads - "+new Date());
	        
			ArrayList<Opportunity> watchList = dao.getFreshOpportunityList("NSE");	
			
			if(watchList.size() > 0)
			{
				String table = "";
				
				int i = 1;
				
				for (Opportunity opty : watchList) {
					dao.updateOptyPicked(opty);
					table+="<tr><td>"+i+++"</td><td>"+opty.MKT+"</td><td>"+opty.Symbol+"</td><td>"+opty.TradeType+"</td><td>"+opty.TimeStamp+"</td><td>"+opty.EntryPrice+"</td><td>"+opty.ExitPrice+"</td></tr>";
				}
				
				HtmlMailSender.send(table);
				
				System.out.print("--mail sent "+new Date()+"\n\n");
		        Util.Logger.log(0, "--mail sent "+new Date());
			}
			else
			{
				System.out.print("--size 0, no mail sent "+new Date()+"\n\n");
		        Util.Logger.log(0, "--size 0, no mail sent "+new Date());	
			}
	        
	        isMarketOpen = util.isMarketOpen();
	        
	        Thread.sleep(60000);
		}
		//else
		{
			System.out.println("isTradingDay && isMarketOpen "+isTradingDay +"-"+ isMarketOpen);
	        Util.Logger.log(0, "isTradingDay && isMarketOpen "+isTradingDay +"-"+ isMarketOpen);
		}
	}
}