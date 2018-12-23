package actions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import entities.*;

import utils.Util;

public class Learner 
{
	private Util util = null;
	private String MKT;
	
	public Learner(String MKT) throws Exception
	{
		this.MKT = MKT;
		util = new Util();
	}
	
	public void predictAndTrade() throws Exception 
	{
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String today = util.getTodayYYMMDD();
		
		LocalDate predictForDate = LocalDate.parse(today, formatter);
		LocalDate prevTradeDate = util.getPreviousTradingDate(predictForDate, 1);
		
		if(!util.isTradingDay(predictForDate))
		{
			Util.Logger.log(1, "Not a trading day");
			return;
		}
		TreeMap<String, Stock> stocks = Util.StocksWatchList;
		
		java.util.Date now = new java.util.Date();
		java.sql.Timestamp timeNow = new java.sql.Timestamp(now.getTime());
		
		ExecutorService executor = Executors.newFixedThreadPool(8);
        
		Set<String> keys = stocks.keySet();
		
		for(String key : keys)
		{
			Stock stock = stocks.get(key);
			Runnable worker = new PredictionWorkerThread(stock,prevTradeDate,predictForDate,timeNow);
            executor.execute(worker);
            //Thread.sleep(5000);
		}
		
		executor.shutdown();
        while (!executor.isTerminated()) {
        }
        System.out.println("Finished all threads");
	}
	
//	public Result learnAndTest() throws Exception {
//		
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//		
//		ArrayList<Stock> stocks = util.getWatchList();
//		LocalDate subjectDate = LocalDate.parse("2018-10-26", formatter);
//		LocalDate testDate = LocalDate.parse("2018-10-29", formatter);
//		
//		java.util.Date now = new java.util.Date();
//		java.sql.Timestamp timeNow = new java.sql.Timestamp(now.getTime());
//		
//		for(int i = 0 ; i < stocks.size(); i++)
//		{
//			Stock stock = stocks.get(i);
//			try {
//				stock = learn(stock.SYMBOL, subjectDate);
//				Result result = test(stock, testDate);
//				
//				double valHigh = result.prediCtedVal*(1.01);
//				double valLow = result.prediCtedVal*(0.99);
//				
//				double reco_buy = result.prediCtedVal*(0.997);
//				double reco_sell = result.prediCtedVal*(1.003);
//				
//				dao.insertToRecomendation(stock.MKT, stock.SYMBOL, testDate, valHigh, valLow, reco_buy, reco_sell,timeNow);
//				System.out.println("");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		
//		return new Result();
//	}

	
//	public Result learnAStock(String SYMBOL) throws Exception {
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//		LocalDate subjectDate = LocalDate.parse("2018-10-26", formatter);
//		LocalDate testDate = LocalDate.parse("2018-10-29", formatter);
//		Result result = null;
//		Stock stock = util.StocksWatchList.get(this.MKT + ":" + SYMBOL);
//		
//		try {
//			stock = learn(stock.SYMBOL, subjectDate);
//			result = test(stock, testDate);
//			
//			
//			System.out.println("targetDate="+testDate+" ActualMidVal=" + result.actualVal + " PredictedMidVal=" + result.prediCtedVal);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		return result;
//	}

	

//	public Stock learn(String SYMBOL, LocalDate targetDate) throws Exception 
//	{
//		boolean isTradingDay = util.isTradingDay(targetDate);
//
//		if (!isTradingDay) {
//			System.out.println("Not a trading day");
//			return null;
//		}
//
//		Stock stock = util.StocksWatchList.get(this.MKT + ":" + SYMBOL);
//
//		if (stock == null) {
//			throw new Exception("Stock not in map");
//		}
//
//		stock = populatePredictionRawData(stock, targetDate);
//
//		float actualVal = getDataFor(stock.dailyData, targetDate, 0);
//		float val1DayAgo = getDataFor(stock.dailyData, targetDate, 1);
//		ArrayList<Weight> optimumWeghts = loopNow(stock, actualVal, val1DayAgo);
//		stock.optimumWeghts = optimumWeghts;
//		return stock;
//	}

	

	




	
}
