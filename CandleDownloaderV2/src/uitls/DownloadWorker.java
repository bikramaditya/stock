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
	Channel channel;
	
	public DownloadWorker(Channel channel,Kite kite, Stock stock, Date to) {
		this.kite = kite;
		this.stock = stock;
		this.to = to;
		this.channel = channel;
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
			String Q = stock.MKT+"-"+stock.SYMBOL;
			channel.queueDeclare(Q, false, false, false, null);

			DAO dao = new DAO();
			System.out.println("");
			Date lastTimeStamp = dao.getLastCandleTimeStamp(stock);
			HistoricalData historicalData = kite.getHistoricalData(lastTimeStamp,to,stock);
			
			if(historicalData.dataArrayList.size() > 0)
			{
				dao.insertToCandleTable(stock,historicalData.dataArrayList);
				dao.updateFreshDataArrived(stock);
				String message = stock.SYMBOL+"Data Arrieved";
				channel.basicPublish("", Q, null, message.getBytes());
			}

			System.out.println("");
		} catch (Exception | KiteException e) {
			e.printStackTrace();
			Util.Logger.log(1,e.getMessage());
		}
	}
}
