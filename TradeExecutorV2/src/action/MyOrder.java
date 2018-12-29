package action;

import java.io.IOException;
import java.util.Map;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import com.zerodhatech.models.Quote;

import entity.Opportunity;
import entity.TradeType;
import uitls.Kite;
import uitls.Util;

public class MyOrder {
	Kite kite = null; 
	Opportunity opty = null;
	double sliceCashToday = 0.0;
	public MyOrder(Kite kite, Opportunity opty, double sliceCashToday) {
		this.kite = kite;
		this.opty = opty;
		this.sliceCashToday = sliceCashToday;
	}

	public Order execute() 
	{
		Order order = null;	
		try {
			double avlCash = kite.getMargins();
			
			avlCash = 12000; //Debug
			
			if(avlCash < sliceCashToday)
			{
				System.out.println("Cant execute, insufficient balance "+avlCash +" < "+ sliceCashToday);
				Util.Logger.log(0, "Cant execute, insufficient balance "+avlCash +" < "+ sliceCashToday);
				
				return null;
			}
			
			opty = updateOptyQuote(opty);
			
			OrderParams orderParams = new OrderParams();
	        orderParams.quantity = (int) (sliceCashToday/opty.EntryPrice);
	        orderParams.orderType = Constants.ORDER_TYPE_LIMIT;
	        orderParams.price = opty.EntryPrice;
	        if(("BUY").equals(opty.TradeType))
	        {
	        	orderParams.transactionType = Constants.TRANSACTION_TYPE_BUY;	
	        }
	        else if(("SELL").equals(opty.TradeType))
	        {
	        	orderParams.transactionType = Constants.TRANSACTION_TYPE_SELL;
	        }
	        orderParams.tradingsymbol = opty.Symbol;
	        orderParams.trailingStoploss = 0.0;
	        orderParams.stoploss = 0.012 * opty.EntryPrice;
	        orderParams.product = Constants.PRODUCT_MIS;
	        orderParams.exchange = Constants.EXCHANGE_NSE;
	        orderParams.validity = Constants.VALIDITY_DAY;
	        
	        orderParams.squareoff = 0.001 * opty.EntryPrice;
	        
	        order = kite.placeOrder(orderParams, Constants.VARIETY_BO);
	        System.out.println(order.orderId);
	        Util.Logger.log(0, order.orderId);
		} catch (Exception | KiteException e) {
			e.printStackTrace();
			Util.Logger.log(0, e.getMessage());
		}
		
		return order;
	}
	
	private Opportunity updateOptyQuote(Opportunity opty) 
	{
		String instrument = opty.MKT+":"+opty.Symbol;
		String[] arr = new String[1];
		arr[0] = instrument;
		Map<String, Quote> quotes = kite.getAllQuotes(arr);
		double lastPrice = quotes.get(instrument).lastPrice;
		
		opty.EntryPrice = lastPrice;
		
		return opty;
	}
}
