package action;

import entity.HistoricalDataEx;
import entity.RulesValidation;
import entity.TradeType;

public class SellOpportunityChecker {
	HistoricalDataEx lastMinus2;
	HistoricalDataEx lastMinus1; 
	HistoricalDataEx lastCandle;

	public SellOpportunityChecker(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle)
	{
		this.lastMinus2 = lastMinus2;
		this.lastMinus1 = lastMinus1; 
		this.lastCandle = lastCandle;
	}
	public RulesValidation checkAllRules(float upDown)
	{
		boolean is_valid = false;
		
		double is_HA_GoAhead = is_HA_GoAhead(lastMinus2, lastMinus1,lastCandle);
		//double is_MA_GoAhead = is_MA_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_MOM_GoAhead = is_MOM_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_MACD_GoAhead = is_MACD_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_PVT_GoAhead = is_PVT_GoAhead(lastMinus2, lastMinus1,lastCandle);
		
		if(upDown > 0)
		{
			if(is_MOM_GoAhead >= 15 && is_MACD_GoAhead > 0.1 && is_PVT_GoAhead > 1 && is_HA_GoAhead > 0)
			{
				is_valid = true;
			}	
		}
		else
		{
			if(is_MOM_GoAhead >= 10 && is_MACD_GoAhead > 0.01 && is_PVT_GoAhead > 1 && is_HA_GoAhead > 0)
			{
				is_valid = true;
			}
		}
		
		RulesValidation rv = new RulesValidation();

		rv.is_MOM_GoAhead = is_MOM_GoAhead;
		rv.is_MACD_GoAhead = is_MACD_GoAhead;
		rv.is_PVT_GoAhead = is_PVT_GoAhead;
		rv.is_valid = is_valid;
		
		rv.Score = Math.abs(is_MOM_GoAhead + is_MACD_GoAhead + is_PVT_GoAhead  + is_HA_GoAhead);
		
		if(rv.Score < 40)
		{
			rv.is_valid = false;
		}
		
		return rv;
	}
	private double is_PVT_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) {
		double is_PVT_GoAhead = 0;
		
		if(lastMinus1.PVT > lastCandle.PVT)
		{
			double perc = 100*(lastCandle.PVT - lastMinus1.PVT)/Math.abs(lastMinus1.PVT);
			//System.out.println(" Perc PVT="+perc+" at "+lastCandle.timeStamp);
			//if(perc > 0)
			{
				is_PVT_GoAhead = Math.abs(perc);	
			}			
		}
		return is_PVT_GoAhead;
	}

	private double is_MACD_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) {
		double is_MACD_GoAhead = 0;
		double diff0 = lastCandle.MACD - lastCandle.Signal;
		double diff1 = lastMinus1.MACD - lastMinus1.Signal;
		double diff2 = lastMinus2.MACD - lastMinus2.Signal;
		
		if(lastCandle.MACD < lastMinus1.MACD)
		{
			if( diff0 < 0.1)
			{
				if((Math.abs(diff0)+Math.abs(diff1)+Math.abs(diff2)) > 0.5)
				{
					is_MACD_GoAhead = diff0;	
				}
			}
		}		
		return Math.abs(is_MACD_GoAhead);
	}

	private double is_MOM_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) 
	{
		double is_MOM_GoAhead = 0;
		//Is MOM falling
		if(lastMinus1.MoM > lastCandle.MoM)
		{
			is_MOM_GoAhead = 100*(lastCandle.MoM - lastMinus1.MoM)/((lastCandle.MoM+lastMinus1.MoM)/2);
		}
		return Math.abs(is_MOM_GoAhead);
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
	private double is_HA_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) 
	{
		double is_HA_GoAhead = 0;
		
		if(lastCandle.HA.TradeType == TradeType.SELL) 
		{
			double top = lastCandle.HA.High - lastCandle.HA.Open;
			double bottom = lastCandle.HA.Close - lastCandle.HA.Low;
			double height = Math.abs(100*(lastCandle.HA.Close - lastCandle.HA.Open)/
					((lastCandle.HA.Close + lastCandle.HA.Open)/2));
			double ratio = Math.abs(bottom/top);
			
			if(("2018-12-28 13:23:00".equals(lastCandle.timeStamp)) || ("2018-12-28 12:12:00".equals(lastCandle.timeStamp)))
			{
				System.out.println();
			}
			if(top==0 
					|| ratio==Double.POSITIVE_INFINITY 
					|| ratio==Double.NEGATIVE_INFINITY || ratio > 1 )
			{
				if(height > 0.04)
				{
					is_HA_GoAhead = 1;	
				}
			}
			System.out.print("");
		}
		return is_HA_GoAhead;
	}
}