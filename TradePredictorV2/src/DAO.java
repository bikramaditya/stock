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
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class DAO {
	String myDriver = "com.mysql.cj.jdbc.Driver";
	String myUrl = "jdbc:mysql://localhost/stock";
	String user = "root";
	String pw = "root";
	Connection conn = null;
	DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public DAO() {
		try {
			if (conn == null || conn.isClosed()) {
				Class.forName(myDriver);
				conn = DriverManager.getConnection(myUrl, user, pw);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void insertToDailyPriceTable(String MKT, String SYMBOL, LocalDate latestDate, CSVRecord csvRecord) {
		checkConnection();

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
	}

	private void checkConnection() {
		if (conn == null) {
			Util.Logger.log(0, "Database connection error");
			System.exit(0);
		}
	}

	public Table<Integer, LocalDate, Stock> loadStockData(String MKT, String SYMBOL) {
		String query = "SELECT * FROM stock_daily_data where MKT=\'" + MKT + "\' and SYMBOL=\'" + SYMBOL
				+ "\' order by TRADE_DATE desc limit 100";
		Table<Integer, LocalDate, Stock> stockData = HashBasedTable.create();

		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			int i = 0;
			while (rs.next()) {
				Stock stock = new Stock();
				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				stock.DATE = LocalDate.parse(rs.getString("TRADE_DATE"), format);
				stock.OPEN = rs.getDouble("OPEN");
				stock.HIGH = rs.getDouble("HIGH");
				stock.LOW = rs.getDouble("LOW");
				stock.CLOSE = rs.getDouble("CLOSE");
				stock.VOLUME = rs.getLong("VOLUME");
				stockData.put(i, stock.DATE, stock);
				i++;
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stockData;
	}

	public double getAverage(String mKT, String sYMBOL, String column, LocalDate fromDate, LocalDate toDate) {
		double avg = 0.0;
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

		return avg;
	}

	public double getColumnValue(String mKT, String sYMBOL, String column, LocalDate theDate) {
		double col = 0.0;
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

		return min_max;
	}

	public double getLatestColumnValue(String mKT, String sYMBOL, String column, LocalDate theDate) {
		double col = 0.0;
		String query = "SELECT " + column + " FROM stock_daily_data where MKT='" + mKT + "' and SYMBOL='" + sYMBOL
				+ "' and date(TRADE_DATE) = (select date(max(TRADE_DATE)) FROM stock_daily_data where MKT='" + mKT
				+ "' and SYMBOL='" + sYMBOL + "' and date(TRADE_DATE) <= date('" + theDate
				+ "') ) order by TRADE_DATE asc";

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

		return col;
	}

	public ArrayList<Stock> getWatchList() {
		ArrayList<Stock> watchList = new ArrayList<Stock>();
		String query = "SELECT * FROM stock.stock_watch_list where IS_ACTIVE=1";

		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				Stock stock = new Stock();

				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				stock.NAME = rs.getString("NAME");
				stock.SECTOR = rs.getString("SECTOR");

				watchList.add(stock);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return watchList;
	}

	public LocalDate getLatestDate(String mKT, String sYMBOL) {
		LocalDate date = null;
		String query = "SELECT max(TRADE_DATE) as max_date FROM stock_intraday_data where MKT='" + mKT
				+ "' and SYMBOL='" + sYMBOL + "' ";

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

		return date;
	}

	public LocalDate getNextDate(String mKT, String sYMBOL, LocalDate currDate) {
		LocalDate date = null;
		String query = "SELECT min(TRADE_DATE) as next_date FROM stock_daily_data where date(TRADE_DATE) > date('"
				+ currDate + "') and MKT='" + mKT + "' and SYMBOL='" + sYMBOL + "' ";

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
	}

	public LocalDateTime getLatestIntraDayTime(String mKT, String sYMBOL) {
		LocalDateTime date = null;
		String query = "SELECT max(TRADE_DATE) as max_date FROM stock_intraday_data where MKT='" + mKT
				+ "' and SYMBOL='" + sYMBOL + "' ";

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

		return date;
	}

	public double getIntraDaySlope(String mKT, String sYMBOL, LocalDateTime startTime, LocalDateTime targetTime) {
		double startVal = getValueAtTime(mKT, sYMBOL, startTime);
		double endVal = getValueAtTime(mKT, sYMBOL, targetTime);
		if(startVal==0.0 || endVal==0.0)
		{
			return Double.NaN;
		}
		return (endVal - startVal) / 2.0;
	}

	double getValueAtTime(String mKT, String sYMBOL, LocalDateTime atTime) {
		String query = "select QUOTE as value from stock_intraday_data where symbol='" + sYMBOL + "' and MKT='" + mKT
				+ "' " + "and TRADE_DATE = (select min(TRADE_DATE) from stock_intraday_data " + "where symbol='"
				+ sYMBOL + "' and MKT='" + mKT + "' and TRADE_DATE >= timestamp('" + atTime + "') )";
		double value = 0.0;
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

		return value;
	}

	public void storeHolidays(Set<String> holidays, String MKT) {
		for (String holiday : holidays) {
			insertHoliday(holiday, MKT);
		}
	}

	private void insertHoliday(String holiday, String MKT) {
		checkConnection();

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
	}

	public ArrayList<String> getTradingHolidays(int year, String MKT) {
		ArrayList<String> holidays = new ArrayList<String>();

		String query = "select TRADE_DATE from market_holidays where MKT='" + MKT + "' and date(TRADE_DATE) > date('"
				+ year + "-01-01') order by TRADE_DATE asc";

		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			String date = "";
			while (rs.next()) {
				date = rs.getString("TRADE_DATE");
				if (date.length() >= 10) {
					date = date.substring(0, 10);
					holidays.add(date);
				}
			}

			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return holidays;
	}

	public void insertToRecomendation(String mKT, String sYMBOL, LocalDate localDate, double valHigh, double valLow,
			double reco_buy, double reco_sell) {
		checkConnection();

		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date date = format.parse(localDate.toString());
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());

			String query = " insert into stock_daily_prediction (MKT, SYMBOL, TRADE_DATE, PREDICTED_HIGH, PREDICTED_LOW,RECO_BUY,RECO_SELL)"
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
	}

	public void insertEODValues(Stock stock) {
		checkConnection();

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

		String query = "select * from stock_daily_prediction where SYMBOL='" + sYMBOL + "' and MKT='" + mKT
				+ "' and date(TRADE_DATE) = date(DATE_SUB(NOW(), INTERVAL 0 DAY))"; 
		try {
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
			}

			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reco;
	}

	public ArrayList<IntraDayStock> intraDayData(String MKT, String SYMBOL) {
		String query = "SELECT * FROM stock_intraday_data where MKT=\'" + MKT + "\' and SYMBOL=\'" + SYMBOL
				+ "\' and date(TRADE_DATE) = date(DATE_SUB(NOW(), INTERVAL 0 DAY)) order by TRADE_DATE asc";
		
		ArrayList<IntraDayStock> stockData = new ArrayList<IntraDayStock>();

		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			while (rs.next()) {
				IntraDayStock stock = new IntraDayStock();
				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				stock.TIME = LocalDateTime.parse(rs.getString("TRADE_DATE"), format);
				stock.OPEN = rs.getDouble("OPEN");
				stock.HIGH = rs.getDouble("HIGH");
				stock.LOW = rs.getDouble("LOW");
				stock.CLOSE = rs.getDouble("CLOSE");
				stock.QUOTE = rs.getDouble("QUOTE");
				stock.VOLUME = rs.getLong("VOLUME");
				stockData.add(stock);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stockData;
	}

	public void updateRecomendation(Recomendation reco) {
		String query = "update stock_daily_prediction set ACTUAL_BUY=?, ACTUAL_SELL= ?, LIVE_QUOTE=? where SYMBOL='" + reco.SYMBOL + "' and MKT='" + reco.MKT
				+ "' and date(TRADE_DATE) = date(DATE_SUB(NOW(), INTERVAL 0 DAY))"; 
		try {
			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.setDouble(1, reco.ACTUAL_BUY);
			preparedStmt.setDouble(2, reco.ACTUAL_SELL);
			preparedStmt.setDouble(3, reco.LIVE_QUOTE);

			preparedStmt.execute();
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}