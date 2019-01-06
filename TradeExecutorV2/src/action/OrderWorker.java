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
import uitls.DAO;
import uitls.Kite;
import uitls.Util;

public class OrderWorker implements Runnable {
	Kite kite = null;
	Opportunity opty = null;
	double sliceCashToday = 0.0;

	public OrderWorker(Kite kite, Opportunity opty, double sliceCashToday) {
		this.kite = kite;
		this.opty = opty;
		this.sliceCashToday = sliceCashToday;
	}

	@Override
	public void run() 
	{
		Order order = null;
		DAO dao = new DAO();
		try {
			double avlCash = kite.getMargins();

			if (avlCash < sliceCashToday) {
				System.out.println("Cant execute, insufficient balance " + avlCash + " < " + sliceCashToday);
				Util.Logger.log(0, "Cant execute, insufficient balance " + avlCash + " < " + sliceCashToday);
				return;
			}

			opty = updateOptyQuote(opty);

			OrderParams orderParams = new OrderParams();
			orderParams.quantity = 1+(int) (sliceCashToday / opty.EntryPrice);
			orderParams.orderType = Constants.ORDER_TYPE_LIMIT;
			double price = 0.0;
			if (("BUY").equals(opty.TradeType)) 
			{				
				price = opty.EntryPrice * (1-0.0001);
				price = round(price, 2);
				price = Math.round(price * 20) / 20.0;
				orderParams.price = price;

				orderParams.transactionType = Constants.TRANSACTION_TYPE_BUY;
			} else if (("SELL").equals(opty.TradeType)) 
			{				
				price = opty.EntryPrice * (1 + 0.0001);

				price = round(price, 2);
				price = Math.round(price * 20) / 20.0;
				orderParams.price = price;

				orderParams.transactionType = Constants.TRANSACTION_TYPE_SELL;
			}
			orderParams.tradingsymbol = opty.Symbol;
			orderParams.trailingStoploss = 0.0;
			orderParams.stoploss = 0.002 * opty.EntryPrice;
			orderParams.product = Constants.PRODUCT_MIS;
			orderParams.exchange = Constants.EXCHANGE_NSE;
			orderParams.validity = Constants.VALIDITY_DAY;

			orderParams.squareoff = 0.0015 * opty.EntryPrice;

			order = kite.placeOrder(orderParams, Constants.VARIETY_BO);
			if(order.averagePrice==null || order.averagePrice.length()==0)
			{
				order.averagePrice = ""+price;
			}
			System.out.println(opty+"->"+order.orderId);
			Util.Logger.log(0, opty+"->"+order.orderId);
			
			dao.updateOptyPicked(opty,order);
			
		} catch (Exception | KiteException e) {
			e.printStackTrace();
			Util.Logger.log(0, e.getMessage());
		}

		//return order;
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	private Opportunity updateOptyQuote(Opportunity opty) {
		String instrument = opty.MKT + ":" + opty.Symbol;
		String[] arr = new String[1];
		arr[0] = instrument;
		Map<String, Quote> quotes = kite.getAllQuotes(arr);
		double lastPrice = quotes.get(instrument).lastPrice;

		opty.EntryPrice = lastPrice;

		return opty;
	}
}
