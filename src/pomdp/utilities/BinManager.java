package pomdp.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pomdp.algorithms.beliefcollection.BeliefCollection;
import pomdp.valuefunction.UpperBoundValueFunctionApproximation;

/***
 * Inflenced from BinManager.cc from the SARSOP project
 * @author rkaplo
 *
 */
public class BinManager {

	Map<Integer,Map<String,Double>> binLevels_intervals = new HashMap<Integer,Map<String,Double>>();
	Map<Integer,Map<BeliefState,Map<String,Double>>> binLevels_nodes = 
		new HashMap<Integer,Map<BeliefState,Map<String,Double>>>();
	
	Map <Integer,Map<String,Map<String,Double>>> binLevels = 
		new HashMap<Integer,Map<String,Map<String,Double>>>();
	
	Map<BeliefState,Double> previousLowerBound = new HashMap<BeliefState,Double>();
	
	BeliefCollection collection;
	UpperBoundValueFunctionApproximation upper;
	UpperBoundValueFunctionApproximation initialUpper;
	int binLevelCount = 2;
	int binGrowthFactor = 2;
	int initialBinSize = 5;
	double lowest;
	double highest;
	
	public BinManager(BeliefCollection _collection, UpperBoundValueFunctionApproximation _upper)
	{
		collection = _collection;
		upper = _upper;
		initializeDynamicBins(); 
	}
	
	
	public void updateNode(BeliefState bs)
	{
	
		if (!bs.isBinned())
		{
			bs.setBinned();
			initializeNode(bs);
			
		}
		else
			updateBin(bs);
	}
	
	
	public void initializeNode(BeliefState bs)
	{
		
		
		double lbValue = collection.valueIteration.valueAt(bs);
		
		//does this need to be a static (initial) upper bound??
		double ubValue = upper.valueAt(bs);
		
		double entropy = bs.getEntropy();
		
		for (int i = 1; i <= binLevelCount; i++)
		{
			/* doesn't have a table for the belief bs yet */
			if (!binLevels_nodes.get(i).containsKey(bs))
				binLevels_nodes.get(i).put(bs, new HashMap<String,Double>()); 
			
			//upper
			binLevels_nodes.get(i).get(bs).put("ub_index", ((ubValue - lowest) / binLevels_intervals.get(i).get("ub_interval"))); 
			
			if (binLevels_nodes.get(i).get(bs).get("ub_index") > initialBinSize * Math.pow((double)binGrowthFactor, (i-1)) - 1)
				binLevels_nodes.get(i).get(bs).put("ub_index", initialBinSize * Math.pow((double)binGrowthFactor, (i-1)) - 1);
			else if (binLevels_nodes.get(i).get(bs).get("ub_index") > 0)
				binLevels_nodes.get(i).get(bs).put("ub_index", 0.0);
			
			//entropy
			binLevels_nodes.get(i).get(bs).put("entropy_index", (int) entropy / binLevels_intervals.get(i).get("entropy_interval")); 
			
			if (binLevels_nodes.get(i).get(bs).get("entropy_index") > initialBinSize * Math.pow((double)binGrowthFactor, (i-1)) - 1)
				binLevels_nodes.get(i).get(bs).put("entropy_index", initialBinSize * Math.pow((double)binGrowthFactor, (i-1)) - 1);
			else if (binLevels_nodes.get(i).get(bs).get("entropy_index") > 0)
				binLevels_nodes.get(i).get(bs).put("entropy_index", 0.0);
			
			
			String access = binLevels_nodes.get(i).get(bs).get("ub_index").intValue() + "-" + binLevels_nodes.get(i).get(bs).get("entropy_index").intValue();
			
			/* prediction error */
			
			/* not existing */
			if (!binLevels.get(i).get("binCount").containsKey(access))
			{
				binLevels.get(i).get("binCount").put(access,0.0);
				binLevels.get(i).get("bins").put(access,0.0);
				double error = ubValue - lbValue;
				binLevels_nodes.get(i).get(bs).put("prev_error",error*error);
				binLevels.get(i).get("binError").put(access, binLevels_nodes.get(i).get(bs).get("prev_error"));	
			}
			else
			{
				double error = binLevels.get(i).get("bins").get(access) - lbValue;
				binLevels_nodes.get(i).get(bs).put("prev_error",error*error);
				double oldValue = binLevels.get(i).get("binError").get(access);
				binLevels.get(i).get("binError").put(access, oldValue + binLevels_nodes.get(i).get(bs).get("prev_error"));	
				
			}
						
			//new average
			double total = ((double)binLevels.get(i).get("bins").get(access) * (double)binLevels.get(i).get("binCount").get(access) + lbValue);
			double average = total / ( binLevels.get(i).get("binCount").get(access) + 1);
				
			
			binLevels.get(i).get("bins").put(access, average);
			binLevels.get(i).get("binCount").put(access, binLevels.get(i).get("binCount").get(access) + 1);

			
		}
	
		previousLowerBound.put(bs, lbValue);
		
		
	}
	
