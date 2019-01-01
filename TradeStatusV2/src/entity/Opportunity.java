package entity;

public class Opportunity {
	public int ID;
	public String MKT;
	public String Symbol;
	public String TradeType;
	public double EntryPrice;
	public double ExitPrice;
	public double StopLoss;
	public String TimeStamp;
	public double Slope;
	public String OrderId;
	
	@Override
	public String toString() {
		return "Opportunity [MKT=" + MKT + ", Symbol=" + Symbol + ", TradeType=" + TradeType + ", EntryPrice="
				+ EntryPrice + ", ExitPrice=" + ExitPrice + ", StopLoss=" + StopLoss + ", TimeStamp=" + TimeStamp
				+ ", Slope=" + Slope + "]";
	}
	
}
