package cpsr.stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes statistics relevant to planning.
 * 
 * @author William Hamilton
 */
public class PlanningStats
{

	public static double getAverageDiscountedReward(List<List<Double>> runRewards, double pDiscount)
	{
		double lAvReward = 0.0;
		double lRunReward;

		for(List<Double> lRewardVec : runRewards)
		{
			lRunReward = 0;
			for(int i = 0; i < lRewardVec.size(); i++)
			{
				if(i == 0)
				{
					lRunReward+=lRewardVec.get(i);
				}
				else
				{
					lRunReward+=Math.pow(pDiscount, (double)i)*lRewardVec.get(i);
				}	
			}
			lAvReward+=lRunReward;
		}

		return lAvReward/((double)runRewards.size());
	}

	public static double getVarOfDiscountedReward(List<List<Double>> runRewards, double pDiscount, double pAv)
	{		
		double lVar = 0.0;
		double lRunReward;

		for(List<Double> lRewardVec : runRewards)
		{
			lRunReward = 0;
			for(int i = 0; i < lRewardVec.size(); i++)
			{
				if(i == 0)
				{
					lRunReward+=lRewardVec.get(i);
				}
				else
				{
					lRunReward+=Math.pow(pDiscount, (double)i)*lRewardVec.get(i);
				}	
			}
			lVar += Math.pow((lRunReward-pAv), 2);
		}

		return lVar/((double)runRewards.size());
		
	}

	public static double getAverageLengthOfRun(List<List<Double>> runRewards)
	{
		ArrayList<Double> lLengthVec = new ArrayList<Double>();

		for(List<Double> pSingleRun : runRewards)
		{
			lLengthVec.add((double)pSingleRun.size());
		}

		return Basic.mean(lLengthVec);
	}
}
