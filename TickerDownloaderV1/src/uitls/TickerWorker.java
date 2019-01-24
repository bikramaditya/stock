package uitls;

import java.util.ArrayList;

import com.zerodhatech.models.Tick;

public class TickerWorker implements Runnable {
	ArrayList<Tick> ticks;

	public TickerWorker(Object parameter) {
		this.ticks = (ArrayList<Tick>) parameter;
	}

	@Override
	public void run() {
		DAO dao = new DAO();
		dao.insertTicks(ticks);
	}
}
