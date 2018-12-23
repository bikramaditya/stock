package entities;

import java.time.LocalDate;

public class DailyData implements Comparable<DailyData>{
	public LocalDate DATE;
	public float OPEN;
	public float CLOSE;
	public float HIGH;
	public float LOW;
	public float MA;
	public long VOLUME;
	@Override
	public String toString() {
		return "DailyData [DATE=" + DATE + ", OPEN=" + OPEN + ", CLOSE=" + CLOSE + ", HIGH=" + HIGH + ", LOW=" + LOW
				+ ", MA=" + MA + ", VOLUME=" + VOLUME + "]";
	}
	
	@Override
	public int compareTo(DailyData data) {
		return this.DATE.compareTo(data.DATE);
	}

}
