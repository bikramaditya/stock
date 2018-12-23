package utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Table;

import entities.Stock;
import entities.DailyData;
import entities.IntraDayStock;
import entities.Recomendation;

public class Util 
{
	public static TreeMap<String, Stock> StocksWatchList = new TreeMap<String, Stock>();
	public static StockLogger Logger = null;
	public static HashMap<String, Table<Integer, LocalDate, Stock>> stockDataMap = new HashMap<String, Table<Integer, LocalDate, Stock>>();
	static ArrayList<LocalDate> tradingHolidays = null;
	static ArrayList<LocalDate> TradingDays = null;
	DAO dao = null;
	
	public static float globalIndices = 0;
	public static float NiftyIndex = 0;

	
	public Util() throws Exception 
	{
		if (Logger == null) {
			Logger = new StockLogger();
		}
		dao = new DAO();
		if (TradingDays == null) {
			TradingDays = getTradingDays(2018, "NSE");// Future bug
		}
		if(StocksWatchList.size()==0)
		{
			StocksWatchList = dao.getWatchList("NSE");
			StocksWatchList = populateSectorIndices(StocksWatchList,"NSE");
		}
		if (globalIndices == 0f) {
			globalIndices = getGlobalIndices2();
		}
		if (NiftyIndex == 0f) {
			NiftyIndex = getNifty();
			NiftyIndex += getGSXNifty();
		}
	}

	enum Pattern {
		ConvexRising, ConvexFalling, ConcaveRising, ConcaveFalling, None;
	}

	public float getValForDate(ArrayList<DailyData> dailyData, LocalDate forDate) {
		float val = 0.0f;
		for (DailyData data : dailyData) {
			if (data.DATE.isEqual(forDate) || data.DATE.isAfter(forDate)) {
				val = (data.LOW + data.HIGH) / 2;
				break;
			}
		}
		return val;
	}
	
	public float getCloseValForDate(ArrayList<DailyData> dailyData, LocalDate forDate) {
		float val = 0.0f;
		for (DailyData data : dailyData) {
			if (data.DATE.isEqual(forDate) || data.DATE.isAfter(forDate)) {
				val = data.CLOSE;
				break;
			}
		}
		return val;
	}
	
	public int mode(ArrayList<Integer> array) {
		HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
		int max = 1;
		int temp = 0;

		for (int i = 0; i < array.size(); i++) {

			if (hm.get(array.get(i)) != null) {

				int count = hm.get(array.get(i));
				count++;
				hm.put(array.get(i), count);

				if (count > max) {
					max = count;
					temp = array.get(i);
				}
			}

			else
				hm.put(array.get(i), 1);
		}

		hm.remove(temp);

		max = 0;
		temp = 0;

		Set<Integer> keys = hm.keySet();

		for (int key : keys) {
			int val = hm.get(key);
			if (val > max) {
				max = val;
				temp = key;
			}
		}

		return temp;
	}
	
	public LocalDate getPreviousTradingDate(LocalDate targetDate, int daysBack) {
		int i = TradingDays.size() - 1;

		while (i > 0) {
			LocalDate date = TradingDays.get(i);

			if (date.isEqual(targetDate)) {
				break;
			}
			i--;
		}

		i = i - daysBack;

		LocalDate retDate = TradingDays.get(i);
		return retDate;
	}
	
