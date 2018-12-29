package uitls;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.HistoricalData;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.Margin;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import com.zerodhatech.models.Quote;
import com.zerodhatech.models.User;

import entity.Stock;
import selenium.ZerodhaBrowser;
import uitls.Util;

import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by sujith on 7/10/16. This class has example of how to initialize
 * kiteSdk and make rest api calls to place order, get orders, modify order,
 * cancel order, get positions, get holdings, convert positions, get
 * instruments, logout user, get historical data dump, get trades
 */
public class Kite {
	static String apikey = "elwe42nofym6o7s5";
	static String userID = "BF2129";
	static String apiSecret = "j81xke1qq25o986qh60o81bj3y7kywp0";
	KiteConnect kiteConnect = null;
	static Util util = null;

	public Kite() throws Exception {
		if (util == null) {
			util = new Util();
		}

		kiteConnect = new KiteConnect(apikey);
		kiteConnect.setUserId(userID);
		kiteConnect.setEnableLogging(true);

		String tokens[] = util.getAccessToken();

		if (tokens != null && tokens.length == 2 && tokens[0] != null && tokens[0].length() > 0 && tokens[1] != null
				&& tokens[1].length() > 0) {
			kiteConnect.setAccessToken(tokens[0]);
			kiteConnect.setPublicToken(tokens[1]);
		} else {
			String url = kiteConnect.getLoginURL();

			String finalURL = ZerodhaBrowser.getRequestToken(url);// "https://bikramaditya.me/?request_token=CfCephk6UOr1nY4jEWomRO72a40vPuRL&action=login&status=success";

			int startPos = finalURL.indexOf("?");
			finalURL = finalURL.substring(startPos + 1);

			String[] splitz = finalURL.split("&");

			for (String param : splitz) {
				if (param != null && param.length() > 0) {
					if (param.startsWith("request_token")) {
						try {
							String[] keyVal = param.split("=");
							String requestToken = keyVal[1];
							if (requestToken == null || requestToken.length() < 1) {
								util.Logger.log(0, "Error in fetching request token");
								System.exit(0);
							}

							User user = kiteConnect.generateSession(requestToken, apiSecret);
							util.storeAccessToken(user.accessToken, user.publicToken);
							kiteConnect.setAccessToken(user.accessToken);
							kiteConnect.setPublicToken(user.publicToken);
						} catch (Exception | KiteException e) {
							util.Logger.log(0, "Error in fetching request token");
							System.exit(0);
						}
						break;
					}
				}
			}
		}

		// Set session expiry callback.
		kiteConnect.setSessionExpiryHook(new SessionExpiryHook() {
			@Override
			public void sessionExpired() {
				System.out.println("session expired");
			}
		});
	}
	
	public double getMargins() throws KiteException, IOException {
        // Get margins returns margin model, you can pass equity or commodity as arguments to get margins of respective segments.
        //Margins margins = kiteConnect.getMargins("equity");
        Margin margins = kiteConnect.getMargins("equity");
        System.out.println(margins.net);
        //System.out.println(margins.utilised.debits);
        //System.out.println(margins.utilised.m2mUnrealised);
        double cash = Double.parseDouble(margins.net);
        return cash;
    }
	
	public Order placeOrder(OrderParams orderParams, String VARIETY) throws KiteException, IOException {
		/**
		 * Place order method requires a orderParams argument which contains,
		 * tradingsymbol, exchange, transaction_type, order_type, quantity, product,
		 * price, trigger_price, disclosed_quantity, validity squareoff_value,
		 * stoploss_value, trailing_stoploss and variety (value can be regular, bo, co,
		 * amo) place order will return order model which will have only orderId in the
		 * order model
		 *
		 * Following is an example param for LIMIT order, if a call fails then
		 * KiteException will have error message in it Success of this call implies only
		 * order has been placed successfully, not order execution.
		 */

		Order order = kiteConnect.placeOrder(orderParams, VARIETY);
		System.out.println(order);
		return order;
	}

	public HistoricalData getHistoricalData(Date from, Date to, Stock stock) throws KiteException, Exception {
		HistoricalData historicalData = kiteConnect.getHistoricalData(from, to, stock.instrument_token, "minute",
				false);
		System.out.println(historicalData.dataArrayList.size());

		return historicalData;
	}

	public Map<String, Quote> getAllQuotes(String[] watchList) {
		Map<String, Quote> map = null;
		try {
			ZerodhaConnect zConnect = new ZerodhaConnect();
			map = zConnect.getQuote(kiteConnect, watchList);
		} catch (KiteException e) {
			System.out.println(e.message + " " + e.code + " " + e.getClass().getName());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public List<Instrument> getAllInstruments() {
		ZerodhaConnect zConnect = new ZerodhaConnect();
		List<Instrument> instruments = null;
		try {
			instruments = zConnect.getAllInstruments(kiteConnect);
			System.out.println(instruments.size());
		} catch (IOException | KiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return instruments;
	}
}