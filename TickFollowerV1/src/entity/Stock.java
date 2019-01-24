package entity;
public class Stock {
	public String MKT;
	public String SYMBOL;
	public String NAME;
	public String instrument_token;
	@Override
	public String toString() {
		return "Stock [MKT=" + MKT + ", SYMBOL=" + SYMBOL + ", NAME=" + NAME + ", instrument_token=" + instrument_token
				+ "]";
	}
}
