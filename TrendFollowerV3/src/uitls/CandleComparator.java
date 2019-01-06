package uitls;

import java.util.Comparator;
import entity.HistoricalDataEx;

public class CandleComparator implements Comparator<HistoricalDataEx>{

	@Override
	public int compare(HistoricalDataEx o1, HistoricalDataEx o2) {
		return o1.timeStamp.compareTo(o2.timeStamp);
	}

}
