package entity;

import com.zerodhatech.models.HistoricalData;

public class HistoricalDataEx extends HistoricalData{
	public HeikinAshi HA = new HeikinAshi();
	public double MovingAvg;
	public double MoM;
	public double PVT;
	public double EMA12;
	public double EMA26;
	public double MACD;
	public double Signal;
	public double Dummy;
	@Override
	public String toString() {
		return "HistoricalDataEx [HA=" + HA + ", MovingAvg=" + MovingAvg + ", MoM=" + MoM + ", PVT=" + PVT + ", MACD="
				+ MACD + ", Signal=" + Signal + "]";
	}
}