	public ArrayList<LocalDate> getTradingDays(int thisYear, String MKT) 
	{
		if (TradingDays != null)
		{
			return TradingDays;
		}
		ArrayList<LocalDate> tradingDays = new ArrayList<LocalDate>();
		ArrayList<LocalDate> holidays = dao.getTradingHolidays(thisYear, MKT);
		int[] years = { thisYear };
		int[] months = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
		for (int year : years) {
			for (int month : months) {
				Calendar cal = new GregorianCalendar(year, month, 1);
				do {
					int day = cal.get(Calendar.DAY_OF_WEEK);
					if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
					} else {
						String monthToPrint = ("" + (month + 1));
						if (monthToPrint.length() == 1) {
							monthToPrint = "0" + monthToPrint;
						}
						String dayToPrint = "" + cal.get(Calendar.DAY_OF_MONTH);
						if (dayToPrint.length() == 1) {
							dayToPrint = "0" + dayToPrint;
						}
						String holiday = year + "-" + monthToPrint + "-" + dayToPrint;
						
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
						LocalDate formatedDate = LocalDate.parse(holiday, formatter);
						
						tradingDays.add(formatedDate);
					}
					cal.add(Calendar.DAY_OF_YEAR, 1);
				} while (cal.get(Calendar.MONTH) == month);
			}
		}
		holidays.forEach(day -> {
			if (tradingDays.contains(day)) {
				tradingDays.remove(day);
			}
		});
		return tradingDays;
	}

	public Table<Integer, LocalDate, Stock> loadStockData(String MKT, String SYMBOL) {
		String key = MKT + ":" + SYMBOL;
		Table<Integer, LocalDate, Stock> stockData = dao.loadStockData(MKT, SYMBOL);
		stockDataMap.put(key, stockData);
		return stockData;
	}


	public Stock getStockByIndex(Map<LocalDate, Stock> row) {
		Collection<Stock> stocks = row.values();
		Iterator<Stock> itr = stocks.iterator();
		Stock stock = null;
		while (itr.hasNext()) {
			stock = itr.next();
		}
		return stock;
	}

	public boolean isTradingDay(String yyymmdd) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate subjectDate = LocalDate.parse(yyymmdd, formatter);
		if (TradingDays != null && TradingDays.contains(subjectDate)) {
			return true;
		} else {
			return false;
		}
	}

	public String getTodayYYMMDD() {
		java.util.Date today = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String strDateToday = formatter.format(today);
		return strDateToday;
	}

	public boolean isTradingDayAfterMarket() {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"));
		int hour24 = cal.get(Calendar.HOUR_OF_DAY);
		if (hour24 > 18 && hour24 <= 19) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isMarketOpen() {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"));
		int hour24 = cal.get(Calendar.HOUR_OF_DAY);
		if (hour24 >= 10 && hour24 <= 14) {
			return true;
		} else {
			return false;
		}
	}

	

	public double logOfBase(double num) {
		double res = Math.log(num) / Math.log(1.1);
		if (Double.isNaN(res) || Double.isInfinite(res)) {
			res = 0.0;
		}
		return res;
	}

	double totalError = 0.0;

	public double CalculateRMSError(Map<LocalDate, Double> predictedhigh200Days, Map<LocalDate, Double> high100Days) {
		totalError = 0.0;
		high100Days.forEach((key, value) -> {
			LocalDate ago15 = key.minusDays(10);
			double oVal = value;
			if (predictedhigh200Days.containsKey(key) && key.isAfter(ago15)) {
				double pVal = predictedhigh200Days.get(key);
				if (oVal > 0.0 && pVal > 0.0) {
					double diff = oVal - pVal;
					double err = Math.pow(diff, 2);
					totalError = totalError + err;
				}
			}
		});

		double sqrt = Math.sqrt(totalError);
		return sqrt;
	}

	
	LocalDate getNextTradingDate(LocalDate thisDate, String MKT) {
		int year = thisDate.getYear();
		if (tradingHolidays == null || tradingHolidays.size() == 0) {
			tradingHolidays = dao.getTradingHolidays(year, MKT);
		}
		int i = 0;

		while (true) {
			thisDate = thisDate.plusDays(1);
			if (!tradingHolidays.contains(thisDate.toString())) {
				break;
			} else if (i >= 7) {
				break;
			}
			i++;
		}

		return thisDate;
	}

	public ArrayList<Stock> getWatchList() {
		return dao.getActiveWatchList();
	}

	public void storeEODResults(Stock stock) {
		dao.insertEODValues(stock);
	}

	public void loadAllStockData(ArrayList<Stock> watchList) {
		stockDataMap = dao.loadAllStockData(watchList);
	}
	
	private IntraDayStock getQuoteAt(ArrayList<IntraDayStock> intraDayData, LocalDateTime timeAt)
	{
		IntraDayStock intraLatest = null;
		for (IntraDayStock intraDayStock : intraDayData) {
			LocalDateTime time = intraDayStock.TIME;
			if (time.isAfter(timeAt) && time.isBefore(timeAt.plusSeconds(90))) 
			{
				intraLatest = intraDayStock;
				break;
			}
		}
		
		return intraLatest;
	}
	
	public boolean isTradePredictionValid(Stock stock) 
	{
		String mKT = stock.MKT;
		String sYMBOL = stock.SYMBOL;
		
		Recomendation reco = dao.getTradeRecomendations(mKT, sYMBOL);
		
		if(reco.MKT==null || reco.MKT.length()==0 || reco.ACTUAL_BUY == 0.0 || reco.ACTUAL_SELL == 0.0)
		{
			return false;
		}
		
		ArrayList<IntraDayStock> intraDayData = dao.intraDayData(mKT, sYMBOL);

		String today = getTodayYYMMDD();
		LocalDateTime time = LocalDateTime.parse(today + "T10:29:30");
		
		IntraDayStock intraLatest = getQuoteAt(intraDayData, time);
		
//		double prevClose = getPreviousDayClose(reco);
		
		
		double sell_dist = Math.abs(intraLatest.QUOTE - reco.RECO_SELL);
		double buy_dist = Math.abs(intraLatest.QUOTE - reco.RECO_BUY);
		
		
		if(1.5*sell_dist < buy_dist)
		{
			if (intraLatest.QUOTE > reco.RECO_SELL) {
				if(NiftyIndex > 0.2 && globalIndices > 0.2 && stock.SectorIndex > 0.2)
				{
					//Don't take sell action if everything is green. 
				}
				else
				{
					if(stock.SYMBOL.equals("ASIANPAINT"))
					{
						System.out.println();
					}
					reco.ORDER_TYPE = "SELL";
					reco.ACTUAL_SELL = intraLatest.QUOTE * (1 - 0.0001);
					reco.ACTUAL_BUY = reco.ACTUAL_SELL * (1 - 0.0075);
				}
			}	
		}
		else if(1.5*buy_dist < sell_dist)
		{
			if (intraLatest.QUOTE < reco.RECO_BUY) {
				if(NiftyIndex < -0.2 && globalIndices < -0.2 && stock.SectorIndex < -0.2)
				{
					//Don't take buy action if everything is red.
				}
				else
				{
					if(stock.SYMBOL.equals("ASIANPAINT"))
					{
						System.out.println();
					}
					reco.ORDER_TYPE = "BUY";
					reco.ACTUAL_BUY = intraLatest.QUOTE * (1 + 0.0001);
					reco.ACTUAL_SELL = reco.ACTUAL_BUY * (1 + 0.0075);
				}
			}	
		}
		
		
		boolean retVal = false;

		if(intraLatest==null || reco.RECO_SELL==0.0 || reco.RECO_BUY == 0.0)
		{
			return false;
		}
		
		if (reco.ACTUAL_BUY > 0 && reco.ACTUAL_SELL > 0 && reco.ACTUAL_SELL > reco.ACTUAL_BUY) 
		{
			reco.LIVE_QUOTE = intraLatest.QUOTE;
			dao.updateRecomendation(reco);
			retVal = true;
			
		} else {
			reco.ACTUAL_BUY = 0.0;
			reco.ACTUAL_SELL = 0.0;
			retVal = false;
		}
		
		Logger.log(0, "retVal="+retVal+"  Reco="+ reco.toString());
		return retVal;
	}

	private double getPreviousDayClose(Recomendation reco) 
	{
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String today = getTodayYYMMDD();
		
		LocalDate predictForDate = LocalDate.parse(today, formatter);
		LocalDate prevTradeDate = getPreviousTradingDate(predictForDate, 1);
		
		return dao.getLatestColumnValue(reco.MKT, reco.SYMBOL, "CLOSE", prevTradeDate);		
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

	public float getGlobalIndices2() throws Exception {
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
	
	public float getGlobalIndices() throws Exception {
		float gloBalSum = 0f;
		float globalAvg = 0f;
		try {
			Document doc = Jsoup.connect("https://www.investing.com/indices/major-indices").get();
	
			Elements divs = doc.getElementsByClass("chgPer");//("accordianSector");
			int i = 1;
			for(Element div : divs)
			{
				String text = div.text();
				if(text.contains("%"))
				{
					text=text.replace("%", "");
					gloBalSum += Double.parseDouble(text);
				}
				if(i==8)
				{
					break;
				}
				i++;
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
	
	public float getSectorMap() throws Exception {
		float gloBalSum = 0f;
		float globalAvg = 0f;
		try {
			Document doc = Jsoup.connect("https://www.investing.com/indices/major-indices").get();
	
			Elements divs = doc.getElementsByClass("chgPer");//("accordianSector");
			int i = 1;
			for(Element div : divs)
			{
				String text = div.text();
				if(text.contains("%"))
				{
					text=text.replace("%", "");
					gloBalSum += Double.parseDouble(text);
				}
				if(i==8)
				{
					break;
				}
				i++;
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
	
	public boolean isTradingDay(LocalDate targetDate) 
	{
		boolean flag = false;
		for(LocalDate date : TradingDays)
		{
			if(date.equals(targetDate))
			{
				flag = true;
			}
		}
		return flag;
	}

	public TreeMap<String, Stock> populateSectorIndices(TreeMap<String, Stock> stocksWatchList, String mkt) throws Exception {
		HashMap<String, Float> indicMap= new HashMap<String, Float>();
		try {
			Document doc = Jsoup.connect("https://www.moneycontrol.com/stocks/marketstats/sector-scan/nse/today.html").get();
	
			Elements tables = doc.getElementsByClass("tabl_secScan");
			
			tables.remove(0);
			
			for(Element table : tables)
			{
				Elements tds = table.getElementsByTag("td");
				String titleText = tds.first().text();
				Element tdPerc = tds.get(2);
				String text = tdPerc.text();
				if(text.contains("%"))
				{
					text = text.replace("%","");
					
					text=text.trim();
					
					float value = Float.parseFloat(text);
					
					indicMap.put(titleText, value);
				}
			}
			
			
		}
		catch(Exception e)
		{			
			e.printStackTrace();
			throw new Exception("Sector Indices not found");
		}
		
		Set<String> keys = stocksWatchList.keySet();
		for(String key : keys)
		{
			Stock stock = stocksWatchList.get(key);
			Set<String> indicKeys = indicMap.keySet();
			for(String indicKey : indicKeys)
			{
				if(indicKey.contains(stock.SECTOR))
				{
					stock.SectorIndex = indicMap.get(indicKey);
					stocksWatchList.put(key, stock);
				}
			}
		}
		
		return stocksWatchList;
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
}