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
	public RulesValidation checkAllRules(float nifty)
	{
		boolean is_valid = false;
		
		double is_HA_GoAhead = is_HA_GoAhead(lastMinus2, lastMinus1,lastCandle);
		//double is_MA_GoAhead = is_MA_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_MOM_GoAhead = is_MOM_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_MACD_GoAhead = is_MACD_GoAhead(lastMinus2, lastMinus1,lastCandle);
		double is_PVT_GoAhead = is_PVT_GoAhead(lastMinus2, lastMinus1,lastCandle);
		
		if(nifty < 0)
		{
			if(is_MOM_GoAhead <= -15 && is_MACD_GoAhead < -5 && is_PVT_GoAhead <= -2 && is_HA_GoAhead < 0)
			{
				is_valid = true;
			}			
		}
		else
		{
			if(is_MOM_GoAhead <= -25 && is_MACD_GoAhead < -10 && is_PVT_GoAhead <= -5 && is_HA_GoAhead < 0)
			{
				is_valid = true;
			}
		}
		
		RulesValidation rv = new RulesValidation();
		//rv.is_MA_GoAhead=is_MA_GoAhead;
		rv.is_MOM_GoAhead = is_MOM_GoAhead;
		rv.is_MACD_GoAhead = is_MACD_GoAhead;
		rv.is_PVT_GoAhead = is_PVT_GoAhead;
		rv.is_valid = is_valid;
		
		return rv;
	}
	private double is_PVT_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) {
		double is_PVT_GoAhead = 0;
		
		if(lastMinus2.PVT > lastCandle.PVT)
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
		//if(lastMinus1.MACD < lastCandle.MACD) 
		{
			//Is signal rising
			//if(lastMinus2.Signal < lastCandle.Signal && lastMinus1.Signal < lastCandle.Signal)
			{
				//Are they converging
				double diff2 = Math.abs(lastMinus1.MACD-lastMinus1.Signal);

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
		if(lastMinus1.MoM > lastCandle.MoM)
		{
			is_MOM_GoAhead = 100*(lastCandle.MoM - lastMinus1.MoM)/lastCandle.MoM;
		}
		else if(lastMinus2.MoM > lastCandle.MoM)
		{
			is_MOM_GoAhead = 100*(lastCandle.MoM - lastMinus2.MoM)/lastCandle.MoM;
		}
		return is_MOM_GoAhead;
	}
	
	private double is_HA_GoAhead(HistoricalDataEx lastMinus2, HistoricalDataEx lastMinus1, HistoricalDataEx lastCandle) 
	{
		double is_HA_GoAhead = 0;
		
		//Is MA rising
		if(lastMinus1.HA.TradeType == TradeType.BUY && lastCandle.HA.TradeType == TradeType.SELL) 
		{
			double h1 = Math.abs(lastMinus1.HA.Open-lastMinus1.HA.Close);
			double h = Math.abs(lastCandle.HA.Open-lastCandle.HA.Close);
			//if( h < h1 )
			{
				is_HA_GoAhead = -1;
			}
		}
		return is_HA_GoAhead;
	}
}