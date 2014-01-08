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
package cpsr.planning.ertapprox.actionensembles;

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
public class ActionEnsemblesFeatureBuilder {
	TrainingDataSet data;
	APSR psr;
	HashMap<Action, ArrayList<Double>> rewardSet;
	ArrayList<Action> actions;
	boolean actionEnsDone;
	
	/**
	 * Constructs feature builder from an DataSet and PSR.
	 * 
	 * @param data DataSet used to construct features.
	 * @param psr Predictive state representation used to construct features.
	 */
	public ActionEnsemblesFeatureBuilder(TrainingDataSet data, APSR psr)
	{
		this.data = data;
		this.psr = psr;
		//no need to check types match since QFitting does. 
		actions = new ArrayList<Action>(data.getActionSet());
		actionEnsDone = false;
	}
	
	/**
	 * Top level method for building features for action ensemble learning.
	 * 
	 * @param runs Number of runs used in estimation.
	 * @return HashMap mapping action to Hashmap which maps string feature names to
	 * an ordered list of the values they take. 
	 */
	public HashMap<Action, HashMap<String, ArrayList<Double>>> buildFeatures(int runs)
	{
		HashMap<Action, HashMap<String, ArrayList<Double>>> featureSet = new HashMap<Action, HashMap<String, ArrayList<Double>>>();
		for(Action act : data.getActionSet())
		{
			featureSet.put(act, buildTreeFeaturesForActionEnsemble(act, runs));
		}
		actionEnsDone = true;
		return featureSet;
	}
	
	/**
	 * Returns list of actions defining order for features
	 */
	public ArrayList<Action> getOrderedListOfActions()
	{
		return actions;
	}
	
	/**
	 * Top level method returns ordered lists of rewards sorted
	 * according to preceding action.
	 * (Note: must call buildActionEnsembleFeatures before calling this
	 * method)
	 * 
	 * @return Ordered lists of rewards sorted according to preceding action
	 */
	public HashMap<Action, ArrayList<Double>>getRewards()
	{
		if(actionEnsDone)
		{
			return rewardSet;
		}
		else
		{
			throw new PSRPlanningException("Must use method buildActionEnsembleFeatures before calling getActionEnsembleRewards");
		}
	}
	
	/**
	 * Builds the input features that will be used with tree in the
	 * case where actions are not included as features.
	 * 
	 * @param Action The action corresponding to this feature set. 
	 * @return The input features.
	 */
	private HashMap<String, ArrayList<Double>> buildTreeFeaturesForActionEnsemble(Action action, int runs)
	{
		HashMap<String, ArrayList<Double>> featuresForAction = intializeFeatureListForActionEnsemble();
		HashMap<Action, ArrayList<PredictionVector>> predictionVectors = constructDataForActionEnsembles(runs);
		for(PredictionVector predVec : predictionVectors.get(action))
		{
			for(int i = 0; i < predVec.getVector().getRows(); i++)
			{
				//TODO: IF THERE ARE PLANNING PROBS IT IS HERE
				double[] stateFeatures = predVec.getVector().transpose().toArray();

				featuresForAction.get(Integer.toString(i+1)).add(stateFeatures[i]);
			}
		}
		return featuresForAction;
	}

	/**
	 * Initializes feature list for action ensemble case
	 * @return The initialized features
	 */
	private HashMap<String, ArrayList<Double>> intializeFeatureListForActionEnsemble()
	{
		HashMap<String, ArrayList<Double>> features = new HashMap<String, ArrayList<Double>>();
		double[] stateFeatures = psr.getPredictionVector().getVector().transpose().toArray();
		
		for(int i = 0; i < stateFeatures.length; i++)
		{
			features.put(Integer.toString(i+1), new ArrayList<Double>());
		}
		return features;
	}
	
	
	/**
	 * Constructs the training data in proper format for planning from DataSet
	 * @param runs The number of training runs to use. 
	 * @return A hashmap mapping actions to corresponding list of prediction vectors
	 */
	private HashMap<Action, ArrayList<PredictionVector>> constructDataForActionEnsembles(int runs)
	{
		psr.resetToStartState();
		actions = new ArrayList<Action>();

		HashMap<Action, ArrayList<PredictionVector>> predictionVectors = intializeActionEnsemblePredVecList();
		
		int runCount = 1;
		data.resetData();
		
		while (runCount < runs)
		{
			stepInActionEnsembleConstruction(predictionVectors);
			if(checkForReset(predictionVectors))
			{
				runCount++;
			}
				
		}
	
		return predictionVectors;
	}
	
	
	/**
	 * Helper method computes iteration of data construction in
	 * action ensemble case.
	 * 
	 * @param predictionVectors The list of prediction vectors
	 * @param fittedQpsr The PSR.
	 */
	private void stepInActionEnsembleConstruction(HashMap<Action, ArrayList<PredictionVector>> predictionVectors)
	{
		ActionObservation actob = data.getNextActionObservationForPlanning();
		Action act = actob.getAction();
		actions.add(act);
		rewardSet.get(act).add(data.getReward());
		predictionVectors.get(act).add(psr.getPredictionVector());
		psr.update(actob);
	}
	

	/**
	 * Helper method intializes HashMap with actions mapping to lists
	 * of prediction vectors
	 * 
	 * @return HashMap mapping actions to list of prediction vectors
	 */
	private HashMap<Action, ArrayList<PredictionVector>> intializeActionEnsemblePredVecList()
	{
		HashMap<Action, ArrayList<PredictionVector>> predictionVectors = new HashMap<Action, ArrayList<PredictionVector>>();
		rewardSet = new HashMap<Action, ArrayList<Double>>();
		for(Action act : data.getActionSet())
		{
			predictionVectors.put(act, new ArrayList<PredictionVector>());
			rewardSet.put(act, new ArrayList<Double>());
		}
		
		return predictionVectors;
	}
	
	/**
	 * Helper method determines if a run terminated.
	 * If so, true returned and prediction vector reset. 
	 * @param predictionVectors 
	 * 
	 * @return Boolean representing whether reset performed.
	 */
	private boolean checkForReset(HashMap<Action, ArrayList<PredictionVector>> predictionVectors)
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
