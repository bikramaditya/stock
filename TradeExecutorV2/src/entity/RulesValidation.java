package entity;

public class RulesValidation {
	public boolean is_valid = false;	
	public boolean is_MA_GoAhead = false;
	public boolean is_MOM_GoAhead = false;
	public boolean is_MACD_GoAhead = false;
	public boolean is_PVT_GoAhead = false;
	@Override
	public String toString() {
		return "RulesValidation [is_buy=" + is_valid + ", is_MA_GoAhead=" + is_MA_GoAhead + ", is_MOM_GoAhead="
				+ is_MOM_GoAhead + ", is_MACD_GoAhead=" + is_MACD_GoAhead + ", is_PVT_GoAhead=" + is_PVT_GoAhead + "]";
	}
}
