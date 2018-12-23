public class Recomendation {
	String MKT;
	String SYMBOL;
	String TRADE_DATE;
	double PREDICTED_HIGH;
	double PREDICTED_LOW;
	double RECO_BUY;
	double RECO_SELL;
	double ACTUAL_BUY;
	double ACTUAL_SELL;
	double LIVE_QUOTE;
	@Override
	public String toString() {
		return "Recomendation [MKT=" + MKT + ", SYMBOL=" + SYMBOL + ", TRADE_DATE=" + TRADE_DATE + ", PREDICTED_HIGH="
				+ PREDICTED_HIGH + ", PREDICTED_LOW=" + PREDICTED_LOW + ", RECO_BUY=" + RECO_BUY + ", RECO_SELL="
				+ RECO_SELL + ", ACTUAL_BUY=" + ACTUAL_BUY + ", ACTUAL_SELL=" + ACTUAL_SELL + ", LIVE_QUOTE="
				+ LIVE_QUOTE + "]";
	}
}
