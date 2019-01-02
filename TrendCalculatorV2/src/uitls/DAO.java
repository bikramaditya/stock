package uitls;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.dbcp2.BasicDataSource;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.zerodhatech.models.HistoricalData;

import entity.HistoricalDataEx;
import entity.Opportunity;
import entity.Stock;
import entity.TradeType;

public class DAO 
{
	static BasicDataSource pool = null;
	DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	static final long ONE_MINUTE_IN_MILLIS=60000;// milli seconds
	public DAO() 
	{
		if(pool==null)
		{
			pool = new BasicDataSource();
			
			pool.setDriverClassName("com.mysql.cj.jdbc.Driver");
			pool.setUsername("root");
			pool.setPassword("root");
			pool.setUrl("jdbc:mysql://localhost/stock1");
			pool.setInitialSize(4);
			pool.setMinIdle(4);
			pool.setMaxIdle(20);
			pool.setMaxOpenPreparedStatements(180);
			pool.setMaxTotal(10);
		}
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
	private Connection getConnection() 
	{
		Connection conn = null;
		try {
			conn = pool.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return conn;
	}
	
	public void insertToRecomendation(Stock stock, LocalDateTime localDate, Opportunity opty) 
	{
		Connection conn = getConnection();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			Date date = format.parse(localDate.toString());
			Timestamp sqlDate = new java.sql.Timestamp(date.getTime());


			String query = " insert into stock_chart_prediction_ha "
					+ "(MKT, SYMBOL, TRADE_DATE, ORDER_TYPE, ENTRY_PRICE, EXIT_PRICE, MA, MOM, MACD, PVT, SLOPE, IS_VALID, SCORE, LastCandleClose)"
					+ " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.setString(1, stock.MKT);
			preparedStmt.setString(2, stock.SYMBOL);
			preparedStmt.setTimestamp(3, sqlDate);
			preparedStmt.setString(4, opty.TradeType.toString());
			preparedStmt.setDouble(5, opty.EntryPrice);
			preparedStmt.setDouble(6, opty.ExitPrice);
			preparedStmt.setDouble(7, opty.MA);
			preparedStmt.setDouble(8, (opty.MOM==Double.NEGATIVE_INFINITY || opty.MOM==Double.POSITIVE_INFINITY)?0:opty.MOM);
			preparedStmt.setDouble(9, opty.MACD);
			preparedStmt.setDouble(10, opty.PVT);
			preparedStmt.setDouble(11, opty.Slope);
			preparedStmt.setInt(12, (opty.is_valid ? 1:0));
			preparedStmt.setDouble(13, (opty.Score==Double.POSITIVE_INFINITY || opty.Score==Double.NEGATIVE_INFINITY)?0:opty.Score);
			preparedStmt.setDouble(14, opty.LastCandleClose);
			
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
	
	public Table<Integer, LocalDate, Stock> loadStockData(String MKT, String SYMBOL) {
		String query = "SELECT * FROM stock_daily_data where MKT=\'" + MKT + "\' and SYMBOL=\'" + SYMBOL
				+ "\' order by TRADE_DATE desc limit 100";
		Table<Integer, LocalDate, Stock> stockData = HashBasedTable.create();
		Connection conn = getConnection();
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
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stockData;
	}

	public ArrayList<Stock> getWatchList() {
		ArrayList<Stock> watchList = new ArrayList<Stock>();
		String query = "SELECT * FROM stock_watch_list where IS_ACTIVE=1 and instrument_token is not null";
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				Stock stock = new Stock();

				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				stock.NAME = rs.getString("NAME");
				stock.instrument_token = rs.getString("instrument_token");
				watchList.add(stock);
			}
			st.close();
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return watchList;
	}

	public LocalDate getLatestDate(String mKT, String sYMBOL) {
		LocalDate date = null;
		String query = "SELECT max(TRADE_DATE) as max_date FROM stock_daily_data where MKT='" + mKT + "' and SYMBOL='"
				+ sYMBOL + "' ";
		Connection conn = getConnection();
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
			releaseConnection(conn);
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
				SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
				Date date = simpleFormat.parse(newDateTime.toString());
				java.sql.Timestamp sqlDate = new java.sql.Timestamp(date.getTime());

				String query = " insert into stock_intraday_data (MKT, SYMBOL, TRADE_DATE, OPEN, HIGH,LOW,CLOSE,VOLUME)"
						+ " values (?, ?, ?, ?, ?,?, ?, ?)";
				Connection conn = getConnection();
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
				releaseConnection(conn);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public LocalDateTime getLatestIntraDayTime(String mKT, String sYMBOL) {
		LocalDateTime date = null;
		String query = "SELECT max(TRADE_DATE) as max_date FROM stock_intraday_data where MKT='" + mKT
				+ "' and SYMBOL='" + sYMBOL + "' ";
		Connection conn = getConnection();
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
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return date;
	}

	public void storeHolidays(Set<String> holidays, String MKT) {
		for (String holiday : holidays) {
			insertHoliday(holiday, MKT);
		}
	}

	private void insertHoliday(String holiday, String MKT) {
		Connection conn = getConnection();
		try {
			String query = " insert into market_holidays (MKT, TRADE_DATE)" + " values (?, ?)";
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date date = format.parse(holiday);
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());

			PreparedStatement preparedStmt = conn.prepareStatement(query);

			preparedStmt.setString(1, MKT);
			preparedStmt.setDate(2, sqlDate);

			preparedStmt.execute();
			
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> getTradingHolidays(int year, String MKT) {
		ArrayList<String> holidays = new ArrayList<String>();

		String query = "select TRADE_DATE from market_holidays where MKT='" + MKT + "' and date(TRADE_DATE) > date('"
				+ year + "-01-01') order by TRADE_DATE asc";
		Connection conn = getConnection();
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
			
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return holidays;
	}

	public String[] getAccessToken(String todayYYMMDD) {
		String[] tokens = null;

		String query = "select access_token, public_token from access_tokens where date(token_date) = date('" + todayYYMMDD + "')";
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			tokens = new String[2];
			while (rs.next()) {
				tokens[0] = rs.getString("access_token");
				tokens[1] = rs.getString("public_token");
			}

			st.close();
			
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tokens;
	}

	public void storeAccessToken(String todayYYMMDD, String accessToken, String publicToken) {
		Connection conn = getConnection();

		try {
			String query = " insert into access_tokens (token_date, access_token, public_token)" + " values (?, ?, ?)";

			PreparedStatement preparedStmt = conn.prepareStatement(query);

			preparedStmt.setString(1, todayYYMMDD);
			preparedStmt.setString(2, accessToken);
			preparedStmt.setString(3, publicToken);
			
			preparedStmt.execute();
			
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertToIntraDayPriceTable(String MKT, String SYMBOL, LocalDateTime newDateTime, double price, double open, double high, double low, double close, long volume) 
	{
		try {

			SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
			Date date = simpleFormat.parse(newDateTime.toString());
			java.sql.Timestamp sqlDate = new java.sql.Timestamp(date.getTime());

			String query = " insert into stock_intraday_data (MKT, SYMBOL, TRADE_DATE, QUOTE, OPEN, HIGH, LOW, CLOSE,VOLUME)"
					+ " values (?, ?, ?, ?, ?,?,?,?,?)";
			Connection conn = getConnection();
			PreparedStatement preparedStmt = conn.prepareStatement(query);
			
			preparedStmt.setString(1, MKT);
			preparedStmt.setString(2, SYMBOL);
			preparedStmt.setTimestamp(3, sqlDate);
			preparedStmt.setDouble(4, price);
			preparedStmt.setDouble(5, open);
			preparedStmt.setDouble(6, high);
			preparedStmt.setDouble(7, low);
			preparedStmt.setDouble(8, close);
			preparedStmt.setLong(9, volume);

			preparedStmt.execute();
			
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateInstrumentToken(String exc, String sym, long inst_token) {
		try {
			Connection conn = getConnection();
			String query = " update stock_watch_list set instrument_token=? where MKT=? and SYMBOL=?";

			PreparedStatement preparedStmt = conn.prepareStatement(query);
			
			preparedStmt.setString(1, ""+inst_token);
			preparedStmt.setString(2, exc);
			preparedStmt.setString(3, sym);
			
			preparedStmt.execute();
			
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Date getLastCandleTimeStamp(Stock stock) {
		Date lastTime = null;
		String query = "select max(TIMESTAMP) as lastTime from stock_candle_data_5min where MKT='"+stock.MKT+"' and SYMBOL='"+stock.SYMBOL+"'";
		Connection conn = getConnection();
		try {
			Timestamp timeStamp = null; 
			
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				timeStamp = rs.getTimestamp("lastTime");
			}
			if (timeStamp != null) {
				lastTime = timeStamp;
			}
			st.close();
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(lastTime==null)
		{
			Calendar date = Calendar.getInstance();			
			long t= date.getTimeInMillis();
			lastTime = new Date(t - (1440 * ONE_MINUTE_IN_MILLIS));
		}
		return lastTime;
	}

	public void insertToCandleTable(Stock stock, List<HistoricalData> dataArrayList) {
		try {

			String query = "INSERT INTO stock_candle_data_5min (`MKT`, `SYMBOL`, `INSTRUMENT_TOKEN`, `TIMESTAMP`, `OPEN`, `HIGH`, `LOW`, `CLOSE`, `VOLUME`) \r\n" + 
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
			Connection conn = getConnection();
			PreparedStatement preparedStmt = conn.prepareStatement(query);
			SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
			
			for(HistoricalData candle :dataArrayList)
			{
				Date date = simpleFormat.parse(candle.timeStamp);
				java.sql.Timestamp TIMESTAMP = new java.sql.Timestamp(date.getTime());
				
				preparedStmt.setString(1, stock.MKT);
				preparedStmt.setString(2, stock.SYMBOL);
				preparedStmt.setString(3, stock.instrument_token);
				preparedStmt.setTimestamp(4, TIMESTAMP);
				preparedStmt.setDouble(5, candle.open);
				preparedStmt.setDouble(6, candle.high);
				preparedStmt.setDouble(7, candle.low);
				preparedStmt.setDouble(8, candle.close);
				preparedStmt.setLong(9, candle.volume);

				preparedStmt.execute();	
			}
			
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Stock> getFreshDataWatchList(String MKT) {
		ArrayList<Stock> watchList = new ArrayList<Stock>();
		String query = "SELECT * FROM stock_watch_list where IS_ACTIVE=1 and IS_FRESH_DATA=1 and instrument_token is not null\r\n" + 
				"and symbol not in (SELECT distinct(symbol) FROM stock1.stock_chart_prediction_ha where date(trade_date)=CURDATE() and is_valid=1 and IS_TRADED=1 and MKT='"+MKT+"')";
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				Stock stock = new Stock();

				stock.MKT = rs.getString("MKT");
				stock.SYMBOL = rs.getString("SYMBOL");
				stock.NAME = rs.getString("NAME");
				stock.instrument_token = rs.getString("instrument_token");
				watchList.add(stock);
			}
			st.close();
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return watchList;
	}

	public ArrayList<HistoricalDataEx> getHistoricalData(Stock stock) {
		ArrayList<HistoricalDataEx> historicalData = new ArrayList<HistoricalDataEx>();
		String query = "SELECT * FROM stock_candle_data_1min  where MKT='"+stock.MKT+"' and SYMBOL='"+stock.SYMBOL+"' order by TIMESTAMP desc limit 450";
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				HistoricalDataEx data = new HistoricalDataEx();

				data.timeStamp = rs.getString("TIMESTAMP");
				data.open = rs.getDouble("OPEN");
				data.high = rs.getDouble("HIGH");
				data.low = rs.getDouble("LOW");
				data.close = rs.getDouble("CLOSE");
				data.volume = rs.getLong("VOLUME");
				
				historicalData.add(data);
			}
			st.close();
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return historicalData;
	}

	public void updateWatchListAnalysisEnd(String MKT) 
	{
		try {
			Connection conn = getConnection();
			String query = " update stock_watch_list set IS_FRESH_DATA=0 where MKT=?";

			PreparedStatement preparedStmt = conn.prepareStatement(query);
			
			preparedStmt.setString(1, MKT);
			
			preparedStmt.execute();
			
			releaseConnection(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}