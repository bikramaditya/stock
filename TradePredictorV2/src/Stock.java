import java.time.LocalDate;

public class Stock {
	public String MKT;
	public String SYMBOL;
	public String NAME;
	public LocalDate DATE;
	public double OPEN;
	public double CLOSE;
	public double HIGH;
	public double LOW;
	public double MA;
	public long VOLUME;
	public float SectorIndex;
	public String SECTOR;
	@Override
	public String toString() {
		return "Stock [MKT=" + MKT + ", SYMBOL=" + SYMBOL + ", NAME=" + NAME + ", DATE=" + DATE + ", OPEN=" + OPEN
				+ ", CLOSE=" + CLOSE + ", HIGH=" + HIGH + ", LOW=" + LOW + ", MA=" + MA + ", VOLUME=" + VOLUME + "]";
	}
}