	public void updateBin(BeliefState bs)
	{
		double lbValue = collection.valueIteration.valueAt(bs);
		double ubValue = upper.valueAt(bs);

		for (int i = 1; i <= binLevelCount; i++)
		{
		
			String access = binLevels_nodes.get(i).get(bs).get("ub_index").intValue() + "-" + binLevels_nodes.get(i).get(bs).get("entropy_index").intValue();
			
			/* prediction error */
			if (binLevels.get(i).get("binCount").get(access) == 1)
			{
				double error = ubValue - lbValue;
				binLevels_nodes.get(i).get(bs).put("prev_error",error*error);
				binLevels.get(i).get("binError").put(access, binLevels_nodes.get(i).get(bs).get("prev_error"));	
			}
			else
			{
				double error = binLevels.get(i).get("bins").get(access) - lbValue;
				
				binLevels.get(i).get("binError").put(access, binLevels.get(i).get("binError").get(access) - binLevels_nodes.get(i).get(bs).get("prev_error"));	
				
				binLevels_nodes.get(i).get(bs).put("prev_error",error*error);
				binLevels.get(i).get("binError").put(access, binLevels.get(i).get("binError").get(access) + binLevels_nodes.get(i).get(bs).get("prev_error"));	
			}
			
			/* update average value */
			double total = (double)binLevels.get(i).get("bins").get(access) * binLevels.get(i).get("binCount").get(access) + lbValue - previousLowerBound.get(bs);
			double average = total / (double) binLevels.get(i).get("binCount").get(access);
			binLevels.get(i).get("bins").put(access, average);
			
		}
		previousLowerBound.put(bs, lbValue);
		
		
	}
	
	public double getBinValue(BeliefState bs)
	{

		double lbValue = collection.valueIteration.valueAt(bs);
		double ubValue = upper.valueAt(bs);
		
		if (!binLevels_nodes.get(1).containsKey(bs))
			return ubValue;
		
		String access = binLevels_nodes.get(1).get(bs).get("ub_index").intValue() + "-" + binLevels_nodes.get(1).get(bs).get("entropy_index").intValue();
		
		/* new node */
		if (binLevels.get(1).get("binCount").get(access) == 1)
		{
			//return upper bound, as described in SARSOP paper
			return ubValue;
			
		}
		
		else
		{
			int bestLevel = 0;
			double smallestError = 0;
			for (int i = 1; i <= binLevelCount; i++)
			{
			
				access = binLevels_nodes.get(i).get(bs).get("ub_index").intValue() + "-" + binLevels_nodes.get(i).get(bs).get("entropy_index").intValue();
				
				/* first bins */
				if (i == 1)
				{
					bestLevel = i;
					smallestError = binLevels.get(i).get("binError").get(access);
				}
				else if (binLevels.get(i).get("binError").get(access) < smallestError)
				{
					bestLevel = i;
					smallestError = binLevels.get(i).get("binError").get(access);
				}
				
			}
			if (!binLevels_intervals.get(bestLevel).containsKey("counter"))
				binLevels_intervals.get(bestLevel).put("counter", 0.0);
			
			binLevels_intervals.get(bestLevel).put("counter", binLevels_intervals.get(bestLevel).get("counter") + 1);
			
			access = binLevels_nodes.get(bestLevel).get(bs).get("ub_index").intValue() + "-" + binLevels_nodes.get(bestLevel).get(bs).get("entropy_index").intValue();
//			
//			
//			Logger.getInstance().logln("bl = " + bestLevel);
//			
//			access = binLevels_nodes.get(1).get(bs).get("ub_index").intValue() + "-" + binLevels_nodes.get(1).get(bs).get("entropy_index").intValue();
//			
//			Logger.getInstance().logln("access1 = " + access);
//			access = binLevels_nodes.get(2).get(bs).get("ub_index").intValue() + "-" + binLevels_nodes.get(2).get(bs).get("entropy_index").intValue();
//			
//			Logger.getInstance().logln("access2 = " + access);
//			Map<String,Double> x = binLevels.get(bestLevel).get("bins");
//			for (String s : x.keySet())
//			{
//				Logger.getInstance().logln(s);
//			}
			
			
			double estimate = binLevels.get(bestLevel).get("bins").get(access);
			
			if (estimate > ubValue)
				return ubValue;
			if (estimate < lbValue)
				return lbValue;
			return estimate;		
		}
		
	}
	
