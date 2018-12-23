package actions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import entities.DailyData;
import entities.IntraDayStock;
import entities.Pattern;
import entities.Result;
import entities.Slope;
import entities.Stock;
import entities.Weight;
import utils.DAO;
import utils.Util;

public class PredictionWorkerThread implements Runnable {
	private Stock stock;
	private LocalDate prevTradeDate;
	private LocalDate predictForDate;
	private DAO dao;
	private Util util;
	private java.sql.Timestamp timeNow;

	public PredictionWorkerThread(Stock stock, LocalDate prevTradeDate, LocalDate predictForDate, java.sql.Timestamp timeNow) throws Exception {
		this.stock = stock;
		this.prevTradeDate = prevTradeDate;
		this.predictForDate = predictForDate;
		this.timeNow = timeNow;
		
		dao = new DAO();
		util = new Util();

	}

	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName() + " Start. Command = " + this.stock.SYMBOL);
		processCommand();
		System.out.println(Thread.currentThread().getName() + " End.");
	}

	private void processCommand() {
		try {
			stock = learn(stock.SYMBOL, prevTradeDate);
			Result result = predict(stock, predictForDate);

			if(result !=null && result.prediCtedVal>0)
			{
				double valHigh = result.prediCtedVal * (1 + 0.01);
				double valLow = result.prediCtedVal * (1 - 0.01);

				double reco_buy = valHigh;//result.prediCtedVal * (1-0.0025);
				double reco_sell = valLow;//.prediCtedVal * (1+0.0025);
				System.out.println();
				dao.insertToRecomendation(stock.MKT, stock.SYMBOL, predictForDate, valHigh, valLow, reco_buy, reco_sell,
						timeNow, stock.SectorIndex);	
			}
			else
			{
				util.Logger.log(0, "Skipped "+stock.SYMBOL+" prediction ="+result.prediCtedVal);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private float getCurrentQote(Stock stock) {
		
		ArrayList<IntraDayStock> intraDayStockData = dao.getIntradayData(stock.MKT, stock.SYMBOL);
		String today = util.getTodayYYMMDD();
		double currPrice = 0.0;
		for (int i = 0; i < intraDayStockData.size(); i++) {
			IntraDayStock intraStock = intraDayStockData.get(i);
			LocalDateTime time = intraStock.TIME;

			if (time.isAfter(LocalDateTime.parse(today + "T10:29:30"))
					&& time.isBefore(LocalDateTime.parse(today + "T10:31:30"))) {
				currPrice = intraStock.QUOTE;
			}
		}

		return (float)currPrice;
	}

	private Result predict(Stock stock, LocalDate targetDate) throws Exception {

		stock = populatePredictionRawData(stock, targetDate);
		float val1DayAgo = getDataFor(stock.dailyData, targetDate, 1);
		float val0DayAgo = getDataFor(stock.dailyData, targetDate, 0);

		double avgPredicted = 0.0f;

		ArrayList<Integer> allPredictedVals = new ArrayList<Integer>();

		allPredictedVals.ensureCapacity(stock.optimumWeghts.size());

		for (Weight optimumWeght : stock.optimumWeghts) {
			float predictedVal = calcMidValue(stock, optimumWeght, val1DayAgo);

			allPredictedVals.add((int) predictedVal);
		}

		avgPredicted = util.mode(allPredictedVals);

		avgPredicted = avgPredicted * (100 + (util.globalIndices + util.NiftyIndex)) / 100;

		avgPredicted = avgPredicted * (1 + stock.SectorIndex / 100);

		LocalDate prevTradingDate = util.getPreviousTradingDate(targetDate, 1);

		double prevClose = util.getCloseValForDate(stock.dailyData, prevTradingDate);

		float currentPrice = getCurrentQote(stock);
		
		Result res = new Result();

		if(currentPrice==0)
		{
			return res;
		}
		
		double delta = (currentPrice-prevClose)/prevClose;
		
		
			res.prediCtedVal = avgPredicted *(1+delta);
			res.actualVal = val0DayAgo;
		
		
		System.out.println(
				"targetDate=" + targetDate + " ActualMidVal=" + val0DayAgo + " PredictedMidVal=" + avgPredicted);
		return res;
	}

	private ArrayList<Weight> loopNow(Stock stock, float actualVal, float val1DayAgo) {
		ArrayList<Weight> optimumWeights = new ArrayList<Weight>();
		for (int a = -10; a <= 10; a++) {
			for (int b = -10; b <= 10; b++) {
				for (int c = -10; c <= 10; c++) {
					for (int d = -10; d <= 10; d++)
						for (int e = -10; e <= 10; e++) {
							int f = 10;
							{
								int g = 10;
								Weight weight = new Weight();
								weight.slope45Days = a / 10.0f;
								weight.slope30Days = b / 10.0f;
								weight.slope15Days = c / 10.0f;
								weight.slope7Days = d / 10.0f;
								weight.slope3Days = e / 10.0f;
								weight.slope2Days = f / 10.0f;
								weight.maDiff = g / 10.0f;
								float predictedVal = calcMidValue(stock, weight, val1DayAgo);

								float diff = Math.abs(100 * (actualVal - predictedVal) / actualVal);

								if (diff < 0.0001) {
									optimumWeights.add(weight);
								}
							}
						}
				}
			}
		}
		return optimumWeights;
	}

	private float calcMidValue(Stock stock, Weight weight, float val1DayAgo) {
		Slope slope = stock.slope;

		float delta = slope.slope45Days * weight.slope45Days + slope.slope30Days * weight.slope30Days
				+ slope.slope15Days * weight.slope15Days + slope.slope7Days * weight.slope7Days
				+ slope.slope3Days * weight.slope3Days + slope.slope2Days * weight.slope2Days
				+ slope.maDiff * weight.maDiff;

		return val1DayAgo + delta;
	}

	public Stock learn(String SYMBOL, LocalDate targetDate) throws Exception {
		boolean isTradingDay = util.isTradingDay(targetDate);

		if (!isTradingDay) {
			System.out.println("Not a trading day");
			return null;
		}

		Stock stock = Util.StocksWatchList.get(this.stock.MKT + ":" + SYMBOL);

		if (stock == null) {
			throw new Exception("Stock not in map");
		}

		stock = populatePredictionRawData(stock, targetDate);

		float actualVal = getDataFor(stock.dailyData, targetDate, 0);
		float val1DayAgo = getDataFor(stock.dailyData, targetDate, 1);
		ArrayList<Weight> optimumWeghts = loopNow(stock, actualVal, val1DayAgo);
		stock.optimumWeghts = optimumWeghts;
		return stock;
	}

	private float getDataFor(ArrayList<DailyData> dailyData, LocalDate targetDate, int daysBack) {
		LocalDate prevTradingDate = util.getPreviousTradingDate(targetDate, daysBack);

		float prevDayVal = util.getValForDate(dailyData, prevTradingDate);
		return prevDayVal;
	}

	private Stock populatePredictionRawData(Stock stock, LocalDate targetDate) throws Exception {
		stock = dao.getStockDailyData(stock);
		Slope slope = getSlopes(stock, targetDate);

		stock.dailyData = populateMAs(stock.dailyData);

		slope = getMADiff(slope, stock, targetDate);

		slope = getSlopeFactor(slope, stock, targetDate);

		stock.slope = slope;

		return stock;
	}

	private Pattern findPattern(float val3dayAgo, float val2dayAgo, float val1dayAgo) {
		Pattern patt = Pattern.None;
		float halfWay = (val3dayAgo + val1dayAgo) / 2;

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

	private Slope getSlopeFactor(Slope slope, Stock stock, LocalDate targetDate) {

		float val3dayAgo = getDataFor(stock.dailyData, targetDate, 3);
		float val2dayAgo = getDataFor(stock.dailyData, targetDate, 2);
		float val1dayAgo = getDataFor(stock.dailyData, targetDate, 1);

		Pattern pattern = findPattern(val3dayAgo, val2dayAgo, val1dayAgo);

		float slopeFactor = 1;

		if (slope.slope2Days > 0) {
			switch (pattern) {
			case ConvexRising:
				slopeFactor = 0.5f;
				break;
			case ConvexFalling:
				slopeFactor = 1.0f;
				break;
			case ConcaveRising:
				slopeFactor = -0.5f;
				break;
			case ConcaveFalling:
				slopeFactor = -0.5f;
				break;
			default:
				slopeFactor = 1;
				break;
			}
		} else {
			switch (pattern) {
			case ConvexRising:
				slopeFactor = -0.5f;
				break;
			case ConvexFalling:
				slopeFactor = -0.5f;
				break;
			case ConcaveRising:
				slopeFactor = 0.5f;
				break;
			case ConcaveFalling:
				slopeFactor = 0.5f;
				break;
			default:
				slopeFactor = 1;
				break;
			}
		}

		slope.slopeFactor = slopeFactor;

		return slope;
	}

	private Slope getMADiff(Slope slope, Stock stock, LocalDate date) {
		float MADiff = 0;
		float latest = getDataFor(stock.dailyData, date, 1);
		float latestMA = getMADataFor(stock.dailyData, date, 1);
		MADiff = (latestMA - latest);

		slope.maDiff = MADiff;
		return slope;
	}

	private float getMADataFor(ArrayList<DailyData> dailyData, LocalDate targetDate, int daysBack) {

		LocalDate prevTradingDate = util.getPreviousTradingDate(targetDate, daysBack);

		float prevDayVal = getMAForDate(dailyData, prevTradingDate);
		return prevDayVal;
	}

	private float getMAForDate(ArrayList<DailyData> dailyData, LocalDate forDate) {
		float val = 0;
		for (DailyData data : dailyData) {
			if (data.DATE.isEqual(forDate) || data.DATE.isAfter(forDate)) {
				val = data.MA;
				break;
			}
		}
		return val;
	}

	private ArrayList<DailyData> populateMAs(ArrayList<DailyData> dailyData) {

		for (int i = 4; i < dailyData.size(); i++) {
			float sum = 0;
			for (int j = i - 4; j <= i; j++) {
				DailyData data = dailyData.get(j);
				sum = sum + (data.HIGH + data.LOW) / 2;
			}
			sum = sum / 5;
			dailyData.get(i).MA = sum;
		}

		return dailyData;
	}

	private Slope getSlopes(Stock stock, LocalDate date) throws Exception {
		Slope slope = new Slope();
		slope.slope45Days = getSlope(stock, date, 45);
		slope.slope30Days = getSlope(stock, date, 30);
		slope.slope15Days = getSlope(stock, date, 15);
		slope.slope7Days = getSlope(stock, date, 7);
		slope.slope3Days = getSlope(stock, date, 3);
		slope.slope2Days = getSlope(stock, date, 2);

		return slope;
	}

	private float getSlope(Stock stock, LocalDate targetDate, int daysBack) throws Exception {
		ArrayList<DailyData> dailyData = stock.dailyData;
		if (dailyData.size() < daysBack) {
			throw new Exception("Not enough data");
		}
		float latest = getDataFor(dailyData, targetDate, 1);
		float past = getDataFor(dailyData, targetDate, daysBack);

		return (latest - past) / 2;
	}
}
