package action;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.jfree.ui.RefineryUtilities;

import com.zerodhatech.models.Quote;

import charts.StockChart;
import entity.HistoricalDataEx;
import entity.Opportunity;
import entity.RulesValidation;
import entity.Stock;
import entity.TradeType;
import uitls.CandleComparator;
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class CandleTrendWorker implements Runnable{
	private Stock stock;
	private DAO dao = null;
	private Util util = null;
	private Kite kite;
	
	public CandleTrendWorker(Stock stock, Kite kite) throws Exception {
		this.stock = stock;
		this.util = new Util();
		this.kite = kite;
	}
	
	public void run() {
		String message = Thread.currentThread().getName() + " Start. Trend = " + stock;
		//System.out.println("\n");
		Util.Logger.log(0, message);
		processCommand();
		//System.out.println(Thread.currentThread().getName() + " End.");
		Util.Logger.log(0,Thread.currentThread().getName() + " End.");
	}

	private RulesValidation checkBuyingOpportunity(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1,
			HistoricalDataEx lastCandle) {

		BuyOpportunityChecker checker = new BuyOpportunityChecker(lastMinus2, lastMinus1, lastCandle);
		
		return  checker.checkAllRules();
		
	}
	
	private RulesValidation checkSellingOpportunity(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1,
			HistoricalDataEx lastCandle) {
		
		SellOpportunityChecker checker = new SellOpportunityChecker(lastMinus2, lastMinus1, lastCandle);
		
		return checker.checkAllRules();
	}

	private Opportunity checkForOpportunity(List<HistoricalDataEx> historicalData, double slope) {
		ArrayList<HistoricalDataEx> last3Candles = new ArrayList<HistoricalDataEx>(); 
		
		Opportunity opty = null;
		
		int size = historicalData.size();
		if(size >= 3)
		{
			for(int i = size-3; i < size; i++)
			{
				last3Candles.add(historicalData.get(i));
			}
		}
		historicalData = null;
		
		
		HistoricalDataEx lastMinus2 = last3Candles.get(0);
		HistoricalDataEx lastMinus1 = last3Candles.get(1);
		HistoricalDataEx lastCandle = last3Candles.get(2);
		
		if(slope > 0)
		{			
			RulesValidation rv = checkBuyingOpportunity(lastMinus2,lastMinus1,lastCandle);
			
			//System.out.println("slope="+slope+" Trying BUY for "+stock+"--"+rv);
			//Util.Logger.log(0, "slope="+slope+" Trying BUY for "+stock+"--"+rv);
			
			if(rv!=null)
			{
				opty = new Opportunity();
				opty.MKT = stock.MKT;
				opty.Symbol = stock.SYMBOL;
				opty.TradeType = TradeType.BUY;
				opty.TimeStamp = lastCandle.timeStamp;
				
				opty.MA = rv.is_MA_GoAhead;
				opty.MOM = rv.is_MOM_GoAhead;
				opty.MACD = rv.is_MACD_GoAhead;
				opty.PVT = rv.is_PVT_GoAhead;
				opty.is_valid = rv.is_valid;
			}
		}
		else if(slope < 0)
		{
			RulesValidation rv = checkSellingOpportunity(lastMinus2,lastMinus1,lastCandle);
			
			//System.out.println("slope="+slope+" Trying SELL for "+stock+"--"+rv);
			//Util.Logger.log(0, "slope="+slope+" Trying SELL for "+stock+"--"+rv);
			
			if(rv!=null)
			{
				opty = new Opportunity();
				opty.MKT = stock.MKT;
				opty.Symbol = stock.SYMBOL;
				opty.TradeType = TradeType.SELL;
				opty.TimeStamp = lastCandle.timeStamp;
				
				opty.MA = rv.is_MA_GoAhead;
				opty.MOM = rv.is_MOM_GoAhead;
				opty.MACD = rv.is_MACD_GoAhead;
				opty.PVT = rv.is_PVT_GoAhead;
				opty.is_valid = rv.is_valid;
			}
		}
		return opty;
	}
	
	private void processCommand() {
		try {
			dao = new DAO();
			//System.out.println("");
			ArrayList<HistoricalDataEx> historicalData = getHistoricalData(stock);//dao.getHistoricalData(stock);
			
			if(historicalData.size() < 11)
			{
				throw new Exception("Not enough data to plot trend");
			}
			Collections.sort(historicalData, new CandleComparator());
			//dao.insertToCandleTable(stock,historicalData.dataArrayList);
			
			calculateMovingAvg(historicalData,12);
			calculateMomentum(historicalData,14);
			calculatePVT(historicalData,1);
			calculateEMA12(historicalData);
			calculateEMA26(historicalData);
			calculateMACD(historicalData);
			calculateSignalLine(historicalData);
			
			/*
			plotChart(new String[] {"MovingAvg","close"},historicalData);
			plotChart(new String[] {"MoM","Dummy"},historicalData);
			plotChart(new String[] {"MACD","Signal"},historicalData);
			plotChart(new String[] {"PVT","PVT"},historicalData);
			
			System.out.println("");
			*/
			
			int start = historicalData.size()-3;
			int end = historicalData.size();

			
			List<HistoricalDataEx> subList =  historicalData.subList(start, end);
			
			//Get slope
			double slope = getSlope(historicalData);
			
			if(slope == -1000 || Math.abs(slope) <= 0.25 )
			{
				return;
			}
			
			Opportunity opty = checkForOpportunity(subList,slope);
			
			if(opty!=null)
			{
				opty.Slope = slope;
				
				if(opty.TradeType==TradeType.BUY)
				{
					if(opty.Slope < 0.2)
					{
						opty.is_valid = false;
					}
				}
				else if(opty.TradeType==TradeType.SELL)
				{
					if(opty.Slope > -0.2)
					{
						opty.is_valid = false;
					}
				}
				opty = updateOptyQuote(opty);
				storeOpportunity(opty);
				System.out.println("Valid -"+opty);
				Util.Logger.log(0, "Valid -"+opty);
			}
			else
			{
				System.out.println("Opty null for "+stock);
				Util.Logger.log(0, "Opty null for "+stock);
			}	
			
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
			Util.Logger.log(1,e.getMessage());
		}
	}
	
	private Opportunity updateOptyQuote(Opportunity opty) 
	{
		String instrument = opty.MKT+":"+opty.Symbol;
		String[] arr = new String[1];
		arr[0] = instrument;
		Map<String, Quote> quotes = kite.getAllQuotes(arr);
		double lastPrice = quotes.get(instrument).lastPrice;
		
		
		if(opty.TradeType == TradeType.BUY)
		{
			opty.EntryPrice = lastPrice;
			opty.ExitPrice = opty.EntryPrice*(1+0.002);
			opty.StopLoss = opty.EntryPrice*(1-0.02);
		}
		else if(opty.TradeType == TradeType.SELL)
		{
			opty.EntryPrice = lastPrice;
			opty.ExitPrice = opty.EntryPrice*(1-0.002);
			opty.StopLoss = opty.EntryPrice*(1+0.02);
		}
		
		return opty;
	}

	private ArrayList<HistoricalDataEx> getHistoricalData(Stock stock) 
	{
		ArrayList<HistoricalDataEx> candles5Min = new ArrayList<HistoricalDataEx>(); 
		
		ArrayList<HistoricalDataEx> candles1Min = dao.getHistoricalData(stock);
		
		for (int i = 0 ; i < candles1Min.size()-5; i=i+5) {
			double low = Double.MAX_VALUE;
			double high = 0;
			long volSum = 0;
			
			int j = i;
			
			double open = candles1Min.get(j).open;
			String timeStamp = candles1Min.get(j).timeStamp;
			for(; j < i+5 &&  j < candles1Min.size(); j++)
			{
				HistoricalDataEx candle = candles1Min.get(j);
				if(candle.high > high)
				{
					high = candle.high;
				}
				if(candle.low < low)
				{
					low = candle.low;
				}
				volSum = volSum+candle.volume;
			}
			
			double close = candles1Min.get(j).close;
			HistoricalDataEx candle = new HistoricalDataEx();
			candle.open = open;
			candle.low = low;
			candle.high = high;
			candle.volume = volSum;
			candle.close = close;
			candle.timeStamp = timeStamp;
			candles5Min.add(candle);
		}
		return candles5Min;
	}

	private double getSlope(ArrayList<HistoricalDataEx> historicalData) {
		Hashtable<Integer,Double> points = new Hashtable<Integer,Double>();
		int i = 0 ;
		List<HistoricalDataEx> subList = new ArrayList<HistoricalDataEx>();
		
		if(historicalData.size()>=10)
		{
			subList =  historicalData.subList(historicalData.size()-10, historicalData.size());	
		}		
		
		for (HistoricalDataEx candle : subList) 
		{
			try {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				java.util.Date recoTime = simpleDateFormat.parse(candle.timeStamp);
				LocalDateTime localDateTime = recoTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				
				String today = util.getTodayYYMMDD();
				LocalDateTime startTime = LocalDateTime.parse(today + "T09:40:00");
				
				LocalDateTime endTime = LocalDateTime.parse(today + "T14:15:00");
				
				if(localDateTime.isAfter(startTime) && localDateTime.isBefore(endTime))
				{
					points.put(i, candle.MovingAvg);
					i++;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		if(i < 9)
		{
			return -1000;
		}
		
		double slope = getSlope(points);
		
		return slope;
	}

	private double getSlope(Hashtable<Integer,Double> points)
	{
	    WeightedObservedPoints obs=new WeightedObservedPoints();
	    Enumeration e=points.keys();
	    int key;
	    while (e.hasMoreElements())
	    {
	        key=(int)e.nextElement();
	        obs.add(key,points.get(key));
	    }
	    PolynomialCurveFitter fitter=PolynomialCurveFitter.create(1);
	    double[] coeff=fitter.fit(obs.toList());

	    BigDecimal bd = new BigDecimal(coeff[1]);
	    BigDecimal slope=bd.setScale(3,BigDecimal.ROUND_HALF_UP);
	    return slope.doubleValue()*10;
	}
	
	private void storeOpportunity(Opportunity opty) 
	{		
		try {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				java.util.Date recoTime = simpleDateFormat.parse(opty.TimeStamp);
				LocalDateTime localDateTime = recoTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				
				dao.insertToRecomendation(stock, localDateTime, opty);
		} catch (ParseException e) {
			e.printStackTrace();
		}		
	}

	private void plotChart(String[] columns, ArrayList<HistoricalDataEx> historicalData) 
	{
		try {
			StockChart chart = new StockChart(columns[0] +" and "+columns[1], historicalData , columns);
			chart.pack();
			RefineryUtilities.centerFrameOnScreen(chart);
			chart.setVisible(true);
			//chart.setDropTarget(dt);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void calculateSignalLine(ArrayList<HistoricalDataEx> historicalData) 
	{
		double multiplier = 2.0 / (9.0 + 1.0) ;// = (2 / (10 + 1) ) = 0.1818 (18.18%)
		for(int i = 35 ; i < historicalData.size() ; i++)
		{
			HistoricalDataEx prev = historicalData.get(i-9);
			HistoricalDataEx curr = historicalData.get(i);
			
			double Signal = (curr.MACD - prev.MACD) * multiplier + prev.MACD; 
			
			historicalData.get(i).Signal=Signal;
		}
	}

	private void calculateMACD(ArrayList<HistoricalDataEx> historicalData) 
	{
		for(int i = 26 ; i < historicalData.size() ; i++)
		{
			HistoricalDataEx candle = historicalData.get(i);
			
			double MACD = candle.EMA12-candle.EMA26; 
			
			historicalData.get(i).MACD=MACD;
		}
	}

	private void calculateEMA26(ArrayList<HistoricalDataEx> historicalData) 
	{
		double multiplier = 2.0 / (26.0 + 1.0) ;// = (2 / (10 + 1) ) = 0.1818 (18.18%)
		for(int i = 26 ; i < historicalData.size() ; i++)
		{
			HistoricalDataEx prev = historicalData.get(i-1);
			HistoricalDataEx curr = historicalData.get(i);
			
			double prevEMA = prev.EMA26;
			if(prevEMA==0.0)
			{
				prevEMA = prev.MovingAvg;
			}
			
			double EMA = (curr.close - prevEMA) * multiplier + prevEMA; 
			
			historicalData.get(i).EMA26=EMA;
		}
	}
	
	private void calculateEMA12(ArrayList<HistoricalDataEx> historicalData) 
	{
		double multiplier = 2.0 / (12.0 + 1.0) ;// = (2 / (10 + 1) ) = 0.1818 (18.18%)
		for(int i = 12 ; i < historicalData.size() ; i++)
		{
			HistoricalDataEx prev = historicalData.get(i-1);
			HistoricalDataEx curr = historicalData.get(i);
			
			double prevEMA = prev.EMA12;
			if(prevEMA==0.0)
			{
				prevEMA = prev.MovingAvg;
			}
			
			double EMA = (curr.close - prevEMA) * multiplier + prevEMA; 
			
			historicalData.get(i).EMA12=EMA;
		}
	}

	private void calculatePVT(ArrayList<HistoricalDataEx> historicalData, int candle) {
		for(int i = 33 ; i < historicalData.size() ; i++)
		{
			HistoricalDataEx prev = historicalData.get(i-1);
			HistoricalDataEx curr = historicalData.get(i);
			
			double PVT = (((curr.close - prev.close) / prev.close) * curr.volume) + prev.PVT;
			historicalData.get(i).PVT=PVT;
		}
	}

	private void calculateMomentum(ArrayList<HistoricalDataEx> historicalData, int candle) 
	{
		//candle=candle-1;
		for(int i = candle ; i < historicalData.size() ; i++)
		{
			HistoricalDataEx prev = historicalData.get(i-candle);
			HistoricalDataEx curr = historicalData.get(i);
			
			double mom = curr.close-prev.close;
			historicalData.get(i).MoM=mom;
		}
	}

	private void calculateMovingAvg(ArrayList<HistoricalDataEx> historicalData, int candle) 
	{
		for(int i = candle ; i < historicalData.size() ; i++)
		{
			double sum = 0;
			for(int j = i-candle; j < i; j++)
			{
				HistoricalDataEx c = historicalData.get(j);
				sum+=c.close;
			}
			double ma = sum/(candle);
			historicalData.get(i).MovingAvg=ma;
			//System.out.println("Close="+historicalData.get(i).close+"----MA="+historicalData.get(i).MovingAvg);
		}
	}
}
