package entities;

public class Weight {
	public float slope45Days;
	public float slope30Days;
	public float slope15Days;
	public float slope7Days;
	public float slope3Days;
	public float slope2Days;

	public float maDiff;

	@Override
	public String toString() {
		return "Weight [slope45Days=" + slope45Days + ", slope30Days=" + slope30Days + ", slope15Days=" + slope15Days
				+ ", slope7Days=" + slope7Days + ", slope3Days=" + slope3Days + ", slope2Days=" + slope2Days
				+ ", maDiff=" + maDiff + "]";
	}
}
