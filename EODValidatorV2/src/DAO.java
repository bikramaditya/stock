import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
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

	
	private void checkConnection() {
		if (conn == null) {
			System.out.println("Database connection error");
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

				watchList.add(stock);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return watchList;
	}

	

	public void insertEODValues(Stock stock, boolean buy_hit, LocalDateTime buy_hit_time, boolean sell_hit, LocalDateTime sell_hit_time) {
		checkConnection();

		try {
			SimpleDateFormat yyyymmdd = new SimpleDateFormat("yyyy-MM-dd");
			//Date date = yyyymmdd.parse(stock.DATE.toString());
			java.sql.Date sqlDate = new java.sql.Date(new Date().getTime());

			String query = " update stock_daily_prediction set DID_BUY_EXECUTE=?, BUY_EXEC_TIME=?, DID_SELL_EXECUTE=?, SELL_EXEC_TIME=? where TRADE_DATE=? and MKT=? and SYMBOL=?" ;
			
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
				if(date.length() >=10)
				{
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


	public ArrayList<IntraDayStock> getIntradayData(String MKT, String SYMBOL) {
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
				String sDate = rs.getString("TRADE_DATE");
				stock.TIME = LocalDateTime.parse(sDate, format);
				stock.OPEN = rs.getDouble("OPEN");
				stock.HIGH = rs.getDouble("HIGH");
				stock.LOW = rs.getDouble("LOW");
				stock.CLOSE = rs.getDouble("CLOSE");
				stock.VOLUME = rs.getLong("VOLUME");
				stock.QUOTE =  rs.getDouble("QUOTE");
				stockData.add(stock);
			}
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stockData;
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
				
				reco.ACTUAL_BUY = rs.getDouble("ACTUAL_BUY");
				reco.ACTUAL_SELL = rs.getDouble("ACTUAL_SELL");
			}

			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reco;
	}
}
