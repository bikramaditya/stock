package entity;

import com.zerodhatech.models.HistoricalData;

public class HistoricalDataEx extends HistoricalData{
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
		return "HistoricalDataEx [MovingAvg=" + MovingAvg + ", MoM=" + MoM + ", PVT=" + PVT + ", MACD=" + MACD
				+ ", Signal=" + Signal + ", timeStamp=" + timeStamp + ", open=" + open + ", high=" + high + ", low="
				+ low + ", close=" + close + ", volume=" + volume + "]";
	}
}
