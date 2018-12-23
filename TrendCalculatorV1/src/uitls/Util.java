package uitls;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import entity.Sentiment;


public class Util 
{
	static ArrayList<String> TradingDays = null;	
	static DAO dao = null;
	String apiKey = "XHVEY8A7PXUZJO1P";
	public static CandleLogger Logger = null;

	private static float globalIndices = 0;
	private static float NiftyIndex = 0;
	private static float GSXNifty = 0;
	
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
			TradingDays = getTradingDays(2018, "NSE");
		}
		/*
		if (globalIndices == 0f) {
			globalIndices = getGlobalIndices();
		}
		if (NiftyIndex == 0f) {
			NiftyIndex = getNifty();
		}
		if (GSXNifty == 0f) {
			//GSXNifty = getGSXNifty();
		}
		
		if(globalIndices+NiftyIndex > 0)
		{
			sentiment = Sentiment.BULLISH;
		}
		else if(globalIndices+NiftyIndex < 0)
		{
			sentiment = Sentiment.BEARISH;
		}
		*/
	}
	public float getGSXNifty() {
		float GSXNIFTY = 0f;
		try {
			Document doc = Jsoup.connect("https://liveindex.org/sgx-nifty-futures/").get();
	
			Elements trs = doc.getElementsByClass("index-line");
			
			for(Element td : trs)
			{
				Elements tds = td.getElementsByClass("index-percent");//("accordianSector");
				
				for(Element td1 : tds)
				{
					String text = td1.text();
					if(text.contains("%"))
					{
						text = text.replace("%", "");
						GSXNIFTY = Float.parseFloat(text);		
					}
				}	
			}
					
		}
		catch(Exception e)
		{
			Logger.log(1, "Could not get NIFTY, exiting"+e.getMessage());
		}
		return GSXNIFTY;
	}	
	public float getNifty() {
		float NIFTY = 0f;
		try {
			Document doc = Jsoup.connect("https://www.moneycontrol.com/indian-indices/nifty-50-9.html").get();
	
			Elements divs = doc.getElementsByClass("PT10");//("accordianSector");
	
			for(Element div : divs)
			{
				String text = div.text();
				if(text.contains("("))
				{
					text=text.substring(text.indexOf("(")+1,text.lastIndexOf("%"));
					NIFTY = Float.parseFloat(text);		
				}
			}		
		}
		catch(Exception e)
		{
			Logger.log(1, "Could not get NIFTY, exiting"+e.getMessage());
			System.exit(0);
		}
		return NIFTY;
	}
	public float getGlobalIndices() throws Exception {
		float gloBalSum = 0f;
		float globalAvg = 0f;
		try {
			Document doc = Jsoup.connect("https://liveindex.org").get();
	
			Elements tables = doc.getElementsByClass("index_table");
			Element table = tables.first();
			
			Elements hrefs = table.getElementsByTag("a");//("accordianSector");
			int i = 1;
			for(Element href : hrefs)
			{
				String text = href.text();
				if(text.contains("%"))
				{
					text=text.replace("%", "");
					gloBalSum += Double.parseDouble(text);
					i++;
				}
				if(i==10)
				{
					break;
				}
			}
			globalAvg = gloBalSum/i;
		}
		catch(Exception e)
		{
			Logger.log(1, "Could not get Global Indices, exiting"+e.getMessage());
			throw new Exception("Global Indices not found");
		}
		return globalAvg;
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
		
		if (now.isAfter(LocalDateTime.parse(today + "T10:30:00"))
				&& now.isBefore(LocalDateTime.parse(today + "T14:15:00")))
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
