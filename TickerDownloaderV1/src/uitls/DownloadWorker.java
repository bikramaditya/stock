package uitls;

import java.util.ArrayList;
import java.util.Date;

import com.rabbitmq.client.Channel;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;

import entity.Stock;

public class DownloadWorker implements Runnable{
	private Date to;
	private Kite kite;
	ArrayList<Long> tokens;
	
	public DownloadWorker(Kite kite, ArrayList<Long> tokens) {
		this.kite = kite;
		this.tokens = tokens;
	}
	
	public void run() {
		String message = Thread.currentThread().getName() + " Start. Command = ";
		System.out.println(message);
		Util.Logger.log(0, message);

		processCommand();
		
		System.out.println(Thread.currentThread().getName() + " End.");
		Util.Logger.log(0,Thread.currentThread().getName() + " End.");
	}

	private void processCommand() {
		try {
			DAO dao = new DAO();
			kite.tickerUsage(tokens,dao);
			
		} catch (Exception | KiteException e) {
			e.printStackTrace();
			Util.Logger.log(1,e.getMessage());
		}
	}
}
