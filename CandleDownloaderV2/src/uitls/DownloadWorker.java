package uitls;

import java.util.Date;

import com.rabbitmq.client.Channel;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;

import entity.Stock;

public class DownloadWorker implements Runnable{
	private Stock stock;
	private Date to;
	private Kite kite;
	
	public DownloadWorker(Kite kite, Stock stock, Date to) {
		this.kite = kite;
		this.stock = stock;
		this.to = to;
	}
	
	public void run() {
		String message = Thread.currentThread().getName() + " Start. Command = " + stock;
		System.out.println(message);
		Util.Logger.log(0, message);

		processCommand();
		
		System.out.println(Thread.currentThread().getName() + " End.");
		Util.Logger.log(0,Thread.currentThread().getName() + " End.");
	}

	private void processCommand() {
		try {
			DAO dao = new DAO();
			System.out.println("");
			Date lastTimeStamp = dao.getLastCandleTimeStamp(stock);
			Util.Logger.log(0,"before dw"+stock);
			HistoricalData historicalData = kite.getHistoricalData(lastTimeStamp,to,stock);
			
			if(historicalData!=null && historicalData.dataArrayList!=null && historicalData.dataArrayList.size() > 0)
			{
				Util.Logger.log(0,"after dw-"+stock+" size="+historicalData.dataArrayList.size());
				
				dao.insertToCandleTable(stock,historicalData.dataArrayList);
				int n = dao.updateFreshDataArrived(stock);
			}

			System.out.println("");
			
		} catch (Exception | KiteException e) {
			e.printStackTrace();
			Util.Logger.log(1,e.getMessage());
		}
	}
}
