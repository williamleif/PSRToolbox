package cpsr.stats;

import java.util.ArrayList;
import java.util.List;

import cpsr.environment.components.ActObSequenceSet;

public class PSRObserver 
{
	private List<List<Double>> singularValuesOverBatchUpdates;
	private List<Integer> numTestsOverBatchUpdates, numHistoriesOverBatchUpdates;
	
	public PSRObserver()
	{
		singularValuesOverBatchUpdates = new ArrayList<List<Double>>();
		numTestsOverBatchUpdates = new ArrayList<Integer>();
		numHistoriesOverBatchUpdates =  new ArrayList<Integer>();
	}
	
	public void modelUpdated(double[] singVals, ActObSequenceSet tests, ActObSequenceSet histories)
	{
		List<Double> singValList = new ArrayList<Double>();
		for(int i = 0; i < singVals.length; i++)
			singValList.add(singVals[i]);
		
		singularValuesOverBatchUpdates.add(singValList);
		numTestsOverBatchUpdates.add(tests.size());
		numHistoriesOverBatchUpdates.add(histories.size());
	}
	
	public List<List<Double>> getSingVals()
	{
		return singularValuesOverBatchUpdates;
	}
	
	public List<Integer> getTestSetSizes()
	{
		return numTestsOverBatchUpdates;
	}
	
	public List<Integer> getHistorySetSizes()
	{
		return numHistoriesOverBatchUpdates;
	}
	
}
