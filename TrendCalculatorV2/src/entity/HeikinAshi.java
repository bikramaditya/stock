package entity;

public class HeikinAshi {
	public double Open;
	public double Close;
	public double High;
	public double Low;
	public TradeType TradeType;
	@Override
	public String toString() {
		return "HeikinAshi [Open=" + Open + ", Close=" + Close + ", High=" + High + ", Low=" + Low + "]";
	}
}
