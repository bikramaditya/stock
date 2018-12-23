import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class App 
{
	DAO dao = null;
	public static void main(String[] args) {
		DAO dao = new DAO();
		
		ArrayList<Stock> watchList = dao.getWatchList();
		
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		watchList.forEach(stock->{
			Runnable task = () -> {
			    String threadName = Thread.currentThread().getName();
			    System.out.println("Loading..."+stock.SYMBOL+" "+ threadName);
			    
			    Util util = new Util();
			    util.loadStockData(stock.MKT, stock.SYMBOL);
			};

			Future<Integer> future = (Future<Integer>) executor.submit(task);
		});

		watchList.forEach(stock->{
			Runnable task = () -> {
			    String threadName = Thread.currentThread().getName();
			    System.out.println("Analyzing..."+stock.SYMBOL+" "+ threadName);
			    
			    /*
			    StockGraphics chart = new StockGraphics(stock.SYMBOL+"Price vs. Days", "Prediction", stock.MKT, stock.SYMBOL,60,5,"HIGH");
				chart.pack();
				RefineryUtilities.positionFrameOnScreen(chart, 0, 0);
				chart.setVisible(true);
			    */
			    Predictor predictor = new Predictor();
			    predictor.PredictAndStore(stock.MKT, stock.SYMBOL,60,5);
			};
			Future<Integer> future = (Future<Integer>) executor.submit(task);
		});
		System.out.println("");
	}
}