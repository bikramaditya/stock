package entities;

import java.util.ArrayList;

public class Stock 
{
	public int ID;
	public String MKT;
	public String SYMBOL;
	public String NAME;
	public String INDUSTRY;
	public String SECTOR;
	public boolean IS_ACTIVE;
	public float SectorIndex;
	
	public Slope slope = new Slope();
	public ArrayList<DailyData> dailyData = new ArrayList<DailyData>(); 
	public ArrayList<Weight> optimumWeghts = new ArrayList<Weight>();
	@Override
	public String toString() {
		return "Stock [ID=" + ID + ", MKT=" + MKT + ", SYMBOL=" + SYMBOL + ", NAME=" + NAME + ", INDUSTRY=" + INDUSTRY
				+ ", SECTOR=" + SECTOR + ", IS_ACTIVE=" + IS_ACTIVE + ", SectorIndex=" + SectorIndex + ", slope="
				+ slope + ", dailyData=" + dailyData + ", optimumWeghts=" + optimumWeghts + "]";
	}
	
}
