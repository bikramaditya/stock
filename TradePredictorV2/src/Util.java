import java.text.DecimalFormat;
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

public class Util {
	static StockLogger Logger = null;
	public static HashMap<String, Table<Integer, LocalDate, Stock>> stockDataMap = new HashMap<String, Table<Integer, LocalDate, Stock>>();
	static ArrayList<String> tradingHolidays = null;
	static ArrayList<String> TradingDays = null;
	DAO dao = null;
	private static DecimalFormat df2 = new DecimalFormat(".##");
	private static double NIFTY;
	private static double GloBalIndices;
	
	public Util() {
		if (Logger == null) {
			Logger = new StockLogger();
		}
		dao = new DAO();
		if (TradingDays == null) {
			TradingDays = getTradingDays(2018, "NSE");// Future bug
		}
	}

	enum Pattern {
		ConvexRising, ConvexFalling, ConcaveRising, ConcaveFalling, None;
	}

	public ArrayList<String> getTradingDays(int thisYear, String MKT) {
		ArrayList<String> tradingDays = new ArrayList<String>();
		ArrayList<String> holidays = dao.getTradingHolidays(thisYear, MKT);
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
						tradingDays.add(holiday);
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

	public Map<LocalDate, Double> calcMovingAverage(String MKT, String SYMBOL, int days, int candles, String column) {

		Map<LocalDate, Double> maData = new TreeMap<LocalDate, Double>();

		Table<Integer, LocalDate, Stock> stockData = stockDataMap.get(MKT + ":" + SYMBOL);
		for (int i = 45; i >= 0; i--) {
			Stock stock = getStockByIndex(stockData.row(i));
			double avg = dao.getAverage(MKT, SYMBOL, column, stock.DATE.minusDays(candles), stock.DATE);
			stock.MA = avg;
			maData.put(stock.DATE, avg);
		}
		stockDataMap.put(MKT + ":" + SYMBOL, stockData);
		return maData;
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

	private Stock getStockByDate(Map<Integer, Stock> map) {
		Collection<Stock> stocks = map.values();
		Iterator<Stock> itr = stocks.iterator();
		Stock stock = null;
		while (itr.hasNext()) {
			stock = itr.next();
		}
		return stock;
	}

	public Map<LocalDate, Double> getColumnValues(String MKT, String SYMBOL, String column, int days) {
		Map<LocalDate, Double> colData = new TreeMap<LocalDate, Double>();
		Table<Integer, LocalDate, Stock> stockData = stockDataMap.get(MKT + ":" + SYMBOL);
		for (int i = stockData.size() - days; i >= 0; i--) {
			Stock stock = getStockByIndex(stockData.row(i));

			if (column.equals("HIGH")) {
				colData.put(stock.DATE, stock.HIGH);
			} else if (column.equals("LOW")) {
				colData.put(stock.DATE, stock.LOW);
			}

		}
		return colData;
	}

	public Map<LocalDate, Double> getPredictionSeries(Stock received_stock, String column,
			Map<LocalDate, Double> maData, int days) {
		String MKT = received_stock.MKT;
		String SYMBOL = received_stock.SYMBOL;
		
		Map<LocalDate, Double> predictedValues = new TreeMap<LocalDate, Double>();

		Table<Integer, LocalDate, Stock> stockData = stockDataMap.get(MKT + ":" + SYMBOL);

		Stock stock = getStockByIndex(stockData.row(0));
		
		if (stock.DATE.toString().contains("2018-10-19") && stock.SYMBOL.equals("HDFC")) {
			System.out.println("");
		}

		double slope45Days = getSlope(MKT, SYMBOL, column, stock.DATE, 45);
		double slope30Days = getSlope(MKT, SYMBOL, column, stock.DATE, 30);
		double slope15Days = getSlope(MKT, SYMBOL, column, stock.DATE, 15);
		double slope7Days = getSlope(MKT, SYMBOL, column, stock.DATE, 7);
		double slope3Days = getSlope(MKT, SYMBOL, column, stock.DATE, 3);
		double slope2Days = getSlope(MKT, SYMBOL, column, stock.DATE, 2);
		double slope1Days = getSlope(MKT, SYMBOL, column, stock.DATE, 1);

		double MA3DaysAgo = (getStockByMovedIndex(stockData, stock.DATE, 3)).MA;
		double MA2DaysAgo = (getStockByMovedIndex(stockData, stock.DATE, 2)).MA;
		double MA1DaysAgo = (getStockByMovedIndex(stockData, stock.DATE, 1)).MA;

		double slope = (slope45Days + slope30Days + slope15Days + slope7Days + 2*slope3Days + 2*slope2Days + slope1Days)
				/ 5;

		double val3dayAgo = 0.0;
		double val2dayAgo = 0.0;
		double val1dayAgo = 0.0;

		double MADiff = 0.0;

		if (column.equals("HIGH")) {
			val3dayAgo = (getStockByMovedIndex(stockData, stock.DATE, 3)).HIGH;
			val2dayAgo = (getStockByMovedIndex(stockData, stock.DATE, 2)).HIGH;
			val1dayAgo = (getStockByMovedIndex(stockData, stock.DATE, 1)).HIGH;

			MADiff = (stock.HIGH - stock.MA) / 3;
			// MADiff = (MADiff * MADiff)/2;
			if (stock.HIGH > stock.MA) {
				MADiff = -1 * Math.abs(MADiff);
			} else if (stock.HIGH < stock.MA) {
				MADiff = Math.abs(MADiff);
			}
		} else if (column.equals("LOW")) {
			val3dayAgo = (getStockByMovedIndex(stockData, stock.DATE, 3)).LOW;
			val2dayAgo = (getStockByMovedIndex(stockData, stock.DATE, 2)).LOW;
			val1dayAgo = (getStockByMovedIndex(stockData, stock.DATE, 1)).LOW;

			MADiff = (stock.LOW - stock.MA) / 3;
			// MADiff = (MADiff * MADiff)/230;
			if (stock.LOW > stock.MA) {
				MADiff = -1 * Math.abs(MADiff);
			} else if (stock.LOW < stock.MA) {
				MADiff = Math.abs(MADiff);
			}
		}

		Pattern pattern = findPattern(val3dayAgo, val2dayAgo, val1dayAgo);

		double slopeFactor = 1.0;
		// double MAslope3Days = getSlope(MKT, SYMBOL, "MA", stock.DATE, 3);
		if (slope2Days > 0) {
			switch (pattern) {
			case ConvexRising:
				slopeFactor = 0.5;
				break;
			case ConvexFalling:
				slopeFactor = 1.0;
				break;
			case ConcaveRising:
				slopeFactor = -0.5;
				break;
			case ConcaveFalling:
				slopeFactor = -0.5;
				break;
			default:
				slopeFactor = 1;
				break;
			}
		} else {
			switch (pattern) {
			case ConvexRising:
				slopeFactor = -0.5;
				break;
			case ConvexFalling:
				slopeFactor = -0.5;
				break;
			case ConcaveRising:
				slopeFactor = 0.5;
				break;
			case ConcaveFalling:
				slopeFactor = 0.5;
				break;
			default:
				slopeFactor = 1;
				break;
			}
		}

		double area = 0.0;

		if (column.equals("HIGH")) {
			Point A = new Point(1, val3dayAgo);
			Point B = new Point(stock.HIGH, val2dayAgo);
			Point C = new Point(2 * stock.HIGH, val1dayAgo);
			area = (100 * Triangle.area(A, B, C)) / (stock.HIGH * stock.HIGH);
		} else if (column.equals("LOW")) {
			Point A = new Point(1, val3dayAgo);
			Point B = new Point(stock.LOW, val2dayAgo);
			Point C = new Point(2 * stock.LOW, val1dayAgo);
			area = (100 * Triangle.area(A, B, C)) / (stock.LOW * stock.LOW);
		}

		if (Math.abs(area) < 0.6) {
			MADiff = MADiff / 2;
			slopeFactor = 1;
		}

		if (slope < 0 && slopeFactor < 0) {
			slope = -1 * Math.abs(slopeFactor * slope);
		} else {
			slope = slopeFactor * slope;
		}

		//double intraDaySlope = 0.0;
		LocalDate latestDate = dao.getLatestDate(MKT, SYMBOL);

		String today = getNextTradingDate(stock.DATE, MKT).toString();
		//today = "2018-10-19";
		if (today.contains(latestDate.toString())) {
			double prediction = 0.0;
			/*
			intraDaySlope = getIntraDaySlope(MKT, SYMBOL);
			if ("NaN".equals("" + intraDaySlope)) {
				return null;
			}
			 */
			LocalDateTime latestTime = dao.getLatestIntraDayTime(MKT, SYMBOL);
			DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			LocalDate datePart = LocalDate.parse(latestTime.toString().substring(0, 10), format);
			DateTimeFormatter finalFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

			LocalDateTime startTime = LocalDateTime.parse((datePart.toString() + " 09:29:30"), finalFormat);
			double at930Val = dao.getValueAtTime(MKT, SYMBOL, startTime);

			double YesterdayToNow = at930Val - (stock.CLOSE+stock.HIGH)/2;

			// intraDaySlope = intraDaySlope;
			double val = 0.0;
			if (column.equals("LOW")) {
				val = stock.LOW;
			} else if (column.equals("HIGH")) {
				val = stock.HIGH;
			}
			
			prediction = val + slope + MADiff +YesterdayToNow;

			prediction = prediction * (1+GloBalIndices/100);
			prediction = prediction * (1+NIFTY/100);
			prediction = prediction * (1+received_stock.SectorIndex/100);
			
			LocalDate nextDate = getNextTradingDate(stock.DATE, MKT);
			// nextDate=nextDate.minusDays(1);
			predictedValues.put(nextDate, prediction);
		}

		return predictedValues;
	}

	public boolean isTradingDay(String yyymmdd) {
		if (TradingDays != null && TradingDays.contains(yyymmdd)) {
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

	private double getIntraDaySlope(String MKT, String SYMBOL) {
		LocalDateTime latestTime = dao.getLatestIntraDayTime(MKT, SYMBOL);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate datePart = LocalDate.parse(latestTime.toString().substring(0, 10), format);
		DateTimeFormatter finalFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		LocalDateTime startTime = LocalDateTime.parse((datePart.toString() + " 09:20:00"), finalFormat);
		LocalDateTime targetTime = LocalDateTime.parse((datePart.toString() + " 10:07:00"), finalFormat);

		double slope = dao.getIntraDaySlope(MKT, SYMBOL, startTime, targetTime);
		return slope;
	}

	private Pattern findPattern(double val3dayAgo, double val2dayAgo, double val1dayAgo) {
		Pattern patt = Pattern.None;
		double halfWay = (val3dayAgo + val1dayAgo) / 2.0;

		if (val3dayAgo > val1dayAgo) // Falling
		{
			if (val2dayAgo < halfWay) {
				patt = Pattern.ConcaveFalling;
			} else {
				patt = Pattern.ConvexFalling;
			}
		} else if (val3dayAgo < val1dayAgo) // Rising
		{
			if (val2dayAgo < halfWay) {
				patt = Pattern.ConcaveRising;
			} else {
				patt = Pattern.ConvexRising;
			}
		}
		return patt;
	}

	public double logOfBase(double num) {
		double res = Math.log(num) / Math.log(1.1);
		if (Double.isNaN(res) || Double.isInfinite(res)) {
			res = 0.0;
		}
		return res;
	}

	private double getSlope(String MKT, String SYMBOL, String column, LocalDate targetDate, int days) {
		Table<Integer, LocalDate, Stock> stockData = stockDataMap.get(MKT + ":" + SYMBOL);

		Stock now = getStockByDate(stockData.column(targetDate));
		Stock old = getStockByMovedIndex(stockData, targetDate, days);
		double nowVal = 0;
		double oldVal = 0;
		if (column.equals("HIGH")) {
			nowVal = (now.CLOSE + now.HIGH) / 2;
			oldVal = (old.CLOSE + old.HIGH) / 2;
		}
		if (column.equals("LOW")) {
			nowVal = (old.CLOSE + now.LOW) / 2;
			oldVal = (old.CLOSE + old.LOW) / 2;
		}
		if (column.equals("MA")) {
			nowVal = now.MA;
			oldVal = old.MA;
		}
		double diff = nowVal - oldVal;
		double val = diff / (double) days;
		return (double) Math.round(val * 100) / 100;
	}

	private Stock getStockByMovedIndex(Table<Integer, LocalDate, Stock> stockData, LocalDate targetDate, int days) {
		Map<Integer, Stock> keyVal = stockData.column(targetDate);
		Set<Integer> keys = keyVal.keySet();
		int i = 0;
		for (Integer intr : keys) {
			i = intr;
		}
		i = i + days;
		return getStockByIndex(stockData.row(i));
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

	public Map<LocalDate, Double> calcAndStoreMidline(String mKT, String sYMBOL, Map<LocalDate, Double> highData,
			Map<LocalDate, Double> predictedhighDays, Map<LocalDate, Double> lowData,
			Map<LocalDate, Double> predictedLOWDays) {
		Map<LocalDate, Double> predictedMidline = new TreeMap<LocalDate, Double>();

		Set<LocalDate> dates = predictedhighDays.keySet();
		Iterator<LocalDate> itr = dates.iterator();
		while (itr.hasNext()) {
			LocalDate date = itr.next();
			double valHigh = predictedhighDays.get(date);
			double valLow = 0.0;
			try {
				valLow = predictedLOWDays.get(date);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (valLow == 0.0) {
				valLow = valHigh;
			}
			double avg = (valHigh + valLow) / 2.0;
			predictedMidline.put(date, avg);
			if (!itr.hasNext()) {
				double reco_buy = avg * (1 - 0.0026);
				double reco_sell = avg * (1 + 0.0026);
				double diff = 100 * (valHigh - valLow) / valLow;
				if (diff >= 1 && diff < 5) {
					dao.insertToRecomendation(mKT, sYMBOL, date, valHigh, valLow, reco_buy, reco_sell);
				}
			}
		}
		return predictedMidline;
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
		return dao.getWatchList();
	}

	public void storeEODResults(Stock stock) {
		dao.insertEODValues(stock);
	}

	public void loadAllStockData(ArrayList<Stock> watchList) {
		stockDataMap = dao.loadAllStockData(watchList);
	}
	
	public ArrayList<Stock>  populateSectorIndices(ArrayList<Stock> watchList , String mkt) throws Exception {
		HashMap<String, Float> indicMap= new HashMap<String, Float>();
		try {
			Document doc = Jsoup.connect("https://www.zeebiz.com/market/sectors-nse").get();
	
			Element table = doc.getElementById("myTable");//("accordianSector");
			
			Elements trs = table.getElementsByTag("tr");
			
			for(Element tr : trs)
			{
				Elements tds = tr.getElementsByTag("td");
				if(tds.size() >2)
				{
					Element title = tds.get(0);
					String titleText = title.text();
					
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
		}
		catch(Exception e)
		{
			Logger.log(1, "Could not get Global Indices, exiting"+e.getMessage());
			throw new Exception("Global Indices not found");
		}
		
		for(Stock stock : watchList)
		{
			Set<String> indicKeys = indicMap.keySet();
			for(String indicKey : indicKeys)
			{
				if(indicKey.contains(stock.SECTOR))
				{
					stock.SectorIndex = indicMap.get(indicKey);
				}
			}
		}
		
		return watchList;
	}	
	
	public boolean isTradePredictionValid(Stock stock) 
	{
		String mKT = stock.MKT;
		String sYMBOL = stock.SYMBOL;
		
		Recomendation reco = dao.getTradeRecomendations(mKT, sYMBOL);
		ArrayList<IntraDayStock> intraDayData = dao.intraDayData(mKT, sYMBOL);

		IntraDayStock intraLatest = null;
		String today = getTodayYYMMDD();

		for (IntraDayStock intraDayStock : intraDayData) {
			LocalDateTime time = intraDayStock.TIME;
			if (time.isAfter(LocalDateTime.parse(today + "T09:29:30"))
					&& time.isBefore(LocalDateTime.parse(today + "T09:31:00"))) {
				intraLatest = intraDayStock;
				break;
			}
		}
		
		Logger.log(0, "Inside Util isTradePredictionValid intraLatest="+intraLatest);
		
		if(intraLatest==null)
		{
			return false;
		}
		
		double delta = 100*(intraLatest.QUOTE - (reco.RECO_SELL+reco.RECO_BUY)/2)/intraLatest.QUOTE;
		
		if (intraLatest.QUOTE > reco.RECO_SELL) {
			if(NIFTY > 0.2 && GloBalIndices > 0.2 && stock.SectorIndex > 0.2)
			{
				//Don't take sell action if everything is green. 
			}
			else
			{
				reco.ACTUAL_SELL = intraLatest.QUOTE;
				reco.ACTUAL_BUY = reco.ACTUAL_SELL * (1 - 0.005);
			}
		}
		else if (intraLatest.QUOTE < reco.RECO_BUY) {
			if(NIFTY < -0.2 && GloBalIndices < -0.2 && stock.SectorIndex < -0.2)
			{
				//Don't take buy action if everything is red.
			}
			else
			{
				reco.ACTUAL_BUY = intraLatest.QUOTE;
				reco.ACTUAL_SELL = reco.ACTUAL_BUY * (1 + 0.005);
			}
		}

		if(reco.ACTUAL_BUY == 0 && reco.ACTUAL_SELL==0)
		{
			reco.ACTUAL_SELL = reco.RECO_SELL;
			reco.ACTUAL_BUY = reco.ACTUAL_SELL * (1 - 0.005);			
		}
		
		boolean retVal = false;

		if (reco.RECO_BUY > 0 && reco.RECO_SELL > 0 && reco.RECO_SELL > reco.RECO_BUY) {
			retVal = true;
		} else {
			reco.ACTUAL_BUY = 0.0;
			reco.ACTUAL_SELL = 0.0;
			retVal = false;
		}
		reco.LIVE_QUOTE = intraLatest.QUOTE;
		if(Math.abs(delta) < 2.5)
		{
			dao.updateRecomendation(reco);
			Logger.log(0, "Trade placed for ->"+reco.toString());	
		}
		else
		{
			System.out.println(stock.SYMBOL+"-->Not placed-->delta="+delta);
			Logger.log(0, "Didn't place trade. Too much diff"+ reco.toString());
		}
		
		return retVal;
	}

	public void getNifty() {
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
	}
	public void getGSXNifty() {
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
		if(GSXNIFTY!=0.0)
		{
			NIFTY=(NIFTY + GSXNIFTY)/2;	
		}
	}
	public void getGlobalIndices() {
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
			System.exit(0);
		}
		GloBalIndices=globalAvg;
	}
}