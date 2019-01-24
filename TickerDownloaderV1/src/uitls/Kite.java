package uitls;
import com.neovisionaries.ws.client.WebSocketException;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.Quote;
import com.zerodhatech.models.Tick;
import com.zerodhatech.models.User;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnDisconnect;
import com.zerodhatech.ticker.OnError;
import com.zerodhatech.ticker.OnOrderUpdate;
import com.zerodhatech.ticker.OnTicks;

import entity.Stock;
import selenium.ZerodhaBrowser;
import uitls.Util;

import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by sujith on 7/10/16.
 * This class has example of how to initialize kiteSdk and make rest api calls to place order, get orders, modify order, cancel order,
 * get positions, get holdings, convert positions, get instruments, logout user, get historical data dump, get trades
 */
public class Kite 
{
	static String apikey = "elwe42nofym6o7s5";
	static String userID = "BF2129";
	static String apiSecret = "j81xke1qq25o986qh60o81bj3y7kywp0";
	KiteConnect kiteConnect = null;
	static Util util = null;
	public Kite()
	{
		if(util==null)
		{
			util = new Util();
		}
		

		kiteConnect = new KiteConnect(apikey);
		kiteConnect.setUserId(userID);
        kiteConnect.setEnableLogging(true);
		
        String tokens[] = util.getAccessToken();
		
        if(tokens!=null && tokens.length==2 && tokens[0]!=null && tokens[0].length() > 0 && tokens[1]!=null && tokens[1].length() > 0 )
        {
        	kiteConnect.setAccessToken(tokens[0]);
	        kiteConnect.setPublicToken(tokens[1]);
        }
        else
        {
        	String url = kiteConnect.getLoginURL();

	        String finalURL = ZerodhaBrowser.getRequestToken(url);//"https://bikramaditya.me/?request_token=CfCephk6UOr1nY4jEWomRO72a40vPuRL&action=login&status=success";
	        
	        int startPos = finalURL.indexOf("?");
	        finalURL=finalURL.substring(startPos+1);
	        
	        String[] splitz = finalURL.split("&");
	        
	        for (String param : splitz) {
				if(param!=null && param.length()>0)
				{
					if(param.startsWith("request_token"))
					{
						try 
						{
							String[] keyVal = param.split("=");
							String requestToken = keyVal[1];
							if(requestToken == null || requestToken.length()<1)
							{
								util.Logger.log(0, "Error in fetching request token");
								System.exit(0);
							}
							
							User user =  kiteConnect.generateSession(requestToken, apiSecret);
							util.storeAccessToken(user.accessToken, user.publicToken);
							kiteConnect.setAccessToken(user.accessToken);
				            kiteConnect.setPublicToken(user.publicToken);
						}catch(Exception | KiteException e)
						{
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
	
	public HistoricalData getHistoricalData(Date from , Date to, Stock stock ) throws KiteException, Exception {
        HistoricalData historicalData = kiteConnect.getHistoricalData(from, to, stock.instrument_token, "minute", false);
        System.out.println(historicalData.dataArrayList.size());
        
        return historicalData;
    }
	
    public Map<String, Quote> getAllQuotes(String[] watchList)
    {
    	Map<String, Quote> map = null;
        try 
        {            
            ZerodhaConnect zConnect = new ZerodhaConnect();
            map = zConnect.getQuote(kiteConnect,watchList);            
        } catch (KiteException e) {
            System.out.println(e.message+" "+e.code+" "+e.getClass().getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (IOException e) {
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
	/** Demonstrates com.zerodhatech.ticker connection, subcribing for instruments, unsubscribing for instruments, set mode of tick data, com.zerodhatech.ticker disconnection
	 * @param dao */
    public void tickerUsage(ArrayList<Long> tokens, DAO dao) throws IOException, WebSocketException, KiteException {
        /** To get live price use websocket connection.
         * It is recommended to use only one websocket connection at any point of time and make sure you stop connection, once user goes out of app.
         * custom url points to new endpoint which can be used till complete Kite Connect 3 migration is done. */
        final KiteTicker tickerProvider = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());
        
        tickerProvider.setOnConnectedListener(new OnConnect() {
            @Override
            public void onConnected() {
                /** Subscribe ticks for token.
                 * By default, all tokens are subscribed for modeQuote.
                 * */
                tickerProvider.subscribe(tokens);
                tickerProvider.setMode(tokens, KiteTicker.modeFull);
                System.out.println("Socket connected");
            }
        });

        tickerProvider.setOnDisconnectedListener(new OnDisconnect() {
            @Override
            public void onDisconnected() {
                System.out.println("Socket Disconnected");
            }
        });

        /** Set listener to get order updates.*/
        tickerProvider.setOnOrderUpdateListener(new OnOrderUpdate() {
            @Override
            public void onOrderUpdate(Order order) {
                System.out.println("order update "+order.orderId);
            }
        });

        /** Set error listener to listen to errors.*/
        tickerProvider.setOnErrorListener(new OnError() {
            @Override
            public void onError(Exception exception) {
                exception.printStackTrace();
            }

            @Override
            public void onError(KiteException exception) {
            	exception.printStackTrace();
            }
        });

        tickerProvider.setOnTickerArrivalListener(new OnTicks() {
            @Override
            public void onTicks(ArrayList<Tick> ticks) {
                if(ticks.size() > 0) 
                {
                	Thread t = new Thread(new TickerWorker(ticks));
				    t.start();
                }
                if(false)
                {
                	ArrayList<Tick> ticks1 = new ArrayList<Tick>();
                	Tick tick = new Tick();
                	tick.setLastTradedPrice(0);
                	tick.setInstrumentToken(1);
                	tick.setLastTradedTime(new Date());
                	tick.setTickTimestamp(new Date());
                	
                	ticks.add(tick);
                	
                	Thread t = new Thread(new TickerWorker(ticks1));
				    t.start();
				    try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				    System.exit(0);
                }
            }
        });
        // Make sure this is called before calling connect.
        tickerProvider.setTryReconnection(true);
        //maximum retries and should be greater than 0
        tickerProvider.setMaximumRetries(10);
        //set maximum retry interval in seconds
        tickerProvider.setMaximumRetryInterval(30);

        /** connects to com.zerodhatech.com.zerodhatech.ticker server for getting live quotes*/
        tickerProvider.connect();

        /** You can check, if websocket connection is open or not using the following method.*/
        boolean isConnected = tickerProvider.isConnectionOpen();
        System.out.println(isConnected);

        /** set mode is used to set mode in which you need tick for list of tokens.
         * Ticker allows three modes, modeFull, modeQuote, modeLTP.
         * For getting only last traded price, use modeLTP
         * For getting last traded price, last traded quantity, average price, volume traded today, total sell quantity and total buy quantity, open, high, low, close, change, use modeQuote
         * For getting all data with depth, use modeFull*/
        //tickerProvider.setMode(tokens, KiteTicker.modeLTP);

        // Unsubscribe for a token.
        //tickerProvider.unsubscribe(tokens);

        // After using com.zerodhatech.com.zerodhatech.ticker, close websocket connection.
        //tickerProvider.disconnect();
    }
}