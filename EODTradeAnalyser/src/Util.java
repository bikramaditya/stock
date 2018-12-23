import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jfree.chart.axis.StandardTickUnitSource;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class Util 
{
	static HashMap<String, Table<Integer, LocalDate, Stock>> stockDataMap = new HashMap<String, Table<Integer, LocalDate, Stock>>();
	static ArrayList tradingHolidays = null;	
	DAO dao = new DAO();
	
	enum Pattern 
	{ 
	    ConvexRising, ConvexFalling, ConcaveRising, ConcaveFalling, None; 
	}
	
	public void loadStockData(String MKT, String SYMBOL) {
		String key = MKT + ":" + SYMBOL;
		Table<Integer, LocalDate, Stock> stockData = dao.loadStockData(MKT, SYMBOL);
		stockDataMap.put(key, stockData);
		System.out.println("Loading complete"+MKT+":"+SYMBOL);
	}

	public Map<LocalDate, Double> calcMovingAverage(String MKT, String SYMBOL, int days, int candles,String column) {
		
		Map<LocalDate, Double> maData = new TreeMap<LocalDate, Double>();
		
		Table<Integer, LocalDate, Stock> stockData = stockDataMap.get(MKT+":"+SYMBOL);
		for(int i = 45; i >= 0; i--)
		{
			Stock stock = getStockByIndex(stockData.row(i));
			double avg = dao.getAverage(MKT, SYMBOL, column, stock.DATE.minusDays(candles), stock.DATE);
			stock.MA = avg;
			maData.put(stock.DATE, avg);
		}
		stockDataMap.put(MKT+":"+SYMBOL, stockData);
		return maData;
	}
	private Stock getStockByIndex(Map<LocalDate, Stock> row) {
		Collection<Stock> stocks = row.values();
		Iterator<Stock> itr = stocks.iterator();
		Stock stock = null;
		while(itr.hasNext())
		{
			stock = itr.next();
		}
		return stock;
	}
	private Stock getStockByDate(Map<Integer, Stock> map) {
		Collection<Stock> stocks = map.values();
		Iterator<Stock> itr = stocks.iterator();
		Stock stock = null;
		while(itr.hasNext())
		{
			stock = itr.next();
		}
		return stock;
	}
	public Map<LocalDate, Double> getColumnValues(String MKT, String SYMBOL, String column, int days) {
		Map<LocalDate, Double> colData = new TreeMap<LocalDate, Double>();
		Table<Integer, LocalDate, Stock> stockData = stockDataMap.get(MKT+":"+SYMBOL);
		for(int i = stockData.size()-days ; i >= 0; i--)
		{
			Stock stock = getStockByIndex(stockData.row(i));
			
			if(column.equals("HIGH"))
			{
				colData.put(stock.DATE, stock.HIGH);
			}
			else if(column.equals("LOW"))
			{
				colData.put(stock.DATE, stock.LOW);
			}
			
		}
		return colData;
	}

	public Map<LocalDate, Double> getPredictionSeries(String MKT, String SYMBOL, String column, Map<LocalDate, Double> maData, int days) 
	{
		Map<LocalDate, Double> predictedHIGHDays = new TreeMap<LocalDate, Double>();
		
		Table<Integer, LocalDate, Stock> stockData = stockDataMap.get(MKT+":"+SYMBOL);
		
		for(int i = 45; i >= 0; i--)
		{
			Stock stock = getStockByIndex(stockData.row(i));
			
			if(stock.DATE.toString().contains("2018-10-03") && stock.SYMBOL.equals("AXISBANK"))
			{
				System.out.println("");
			}
			
			double slope45Days = getSlope(MKT, SYMBOL, column, stock.DATE, 45);
			double slope30Days = getSlope(MKT, SYMBOL, column, stock.DATE, 30);
			double slope7Days = getSlope(MKT, SYMBOL, column, stock.DATE, 7);
			double slope3Days = getSlope(MKT, SYMBOL, column, stock.DATE, 3);
			double slope2Days = getSlope(MKT, SYMBOL, column, stock.DATE, 2);
			double slope1Days = getSlope(MKT, SYMBOL, column, stock.DATE, 1);
			
			double MA3DaysAgo = (getStockByMovedIndex(stockData, stock.DATE,3)).MA;
			double MA2DaysAgo = (getStockByMovedIndex(stockData, stock.DATE,2)).MA;
			double MA1DaysAgo = (getStockByMovedIndex(stockData, stock.DATE,1)).MA;
			
			double slope = (slope45Days+slope30Days+slope7Days+slope3Days+slope2Days+slope1Days)/10;
			
			double val3dayAgo = 0.0;
			double val2dayAgo = 0.0;
			double val1dayAgo = 0.0;
			
			double MADiff = 0.0;

			if(column.equals("HIGH"))
			{
				val3dayAgo = (getStockByMovedIndex(stockData, stock.DATE,3)).HIGH;
				val2dayAgo = (getStockByMovedIndex(stockData, stock.DATE,2)).HIGH;
				val1dayAgo = (getStockByMovedIndex(stockData, stock.DATE,1)).HIGH;
				
				MADiff = (stock.HIGH-stock.MA);
				MADiff = (MADiff * MADiff)/200;
				if(stock.HIGH > stock.MA)
				{
					MADiff = -1*Math.abs(MADiff);
				}
				else if(stock.HIGH < stock.MA)
				{
					MADiff = Math.abs(MADiff);
				}
			}
			else if(column.equals("LOW"))
			{
				val3dayAgo = (getStockByMovedIndex(stockData, stock.DATE,3)).LOW;
				val2dayAgo = (getStockByMovedIndex(stockData, stock.DATE,2)).LOW;
				val1dayAgo = (getStockByMovedIndex(stockData, stock.DATE,1)).LOW;
				
				MADiff = (stock.LOW-stock.MA);
				MADiff = (MADiff * MADiff)/230;
				if(stock.LOW > stock.MA)
				{
					MADiff = -1*Math.abs(MADiff);
				}
				else if(stock.LOW < stock.MA)
				{
					MADiff = Math.abs(MADiff);
				}
			}
			
			Pattern pattern = findPattern(val3dayAgo, val2dayAgo, val1dayAgo);
			
			double slopeFactor = 1.0;
			//double MAslope3Days = getSlope(MKT, SYMBOL, "MA", stock.DATE, 3);
			if(slope2Days > 0)
			{
				switch (pattern) 
		        { 
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
			}
			else
			{
				switch (pattern) 
		        { 
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
			double prediction = 0.0;
			if(column.equals("HIGH"))
			{
				prediction = stock.HIGH+prediction;
				Point A = new Point(1, val3dayAgo); 
				Point B = new Point(stock.HIGH, val2dayAgo); 
				Point C = new Point(2*stock.HIGH, val1dayAgo); 
				area = (100*Triangle.area(A, B, C))/(stock.HIGH*stock.HIGH);
			}
			else if(column.equals("LOW"))
			{
				prediction = stock.LOW+prediction;
				Point A = new Point(1, val3dayAgo); 
				Point B = new Point(stock.LOW, val2dayAgo); 
				Point C = new Point(2*stock.LOW, val1dayAgo); 
				area = (100*Triangle.area(A, B, C))/(stock.LOW*stock.LOW);
			}
			 
			if(Math.abs(area) < 0.6)
			{
				MADiff = MADiff/2;
				slopeFactor = 1;
			}
			
			if(slope < 0 && slopeFactor < 0)
			{
				slope = -1*Math.abs(slopeFactor*slope);
			}
			else
			{
				slope = slopeFactor*slope;
			}
			 			
			double intraDaySlope = 0.0;
			LocalDate today = dao.getLatestDate(MKT, SYMBOL);
			boolean isPredictionForToday = false;
			if(stock.DATE.toString().equals(today.toString()))
			{
				intraDaySlope = getIntraDaySlope(MKT, SYMBOL); 
				isPredictionForToday = true;
				intraDaySlope = intraDaySlope/3.0;
			}
			
			prediction = prediction+slope+MADiff+intraDaySlope; 
			
			if(MA3DaysAgo>0.0 && MA2DaysAgo>0.0 && MA1DaysAgo>0.0 )//&& area > 0.5 && area <3
			{
				LocalDate nextDate = stock.DATE;
				if(!isPredictionForToday)
				{
					nextDate = getNextTradingDate(stock.DATE, MKT);
				}
				predictedHIGHDays.put(nextDate, prediction);
				//System.out.println(stock.SYMBOL+"|"+column+"|Date="+stock.DATE+"|slope="+slope+"|slopeFactor="+slopeFactor+"|MADiff="+MADiff+"|prediction="+prediction+"|actual="+stock.HIGH+"|score"+area);
			}
	    }		
		return predictedHIGHDays;
	}

	private double getIntraDaySlope(String MKT, String SYMBOL) 
	{
		LocalDateTime latestTime = dao.getLatestIntraDayTime(MKT, SYMBOL);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate datePart = LocalDate.parse(latestTime.toString().substring(0, 10), format);
		DateTimeFormatter finalFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime targetTime = LocalDateTime.parse((datePart.toString()+" 10:00:00"),finalFormat);
		LocalDateTime startTime = LocalDateTime.parse((datePart.toString()+" 09:15:00"),finalFormat);
		double slope = dao.getIntraDaySlope(MKT, SYMBOL, startTime, targetTime);
		return slope;
	}

	private Pattern findPattern(double val3dayAgo, double val2dayAgo, double val1dayAgo) 
	{
		Pattern patt = Pattern.None;
		double halfWay = (val3dayAgo + val1dayAgo)/2.0;
		
		if(val3dayAgo > val1dayAgo) //Falling
		{
			if(val2dayAgo < halfWay)
			{
				patt = Pattern.ConcaveFalling;
			}
			else
			{
				patt = Pattern.ConvexFalling;
			}
		}
		else if(val3dayAgo < val1dayAgo) //Rising
		{
			if(val2dayAgo < halfWay)
			{
				patt = Pattern.ConcaveRising;
			}
			else
			{
				patt = Pattern.ConvexRising;
			}
		}
		return patt;
	}

	public double logOfBase(double num) {
	    double res = Math.log(num) / Math.log(1.1);
	    if(Double.isNaN(res) || Double.isInfinite(res))
	    {
	    	res = 0.0;
	    }
	    return res;
	}
	private double getSlope(String MKT, String SYMBOL, String column, LocalDate targetDate, int days) 
	{
		Table<Integer, LocalDate, Stock> stockData = stockDataMap.get(MKT+":"+SYMBOL);

		Stock now = getStockByDate(stockData.column(targetDate));
		Stock old = getStockByMovedIndex(stockData,targetDate,days);
		double nowVal = 0;
		double oldVal = 0;
		if(column.equals("HIGH"))
		{
			nowVal = now.HIGH;
			oldVal = old.HIGH;
		}
		if(column.equals("LOW"))
		{
			nowVal = now.LOW;
			oldVal = old.LOW;
		}
		if(column.equals("MA"))
		{
			nowVal = now.MA;
			oldVal = old.MA;
		}
		double diff = nowVal-oldVal;
		double val = diff/(double)days;
		return (double)Math.round(val*100)/100;
	}

	private Stock getStockByMovedIndex(Table<Integer, LocalDate, Stock> stockData, LocalDate targetDate, int days) 
	{
		Map<Integer, Stock> keyVal = stockData.column(targetDate);
		Set<Integer> keys = keyVal.keySet();
		int i = 0;
		for (Integer intr : keys) {
			i = intr;
		}
		i=i+days;
		return getStockByIndex(stockData.row(i));
	}

	double totalError = 0.0;
	public double CalculateRMSError(Map<LocalDate, Double> predictedhigh200Days, Map<LocalDate, Double> high100Days) {
		totalError = 0.0;
		high100Days.forEach((key,value)->{
			LocalDate ago15 = key.minusDays(10);
			double oVal = value;
			if(predictedhigh200Days.containsKey(key) && key.isAfter(ago15))
			{
				double pVal = predictedhigh200Days.get(key);
				if(oVal > 0.0 && pVal>0.0)
				{
					double diff = oVal-pVal;
					double err = Math.pow(diff, 2);
					totalError = totalError + err;
				}	
			}
		});
		
		double sqrt = Math.sqrt(totalError);
		return sqrt;
	}

	public Map<LocalDate, Double> calcAndStoreMidline(String mKT, String sYMBOL, 
			Map<LocalDate, Double>  highData, Map<LocalDate, Double> predictedhighDays,
			Map<LocalDate, Double> lowData, Map<LocalDate, Double> predictedLOWDays) {
		Map<LocalDate, Double> predictedMidline = new TreeMap<LocalDate, Double>();
		
		Set<LocalDate> dates = predictedhighDays.keySet();
		Iterator<LocalDate> itr = dates.iterator();
		while(itr.hasNext())
		{
			LocalDate date = itr.next();
			double valHigh = predictedhighDays.get(date);
			double valLow = 0.0;
			try {
				valLow = predictedLOWDays.get(date);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			if(valLow==0.0)
			{
				valLow=valHigh;
			}
			double avg = (valHigh+valLow)/2.0;
			predictedMidline.put(date, avg);
			if(!itr.hasNext())
			{
				double reco_buy = avg*0.9975;
				double reco_sell = avg*1.0025;
				System.out.println(
						"MKT="+mKT+
						"|Date="+date+
						"|STOCK="+sYMBOL+
						"|High="+valHigh+
						"|Low="+valLow+
						"|Reco Buy="+reco_buy+
						"|Reco Sell="+reco_sell+
						"| Pot. Profit for Rs.10000 = "+0.005*10000);
				dao.insertToRecomendation(mKT,sYMBOL,date,valHigh,valLow,reco_buy,reco_sell);
			}			
		}
		return predictedMidline;
	}
	
	LocalDate getNextTradingDate(LocalDate thisDate, String MKT)
	{
		int year = thisDate.getYear();
		if(tradingHolidays==null || tradingHolidays.size()==0)
		{
			tradingHolidays = dao.getTradingHolidays(year, MKT);
		}
		int i = 0;
		
		while(true)
		{
			thisDate = thisDate.plusDays(1);
			if(!tradingHolidays.contains(thisDate.toString()))
			{
				break;
			}
			else if( i>=7)
			{
				break;
			}
			i++;
		}
		
		return thisDate;
	}
}
