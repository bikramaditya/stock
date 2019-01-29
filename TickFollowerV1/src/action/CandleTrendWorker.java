package action;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.zerodhatech.models.Tick;

import entity.HistoricalDataEx;
import entity.Opportunity;
import entity.Stock;
import entity.TradeType;
import uitls.CandleComparatorAsc;
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class CandleTrendWorker implements Runnable {
	private Stock stock;
	private DAO dao = null;
	private Util util = null;
	float upDown = 0f;

	private static ConcurrentHashMap<String,TradeType> sessionMap;
	private static String sessionXML = "";
	private ArrayList<Tick> ticks = new ArrayList<Tick>();
	
	public CandleTrendWorker(Channel channel, Stock stock, Kite kite) throws Exception {
		this.stock = stock;
		this.util = new Util();
		
		String user_home = System.getProperty("user.home");
		String today = util.getTodayYYMMDD();
		sessionXML = user_home+"\\tickerSessionMap_"+today+"_.xml";
		
		if(sessionMap==null)
		{
			sessionMap = readSerializedZML();	
		}
	}

	@SuppressWarnings("unchecked")
	private ConcurrentHashMap<String, TradeType> readSerializedZML() 
	{
		ConcurrentHashMap<String,TradeType> sessionMap = new ConcurrentHashMap<String,TradeType>();
		
		XMLDecoder decoder=null;
		try {
			
			decoder=new XMLDecoder(new BufferedInputStream(new FileInputStream(sessionXML)));
			sessionMap = (ConcurrentHashMap<String, TradeType>)decoder.readObject();
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: File xml not found");
		}
		if(sessionMap==null)
		{
			return new ConcurrentHashMap<String,TradeType>();
		}
		else
		{
			return sessionMap;
		}		
	}

	private void writeSerializedXML()
	{
		XMLEncoder encoder=null;
		try{

		encoder=new XMLEncoder(new BufferedOutputStream(new FileOutputStream(sessionXML)));
		}catch(FileNotFoundException fileNotFound){
			System.out.println("ERROR: While Creating or Opening the File xml");
		}
		encoder.writeObject(sessionMap);
		encoder.close();	
	}
	
	public void run() {
		String message = Thread.currentThread().getName() + " Start. Trend = " + stock;

		Util.Logger.log(0, message);
		System.out.println(message);

		dao = new DAO();

		ticks = dao.getTicks(stock);
		
		try {
			String Q = stock.instrument_token;
			
			sessionMap.putIfAbsent(Q, TradeType.NONE);
			
			writeSerializedXML();

			boolean isMarketOpen = util.isMarketOpen();
			
			while(isMarketOpen)
			{
				int prevSize = ticks.size();
				ticks = dao.updateTicks(ticks, stock);
				int postSize = ticks.size();
				
				if(postSize > prevSize)
				{					
					System.out.println("Tick Received '" + stock.SYMBOL + "'");
					
					ArrayList<HistoricalDataEx> historicalData1min = convertTickToData(ticks);// 1 Min
					
					int n = historicalData1min.size()-1;	
					Collections.sort(historicalData1min, new CandleComparatorAsc());
					
					ArrayList<HistoricalDataEx> historicalData10min = getHistoricalDataXMin(historicalData1min, 10);// 15													// Min
					ArrayList<HistoricalDataEx> historicalData5min = getHistoricalDataXMin(historicalData1min, 5);// 5Min
					ArrayList<HistoricalDataEx> historicalData3min = getHistoricalDataXMin(historicalData1min, 3);// 3Min

					String today = util.getTodayYYMMDD();
					String timestamp = historicalData1min.get(n).timeStamp;
					
					if(timestamp.contains(today) && util.isTradeAllowed())//
					{
						double[] sign10Min = GetMACD(historicalData10min);
						double[] sign5Min = GetMACD(historicalData5min);
						double[] sign3Min = GetMACD(historicalData3min);
						double[] sign1Min = GetMACD(historicalData1min);
						
						Util.Logger.log(0,"MACD-"+ historicalData1min.get(n).timeStamp+"-"+stock.SYMBOL+"-"+" macd15Min="+sign10Min[0]+" macd5Min="+sign5Min[0]+" macd3Min="+sign3Min[0]+" macd1Min="+sign1Min[0]);
						System.out.println("MACD-"+ historicalData1min.get(n).timeStamp+"-"+stock.SYMBOL+"-"+" macd15Min="+sign10Min[0]+" macd5Min="+sign5Min[0]+" macd3Min="+sign3Min[0]+" macd1Min="+sign1Min[0]);

						Util.Logger.log(0,"Signal-"+ historicalData1min.get(n).timeStamp+"-"+stock.SYMBOL+"-"+" sign10Min="+sign10Min[1]+" sign5Min="+sign5Min[1]+" sign3Min="+sign3Min[1]+" sign1Min="+sign1Min[1]);
						System.out.println("Signal-"+ historicalData1min.get(n).timeStamp+"-"+stock.SYMBOL+"-"+" sign10Min="+sign10Min[1]+" sign5Min="+sign5Min[1]+" sign3Min="+sign3Min[1]+" sign1Min="+sign1Min[1]);
						
						
						if(sign10Min[0]>0.0 && sign5Min[0]>0.0 && sign3Min[0]>0.0 && sign1Min[0]>0.0
								&& 
								sign10Min[1]>-0.02 && sign5Min[1]>-0.02 && sign3Min[1]>0.0 && sign1Min[1]>0.0)
						{
							if(Math.abs(sign10Min[0])>0.0001 && Math.abs(sign5Min[0])>0.0001 && Math.abs(sign3Min[0])>0.0001 && Math.abs(sign1Min[0])>0.0001 
									&& 
							   Math.abs(sign10Min[1])>0.0001 && Math.abs(sign5Min[1])>0.0001 && Math.abs(sign3Min[1])>0.0001 && Math.abs(sign1Min[1])>0.0001)
							{
								double is_buy_10Min = is_BUY_HA_GoAhead(historicalData10min);
								double is_buy_5Min = is_BUY_HA_GoAhead(historicalData5min);
								double is_buy_3Min = is_BUY_HA_GoAhead(historicalData3min);
								double is_buy = is_BUY_HA_GoAhead(historicalData1min);
								
								double totalHA = is_buy_10Min+is_buy_5Min+is_buy_3Min+is_buy;

								//if(totalHA > 0.2)// && is_buy_15Min> 0.04 && is_buy_5Min> 0.04 && is_buy_3Min> 0.04 && is_buy> 0.04)
								{
									TradeType trade_type = sessionMap.get(Q);
									
									if(trade_type==TradeType.BUY)
									{
										Util.Logger.log(0, stock.SYMBOL+" Duplicate trade exists, skipping it");
										System.out.println(stock.SYMBOL+" Duplicate trade exists, skipping it");
									}
									else
									{

										sessionMap.put(Q, TradeType.BUY);	
										writeSerializedXML();
										Opportunity opty = new Opportunity();
										opty.MA = sign10Min[0];
										opty.MOM = sign5Min[0];
										opty.MACD = sign3Min[0];
										opty.PVT = sign1Min[0];
										
										opty.Score = totalHA;
										opty.MKT = "NSE";
										opty.Symbol = stock.SYMBOL;
										opty.TradeType = TradeType.BUY;
										opty.TimeStamp = historicalData1min.get(n).timeStamp;
										
										opty.EntryPrice = (historicalData1min.get(n).high+historicalData1min.get(n).low)/2;
										opty.ExitPrice = opty.EntryPrice * (1 + 0.003);
										opty.StopLoss = opty.EntryPrice * (1 - 0.006);

										opty.is_valid = true;

										System.out.println(opty);
										util.Logger.log(0, opty.toString());
										
										storeOpportunity(opty);
									}
								}
							}
							
						}
						else if(sign10Min[0]<0.0 && sign5Min[0]<0.0 && sign3Min[0]<0.0 && sign1Min[0]<0.0
								&& 
								sign10Min[1]<0.02 && sign5Min[1]<0.02 && sign3Min[1]<0.0 && sign1Min[1]<0.0)
						{
							if(Math.abs(sign10Min[0])>0.0001 && Math.abs(sign5Min[0])>0.0001 && Math.abs(sign3Min[0])>0.0001 && Math.abs(sign1Min[0])>0.0001 
									&& 
							   Math.abs(sign10Min[1])>0.0001 && Math.abs(sign5Min[1])>0.0001 && Math.abs(sign3Min[1])>0.0001 && Math.abs(sign1Min[1])>0.0001)
							{
								double is_sell_15Min = is_SELL_HA_GoAhead(historicalData10min);
								double is_sell_5Min = is_SELL_HA_GoAhead(historicalData5min);
								double is_sell_3Min = is_SELL_HA_GoAhead(historicalData3min);
								double is_sell = is_SELL_HA_GoAhead(historicalData1min);
								
								double totalHA = is_sell_15Min+is_sell_5Min+is_sell_3Min+is_sell;

								//if(totalHA > 0.2)// && is_sell_15Min > 0.04 && is_sell_5Min > 0.04 && is_sell_3Min > 0.04 && is_sell > 0.04)
								{
									TradeType trade_type = sessionMap.get(Q);
									
									if(trade_type==TradeType.SELL)
									{
										Util.Logger.log(0, stock.SYMBOL+" Duplicate trade exists, skipping it");
										System.out.println(stock.SYMBOL+" Duplicate trade exists, skipping it");
									}
									else
									{
										sessionMap.put(Q, TradeType.SELL);	
										writeSerializedXML();
										
										Opportunity opty = new Opportunity();
										opty.MA = sign10Min[0];
										opty.MOM = sign5Min[0];
										opty.MACD = sign3Min[0];
										opty.PVT = sign1Min[0];

										opty.Score = totalHA;
										opty.MKT = "NSE";
										opty.Symbol = stock.SYMBOL;
										opty.TradeType = TradeType.SELL;
										opty.TimeStamp = historicalData1min.get(n).timeStamp;

										opty.EntryPrice = (historicalData1min.get(n).high+historicalData1min.get(n).low)/2;
										opty.ExitPrice = opty.EntryPrice * (1 - 0.003);
										opty.StopLoss = opty.EntryPrice * (1 + 0.006);

										opty.is_valid = true;
										
										System.out.println(opty);
										util.Logger.log(0, opty.toString());
										
										storeOpportunity(opty);	
									}
								}
							}										
						}

					
					}
					else
					{
						Util.Logger.log(0, "Trade not allowed or Old data received");
						System.out.println("= Trade not allowed or Old data received");
					}
				}
				Thread.sleep(5000);
				isMarketOpen = util.isMarketOpen();
			}
		} catch (Exception e) {
			Util.Logger.log(1, e.getMessage());
			e.printStackTrace();
		}

		// System.out.println(Thread.currentThread().getName() + " End.");
		Util.Logger.log(0, Thread.currentThread().getName() + " End.");
	}

	private ArrayList<HistoricalDataEx> convertTickToData(ArrayList<Tick> ticks2) 
	{
		ArrayList<HistoricalDataEx> historicalData = new ArrayList<HistoricalDataEx>();
		
		TimeZone zone = TimeZone.getTimeZone("Asia/Kolkata");

		int n = 0;
		
		for(int i = ticks2.size()-1; i > 0 ; i=i-n)
		{
			ArrayList<Tick> ticksSubset = new ArrayList<Tick>();
			n=0;
			
			LocalDateTime endTime = Instant.ofEpochMilli( ticks2.get(i).getTickTimestamp().getTime()).atZone( zone.toZoneId() ).toLocalDateTime();
			
			LocalDateTime startTime = endTime.minusSeconds(60);
			
			for(int j = i; j > 0 ;j--)
			{
				Tick tick = ticks2.get(j);
				LocalDateTime instTime = Instant.ofEpochMilli(tick.getTickTimestamp().getTime()).atZone( zone.toZoneId() ).toLocalDateTime();
				if(instTime.isBefore(startTime))
				{
					break;
				}
				else
				{
					n++;
				}
				ticksSubset.add(tick);
			}
			if(ticksSubset!=null && ticksSubset.size() > 0)
			{
				HistoricalDataEx candle = createCandle(ticksSubset);
				historicalData.add(candle);				
			}
		}
		//Collections.sort(historicalData, new CandleComparatorAsc());
		return historicalData;
	}
	private double is_BUY_HA_GoAhead(ArrayList<HistoricalDataEx> candles) 
	{
		HistoricalDataEx lastCandle = candles.get(0);
		
		double is_HA_GoAhead = 0;
		
		double top = lastCandle.HA.High - lastCandle.HA.Close;
		double bottom = lastCandle.HA.Open - lastCandle.HA.Low;
		double height = Math.abs(100*(lastCandle.HA.Close - lastCandle.HA.Open)/lastCandle.HA.Open);
		double ratio = Math.abs(top/bottom);
		
		System.out.println(stock.SYMBOL+"-BUYHA-ratio="+ratio+" height="+height);
		Util.Logger.log(0, stock.SYMBOL+"-BUYHA-ratio="+ratio+" height="+height);
		if(top==0 
				|| ratio==Double.POSITIVE_INFINITY 
				|| ratio==Double.NEGATIVE_INFINITY || ratio > 1 || bottom < 0)
		{
			if(height > 0.04)
			{
				is_HA_GoAhead = height;	
			}
		}
		
		return is_HA_GoAhead;
	}
	
	private double is_SELL_HA_GoAhead(ArrayList<HistoricalDataEx> candles) 
	{
		HistoricalDataEx lastCandle = candles.get(0);
		double is_HA_GoAhead = 0;
		
		double top = lastCandle.HA.High - lastCandle.HA.Open;
		double bottom = lastCandle.HA.Close - lastCandle.HA.Low;
		double height = Math.abs(100*(lastCandle.HA.Close - lastCandle.HA.Open)/lastCandle.HA.Open);
		double ratio = Math.abs(bottom/top);
		
		System.out.println(stock.SYMBOL+"-SELLHA-ratio="+ratio+" height="+height);
		Util.Logger.log(0, stock.SYMBOL+"-SELLHA-ratio="+ratio+" height="+height);
		
		if(top==0 
				|| ratio==Double.POSITIVE_INFINITY 
				|| ratio==Double.NEGATIVE_INFINITY || ratio > 1 || top <0)
		{
			if(height > 0.04)
			{
				is_HA_GoAhead = height;	
			}
		}
		
		return is_HA_GoAhead;
	}
	
	private double[] GetMACD(ArrayList<HistoricalDataEx> historicalData) {
		double[] macd = {0,0};
		
		try {

			//Collections.sort(historicalData, new CandleComparatorAsc());

			calculateHeikinAshi(historicalData);
			calculateMovingAvg(historicalData, 12);
			calculateMomentum(historicalData, 14);
			calculatePVT(historicalData, 1);
			calculateEMA12(historicalData);
			calculateEMA26(historicalData);
			calculateMACD(historicalData);
			calculateSignalLine(historicalData);
			
			int n = historicalData.size()-1;	
		
			double macdDiff = historicalData.get(n).MACD-historicalData.get(n-1).MACD;
			double signalDiff = historicalData.get(n).Signal-historicalData.get(n-1).Signal;
			
			macd[0]=macdDiff;
			macd[1]=signalDiff;
			
			//Util.Logger.log(0, historicalData.get(n).timeStamp+"- nowMACD="+nowMACD );
		} catch (Exception e) {
			e.printStackTrace();
			Util.Logger.log(1, e.getMessage());
		}

		//Collections.sort(historicalData, new CandleComparatorDesc());

		
		return macd;
	}

	private void calculateHeikinAshi(ArrayList<HistoricalDataEx> historicalData) {
		int cnt = historicalData.size();
		// Calc HA close
		historicalData.get(0).HA.Open = historicalData.get(0).open;
		for (int i = 0; i < cnt; i++) {
			HistoricalDataEx c = historicalData.get(i);
			double HAClose = (c.open + c.high + c.low + c.close) / 4;
			c.HA.Close = HAClose;
		}
		// HA open
		for (int i = 1; i < cnt; i++) {
			HistoricalDataEx c = historicalData.get(i - 1);
			double HAOpen = (c.HA.Open + c.HA.Close) / 2;
			historicalData.get(i).HA.Open = HAOpen;
			if (HAOpen <= historicalData.get(i).HA.Close) {
				historicalData.get(i).HA.TradeType = TradeType.BUY;
			} else {
				historicalData.get(i).HA.TradeType = TradeType.SELL;
			}
		}
		// HA high
		for (int i = 0; i < cnt; i++) {
			HistoricalDataEx c = historicalData.get(i);
			double HAHigh = Math.max(c.high, Math.max(c.HA.Open, c.HA.Close));
			c.HA.High = HAHigh;
		}
		// HA low
		for (int i = 0; i < cnt; i++) {
			HistoricalDataEx c = historicalData.get(i);
			double HALow = Math.min(c.low, Math.max(c.HA.Open, c.HA.Close));
			c.HA.Low = HALow;
		}
	}

	private ArrayList<HistoricalDataEx> getHistoricalDataXMin(ArrayList<HistoricalDataEx> candles1Min, int min) {
		ArrayList<HistoricalDataEx> candlesXMin = new ArrayList<HistoricalDataEx>();

		for (int i = 0; i < candles1Min.size() - min; i = i + min) {
			double low = Double.MAX_VALUE;
			double high = 0;
			long volSum = 0;

			int j = i;

			double open = candles1Min.get(j).open;
			
			HistoricalDataEx currCandle = new HistoricalDataEx();
			
			for (; j < i + min && j < candles1Min.size(); j++) {
				currCandle = candles1Min.get(j);
				if (currCandle.high > high) {
					high = currCandle.high;
				}
				if (currCandle.low < low) {
					low = currCandle.low;
				}
				volSum = volSum + currCandle.volume;
			}
			
			
			double close = currCandle.close;
			HistoricalDataEx candle = new HistoricalDataEx();
			candle.open = open;
			candle.low = low;
			candle.high = high;
			candle.volume = volSum;
			candle.close = close;
			candle.timeStamp = currCandle.timeStamp;
			candlesXMin.add(candle);
		}
		return candlesXMin;
	}

	private HistoricalDataEx createCandle(ArrayList<Tick> ticksSubset) 
	{
		double low = Double.MAX_VALUE;
		double high = 0;
		long volSum = 0;


		double open = ticksSubset.get(0).getLastTradedPrice();
		Date timeStamp = ticksSubset.get(0).getTickTimestamp();
		for (int i =0; i < ticksSubset.size(); i++) {
			Tick tick = ticksSubset.get(i);
			
			if (tick.getLastTradedPrice() > high) {
				high = tick.getLastTradedPrice();
			}
			if (tick.getLastTradedPrice() < low) {
				low = tick.getLastTradedPrice();
			}
			volSum = (long) (volSum + tick.getLastTradedQuantity());
		}

		double close = ticksSubset.get(ticksSubset.size()-1).getLastTradedPrice();
		HistoricalDataEx candle = new HistoricalDataEx();
		candle.open = open;
		candle.low = low;
		candle.high = high;
		candle.volume = volSum;
		candle.close = close;
		candle.timeStamp = timeStamp.toString();
		
		return candle;
	}


	private void storeOpportunity(Opportunity opty) {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			java.util.Date recoTime = simpleDateFormat.parse(opty.TimeStamp);
			LocalDateTime localDateTime = recoTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

			dao.insertToRecomendation(stock, localDateTime, opty);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void calculateSignalLine(ArrayList<HistoricalDataEx> historicalData) {
		double multiplier = 2.0 / (9.0 + 1.0);// = (2 / (10 + 1) ) = 0.1818 (18.18%)
		for (int i = 35; i < historicalData.size(); i++) {
			HistoricalDataEx prev = historicalData.get(i - 9);
			HistoricalDataEx curr = historicalData.get(i);

			//double Signal = (curr.MACD - prev.MACD) * multiplier + prev.MACD;
			double Signal = curr.MACD*multiplier + prev.MACD*(1- multiplier);
			historicalData.get(i).Signal = Signal;
		}
	}

	private void calculateMACD(ArrayList<HistoricalDataEx> historicalData) {
		for (int i = 26; i < historicalData.size(); i++) {
			HistoricalDataEx candle = historicalData.get(i);

			double MACD = candle.EMA12 - candle.EMA26;

			historicalData.get(i).MACD = MACD;
		}
	}

	private void calculateEMA26(ArrayList<HistoricalDataEx> historicalData) {
		double initEMA = 0;
		double multiplier = 2.0 / (26.0 + 1.0);// = (2 / (10 + 1) ) = 0.1818 (18.18%)
		int n = 26;
		
		if(historicalData.size() < n)
		{
			n = historicalData.size()-1;
		}
		
		for(int i = 0 ; i < n; i++)
		{
			initEMA+=historicalData.get(i).close;
		}
		initEMA = initEMA/n;
		
		for (int i = n; i < historicalData.size(); i++) {
			HistoricalDataEx prev = historicalData.get(i - 1);
			HistoricalDataEx curr = historicalData.get(i);

			double prevEMA = prev.EMA26;
			if (prevEMA == 0.0) {
				prevEMA = initEMA;
			}

			double EMA = curr.close*multiplier + prevEMA*(1- multiplier);

			historicalData.get(i).EMA26 = EMA;
		}
	}

	private void calculateEMA12(ArrayList<HistoricalDataEx> historicalData) {
		double multiplier = 2.0 / (12.0 + 1.0);// = (2 / (10 + 1) ) = 0.1818 (18.18%)
		double initEMA = 0;
		int n = 12;
		
		if(historicalData.size() < n)
		{
			n = historicalData.size()-1;
		}
		
		for(int i = 0 ; i < n; i++)
		{
			initEMA+=historicalData.get(i).close;
		}
		initEMA = initEMA/n;
		
		for (int i = n; i < historicalData.size(); i++) {
			HistoricalDataEx prev = historicalData.get(i - 1);
			HistoricalDataEx curr = historicalData.get(i);

			double prevEMA = prev.EMA12;
			if (prevEMA == 0.0) {
				prevEMA = initEMA;
			}

			double EMA = (curr.close - prevEMA)*multiplier + prevEMA;

			historicalData.get(i).EMA12 = EMA;
		}
	}

	private void calculatePVT(ArrayList<HistoricalDataEx> historicalData, int candle) {
		for (int i = 33; i < historicalData.size(); i++) {
			HistoricalDataEx prev = historicalData.get(i - 1);
			HistoricalDataEx curr = historicalData.get(i);

			double PVT = (((curr.close - prev.close) / prev.close) * curr.volume) + prev.PVT;
			historicalData.get(i).PVT = PVT;
		}
	}

	private void calculateMomentum(ArrayList<HistoricalDataEx> historicalData, int candle) {
		// candle=candle-1;
		for (int i = candle; i < historicalData.size(); i++) {
			HistoricalDataEx prev = historicalData.get(i - candle);
			HistoricalDataEx curr = historicalData.get(i);

			double mom = curr.close - prev.close;
			historicalData.get(i).MoM = mom;
		}
	}

	private void calculateMovingAvg(ArrayList<HistoricalDataEx> historicalData, int candle) {
		for (int i = candle; i < historicalData.size(); i++) {
			double sum = 0;
			for (int j = i - candle; j < i; j++) {
				HistoricalDataEx c = historicalData.get(j);
				sum += c.close;
			}
			double ma = sum / (candle);
			historicalData.get(i).MovingAvg = ma;
			// System.out.println("Close="+historicalData.get(i).close+"----MA="+historicalData.get(i).MovingAvg);
		}
	}
}
