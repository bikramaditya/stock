import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class Util 
{
	static ArrayList<String> TradingDays = null;	
	static DAO dao = null;
	String apiKey = "XHVEY8A7PXUZJO1P";
	static StockLogger Logger = null;
	public Util()
	{
		if(Logger==null)
		{
			Logger = new StockLogger();
		}
		if(dao==null)
		{
			dao = new DAO();
		}
		if(TradingDays==null)
		{
			TradingDays = getTradingDays(2018, "NSE");//Future bug
		}
	}
	public boolean isTradingDay(String yyymmdd)
	{
		if(TradingDays !=null && TradingDays.contains(yyymmdd))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public static String getNextTradingDay(String yyymmdd)
	{
		int i = TradingDays.indexOf(yyymmdd);
		if(i==-1)
		{
			return null;
		}
		else
		{
			return TradingDays.get(i+1);
		}
	}
	public ArrayList<String> getTradingDays(int thisYear, String MKT)
	{
		ArrayList<String> tradingDays = new ArrayList<String>();
		ArrayList<String> holidays = dao.getTradingHolidays(thisYear, MKT);
		int[] years = {thisYear};
		int[] months = {0,1,2,3,4,5,6,7,8,9,10,11};
		for (int year : years) {
			for(int month : months)
			{
				Calendar cal = new GregorianCalendar(year, month, 1);
				do {
				    int day = cal.get(Calendar.DAY_OF_WEEK);
				    if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
				    }
				    else
				    {
				    	String monthToPrint = (""+(month+1));
				        if(monthToPrint.length()==1)
				        {
				        	monthToPrint = "0"+monthToPrint;
				        }
				        String dayToPrint = ""+cal.get(Calendar.DAY_OF_MONTH);
				        if(dayToPrint.length()==1)
				        {
				        	dayToPrint = "0"+dayToPrint;
				        }
				        String holiday = year+"-"+monthToPrint+"-"+dayToPrint;
				        tradingDays.add(holiday);
				    }
				    cal.add(Calendar.DAY_OF_YEAR, 1);
				}  while (cal.get(Calendar.MONTH) == month);
			}
		}
		holidays.forEach(day->{
			if(tradingDays.contains(day))
			{
				tradingDays.remove(day);
			}
		});
		return tradingDays;
	}
	
	public void downloadAndStoreIntradayData(String MKT, String SYMBOL) 
	{
		if(dao == null)
		{
			dao = new DAO();
		}
		
		String fileURL = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol="+MKT+":"+SYMBOL+"&interval=5min&apikey="+apiKey+"&datatype=csv&outputsize=compact";
        String saveDir = "C:/Temp";
        try {
            String filePath = new HttpDownloadUtility().downloadFile(fileURL, saveDir, MKT, SYMBOL);
        
	    	Reader reader = Files.newBufferedReader(Paths.get(filePath));
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
            
			LocalDateTime latestTime = dao.getLatestIntraDayTime(MKT, SYMBOL);

			for (CSVRecord csvRecord : csvParser) {
                dao.insertToIntraDayPriceTable(MKT, SYMBOL, latestTime, csvRecord);
            }
			csvParser.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	public void downloadAndStoreDailyData(String MKT, String SYMBOL) 
	{
		if(dao == null)
		{
			dao = new DAO();
		}
		
		String fileURL = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="+MKT+":"+SYMBOL+"&interval=5min&apikey="+apiKey+"&datatype=csv&outputsize=compact";
        String saveDir = "C:/Temp";
        try {
            String filePath = new HttpDownloadUtility().downloadFile(fileURL, saveDir, MKT, SYMBOL);
        
	    	Reader reader = Files.newBufferedReader(Paths.get(filePath));
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
            
			LocalDate latestDate = dao.getLatestDate(MKT, SYMBOL);

			for (CSVRecord csvRecord : csvParser) {
                dao.insertToDailyPriceTable(MKT, SYMBOL, latestDate, csvRecord);
            }
			csvParser.close();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	public String getTodayYYMMDD() {
	    java.util.Date today = new java.util.Date();
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");  
	    String strDateToday= formatter.format(today);
		return strDateToday;  
	}
	public boolean isTradingDayAfterMarket() {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"));
		int hour24 = cal.get(Calendar.HOUR_OF_DAY);
		if(hour24==18)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public boolean isMarketOpen() {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"));
		int hour24 = cal.get(Calendar.HOUR_OF_DAY);
		if(hour24 >= 10 && hour24 <= 14)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public boolean isDownloadHoursOpen() {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"));
		int hour24 = cal.get(Calendar.HOUR_OF_DAY);
		if(hour24 >= 10 && hour24 <= 19)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public String[] getAccessToken() 
	{
		return dao.getAccessToken(getTodayYYMMDD());
	}
	public void storeAccessToken(String AccessToken, String publicToken) {
		dao.storeAccessToken(getTodayYYMMDD(),AccessToken,publicToken);
	}
}
