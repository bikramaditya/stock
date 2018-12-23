package entities;
public class Recomendation {
	public String MKT;
	public String SYMBOL;
	public String TRADE_DATE;
	public double PREDICTED_HIGH;
	public double PREDICTED_LOW;
	public double RECO_BUY;
	public double RECO_SELL;
	public double ACTUAL_BUY;
	public double ACTUAL_SELL;
	public double LIVE_QUOTE;
	public double SECTOR_INDEX;
	public String ORDER_TYPE;
	@Override
	public String toString() {
		return "Recomendation [MKT=" + MKT + ", SYMBOL=" + SYMBOL + ", TRADE_DATE=" + TRADE_DATE + ", PREDICTED_HIGH="
				+ PREDICTED_HIGH + ", PREDICTED_LOW=" + PREDICTED_LOW + ", RECO_BUY=" + RECO_BUY + ", RECO_SELL="
				+ RECO_SELL + ", ACTUAL_BUY=" + ACTUAL_BUY + ", ACTUAL_SELL=" + ACTUAL_SELL + ", LIVE_QUOTE="
				+ LIVE_QUOTE + "]";
	}
}
