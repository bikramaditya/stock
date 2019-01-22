package action;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
import uitls.CandleComparatorAsc;
import uitls.CandleComparatorDesc;
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class CandleTrendWorker implements Runnable {
	private Stock stock;
	private DAO dao = null;
	private Util util = null;
	float upDown = 0f;
	Channel channel;
	private static ConcurrentHashMap<String,TradeType> sessionMap;
	private static String sessionXML = "";
	
	public CandleTrendWorker(Channel channel, Stock stock, Kite kite) throws Exception {
		this.stock = stock;
		this.util = new Util();
		this.channel = channel;
		
		String user_home = System.getProperty("user.home");
		String today = util.getTodayYYMMDD();
		sessionXML = user_home+"\\sessionMap_"+today+"_.xml";
		
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

		try {
			String Q = stock.MKT + "-" + stock.SYMBOL;
			
			sessionMap.putIfAbsent(Q, TradeType.NONE);
			
			channel.queueDeclare(Q, false, false, false, null);
			System.out.println(Q + "Waiting for messages.");
			writeSerializedXML();
			
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String msg = new String(delivery.getBody(), "UTF-8");
				if (msg != null && msg.equals(stock.SYMBOL + "Data Arrieved")) 
				{
					/*
					//Is last BO order executed completely
					
					boolean UnexecutedTradeExists = dao.CheckIfUnexecutedTradeExists(stock);
					
					if(UnexecutedTradeExists)
					{
						Util.Logger.log(0, "Unexecuted trade exists, skipping it");
						System.out.println("Unexecuted trade exists, skipping it");
						return;
					}

					//Is last BO order executed completely*/
					
					System.out.println(" Received '" + msg + "'");
					//ArrayList<HistoricalDataEx> historicalData = dao.getHistoricalData(stock);// 1 Min
					
					//for(int i = 375 ; i > 0; i--)
					{
						//ArrayList<HistoricalDataEx> historicalData1min = new ArrayList<HistoricalDataEx>(historicalData.subList(i, historicalData.size()-1));
						ArrayList<HistoricalDataEx> historicalData1min = dao.getHistoricalData(stock);// 1 Min
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
				}
			};
			channel.basicConsume(Q, true, deliverCallback, consumerTag -> {
			});

		} catch (Exception e) {
			Util.Logger.log(1, e.getMessage());
			e.printStackTrace();
		}

		// System.out.println(Thread.currentThread().getName() + " End.");
		Util.Logger.log(0, Thread.currentThread().getName() + " End.");
	}

	private double checkSignal(ArrayList<HistoricalDataEx> historicalData) {
		double preMACD = 0;
		double nowMACD = 0;
		
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
			
			int i = historicalData.size()-1;
			
			preMACD = historicalData.get(i-1).Signal;
			nowMACD = historicalData.get(i).Signal;
			Util.Logger.log(0, historicalData.get(i).timeStamp+" - nowSignal="+nowMACD );
		} catch (Exception e) {
			e.printStackTrace();
			Util.Logger.log(1, e.getMessage());
		}

		//Collections.sort(historicalData, new CandleComparatorDesc());

		double diff = nowMACD-preMACD;
		
		return diff;
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
	
	private RulesValidation checkBuyingOpportunity(HistoricalDataEx lastMinus3, HistoricalDataEx lastMinus2,
			HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) {

		BuyOpportunityChecker checker = new BuyOpportunityChecker(lastMinus3, lastMinus2, lastMinus1, lastCandle);

		return checker.checkAllRules(upDown);

	}

	private RulesValidation checkSellingOpportunity(HistoricalDataEx lastMinus3, HistoricalDataEx lastMinus2,
			HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) {

		SellOpportunityChecker checker = new SellOpportunityChecker(lastMinus3, lastMinus2, lastMinus1, lastCandle);

		return checker.checkAllRules(upDown);
	}

	private Opportunity checkForOpportunity(List<HistoricalDataEx> historicalData) {
		ArrayList<HistoricalDataEx> last3Candles = new ArrayList<HistoricalDataEx>();

		Opportunity opty = null;

		int size = historicalData.size();
		if (size >= 4) {
			for (int i = size - 4; i < size; i++) {
				last3Candles.add(historicalData.get(i));
			}
		}
		historicalData = null;

		HistoricalDataEx lastMinus3 = last3Candles.get(0);
		HistoricalDataEx lastMinus2 = last3Candles.get(1);
		HistoricalDataEx lastMinus1 = last3Candles.get(2);
		HistoricalDataEx lastCandle = last3Candles.get(3);
		RulesValidation rv = null;

		if (upDown > 0 && upDown < 0.8) {
			rv = checkBuyingOpportunity(lastMinus3, lastMinus2, lastMinus1, lastCandle);
		} else if (upDown > -0.8 && upDown < 0) {
			rv = checkSellingOpportunity(lastMinus3, lastMinus2, lastMinus1, lastCandle);
		}

		if (rv != null) {
			opty = new Opportunity();
			opty.MKT = stock.MKT;
			opty.Symbol = stock.SYMBOL;
			opty.TradeType = rv.tradeType;
			opty.TimeStamp = lastCandle.timeStamp;

			opty.EntryPrice = lastCandle.close;

			if (opty.TradeType == TradeType.BUY) {
				opty.ExitPrice = opty.EntryPrice * (1 + 0.001);
				opty.StopLoss = opty.EntryPrice * (1 - 0.01);
			} else if (opty.TradeType == TradeType.SELL) {
				opty.ExitPrice = opty.EntryPrice * (1 - 0.001);
				opty.StopLoss = opty.EntryPrice * (1 + 0.01);
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

	private void processCommand() {
		try {

			ArrayList<HistoricalDataEx> historicalData = dao.getHistoricalData(stock);// 1 Min

			String sign = "";

			double MACDghada = 0.0;
			double valghada = 0.0;

			Collections.sort(historicalData, new CandleComparatorAsc());

			calculateHeikinAshi(historicalData);
			calculateMovingAvg(historicalData, 12);
			calculateMomentum(historicalData, 14);
			calculatePVT(historicalData, 1);
			calculateEMA12(historicalData);
			calculateEMA26(historicalData);
			calculateMACD(historicalData);
			calculateSignalLine(historicalData);

			/*
			 * plotChart(new String[] {"MovingAvg","close"},historicalData); plotChart(new
			 * String[] {"MoM","Dummy"},historicalData); plotChart(new String[]
			 * {"MACD","Signal"},historicalData); plotChart(new String[]
			 * {"PVT","PVT"},historicalData);
			 * 
			 * System.out.println("");
			 */

			for (int i = historicalData.size() - 40; i < historicalData.size(); i++) {
				// int start = historicalData.size()-4;
				// int end = historicalData.size();

				// start = i-4;
				// end = i;

				double factor = Math.abs(400 / historicalData.get(i).high);

				double diffNow = factor * Math.abs(historicalData.get(i).Signal - historicalData.get(i).MACD);
				double diffPre1 = factor * Math.abs(historicalData.get(i - 1).Signal - historicalData.get(i - 1).MACD);
				double diffPre2 = factor * Math.abs(historicalData.get(i - 2).Signal - historicalData.get(i - 2).MACD);

				MACDghada += diffNow;

				double valDiffNow = factor * Math.abs(historicalData.get(i).close - historicalData.get(i - 1).close);
				valghada += valDiffNow;

				System.out.println(historicalData.get(i).timeStamp + " | " + diffPre2 + " | " + diffPre1 + " | "
						+ diffNow + " - " + sign + "--ghada=" + MACDghada);
				Util.Logger.log(0, historicalData.get(i).timeStamp + " | " + diffPre2 + " | " + diffPre1 + " | "
						+ diffNow + " - " + sign + "--ghada=" + MACDghada);
				if (historicalData.get(i).timeStamp.contains("2019-01-04 09:25:00")) {
					System.out.println();
				}

				if (diffPre2 > diffPre1 && diffPre1 > diffNow && diffNow <= 0.15) {
					double delta0 = Math.abs(100 * (diffPre1 - diffNow) / diffPre1);
					double delta1 = Math.abs(100 * (diffPre2 - diffPre1) / diffPre2);

					double delta = (delta0 + delta1) / 2;

					if (historicalData.get(i).Signal < historicalData.get(i).MACD
							&& historicalData.get(i - 1).Signal < historicalData.get(i - 1).MACD
							&& historicalData.get(i - 2).Signal < historicalData.get(i - 2).MACD) {

						if (MACDghada > 1.5 && delta > 25) {
							double sum = 10 * MACDghada + delta;

							boolean orderNow = false;
							if (historicalData.size() == i + 1) {
								orderNow = true;
								Opportunity opty = new Opportunity();
								opty.MACD = MACDghada;
								opty.MA = valghada;

								opty.Slope = delta;
								opty.Score = sum;

								opty.MKT = stock.MKT;
								opty.Symbol = stock.SYMBOL;
								opty.TradeType = TradeType.SELL;
								opty.TimeStamp = historicalData.get(i).timeStamp;

								opty.EntryPrice = historicalData.get(i).close;
								opty.ExitPrice = opty.EntryPrice * (1 - 0.003);
								opty.StopLoss = opty.EntryPrice * (1 + 0.01);

								opty.is_valid = true;
								storeOpportunity(opty);
							}

							sign = "Sign Change Down";

							System.out.println(stock.SYMBOL + "-OrderNow =" + orderNow + " at "
									+ historicalData.get(i).timeStamp + "--SELL Now--Delta=" + delta + "--ghada="
									+ MACDghada + " sum=" + sum + "--valghada=" + valghada);
							Util.Logger.log(0,
									stock.SYMBOL + "-OrderNow =" + orderNow + " at " + historicalData.get(i).timeStamp
											+ "--SELL Now--Delta=" + delta + "--ghada=" + MACDghada + " sum=" + sum
											+ "--valghada=" + valghada);
						}
					} else if (historicalData.get(i).Signal > historicalData.get(i).MACD
							&& historicalData.get(i - 1).Signal > historicalData.get(i - 1).MACD
							&& historicalData.get(i - 2).Signal > historicalData.get(i - 2).MACD) {

						if (MACDghada > 1.8 && delta > 25) {

							double sum = 10 * MACDghada + delta;

							boolean orderNow = false;
							if (historicalData.size() == i + 1) {
								orderNow = true;
								Opportunity opty = new Opportunity();
								opty.MACD = MACDghada;
								opty.Slope = delta;
								opty.Score = sum;
								opty.MA = valghada;

								opty.MKT = stock.MKT;
								opty.Symbol = stock.SYMBOL;
								opty.TradeType = TradeType.BUY;
								opty.TimeStamp = historicalData.get(i).timeStamp;

								opty.EntryPrice = historicalData.get(i).close;
								opty.ExitPrice = opty.EntryPrice * (1 + 0.003);
								opty.StopLoss = opty.EntryPrice * (1 - 0.01);

								opty.is_valid = true;
								storeOpportunity(opty);
							}

							sign = "Sign Change Up";

							System.out.println(stock.SYMBOL + "-OrderNow =" + orderNow + " at "
									+ historicalData.get(i).timeStamp + "---BUY Now--Delta=" + delta + "--ghada="
									+ MACDghada + " sum=" + sum + "--valghada=" + valghada);
							Util.Logger.log(0,
									stock.SYMBOL + "-OrderNow =" + orderNow + " at " + historicalData.get(i).timeStamp
											+ "---BUY Now--Delta=" + delta + "--ghada=" + MACDghada + " sum=" + sum
											+ "--valghada=" + valghada);
						}
					}

					MACDghada = 0;
					valghada = 0;
				}

				/*
				 * 
				 * Opportunity opty = checkForOpportunity(subList);
				 * 
				 * if(opty!=null) { HistoricalDataEx nowCandle = subList.get(2);
				 * 
				 * double slope = getSlope(subList);
				 * 
				 * opty.LastCandleClose = nowCandle.close; opty.Slope = slope;
				 * 
				 * if(opty.is_valid) { storeOpportunity(opty);
				 * System.out.println(opty.is_valid+"-"+opty); Util.Logger.log(0,
				 * opty.is_valid+"-"+opty); } }
				 * 
				 */
			}
		} catch (Exception e) {
			e.printStackTrace();
			Util.Logger.log(1, e.getMessage());
		}
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
			String timeStamp = candles1Min.get(j).timeStamp;
			for (; j < i + min && j < candles1Min.size(); j++) {
				HistoricalDataEx candle = candles1Min.get(j);
				if (candle.high > high) {
					high = candle.high;
				}
				if (candle.low < low) {
					low = candle.low;
				}
				volSum = volSum + candle.volume;
			}

			double close = candles1Min.get(j).close;
			HistoricalDataEx candle = new HistoricalDataEx();
			candle.open = open;
			candle.low = low;
			candle.high = high;
			candle.volume = volSum;
			candle.close = close;
			candle.timeStamp = timeStamp;
			candlesXMin.add(candle);
		}
		return candlesXMin;
	}

	private double getSlope(List<HistoricalDataEx> subList) {
		Hashtable<Integer, Double> points = new Hashtable<Integer, Double>();
		int i = 0;

		for (HistoricalDataEx candle : subList) {
			points.put(i, candle.close);
			i++;
		}

		double slope = getSlope(points);

		return slope;
	}

	private double getSlope(Hashtable<Integer, Double> points) {
		WeightedObservedPoints obs = new WeightedObservedPoints();
		Enumeration<Integer> e = points.keys();
		int key;
		while (e.hasMoreElements()) {
			key = (int) e.nextElement();
			obs.add(key, points.get(key));
		}
		PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
		double[] coeff = fitter.fit(obs.toList());

		BigDecimal bd = new BigDecimal(coeff[1]);
		BigDecimal slope = bd.setScale(3, BigDecimal.ROUND_HALF_UP);
		return slope.doubleValue() * 10;
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

	private void plotChart(String[] columns, ArrayList<HistoricalDataEx> historicalData) {
		try {
			StockChart chart = new StockChart(columns[0] + " and " + columns[1], historicalData, columns);
			chart.pack();
			RefineryUtilities.centerFrameOnScreen(chart);
			chart.setVisible(true);
			// chart.setDropTarget(dt);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
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
