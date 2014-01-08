package cpsr.environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cpsr.environment.components.ActionObservation;

public class DataSet implements Serializable {

	protected int batchNum = -1;
	/**
	 * 
	 */
	private static final long serialVersionUID = 3062257041262864194L;
	/**
	 * @serialField
	 */
	protected List<List<List<ActionObservation>>> data;
	/**
	 * @serialField
	 */
	protected List<List<List<Double>>> rewards;
	private List<Integer> runLengths;

	public DataSet() 
	{
		data = new ArrayList<List<List<ActionObservation>>>();
		rewards = new ArrayList<List<List<Double>>>();
		runLengths = new ArrayList<Integer>();
	}

	public void addRunData(List<ActionObservation> runActObs, List<Double> runRewards)
	{
		data.get(batchNum).add(runActObs);
		rewards.get(batchNum).add(runRewards);
		runLengths.add(runRewards.size());
	}

	public void newDataBatch(int maxSize)
	{
		batchNum++;
		data.add(new ArrayList<List<ActionObservation>>());
		rewards.add(new ArrayList<List<Double>>());
	}


	public List<Integer> getRunLengths()
	{
		return runLengths;
	}
	
	public List<List<Double>> getRewards()
	{
		List<List<Double>> allRewards = new ArrayList<List<Double>>();
	
		for(int i = 0; i < rewards.size(); i++)
		{
			allRewards.addAll(rewards.get(i));
		}
		
		return allRewards;
		
		
	}


}