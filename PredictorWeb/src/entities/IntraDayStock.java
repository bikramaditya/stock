package entities;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class IntraDayStock extends Stock {
	public LocalDateTime TIME;
	public double QUOTE;
	
	public LocalDate DATE;
	public float OPEN;
	public float CLOSE;
	public float HIGH;
	public float LOW;
	public float MA;
	public long VOLUME;
	
	@Override
	public String toString() {
		return "IntraDayStock [TIME=" + TIME + ", QUOTE=" + QUOTE + ", MKT=" + MKT + ", SYMBOL=" + SYMBOL + ", NAME="
				+ NAME + ", DATE=" + DATE + ", OPEN=" + OPEN + ", CLOSE=" + CLOSE + ", HIGH=" + HIGH + ", LOW=" + LOW
				+ ", MA=" + MA + ", VOLUME=" + VOLUME + "]";
	}
}
