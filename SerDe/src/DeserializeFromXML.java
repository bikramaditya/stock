import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.ConcurrentHashMap;

public class DeserializeFromXML {	

	public static void main(String[] args) {
		String user_home = System.getProperty("user.home");
		
		XMLDecoder decoder=null;
		try {
			decoder=new XMLDecoder(new BufferedInputStream(new FileInputStream(user_home+"/sessionMap.xml")));
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: File dvd.xml not found");
		}
		ConcurrentHashMap<String,TradeType> sessionMap = (ConcurrentHashMap)decoder.readObject();
		
		System.out.println(sessionMap);

	}
}