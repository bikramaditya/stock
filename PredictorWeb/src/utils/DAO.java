package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.csv.CSVRecord;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import entities.DailyData;
import entities.IntraDayStock;
import entities.Recomendation;
import entities.Stock;
import javafx.print.Collation;

public class DAO {
	Connection conn = null;
	DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public DAO() {
	}

	private Connection getConnection() {
		Connection conn = null;
		try {
			Context initialContext = new InitialContext();
			Context environmentContext = (Context) initialContext.lookup("java:comp/env");
			String dataResourceName = "jdbc/stock";
			DataSource dataSource = (DataSource) environmentContext.lookup(dataResourceName);
			conn = dataSource.getConnection();

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return conn;
	}

	private void releaseConnection(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public void insertToDailyPriceTable(String MKT, String SYMBOL, LocalDate latestDate, CSVRecord csvRecord) {
		conn = getConnection();

		try {
			String timestamp = csvRecord.get(0);
			if ("timestamp".equals(timestamp)) {
				return;
			}
			String localDate = timestamp.substring(0, 10);
			DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			LocalDate stockDate = LocalDate.parse(localDate, yyyymmdd);

			String OPEN = csvRecord.get(1);
			String HIGH = csvRecord.get(2);
			String LOW = csvRecord.get(3);
			String CLOSE = csvRecord.get(4);
			String VOLUME = csvRecord.get(5);

			if (latestDate == null || stockDate.isAfter(latestDate)) {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				Date date = format.parse(localDate);
				java.sql.Date sqlDate = new java.sql.Date(date.getTime());

				String query = " insert into stock_daily_data (MKT, SYMBOL, TRADE_DATE, OPEN, HIGH,LOW,CLOSE,VOLUME)"
						+ " values (?, ?, ?, ?, ?,?, ?, ?)";

				PreparedStatement preparedStmt = conn.prepareStatement(query);
				preparedStmt.setString(1, MKT);
				preparedStmt.setString(2, SYMBOL);
				preparedStmt.setDate(3, sqlDate);
				preparedStmt.setDouble(4, Double.parseDouble(OPEN));
				preparedStmt.setDouble(5, Double.parseDouble(HIGH));
				preparedStmt.setDouble(6, Double.parseDouble(LOW));
				preparedStmt.setDouble(7, Double.parseDouble(CLOSE));
				preparedStmt.setLong(8, Long.parseLong(VOLUME));

				preparedStmt.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	public Table<Integer, LocalDate, Stock> loadStockData(String MKT, String SYMBOL) {
		String query = "SELECT * FROM stock_daily_data where MKT=\'" + MKT + "\' and SYMBOL=\'" + SYMBOL
				+ "\' order by TRADE_DATE desc limit 100";
		Table<Integer, LocalDate, Stock> stockData = HashBasedTable.create();

		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			int i = 0;
			while (rs.next()) {
				Stock stock = new Stock();
				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				/*
				stock.DATE = LocalDate.parse(rs.getString("TRADE_DATE"), format);
				stock.OPEN = rs.getDouble("OPEN");
				stock.HIGH = rs.getDouble("HIGH");
				stock.LOW = rs.getDouble("LOW");
				stock.CLOSE = rs.getDouble("CLOSE");
				stock.VOLUME = rs.getLong("VOLUME");
				
				stockData.put(i, stock.DATE, stock);
				*/
				i++;
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			releaseConnection(conn);
		}
		return stockData;
	}

	public double getAverage(String mKT, String sYMBOL, String column, LocalDate fromDate, LocalDate toDate) {
		double avg = 0.0;
		conn = getConnection();
		String query = "SELECT avg(HIGH) as AVERAGE FROM stock_daily_data \r\n" + "where \r\n" + "MKT='" + mKT
				+ "' \r\n" + "and SYMBOL='" + sYMBOL + "' \r\n" + "and TRADE_DATE < '" + toDate + "' \r\n"
				+ "and TRADE_DATE > '" + fromDate.toString() + "'";

		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				avg = rs.getDouble("AVERAGE");
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return avg;
	}

	public double getColumnValue(String mKT, String sYMBOL, String column, LocalDate theDate) {
		double col = 0.0;
		conn = getConnection();
		String query = "SELECT " + column + " FROM stock_daily_data \r\n" + "where \r\n" + "MKT='" + mKT + "' \r\n"
				+ "and SYMBOL='" + sYMBOL + "' \r\n" + "and date(TRADE_DATE) = date('" + theDate
				+ "') order by TRADE_DATE asc";

		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				col = rs.getDouble(column);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return col;
	}

	public double getSlope(String mKT, String sYMBOL, String column, int days, LocalDate date) {
		double slope = 0.0;
		
		LocalDate[] minMax = getActualMinMaxDate(mKT, sYMBOL, column, days, date);
		if (minMax == null || minMax.length < 2 || minMax[0] == null || minMax[1] == null) {
			return slope;
		}
		double valPrev = getColumnValue(mKT, sYMBOL, column, minMax[0]);
		double valCurr = getColumnValue(mKT, sYMBOL, column, minMax[1]);

		LocalDate tempDateTime = LocalDate.from(minMax[0]);

		long days_count = tempDateTime.until(minMax[1], ChronoUnit.DAYS);

		slope = (valCurr - valPrev) / days_count;
		if (Double.isNaN(slope)) {
			slope = 0.0;
		}
		return slope;
	}

	private LocalDate[] getActualMinMaxDate(String mKT, String sYMBOL, String column, int days, LocalDate theDate) {
		LocalDate lastDate = theDate.minusDays(days);
		String obj1 = null;
		String obj2 = null;
		LocalDate[] min_max = new LocalDate[2];
		String query = "SELECT max(TRADE_DATE) as max_date, min(TRADE_DATE) as min_date FROM stock_daily_data \r\n"
				+ "where \r\n" + "MKT='" + mKT + "' \r\n" + "and SYMBOL='" + sYMBOL + "' \r\n"
				+ "and date(TRADE_DATE) >= date(\'" + lastDate + "\') and date(TRADE_DATE) <= date('" + theDate
				+ "') order by TRADE_DATE asc";
		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			LocalDate max_date = null;
			LocalDate min_date = null;

			while (rs.next()) {
				obj1 = rs.getString("max_date");
				obj2 = rs.getString("min_date");
				if (obj1 != null && obj2 != null) {
					max_date = LocalDate.parse(obj1, format);
					min_date = LocalDate.parse(obj2, format);
				}
			}

			min_max[0] = min_date;
			min_max[1] = max_date;

			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return min_max;
	}

	public double getLatestColumnValue(String mKT, String sYMBOL, String column, LocalDate theDate) {
		double col = 0.0;
		String query = "SELECT " + column + " FROM stock_daily_data where MKT='" + mKT + "' and SYMBOL='" + sYMBOL
				+ "' and date(TRADE_DATE) = (select date(max(TRADE_DATE)) FROM stock_daily_data where MKT='" + mKT
				+ "' and SYMBOL='" + sYMBOL + "' and date(TRADE_DATE) <= date('" + theDate
				+ "') ) order by TRADE_DATE asc";
		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				col = rs.getDouble(column);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return col;
	}

	public ArrayList<Stock> getActiveWatchList() {
		conn = getConnection();
		ArrayList<Stock> watchList = new ArrayList<Stock>();
		String query = "SELECT * FROM stock.stock_watch_list where IS_ACTIVE=1";

		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				Stock stock = new Stock();

				stock.ID = rs.getInt("ID");
				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				stock.NAME = rs.getString("NAME");
				stock.INDUSTRY = rs.getString("INDUSTRY");
				stock.SECTOR = rs.getString("SECTOR");

				watchList.add(stock);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			releaseConnection(conn);
		}
		return watchList;
	}
	
	public TreeMap<String, Stock> getWatchList(String MKT) {
		TreeMap<String, Stock> watchList = new TreeMap<String, Stock>();
		String query = "SELECT * FROM stock.stock_watch_list where IS_ACTIVE=1 and MKT='"+MKT+"'";

		try {
			conn = getConnection();
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				Stock stock = new Stock();
				stock.ID = rs.getInt("ID");
				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				stock.NAME = rs.getString("NAME");
				stock.INDUSTRY = rs.getString("INDUSTRY");
				stock.SECTOR = rs.getString("SECTOR");
				stock.IS_ACTIVE =rs.getBoolean("IS_ACTIVE"); 
				watchList.put(stock.MKT+":"+stock.SYMBOL,stock);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
		return watchList;
	}

	public LocalDate getLatestDate(String mKT, String sYMBOL) {
		LocalDate date = null;
		String query = "SELECT max(TRADE_DATE) as max_date FROM stock_intraday_data where MKT='" + mKT
				+ "' and SYMBOL='" + sYMBOL + "' ";
		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			String obj1 = null;
			while (rs.next()) {
				obj1 = rs.getString("max_date");
			}
			if (obj1 != null) {
				date = LocalDate.parse(obj1, format);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return date;
	}

	public LocalDate getNextDate(String mKT, String sYMBOL, LocalDate currDate) {
		LocalDate date = null;
		String query = "SELECT min(TRADE_DATE) as next_date FROM stock_daily_data where date(TRADE_DATE) > date('"
				+ currDate + "') and MKT='" + mKT + "' and SYMBOL='" + sYMBOL + "' ";
		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			String obj1 = null;
			while (rs.next()) {
				obj1 = rs.getString("next_date");
			}
			if (obj1 != null) {
				date = LocalDate.parse(obj1, format);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return date;
	}

	public void insertToIntraDayPriceTable(String MKT, String SYMBOL, LocalDateTime latestDateTime,
			CSVRecord csvRecord) {
		try {

			String timestamp = csvRecord.get(0);
			if ("timestamp".equals(timestamp)) {
				return;
			}

			String OPEN = csvRecord.get(1);
			String HIGH = csvRecord.get(2);
			String LOW = csvRecord.get(3);
			String CLOSE = csvRecord.get(4);
			String VOLUME = csvRecord.get(5);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

			LocalDateTime oldDateTime = LocalDateTime.parse(timestamp, formatter);
			ZoneId oldZone = ZoneId.of("America/New_York");
			ZoneId newZone = ZoneId.of("Asia/Kolkata");
			LocalDateTime newDateTime = oldDateTime.atZone(oldZone).withZoneSameInstant(newZone).toLocalDateTime();

			if (latestDateTime == null || newDateTime.isAfter(latestDateTime)) {
				SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
				Date date = simpleFormat.parse(newDateTime.toString());
				java.sql.Timestamp sqlDate = new java.sql.Timestamp(date.getTime());
				conn = getConnection();
				String query = " insert into stock_intraday_data (MKT, SYMBOL, TRADE_DATE, OPEN, HIGH,LOW,CLOSE,VOLUME)"
						+ " values (?, ?, ?, ?, ?,?, ?, ?)";

				PreparedStatement preparedStmt = conn.prepareStatement(query);
				preparedStmt.setString(1, MKT);
				preparedStmt.setString(2, SYMBOL);
				preparedStmt.setTimestamp(3, sqlDate);
				preparedStmt.setDouble(4, Double.parseDouble(OPEN));
				preparedStmt.setDouble(5, Double.parseDouble(HIGH));
				preparedStmt.setDouble(6, Double.parseDouble(LOW));
				preparedStmt.setDouble(7, Double.parseDouble(CLOSE));
				preparedStmt.setLong(8, Long.parseLong(VOLUME));

				preparedStmt.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	public LocalDateTime getLatestIntraDayTime(String mKT, String sYMBOL) {
		LocalDateTime date = null;
		String query = "SELECT max(TRADE_DATE) as max_date FROM stock_intraday_data where MKT='" + mKT
				+ "' and SYMBOL='" + sYMBOL + "' ";
		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			String obj1 = null;
			while (rs.next()) {
				obj1 = rs.getString("max_date");
			}
			if (obj1 != null) {
				date = LocalDateTime.parse(obj1, format);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return date;
	}

	public double getIntraDaySlope(String mKT, String sYMBOL, LocalDateTime startTime, LocalDateTime targetTime) {
		double startVal = getValueAtTime(mKT, sYMBOL, startTime);
		double endVal = getValueAtTime(mKT, sYMBOL, targetTime);
		if (startVal == 0.0 || endVal == 0.0) {
			return Double.NaN;
		}
		return (endVal - startVal) / 2.0;
	}

	double getValueAtTime(String mKT, String sYMBOL, LocalDateTime atTime) {
		String query = "select QUOTE as value from stock_intraday_data where symbol='" + sYMBOL + "' and MKT='" + mKT
				+ "' " + "and TRADE_DATE = (select min(TRADE_DATE) from stock_intraday_data " + "where symbol='"
				+ sYMBOL + "' and MKT='" + mKT + "' and TRADE_DATE >= timestamp('" + atTime + "') )";
		double value = 0.0;
		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				value = rs.getDouble("value");
			}

			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return value;
	}

	public void storeHolidays(Set<String> holidays, String MKT) {
		for (String holiday : holidays) {
			insertHoliday(holiday, MKT);
		}
	}

	private void insertHoliday(String holiday, String MKT) {
		conn = getConnection();

		try {
			String query = " insert into market_holidays (MKT, TRADE_DATE)" + " values (?, ?)";
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date date = format.parse(holiday);
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());

			PreparedStatement preparedStmt = conn.prepareStatement(query);

			preparedStmt.setString(1, MKT);
			preparedStmt.setDate(2, sqlDate);

			preparedStmt.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	public ArrayList<LocalDate> getTradingHolidays(int year, String MKT) {
		ArrayList<LocalDate> holidays = new ArrayList<LocalDate>();

		String query = "select TRADE_DATE from market_holidays where MKT='" + MKT + "' and date(TRADE_DATE) > date('"
				+ year + "-01-01') order by TRADE_DATE asc";
		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			String date = "";
			while (rs.next()) {
				date = rs.getString("TRADE_DATE");
				if (date.length() >= 10) {
					date = date.substring(0, 10);
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
					LocalDate formatedDate = LocalDate.parse(date, formatter);
					holidays.add(formatedDate);
				}
			}

			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}

		return holidays;
	}

	public void insertToRecomendationWeb(String mKT, String sYMBOL, LocalDate localDate, double valHigh, double valLow,
			double reco_buy, double reco_sell) {
		conn = getConnection();

		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date date = format.parse(localDate.toString());
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());

			String query = " insert into web_stock_daily_prediction (MKT, SYMBOL, TRADE_DATE, PREDICTED_HIGH, PREDICTED_LOW,RECO_BUY,RECO_SELL)"
					+ " values (?, ?, ?, ?, ?,?, ?)";

			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.setString(1, mKT);
			preparedStmt.setString(2, sYMBOL);
			preparedStmt.setDate(3, sqlDate);
			preparedStmt.setDouble(4, valHigh);
			preparedStmt.setDouble(5, valLow);
			preparedStmt.setDouble(6, reco_buy);
			preparedStmt.setDouble(7, reco_sell);

			preparedStmt.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
	}
	
	public void insertToRecomendation(String mKT, String sYMBOL, LocalDate localDate, double valHigh, double valLow,
			double reco_buy, double reco_sell, java.sql.Timestamp timeNow, float sectorIndex) {
		conn = getConnection();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date date = format.parse(localDate.toString());
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());


			String query = " insert into web_stock_daily_prediction (MKT, SYMBOL, TRADE_DATE, PREDICTED_HIGH, PREDICTED_LOW,RECO_BUY,RECO_SELL,PREDICTION_TIME, SECTOR_INDEX)"
					+ " values (?, ?, ?, ?, ?,?, ?,?,?)";

			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.setString(1, mKT);
			preparedStmt.setString(2, sYMBOL);
			preparedStmt.setDate(3, sqlDate);
			preparedStmt.setDouble(4, valHigh);
			preparedStmt.setDouble(5, valLow);
			preparedStmt.setDouble(6, reco_buy);
			preparedStmt.setDouble(7, reco_sell);
			preparedStmt.setTimestamp(8, timeNow); 
			preparedStmt.setDouble(9, sectorIndex);
			
			preparedStmt.execute();
			preparedStmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	public void insertEODValues(Stock stock) {
		
/*	
  		conn = getConnection();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date date = format.parse(stock.DATE.toString());
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());

			String query = " update stock_daily_prediction set ACTUAL_HIGH=" + stock.HIGH + ", ACTUAL_LOW=" + stock.LOW
					+ " where TRADE_DATE=? and MKT=? and SYMBOL=?";

			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.setDate(1, sqlDate);
			preparedStmt.setString(2, stock.MKT);
			preparedStmt.setString(3, stock.SYMBOL);

			preparedStmt.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
*/
	}

	public HashMap<String, Table<Integer, LocalDate, Stock>> loadAllStockData(ArrayList<Stock> watchList) {
		HashMap<String, Table<Integer, LocalDate, Stock>> stockDataMap = new HashMap<String, Table<Integer, LocalDate, Stock>>();

		watchList.forEach(stock -> {
			Table<Integer, LocalDate, Stock> stockData = loadStockData(stock.MKT, stock.SYMBOL);
			String key = stock.MKT + ":" + stock.SYMBOL;
			stockDataMap.put(key, stockData);
		});

		return stockDataMap;
	}

	public Recomendation getTradeRecomendations(String mKT, String sYMBOL) {
		Recomendation reco = new Recomendation();

		String query = "select * from web_stock_daily_prediction where SYMBOL='" + sYMBOL + "' and MKT='" + mKT
				+ "' and date(TRADE_DATE) = date(DATE_SUB(NOW(), INTERVAL 0 DAY))";
		try {
			conn = getConnection();
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				reco.MKT = rs.getString("MKT");
				reco.SYMBOL = rs.getString("SYMBOL");
				reco.TRADE_DATE = rs.getString("TRADE_DATE");
				reco.PREDICTED_HIGH = rs.getDouble("PREDICTED_HIGH");
				reco.PREDICTED_LOW = rs.getDouble("PREDICTED_LOW");
				reco.RECO_BUY = rs.getDouble("RECO_BUY");
				reco.RECO_SELL = rs.getDouble("RECO_SELL");
				reco.ACTUAL_BUY = rs.getDouble("ACTUAL_BUY");
				reco.ACTUAL_SELL = rs.getDouble("ACTUAL_SELL");
				reco.SECTOR_INDEX = rs.getDouble("SECTOR_INDEX");
			}

			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
		return reco;
	}

	public ArrayList<IntraDayStock> intraDayData(String MKT, String SYMBOL) {
		String query = "SELECT * FROM stock_intraday_data where MKT=\'" + MKT + "\' and SYMBOL=\'" + SYMBOL
				+ "\' and date(TRADE_DATE) = date(DATE_SUB(NOW(), INTERVAL 0 DAY)) order by TRADE_DATE asc";

		ArrayList<IntraDayStock> stockData = new ArrayList<IntraDayStock>();
		conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			while (rs.next()) {
				IntraDayStock stock = new IntraDayStock();
				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				stock.TIME = LocalDateTime.parse(rs.getString("TRADE_DATE"), format);
				stock.OPEN = rs.getFloat("OPEN");
				stock.HIGH = rs.getFloat("HIGH");
				stock.LOW = rs.getFloat("LOW");
				stock.CLOSE = rs.getFloat("CLOSE");
				stock.QUOTE = rs.getDouble("QUOTE");
				stock.VOLUME = rs.getLong("VOLUME");
				stockData.add(stock);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
		
		return stockData;
	}

	public void updateRecomendation(Recomendation reco) {
		conn = getConnection();
		String query = "update web_stock_daily_prediction set ACTUAL_BUY=?, ACTUAL_SELL= ?, LIVE_QUOTE=?, ORDER_TYPE=? where SYMBOL='"
				+ reco.SYMBOL + "' and MKT='" + reco.MKT
				+ "' and date(TRADE_DATE) = date(DATE_SUB(NOW(), INTERVAL 0 DAY))";
		try {
			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.setDouble(1, reco.ACTUAL_BUY);
			preparedStmt.setDouble(2, reco.ACTUAL_SELL);
			preparedStmt.setDouble(3, reco.LIVE_QUOTE);
			preparedStmt.setString(4, reco.ORDER_TYPE);
			
			preparedStmt.execute();

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
	}

	public Stock getStockDailyData(Stock stock) 
	{
		conn = getConnection();
		
		String query = "SELECT * FROM stock_daily_data where MKT=\'" + stock.MKT + "\' and SYMBOL=\'" + stock.SYMBOL
				+ "\' order by TRADE_DATE desc limit 60";
		
		
		ArrayList<DailyData> dailyData = new ArrayList<DailyData>();
		
		try {
			
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			
			while (rs.next()) 
			{				
				DailyData data = new DailyData();
				
				data.DATE = LocalDate.parse(rs.getString("TRADE_DATE"), format);
				data.OPEN = rs.getFloat("OPEN");
				data.HIGH = rs.getFloat("HIGH");
				data.LOW = rs.getFloat("LOW");
				data.CLOSE = rs.getFloat("CLOSE");
				data.VOLUME = rs.getLong("VOLUME");
				dailyData.add(data);
			}
			Collections.sort(dailyData);
			stock.dailyData=dailyData;
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
		return stock;
	}

	public ArrayList<IntraDayStock> getIntradayData(String MKT, String SYMBOL) {
		String query = "SELECT * FROM stock_intraday_data where MKT=\'" + MKT + "\' and SYMBOL=\'" + SYMBOL
				+ "\' and date(TRADE_DATE) = date(DATE_SUB(NOW(), INTERVAL 0 DAY)) order by TRADE_DATE asc";
		
		ArrayList<IntraDayStock> stockData = new ArrayList<IntraDayStock>();

		try {
			conn = getConnection();
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			while (rs.next()) {
				IntraDayStock stock = new IntraDayStock();
				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				String sDate = rs.getString("TRADE_DATE");
				stock.TIME = LocalDateTime.parse(sDate, format);
				stock.OPEN = rs.getFloat("OPEN");
				stock.HIGH = rs.getFloat("HIGH");
				stock.LOW = rs.getFloat("LOW");
				stock.CLOSE = rs.getFloat("CLOSE");
				stock.VOLUME = rs.getLong("VOLUME");
				stock.QUOTE =  rs.getDouble("QUOTE");
				stockData.add(stock);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
		return stockData;
	}

	public void insertEODValues(Stock stock, boolean buy_hit, LocalDateTime buy_hit_time, boolean sell_hit,
			LocalDateTime sell_hit_time) {
		try {
			SimpleDateFormat yyyymmdd = new SimpleDateFormat("yyyy-MM-dd");
			//Date date = yyyymmdd.parse(stock.DATE.toString());
			java.sql.Date sqlDate = new java.sql.Date(new Date().getTime());

			String query = " update web_stock_daily_prediction set DID_BUY_EXECUTE=?, BUY_EXEC_TIME=?, DID_SELL_EXECUTE=?, SELL_EXEC_TIME=? where TRADE_DATE=? and MKT=? and SYMBOL=?" ;
			
			SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			
			java.sql.Timestamp buy_exec_time = null;
			java.sql.Timestamp sell_exec_time = null;
			
			try {
				
				Date temp = timeFormat.parse(buy_hit_time.toString());
				buy_exec_time = new java.sql.Timestamp(temp.getTime());
			}
			catch(Exception e) 
			{
				Util.Logger.log(0, e.getMessage());
			}
			
			try {
				
				Date temp = timeFormat.parse(sell_hit_time.toString());
				sell_exec_time = new java.sql.Timestamp(temp.getTime());
			}
			catch(Exception e) {Util.Logger.log(0, e.getMessage());}
			
			conn = getConnection();
			
			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.setBoolean(1, buy_hit);
			preparedStmt.setTimestamp(2, buy_exec_time);
			preparedStmt.setBoolean(3, sell_hit);
			preparedStmt.setTimestamp(4, sell_exec_time);
			
			preparedStmt.setDate(5, sqlDate);
			preparedStmt.setString(6, stock.MKT);
			preparedStmt.setString(7, stock.SYMBOL);
			
			preparedStmt.execute();			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			releaseConnection(conn);
		}
	}
}