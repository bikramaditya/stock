import java.time.LocalDateTime;

public class IntraDayStock extends Stock {
	LocalDateTime TIME;
	double QUOTE;
	@Override
	public String toString() {
		return "IntraDayStock [TIME=" + TIME + ", QUOTE=" + QUOTE + ", MKT=" + MKT + ", SYMBOL=" + SYMBOL + ", NAME="
				+ NAME + ", DATE=" + DATE + ", OPEN=" + OPEN + ", CLOSE=" + CLOSE + ", HIGH=" + HIGH + ", LOW=" + LOW
				+ ", MA=" + MA + ", VOLUME=" + VOLUME + "]";
	}
}
