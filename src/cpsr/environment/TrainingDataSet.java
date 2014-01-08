/*
 *   Copyright 2012 William Hamilton
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package cpsr.environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import cpsr.environment.components.ActObSequenceSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.environment.components.Observation;
import cpsr.environment.components.ResetActionObservation;

/**
 * Class encapsulates a data set generated from an environment
 * simulator or taken from file.
 * Contains all information necessary to build PSR representation
 * of a domain.
 * 
 * @author William Hamilton
 *
 */
@SuppressWarnings("serial")
public class TrainingDataSet extends DataSet implements Serializable
{

	protected HashSet<Action> validActs;
	protected HashSet<Observation> validObs;
	protected HashSet<ActionObservation> validActObs;
	protected int maxTestLength;
	protected int runCounter, stepCounter;
	protected int psrRunCounter, psrStepCounter, planBatchCounter;
	protected ActObSequenceSet histories, tests;
	private int currentBatchMaxSize, planRuns;
	private double lastReward;

	/**
	 * Explicit default constructor for inheritance.
	 * @param goalSampleRatio TODO
	 */
	public TrainingDataSet(int maxTestLength)
	{
		super();
		validActObs = new HashSet<ActionObservation>();
		validObs = new HashSet<Observation>();
		validActs = new HashSet<Action>();
		histories = new ActObSequenceSet();
		tests = new ActObSequenceSet();
		
		this.maxTestLength = maxTestLength;
		
	}
	
	/**
	 * Explicit default constructor for inheritance.
	 * @param goalSampleRatio TODO
	 */
	public TrainingDataSet(int maxTestLength, int planRuns)
	{
		super();
		validActObs = new HashSet<ActionObservation>();
		validObs = new HashSet<Observation>();
		validActs = new HashSet<Action>();
		histories = new ActObSequenceSet();
		tests = new ActObSequenceSet();
		this.planRuns = planRuns;
		
		this.maxTestLength = maxTestLength;
		
	}


	/**
	 * Check and if necessary add a partial sequence to test and history set.
	 * 
	 * @param actobs
	 */
	public void addRunDataForTraining(ArrayList<ActionObservation> actobs)
	{
		histories.addActObSequence(actobs);
		
		for(int i = 0; i < actobs.size(); i++)
		{
			if(actobs.size() - i <= maxTestLength)
				tests.addActObSequence(new ArrayList<ActionObservation>(actobs.subList(i, actobs.size())));
		}
		
		ActionObservation mostRecentActOb = actobs.get(actobs.size()-1);
		validActObs.add(mostRecentActOb);
		validActs.add(mostRecentActOb.getAction());
		validObs.add(mostRecentActOb.getObservation());
	}
	
	@Override
	public void addRunData(List<ActionObservation> runActObs, List<Double> runRewards)
	{
		runActObs.add(new ResetActionObservation());
		tests.addActObSequence(runActObs.subList(runActObs.size()-1, runActObs.size()));
		super.addRunData(runActObs, runRewards);
	}


	/**
	 * Returns next action-observation pair
	 * @return Next action-observation pair
	 */
	public ActionObservation getNextActionObservation()
	{
		ActionObservation actob =  data.get(batchNum).get(runCounter).get(stepCounter);
		stepCounter++;
		if(stepCounter == data.get(batchNum).get(runCounter).size())
		{
			stepCounter = 0;
			runCounter = (runCounter+1)%data.get(batchNum).size();
		}
		return actob;
	}

	/**
	 * Returns next action-observation pair
	 * @return Next action-observation pair
	 */
	public ActionObservation getNextActionObservationForPlanning()
	{
		
		if(runCounter >= data.get(planBatchCounter).size() || runCounter >= planRuns)
		{
			planBatchCounter++;
			runCounter = 0;
		}
		
		ActionObservation actob =  data.get(planBatchCounter).get(runCounter).get(stepCounter);
		lastReward = rewards.get(planBatchCounter).get(runCounter).get(stepCounter);
		stepCounter++;
		if(stepCounter == data.get(planBatchCounter).get(runCounter).size()-1)
		{
			stepCounter = 0;
			runCounter = (runCounter+1)%data.get(planBatchCounter).size();
		}
		return actob;
	}
	
	public void resetData()
	{
		stepCounter = 0;
		runCounter = 0;
		planBatchCounter = 0;
	}
	
	public void newDataBatch(int maxSize)
	{
		super.newDataBatch(maxSize);
		currentBatchMaxSize = maxSize;
		resetData();
	}
	
	public int getNumberOfRunsInBatch()
	{
		return data.get(batchNum).size();
	}
	
	public void importanceSample(double sampleRatio)
	{
		ArrayList<Integer> goalRuns = new ArrayList<Integer>();
		ArrayList<Integer> badRuns = new ArrayList<Integer>();
		
		for(int i = 0; i < data.get(batchNum).size(); i++)
		{
			if(rewards.get(batchNum).get(i).get(rewards.get(batchNum).get(i).size()-1) > 0.0)
			{
				goalRuns.add(i);
			}
			else
			{
				badRuns.add(i);
			}
		}
		
		List<List<ActionObservation>> sampledBatch = new ArrayList<List<ActionObservation>>();
		List<List<Double>> sampledRewards = new ArrayList<List<Double>>();
		
		Random rando = new Random();
		for(int i = 0; i < currentBatchMaxSize; i++)
		{
			int index;
			
			if(rando.nextDouble() < sampleRatio)
			{
				index = goalRuns.get(rando.nextInt(goalRuns.size()));
			}
			else
			{
				index = badRuns.get(rando.nextInt(badRuns.size()));
			}
			
			sampledBatch.add(data.get(batchNum).get(index));
			sampledRewards.add(rewards.get(batchNum).get(index));
		}
		
		data.set(batchNum, sampledBatch);
		rewards.set(batchNum, sampledRewards);
		
	}

	/**
	 * Returns reward associated with current (i.e. last returned
	 * action observation pair).
	 * 
	 * @return Reward.
	 */
	public double getReward()
	{
		return lastReward;
	}
	
	
	/**
	 * Test whether reset performed on last iterations
	 * 
	 * @return Boolean representing whether reset performed
	 */
	public boolean resetPerformed()
	{
		return stepCounter == 0;
	}

	/**
	 * Get the dimension of observation space
	 * 
	 * @return The size of the observations space
	 */
	public int getNumberObservations()
	{
		return validObs.size();
	}

	/**
	 * Returns an ArrayList of all valid action-observations pairs.
	 * Action-observation pairs are valid if they occur in training set.
	 * 
	 * @return
	 */
	public HashSet<ActionObservation> getValidActionObservationSet()
	{
		return validActObs;
	}

	/**
	 * Returns list of all possible actions in the domain.
	 * @return All possible actions in the domain. 
	 */
	public HashSet<Action> getActionSet()
	{
		return validActs;
	}
	
	public ActObSequenceSet getTests()
	{
		return tests;
	}
	
	public ActObSequenceSet getHistories()
	{
		return histories;
	}
	
}
