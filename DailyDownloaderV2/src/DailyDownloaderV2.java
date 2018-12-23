import java.util.ArrayList;

public class DailyDownloaderV2 {
	DAO dao = null;

	@SuppressWarnings("static-access")
	public static void main(String[] args) 
	{
		Util util = new Util();
		
		util.Logger.log(0, "Starting Downloader");
		
		String today = util.getTodayYYMMDD();
		boolean isTradingDay = util.isTradingDay(today);
		if (isTradingDay) 
		{
			ArrayList<Stock> watchList = util.getWatchList();

			for (Stock stock : watchList) 
			{
				util.downloadAndStoreDailyData(stock.MKT, stock.SYMBOL);
				
				util.Logger.log(0, "EOD data Downloaded..." + stock.SYMBOL);
				
				try { Thread.sleep(16000); } catch (InterruptedException e) { e.printStackTrace();}	
			}
		}
		else
		{
			util.Logger.log(0, "Didn't download. isTradingDay="+isTradingDay);
		}
	}
}