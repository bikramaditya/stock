package entity;

public class RulesValidation {
	public boolean is_valid = false;	
	public double is_MA_GoAhead = 0;
	public double is_MOM_GoAhead = 0;
	public double is_MACD_GoAhead = 0;
	public double is_PVT_GoAhead = 0;
	@Override
	public String toString() {
		return "RulesValidation [is_buy=" + is_valid + ", is_MA_GoAhead=" + is_MA_GoAhead + ", is_MOM_GoAhead="
				+ is_MOM_GoAhead + ", is_MACD_GoAhead=" + is_MACD_GoAhead + ", is_PVT_GoAhead=" + is_PVT_GoAhead + "]";
	}
}
