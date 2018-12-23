import java.util.ArrayList;

public class App {	
	DAO dao = null;
	static boolean download = true;
	
	public static void main(String[] args) 
	{
		DAO dao = new DAO();
		
		ArrayList<Stock> watchList = dao.getWatchList();
		
		if(download==true)
		{
			Util util = new Util();
			String today = util.getTodayYYMMDD();
			boolean isTradingDay = util.isTradingDay(today);
			boolean isTradingDayAfterMarket = util.isTradingDayAfterMarket();
			boolean isMarketOpen = util.isMarketOpen();
			boolean isDownloadHoursOpen = util.isDownloadHoursOpen();
			
			if(isTradingDay && isDownloadHoursOpen)
		    {
				watchList.forEach(stock->
				{								    
					if(isMarketOpen)
			    	{
			    		System.out.println("Intraday Downloading..."+stock.SYMBOL);	
			    		util.downloadAndStoreIntradayData(stock.MKT, stock.SYMBOL);
			    		
			    		try { Thread.sleep(16000); } catch (InterruptedException e) { e.printStackTrace(); }
			    	}
			    	else if(isTradingDayAfterMarket)
			    	{
			    		System.out.println("Daily data Downloading..."+stock.SYMBOL);
			    		util.downloadAndStoreDailyData(stock.MKT, stock.SYMBOL);
					    
					    try { Thread.sleep(16000); } catch (InterruptedException e) { e.printStackTrace(); }
			    	}
					});				
				}	
		    }
		}
	}