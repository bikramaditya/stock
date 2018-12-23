import java.time.LocalDateTime;
import java.util.ArrayList;

public class Validator 
{
	static Util util = null;
	static DAO dao = null;
	public Validator()
	{
		if(util==null)
		{
			util = new Util();	
		}
		if(dao==null)
		{
			dao = new DAO();	
		}
	}

	@SuppressWarnings({ "static-access", "unused" })
	public void validate(Stock stock) 
	{
		util.Logger.log(0, "Validating..."+stock.SYMBOL+" ");
		ArrayList<IntraDayStock> intraDayStockData = util.getIntraDayData(stock.MKT, stock.SYMBOL);
		
		boolean buy_hit = false;
		LocalDateTime buy_hit_time = null;
		
		boolean sell_hit = false;
		LocalDateTime sell_hit_time = null;
	
		Recomendation reco = dao.getTradeRecomendations(stock.MKT, stock.SYMBOL);
		String today = util.getTodayYYMMDD();
		
		if(reco.MKT==null || reco.MKT.length()==0)
		{
			return;
		}
		
		for(int i = 0 ; i < intraDayStockData.size(); i++)
		{	
			IntraDayStock intraStock = intraDayStockData.get(i);
			LocalDateTime time = intraStock.TIME;
			
			if(time.isAfter(LocalDateTime.parse(today+"T09:30:00")) && time.isBefore(LocalDateTime.parse(today+"T15:30:00")))
			{
				double currPrice = intraStock.QUOTE;
				if(currPrice <= reco.ACTUAL_BUY) 
				{
					buy_hit = true;
					buy_hit_time = time;
					break;
				}
			}		
		}
		for(int i = 0 ; i < intraDayStockData.size(); i++)
		{	
			IntraDayStock intraStock = intraDayStockData.get(i);
			LocalDateTime time = intraStock.TIME;
			
			if(time.isAfter(LocalDateTime.parse(today+"T09:15:00")) && time.isBefore(LocalDateTime.parse(today+"T15:30:00")))
			{
				double currPrice = intraStock.QUOTE;
				if(currPrice >= reco.ACTUAL_SELL)
				{
					sell_hit=true;
					sell_hit_time = time;
					break;
				}				
			}		
		}
		util.storeEODResults(stock, buy_hit, buy_hit_time, sell_hit, sell_hit_time);
	}
}
