package actions;
import java.time.LocalDateTime;
import java.util.ArrayList;

import entities.*;
import utils.*;


public class Validator 
{
	static Util util = null;
	static DAO dao = null;
	public Validator() throws Exception
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

	public void validate(Stock stock) 
	{
		util.Logger.log(0, "Validating..."+stock.SYMBOL+" ");
		System.out.println("Validating..."+stock.SYMBOL+" ");
		Recomendation reco = dao.getTradeRecomendations(stock.MKT, stock.SYMBOL);
		if(reco.MKT==null || reco.MKT.length()==0 || reco.ACTUAL_BUY == 0.0 || reco.ACTUAL_SELL == 0.0)
		{
			return;
		}
		
		ArrayList<IntraDayStock> intraDayStockData = dao.getIntradayData(stock.MKT, stock.SYMBOL);
		
		boolean buy_hit = false;
		LocalDateTime buy_hit_time = null;
		
		boolean sell_hit = false;
		LocalDateTime sell_hit_time = null;
	
		
		String today = util.getTodayYYMMDD();
		
		
		
		for(int i = 0 ; i < intraDayStockData.size(); i++)
		{	
			IntraDayStock intraStock = intraDayStockData.get(i);
			LocalDateTime time = intraStock.TIME;
			
			if(time.isAfter(LocalDateTime.parse(today+"T10:29:30")) && time.isBefore(LocalDateTime.parse(today+"T15:30:00")))
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
			
			if(time.isAfter(LocalDateTime.parse(today+"T10:30:30")) && time.isBefore(LocalDateTime.parse(today+"T15:30:00")))
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
		dao.insertEODValues(stock, buy_hit, buy_hit_time, sell_hit, sell_hit_time);
	}
}
