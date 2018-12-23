
public class Slope {
	public double slope100Days;
	public double slope30Days;
	public double slope7Days;
	public double slope3Days;
	public double slope1Days;
	public double maDiff;
	public double value;
	
	public void clearNAN() 
	{
		if(Double.isNaN(slope100Days))
		{
			slope100Days = 0.0;
		}
		if(Double.isNaN(slope30Days))
		{
			slope30Days = 0.0;
		}
		if(Double.isNaN(slope7Days))
		{
			slope7Days = 0.0;
		}
		if(Double.isNaN(slope3Days))
		{
			slope3Days = 0.0;
		}
		if(Double.isNaN(maDiff))
		{
			maDiff = 0.0;
		}
		if(Double.isNaN(value))
		{
			value = 0.0;
		}
	}
}
