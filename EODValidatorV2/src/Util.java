import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import com.google.common.collect.Table;

public class Util 
{
	static ArrayList<String> TradingDays = null;	
	DAO dao = null;
	static StockLogger Logger = null;
	public Util()
	{
		if(Logger==null)
		{
			Logger = new StockLogger();
		}
		dao = new DAO();
		if(TradingDays==null)
		{
			TradingDays = getTradingDays(2018, "NSE");//Future bug
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
	enum Pattern 
	{ 
	    ConvexRising, ConvexFalling, ConcaveRising, ConcaveFalling, None; 
	}
	
	public Table<Integer, LocalDate, Stock> loadStockData(String MKT, String SYMBOL) 
	{
		Table<Integer, LocalDate, Stock> stockData = dao.loadStockData(MKT, SYMBOL);
		return stockData;
	}

	
	public Stock getStockByIndex(Map<LocalDate, Stock> row) {
		Collection<Stock> stocks = row.values();
		Iterator<Stock> itr = stocks.iterator();
		Stock stock = null;
		while(itr.hasNext())
		{
			stock = itr.next();
		}
		return stock;
	}
	
	public String getTodayYYMMDD() {
	    java.util.Date today = new java.util.Date();
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");  
	    String strDateToday= formatter.format(today);
		return strDateToday;  
	}
	
	
	public ArrayList<Stock> getWatchList() 
	{
		return dao.getWatchList();
	}

	public void storeEODResults(Stock stock, boolean buy_hit, LocalDateTime buy_hit_time, boolean sell_hit, LocalDateTime sell_hit_time) 
	{
		dao.insertEODValues(stock, buy_hit, buy_hit_time, sell_hit, sell_hit_time);
	}
	public ArrayList<IntraDayStock> getIntraDayData(String mKT, String sYMBOL) {
		return dao.getIntradayData(mKT,sYMBOL);
	}
}