	private void initializeDynamicBins()
	{
		
		//get bounds of upper bound
		highest = upper.getMaxValueUpperBoundPoints();
		lowest = upper.getMinValueUpperBoundPoints();

		int numUBPoints = upper.getUpperBoundPointCount();
		//compute entropy
		 double maxEntropy = (1.0/numUBPoints) * (Math.log(1.0/numUBPoints) / Math.log(2.0) ) * (-1) * numUBPoints;                                                                 
	        
		 for (int i = 1; i <= binLevelCount; i++)                                                                                                                                             
	     {
			 
			 //intervals
			 Map <String,//ub_interval or entropy_interval                                                                                                                            
             	Double//value of interval                                                                                                                                            
             	> Intervals = new HashMap<String,Double>();
			 Intervals.put("ub_interval", ((highest - lowest) / (initialBinSize * Math.pow((double)binGrowthFactor,i-1))));                                                         
			 Intervals.put("entropy_interval", maxEntropy / (initialBinSize * Math.pow((double)binGrowthFactor,i-1)));                                                            
			 binLevels_intervals.put(i, Intervals);
			 
			 //	binLevels_nodes;                                                                                                                                                       
			 Map <BeliefState, //beleif
	                Map <String, //ub_index, entropy_index, prev_error
	                Double//value                                                                  
	                >                                                                                                                                                             
	            > nodes = new HashMap<BeliefState,Map<String,Double>>();                                                                                                                                                              
			 binLevels_nodes.put(i, nodes); 
			 
			 //binLevels;                                                                                                                                                             
			 Map <String, //bins, binCount, binError                                                                                                                                  
	                Map <String,//ub-entropy (bin id)                                                                                                                                    
	                    Double//value inside bin                                                                                                                                         
	                >                                                                                                                                                                    
	            > level = new HashMap<String,Map<String,Double>>();
			 
			 Map <String,//ub-entropy (bin id)                                                                                                                                        
			 Double//value inside bin                                                                                                                                             
			 > bins = new HashMap<String,Double>();                                                                                                                                                              
                                                                                                                                                                                  
			 Map <String,//ub-entropy (bin id)                                                                                                                                        
			 Double//value inside bin                                                                                                                                             
			 > binCount= new HashMap<String,Double>();                                                                                                                                                             
                                                                                                                                                                                  
			 Map <String,//ub-entropy (bin id)                                                                                                                                        
			 Double//value inside bin                                                                                                                                             
			 > binError = new HashMap<String,Double>();       
			 
			 level.put("bins",bins);        
			 level.put("binCount",binCount);
			 level.put("binError",binError);
	         binLevels.put(i,level);
	     }
		
		
	}
	
}
