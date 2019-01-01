package uitls;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import entity.Sentiment;


public class Util 
{
	static ArrayList<String> TradingDays = null;	
	static DAO dao = null;
	String apiKey = "XHVEY8A7PXUZJO1P";
	public static CandleLogger Logger = null;

	
	public static Sentiment sentiment = Sentiment.NORMAL;

	public Util() throws Exception
	{
		if(Logger==null)
		{
			Logger = new CandleLogger();
		}
		if(dao==null)
		{
			dao = new DAO();
		}
		if(TradingDays==null)
		{
			TradingDays = getTradingDays(2019, "NSE");
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
		TimeZone zone = TimeZone.getTimeZone("Asia/Kolkata");
		
		LocalDateTime now = LocalDateTime.now(zone.toZoneId());
		
		String today = getTodayYYMMDD();
		
		if (now.isAfter(LocalDateTime.parse(today + "T09:15:00"))
				&& now.isBefore(LocalDateTime.parse(today + "T13:35:00")))
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
