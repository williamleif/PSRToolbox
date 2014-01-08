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
package cpsr.planning.ertapprox.singleensemble;

import java.util.ArrayList;
import java.util.HashMap;

import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.model.APSR;
import cpsr.model.components.PredictionVector;
import cpsr.planning.exceptions.PSRPlanningException;

/**
 * Class defines methods used to build features for use
 * use Q-iteration planning algorithm.
 * 
 * @author William Hamilton
 */
public class SingleEnsembleFeatureBuilder {
	TrainingDataSet data;
	APSR psr;
	ArrayList<Double> rewards;
	ArrayList<Action> actions;
	boolean singleEnsDone;
	
	/**
	 * Constructs feature builder from an DataSet and PSR.
	 * @param data DataSet used to construct features/
	 * @param psr Predictive state representation used to construct features/
	 */
	public SingleEnsembleFeatureBuilder(TrainingDataSet data, APSR psr)
	{
		this.data = data;
		this.psr = psr;
		//no need to check types match since QFitting does. 
		actions = new ArrayList<Action>(data.getActionSet());
		singleEnsDone = false;
	}
	
	/**
	 * Top level method for building features for single ensemble learning
	 * set.
	 * (Note: method simply calls private method, included for syntax consistency).
	 * @param runs Number of runs used in estimation
	 * @return HashMap mapping string feature names to an ordered list of the
	 * values they take. 
	 */
	public HashMap<String, ArrayList<Double>> buildFeatures(int runs)
	{
		singleEnsDone = true;
		return buildTreeFeaturesForSingleEnsemble(runs);
	}
	
	/**
	 * Top level method returns ordered list of rewards.
	 * (Note: must call buildSingleEnsembleFeatures before calling 
	 * this method).
	 * 
	 * @return Ordered list of rewards
	 */
	public ArrayList<Double> getRewards()
	{
		if(singleEnsDone)
		{
			return rewards;
		}
		else
		{
			throw new PSRPlanningException("Must use method buildSingleEnsembleFeatures before calling getSingleEnsembleRewards");
		}
	}

	/**
	 * Builds input features that will be used with tree in the case
	 * where actions are included as features.
	 */
	private HashMap<String, ArrayList<Double>> buildTreeFeaturesForSingleEnsemble(int runs)
	{
			HashMap<String, ArrayList<Double>> features = intializeFeatureListForSingleEnsemble();
			ArrayList<PredictionVector> predictionVectors = constructDataForSingleEnsemble(runs);
			int count = 0;
			for(PredictionVector predVec : predictionVectors)
			{
				double[] stateFeatures = predVec.getVector().transpose().toArray();
				for(int i = 0; i < stateFeatures.length; i++)
				{
					features.get(Integer.toString(i+1)).add(stateFeatures[i]);
				}
				features.get("Action").add((double)actions.get(count).getID());
				count++;
			}
		return features;
	}


	/**
	 * Initializes feature list for single ensemble case
	 * @return The initialized features
	 */
	private HashMap<String, ArrayList<Double>> intializeFeatureListForSingleEnsemble()
	{
		HashMap<String, ArrayList<Double>> features = new HashMap<String, ArrayList<Double>>();
		double[] stateFeatures = psr.getPredictionVector().getVector().transpose().toArray();
		
		for(int i = 0; i < stateFeatures.length; i++)
		{
			features.put(Integer.toString(i+1), new ArrayList<Double>());
		}
		features.put("Action", new ArrayList<Double>());
		
		return features;
	}


	/**
	 * Constructs training data for the single ensemble case.
	 * @param runs Number of runs to collect training data. 
	 * @return An ordered list of prediction vectors. 
	 */
	private ArrayList<PredictionVector> constructDataForSingleEnsemble(int runs)
	{
		psr.resetToStartState();
		rewards = new ArrayList<Double>();
		ArrayList<PredictionVector> predictionVectors = new ArrayList<PredictionVector>();
		
		int runCount = 1;
		while(runCount < runs)
		{
			stepInSingleEnsembleConstruction(predictionVectors);
			if(checkForReset())
			{
				runCount++;
			}
		}	
		return predictionVectors;
	}
	
	/**
	 * Helper method computes step in single ensemble data construction
	 * 
	 * @param predictionVectors The ordered list of prediction vectors. 
	 */
	private void stepInSingleEnsembleConstruction(ArrayList<PredictionVector> predictionVectors)
	{
		ActionObservation actob = data.getNextActionObservationForPlanning();
		Action act = actob.getAction();
		actions.add(act);
		rewards.add(data.getReward());
		predictionVectors.add(psr.getPredictionVector());
		psr.update(actob);	
		
	}
	
	
	/**
	 * Helper method determines if a run terminated.
	 * If so, true returned and prediction vector reset. 
	 * 
	 * @return Boolean representing whether reset performed.
	 */
	private boolean checkForReset()
	{
		if(data.resetPerformed())
		{
			psr.resetToStartState();
			return true;
		}
		else
		{
			return false;
		}
	}
}
