package entity;

public class Opportunity {
	public String MKT;
	public String Symbol;
	public TradeType TradeType;
	public double EntryPrice;
	public double ExitPrice;
	public double StopLoss;
	public String TimeStamp;
	public double Slope;
	public double MA;
	public double MOM;
	public double MACD;
	public double PVT;
	public boolean is_valid;
	@Override
	public String toString() {
		return "Opportunity [MKT=" + MKT + ", Symbol=" + Symbol + ", TradeType=" + TradeType + ", EntryPrice="
				+ EntryPrice + ", ExitPrice=" + ExitPrice + ", StopLoss=" + StopLoss + ", TimeStamp=" + TimeStamp
				+ ", Slope=" + Slope + ", MA=" + MA + ", MOM=" + MOM + ", MACD=" + MACD + ", PVT=" + PVT + ", is_valid="
				+ is_valid + "]";
	}
		
}
