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
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.jfree.ui.RefineryUtilities;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

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
	float upDown = 0f;
	Channel channel;
	public CandleTrendWorker(Channel channel, Stock stock, Kite kite) throws Exception {
		this.stock = stock;
		this.util = new Util();
		this.channel = channel;
	}
	
	public void run() {
		String message = Thread.currentThread().getName() + " Start. Trend = " + stock;

		Util.Logger.log(0, message);
		System.out.println(message);
		
		try {
			String Q = stock.MKT+"-"+stock.SYMBOL;
			channel.queueDeclare(Q, false, false, false, null);
		    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
		        String msg = new String(delivery.getBody(), "UTF-8");
		        if(msg!=null && msg.equals(stock.SYMBOL+"Data Arrieved"))
		        {
		        	System.out.println(" Received '" + msg + "'");	
		        	processCommand();
		        }
		        
		    };
		    channel.basicConsume(Q, true, deliverCallback, consumerTag -> { });
		    
			
		} catch (Exception e) {
			Util.Logger.log(1, e.getMessage());
			e.printStackTrace();
		}

		//System.out.println(Thread.currentThread().getName() + " End.");
		Util.Logger.log(0,Thread.currentThread().getName() + " End.");
	}

	private RulesValidation checkBuyingOpportunity(HistoricalDataEx lastMinus3,HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1,
			HistoricalDataEx lastCandle) {

		BuyOpportunityChecker checker = new BuyOpportunityChecker(lastMinus3,lastMinus2, lastMinus1, lastCandle);
		
		return  checker.checkAllRules(upDown);
		
	}
	
	private RulesValidation checkSellingOpportunity(HistoricalDataEx lastMinus3, HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1,
			HistoricalDataEx lastCandle) {
		
		SellOpportunityChecker checker = new SellOpportunityChecker(lastMinus3,lastMinus2, lastMinus1, lastCandle);
		
		return checker.checkAllRules(upDown);
	}

	private Opportunity checkForOpportunity(List<HistoricalDataEx> historicalData) {
		ArrayList<HistoricalDataEx> last3Candles = new ArrayList<HistoricalDataEx>(); 
		
		Opportunity opty = null;
		
		int size = historicalData.size();
		if(size >= 4)
		{
			for(int i = size-4; i < size; i++)
			{
				last3Candles.add(historicalData.get(i));
			}
		}
		historicalData = null;
		
		HistoricalDataEx lastMinus3 = last3Candles.get(0);
		HistoricalDataEx lastMinus2 = last3Candles.get(1);
		HistoricalDataEx lastMinus1 = last3Candles.get(2);
		HistoricalDataEx lastCandle = last3Candles.get(3);
		RulesValidation rv = null;
		
		if(upDown > 0 && upDown < 0.8)
		{
			rv = checkBuyingOpportunity(lastMinus3,lastMinus2,lastMinus1,lastCandle);	
		}
		else if (upDown > -0.8 && upDown < 0)
		{
			rv = checkSellingOpportunity(lastMinus3,lastMinus2,lastMinus1,lastCandle);
		}
		
		
		if(rv!=null)
		{
			opty = new Opportunity();
			opty.MKT = stock.MKT;
			opty.Symbol = stock.SYMBOL;
			opty.TradeType = rv.tradeType;
			opty.TimeStamp = lastCandle.timeStamp;
			
			opty.EntryPrice = lastCandle.close;
			
			if(opty.TradeType==TradeType.BUY)
			{
				opty.ExitPrice = opty.EntryPrice*(1+0.001);
				opty.StopLoss = opty.EntryPrice*(1-0.01);
			}
			else if(opty.TradeType==TradeType.SELL)
			{
				opty.ExitPrice = opty.EntryPrice*(1-0.001);
				opty.StopLoss = opty.EntryPrice*(1+0.01);
			}
			opty.MA = rv.is_MA_GoAhead;
			opty.MOM = rv.is_MOM_GoAhead;
			opty.MACD = rv.is_MACD_GoAhead;
			opty.PVT = rv.is_PVT_GoAhead;
			opty.is_valid = rv.is_valid;
			opty.Score = rv.Score;	
		}
		return opty;
	}
	
	private void processCommand() {
		try {
			dao = new DAO();

			String sign = "";
			
			double ghada = 0.0;
			
			ArrayList<HistoricalDataEx> historicalData = dao.getHistoricalData(stock);//1 Min
			//ArrayList<HistoricalDataEx> historicalData = getHistoricalData5Min(stock);//5 Min
			
			Collections.sort(historicalData, new CandleComparator());
			
			calculateHeikinAshi(historicalData);
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
			
			for(int i = 2; i < historicalData.size();i++)
			{
				//int start = historicalData.size()-4;
				//int end = historicalData.size();

				//start = i-4;
				//end = i;

				//List<HistoricalDataEx> subList =  historicalData.subList(start, end);
				
				//double MACD = historicalData.get(i).MACD;
				//MACD = Math.round(MACD * 1000.0) / 1000.0;
				
				
				double diffNow = Math.abs(historicalData.get(i).Signal - historicalData.get(i).MACD);
				double diffPre1 = Math.abs(historicalData.get(i-1).Signal - historicalData.get(i-1).MACD);
				double diffPre2 = Math.abs(historicalData.get(i-2).Signal - historicalData.get(i-2).MACD);
				
				ghada += diffNow;
				
				System.out.println(historicalData.get(i).timeStamp+" | "+diffPre2 +" | "+diffPre1+" | "+diffNow+" - "+sign+"--ghada="+ghada);
				
				if(historicalData.get(i).timeStamp.contains("2019-01-04 14:17:00"))
				{
					System.out.println();
				}
				
				if(diffPre2 > diffPre1 && diffPre1 > diffNow &&  diffNow <= 0.1)
				{
					if(historicalData.get(i).Signal < historicalData.get(i).MACD)
					{
						if(historicalData.get(i).MACD < historicalData.get(i-1).MACD)
						//if(!sign.equals("Sign Change Down"))
						{
							if(ghada>1)
							{
								sign = "Sign Change Down";
								System.out.println(historicalData.get(i).timeStamp+"--SELL Now-------------ghada="+ghada);
								ghada = 0;	
							}
						}
					}
					else if(historicalData.get(i).Signal > historicalData.get(i).MACD)
					{
						if(historicalData.get(i).MACD > historicalData.get(i-1).MACD)
						//if(!sign.equals("Sign Change Up"))
						{
							if(ghada>1)
							{
								sign = "Sign Change Up";
								System.out.println(historicalData.get(i).timeStamp+"---BUY Now-------------ghada="+ghada);
								ghada = 0;	
							}
						}
					}
				}
				

				
				
				/*
				
				Opportunity opty = checkForOpportunity(subList);
				
				if(opty!=null)
				{
					HistoricalDataEx nowCandle = subList.get(2);
					
					double slope = getSlope(subList);
					
					opty.LastCandleClose = nowCandle.close;
					opty.Slope = slope;
					
					if(opty.is_valid)
					{
						storeOpportunity(opty);
						System.out.println(opty.is_valid+"-"+opty);
						Util.Logger.log(0, opty.is_valid+"-"+opty);
					}
				}
				
				*/
			}
		} catch (Exception e) {
			e.printStackTrace();
			Util.Logger.log(1,e.getMessage());
		}
	}
	
	private void calculateHeikinAshi(ArrayList<HistoricalDataEx> historicalData) 
	{
		int cnt = historicalData.size();
		//Calc HA close
		historicalData.get(0).HA.Open = historicalData.get(0).open; 
		for(int i = 0; i < cnt; i++)
		{
			HistoricalDataEx c = historicalData.get(i);
			double HAClose = (c.open+c.high+c.low+c.close)/4;
			c.HA.Close = HAClose;
		}
		//HA open
		for(int i = 1; i < cnt; i++)
		{
			HistoricalDataEx c = historicalData.get(i-1);
			double HAOpen = (c.HA.Open + c.HA.Close) / 2;
			historicalData.get(i).HA.Open=HAOpen;
			if(HAOpen <= historicalData.get(i).HA.Close)
			{
				historicalData.get(i).HA.TradeType=TradeType.BUY;
			}
			else
			{
				historicalData.get(i).HA.TradeType=TradeType.SELL;
			}
		}
		//HA high
		for(int i = 0; i < cnt; i++)
		{
			HistoricalDataEx c = historicalData.get(i);
			double HAHigh = Math.max(c.high, Math.max(c.HA.Open, c.HA.Close));
			c.HA.High = HAHigh;
		}
		//HA low
		for(int i = 0; i < cnt; i++)
		{
			HistoricalDataEx c = historicalData.get(i);
			double HALow = Math.min(c.low, Math.max(c.HA.Open, c.HA.Close));
			c.HA.Low = HALow;
		}
	}

	private ArrayList<HistoricalDataEx> getHistoricalData5Min(Stock stock) 
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

	private double getSlope(List<HistoricalDataEx> subList) {
		Hashtable<Integer,Double> points = new Hashtable<Integer,Double>();
		int i = 0 ;
		
		for (HistoricalDataEx candle : subList) 
		{
			points.put(i, candle.close);
			i++;
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
