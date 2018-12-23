package action;

import entity.HistoricalDataEx;
import entity.RulesValidation;

public class BuyOpportunityChecker 
{
	HistoricalDataEx lastMinus2;
	HistoricalDataEx lastMinus1; 
	HistoricalDataEx lastCandle;

	public BuyOpportunityChecker(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle)
	{
		this.lastMinus2 = lastMinus2;
		this.lastMinus1 = lastMinus1; 
		this.lastCandle = lastCandle;
	}
	public RulesValidation checkAllRules()
	{
		boolean is_valid = false;
		
		double is_MA_GoAhead = is_MA_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_MOM_GoAhead = is_MOM_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_MACD_GoAhead = is_MACD_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_PVT_GoAhead = is_PVT_GoAhead(lastMinus2, lastMinus1,lastCandle);
		
		if(is_MA_GoAhead > 0 && is_MOM_GoAhead > 0 && is_MACD_GoAhead > 0 && is_PVT_GoAhead > 0)
		{
			is_valid = true;
		}
		
		RulesValidation rv = new RulesValidation();
		rv.is_MA_GoAhead=is_MA_GoAhead;
		rv.is_MOM_GoAhead = is_MOM_GoAhead;
		rv.is_MACD_GoAhead = is_MACD_GoAhead;
		rv.is_PVT_GoAhead = is_PVT_GoAhead;
		rv.is_valid = is_valid;
		
		return rv;
	}
	private double is_PVT_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) {
		double is_PVT_GoAhead = 0;
		
		if(lastMinus2.PVT < lastCandle.PVT)
		{
			double perc = 100*(lastCandle.PVT - lastMinus2.PVT)/Math.abs(lastMinus2.PVT);
			//System.out.println(" Perc PVT="+perc+" at "+lastCandle.timeStamp);
			//if(perc > 0)
			{
				is_PVT_GoAhead = perc;	
			}			
		}
		return is_PVT_GoAhead;
	}

	private double is_MACD_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) {
		double is_MACD_GoAhead = 0;
		//Is MACD line rising
		if((lastMinus2.MACD < lastMinus1.MACD) && (lastMinus1.MACD < lastCandle.MACD)) 
		{
			//Is signal rising
			//if(lastMinus2.Signal < lastCandle.Signal && lastMinus1.Signal < lastCandle.Signal)
			{
				//Are they converging
				double diff2 = Math.abs(lastMinus2.MACD-lastMinus2.Signal);

				double diff0 = Math.abs(lastCandle.MACD-lastCandle.Signal);
				//if(diff2 > diff0)
				{
					//Is signal slope positive
					//double siggSlope = lastCandle.Signal-lastMinus1.Signal;
					//if(siggSlope > 0)
					{
						is_MACD_GoAhead = 100*(diff2 - diff0)/diff2;	
					}
				}
			}
		}
		
		return is_MACD_GoAhead;
	}

	private double is_MOM_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) 
	{
		double is_MOM_GoAhead = 0;
		//Is MOM rising
		if(lastMinus1.MoM < lastCandle.MoM)
		{
			is_MOM_GoAhead = 100*(lastCandle.MoM - lastMinus1.MoM)/lastCandle.MoM;
		}
		else if(lastMinus2.MoM < lastCandle.MoM)
		{
			is_MOM_GoAhead = 100*(lastCandle.MoM - lastMinus2.MoM)/lastCandle.MoM;
		}
		return is_MOM_GoAhead;
	}
	
	private double is_MA_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) 
	{
		double is_MA_GoAhead = 0;
		
		//Is MA rising
		if(lastMinus2.MovingAvg < lastMinus1.MovingAvg && lastMinus1.MovingAvg < lastCandle.MovingAvg) 
		{
			//Is current close below  MA 
			//double maGap = 100*(lastCandle.close - lastCandle.MovingAvg)/lastCandle.MovingAvg;
			//if(lastCandle.close < lastCandle.MovingAvg)
			{
				//Is current value rising
				if(lastMinus1.close < lastCandle.close)
				{
					is_MA_GoAhead = 100*(lastCandle.MovingAvg - lastMinus1.MovingAvg)/lastMinus1.MovingAvg;
				}
				else if(lastMinus2.close < lastCandle.close)
				{
					is_MA_GoAhead = 100*(lastCandle.MovingAvg - lastMinus2.MovingAvg)/lastMinus2.MovingAvg;
				}
			}
		}
		return is_MA_GoAhead;
	}
}
