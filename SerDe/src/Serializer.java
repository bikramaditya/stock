import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentHashMap;


public class Serializer {
	public static void main(String args[])
	{
		String user_home = System.getProperty("user.home");
		ConcurrentHashMap<String,TradeType> sessionMap = new ConcurrentHashMap<String,TradeType>();
		for(int i  = 0 ; i < 10; i++)
		{
			if(i%2==0)
			{
				sessionMap.put("stock-"+i, TradeType.BUY);	
			}
			else
			{
				sessionMap.put("stock-"+i, TradeType.SELL);
			}
			
		}
		XMLEncoder encoder=null;
		try{
		encoder=new XMLEncoder(new BufferedOutputStream(new FileOutputStream(user_home+"/sessionMap.xml")));
		}catch(FileNotFoundException fileNotFound){
			System.out.println("ERROR: While Creating or Opening the File dvd.xml");
		}
		encoder.writeObject(sessionMap);
		encoder.close();
	}
}
