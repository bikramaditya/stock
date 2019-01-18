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
import entity.MACDObj;
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

	private static ConcurrentHashMap<String, TradeType> sessionMap;
	private static String sessionXML = "";

	private String aPIKey;
	
	public CandleTrendWorker(Stock stock, String aPIKey) throws Exception {
		this.stock = stock;
		this.util = new Util();
		this.aPIKey = aPIKey;
		
		String user_home = System.getProperty("user.home");
		String today = util.getTodayYYMMDD();
		sessionXML = user_home + "\\sessionMap_" + today + "_.xml";

		if (sessionMap == null) {
			sessionMap = readSerializedZML();
		}

	}

	@SuppressWarnings("unchecked")
	private ConcurrentHashMap<String, TradeType> readSerializedZML() {
		ConcurrentHashMap<String, TradeType> sessionMap = new ConcurrentHashMap<String, TradeType>();

		XMLDecoder decoder = null;
		try {

			decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(sessionXML)));
			sessionMap = (ConcurrentHashMap<String, TradeType>) decoder.readObject();
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: File xml not found");
		}
		if (sessionMap == null) {
			return new ConcurrentHashMap<String, TradeType>();
		} else {
			return sessionMap;
		}
	}

	private void writeSerializedXML() {
		XMLEncoder encoder = null;
		try {

			encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(sessionXML)));
		} catch (FileNotFoundException fileNotFound) {
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
		String Q = stock.MKT + ":" + stock.SYMBOL;

		try {
			sessionMap.putIfAbsent(Q, TradeType.NONE);

			writeSerializedXML();

			System.out.println(" starting  '" + stock.SYMBOL + "'");

			while (util.isTradeAllowed()) {
				double[] macd15Min = GetMACD("15min");
				double[] macd5Min = GetMACD("5min");
				double[] macd1Min = GetMACD("1min");

				Util.Logger.log(0, new Date() + "-" + stock.SYMBOL + "-" + " macd15Min=" + macd15Min[0] + " macd5Min="
						+ macd5Min[0] + "macd1Min=" + macd1Min[0] + " signal15Min=" + macd15Min[1]);
				System.out.println(new Date() + "-" + stock.SYMBOL + "-" + " macd15Min=" + macd15Min[0] + " macd5Min="
						+ macd5Min[0] + " macd1Min=" + macd1Min[0] + " signal15Min=" + macd15Min[1]);

				double absSlope = Math.abs(macd15Min[0] + macd5Min[0] + macd1Min[0]);

				if (macd15Min[0] > 0.0 && macd5Min[0] > 0.0 && macd1Min[0] > 0.0 && macd15Min[1] > 0) {
					TradeType trade_type = sessionMap.get(Q);

					if (trade_type == TradeType.BUY) {
						Util.Logger.log(0, stock.SYMBOL + " Duplicate trade exists, skipping it");
						System.out.println(stock.SYMBOL + " Duplicate trade exists, skipping it");
					} else {
						sessionMap.put(Q, TradeType.BUY);
						writeSerializedXML();
						Opportunity opty = new Opportunity();
						opty.MA = macd15Min[0];
						opty.MOM = macd5Min[0];
						opty.PVT = macd1Min[0];
						opty.Slope = absSlope;

						opty.MKT = "NSE";
						opty.Symbol = stock.SYMBOL;
						opty.TradeType = TradeType.BUY;

						String pattern = "yyyy-MM-dd hh.mm.ss";
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

						String date = simpleDateFormat.format(new Date());

						opty.TimeStamp = date;

						opty.is_valid = true;

						System.out.println(opty);
						util.Logger.log(0, opty.toString());

						storeOpportunity(opty);
					}
				} else if (macd15Min[0] < 0 && macd5Min[0] < 0 && macd1Min[0] < 0 && macd15Min[1] < 0) {

					TradeType trade_type = sessionMap.get(Q);

					if (trade_type == TradeType.SELL) {
						Util.Logger.log(0, stock.SYMBOL + " Duplicate trade exists, skipping it");
						System.out.println(stock.SYMBOL + " Duplicate trade exists, skipping it");
					} else {
						sessionMap.put(Q, TradeType.SELL);
						writeSerializedXML();

						Opportunity opty = new Opportunity();
						opty.MA = macd15Min[0];
						opty.MOM = macd5Min[0];
						opty.PVT = macd1Min[0];
						opty.Slope = absSlope;

						opty.MKT = "NSE";
						opty.Symbol = stock.SYMBOL;
						opty.TradeType = TradeType.SELL;

						String pattern = "yyyy-MM-dd HH:mm:ss";
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

						String date = simpleDateFormat.format(new Date());

						opty.TimeStamp = date;

						opty.is_valid = true;

						System.out.println(opty);
						util.Logger.log(0, opty.toString());

						storeOpportunity(opty);
					}
				}
				Thread.sleep(60000);
			}

			Util.Logger.log(0, "Trade not allowed or Old data received");
			System.out.println("Trade not allowed or Old data received");

		} catch (Exception e) {
			Util.Logger.log(1, e.getMessage());
			e.printStackTrace();
		}

		// System.out.println(Thread.currentThread().getName() + " End.");
		Util.Logger.log(0, Thread.currentThread().getName() + " End.");
	}

	private double[] GetMACD(String interval) {

		double[] macd_signal = { 0, 0 };

		try {
			ArrayList<MACDObj> macdList = util.getMACDFromAlpha(stock, interval, aPIKey);

			MACDObj MACDNow = macdList.get(0);
			MACDObj MACDPrev = macdList.get(1);

			double macdDiff = MACDNow.MACD - MACDPrev.MACD;
			double signalDiff = MACDNow.Signal - MACDPrev.Signal;

			macd_signal[0] = macdDiff;
			macd_signal[1] = signalDiff;

			Util.Logger.log(0, stock.SYMBOL+" interval=" + interval + "nowPrev=" + MACDPrev.MACD + "nowMACD=" + MACDNow.MACD + "diff="
					+ "macdDiff=" + macdDiff + "signalDiff=" + signalDiff);
			System.out.println(stock.SYMBOL+" interval=" + interval + "nowPrev=" + MACDPrev.MACD + "nowMACD=" + MACDNow.MACD + "diff="
					+ "macdDiff=" + macdDiff + "signalDiff=" + signalDiff);
		} catch (Exception e) {
			e.printStackTrace();
			Util.Logger.log(1, e.getMessage());
		}

		return macd_signal;
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
}
