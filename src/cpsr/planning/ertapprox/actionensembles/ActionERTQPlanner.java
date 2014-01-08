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

import afest.datastructures.tree.decision.erts.ERTPoint;
import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.model.APSR;
import cpsr.planning.APSRPlanner;
import cpsr.planning.IQFunction;
import cpsr.planning.exceptions.PSRPlanningException;

/**
 * Class for planning with fitted q and action ensembles.
 * 
 * @author William Hamilton
 */
public class ActionERTQPlanner extends APSRPlanner {

	ArrayList<Double> rewards;
	double aDiscount;

	/**
	 * Constructs fittedQ from PSR and DataSet.
	 * 
	 * @param psr The predictive state representation used in planning.
	 * @param data The data used to plan.
	 * @param type Type of ERT ensemble(s). Either "ActionEnsembles" or 
	 * "ActionEnsemble."
	 * In the ActionEnsemble case actions are treated as features, and in the 
	 * ActionEnsemble case separates ensembles are built for each action. 
	 * @deprecated
	 */
	public ActionERTQPlanner(APSR psr, TrainingDataSet data) 
	{
		if(psr.getDataSet().getClass() != data.getClass())
		{
			throw new PSRPlanningException("DataSets used to learn reward" +
					" must be same type as one used to learn PSR");
		}
		this.psr = psr;
		this.data = data;
	}

	/**
	 * Constructs fittedQ from PSR and DataSet.
	 * 
	 * @param psr The predictive state representation used in planning.
	 * @param type Type of ERT ensemble(s). Either "ActionEnsembles" or 
	 * "ActionEnsemble."
	 * In the ActionEnsemble case actions are treated as features, and in the 
	 * ActionEnsemble case separates ensembles are built for each action. 
	 */
	public ActionERTQPlanner(APSR psr) 
	{
		this.psr = psr;
		this.data = null;
	}

	/**
	 * Learns a policy using specified parameters.
	 * 
	 * @param data The data set used.
	 * @param runs Number of training runs to use when collecting initial data.
	 * @param iterations Number of iterations to use when training trees.
	 * @param treesPerEnsemble Number of trees per ensemble.
	 * @param k Number of splits to create at each inner node.  
	 * If k is null, then sqrt(number of attribute) will be used.
	 * @param nMin Specifies when trees stop growing if |set| < nMin at leaf, growing stops.
	 * @return ActionEnsembleQFunction learned using specified PSR, 
	 * DataSet,and tree ensemble parameters
	 */
	public IQFunction learnQFunction(TrainingDataSet data, int runs, int iterations, int k, int nMin, int treesPerEnsemble, double pDiscount)
	{
		this.data = data;
		aDiscount = pDiscount;
		this.qFunction = learnQFunctionHelper(runs, iterations, treesPerEnsemble, k, nMin);
		return qFunction;
	}


	public ActionEnsemblesQFunction learnQFunctionHelper(int runs, int iterations, 
			int treesPerEnsemble, int k, int nMin)
	{
		ActionEnsemblesFeatureBuilder featBuilder = new ActionEnsemblesFeatureBuilder(data, psr);	
		HashMap<Action, HashMap<String, ArrayList<Double>>> features = featBuilder.buildFeatures(runs);

		return new ActionEnsemblesQFunction(psr, learnERTEnsembleQFunction(features, featBuilder.getRewards(),
				featBuilder.getOrderedListOfActions(), iterations, treesPerEnsemble, k, nMin));
	}


	/**
	 * Returns an ERTEnsemble (used in QFunctions) learned using specified psr, DataSet, 
	 * and tree ensemble parameters.  
	 *
	 * @param features Features used to train regression ensemble.
	 * @param targets Targets (labels) used to train regression ensemble. 
	 * @param iterations Number of iterations to use when training trees.
	 * @param treesPerEnsemble Number of trees per ensemble.
	 * @param k Number of splits to create at each inner node. 
	 *  If k is null, then sqrt(number of attribute) will be used.
	 * @param nMin Specifies when trees stop growing if |set| < nMin at leaf, growing stops.
	 * @return Mapping of actions to ActionERTEnsembles
	 */
	private HashMap<Action, ActionERTEnsemble> learnERTEnsembleQFunction(HashMap<Action, HashMap<String,
			ArrayList<Double>>> features, HashMap<Action, ArrayList<Double>> rewards,
			ArrayList<Action> actions, int iterations, int treesPerEnsemble, int k, int nMin)
			{
		boolean firstIteration = true;
		HashMap<Action, ActionERTEnsemble> actionEnsembles = new HashMap<Action, ActionERTEnsemble>();
		HashMap<Action, ArrayList<Double>> targets = rewards;	
		for(int i = 0; i < iterations; i++)
		{
			if(firstIteration)
			{
				actionEnsembles = nthIteration(features, targets, treesPerEnsemble, k, nMin);
				firstIteration = false;
			}
			else
			{
				targets = computeTargets(features, actionEnsembles, actions, rewards);
				actionEnsembles = nthIteration(features, targets, treesPerEnsemble, k, nMin);
			}
		}
		return actionEnsembles;
			}

	/**
	 * Helper method performs one iteration of learning.
	 *
	 * @param features Features used to train regression ensemble.
	 * @param targets Targets (labels) used to train regression ensemble. 
	 * @param iterations Number of iterations to use when training trees.
	 * @param treesPerEnsemble Number of trees per ensemble.
	 * @param k Number of splits to create at each inner node.  If k is null, 
	 * then sqrt(number of attribute) will be used.
	 * @param nMin Specifies when trees stop growing if |set| < nMin at leaf, growing stops.
	 * @return Mapping of actions to ActionERTEnsembles
	 */
	private HashMap<Action, ActionERTEnsemble> nthIteration(HashMap<Action, HashMap<String, 
			ArrayList<Double>>> features,HashMap<Action, ArrayList<Double>> targets, 
			int treesPerEnsemble, int k, int nMin)
			{
		HashMap<Action, ActionERTEnsemble> actionEnsembles = new HashMap<Action, ActionERTEnsemble>();
		ActionERTEnsembleGrower grower = new ActionERTEnsembleGrower(data, psr);

		for(Action act : data.getActionSet())
		{
			grower.setTrainData(features.get(act), targets.get(act));
			actionEnsembles.put(act, grower.growActionERTEnsemble(k, treesPerEnsemble, nMin, act));
		}

		return actionEnsembles;
			}

	/**
	 * Computes targets used in Q-iteration.
	 * 
	 * @param features Features of each point.
	 * @param actionEnsembles Mapping of actions to ActionERTEnsembles.
	 * @param actions Ordered list of actions.
	 * @param rewards Ordered list of immediate rewards. 
	 * @return Mapping of actions to discounted rewards (i.e. targets).
	 */
	private HashMap<Action, ArrayList<Double>> computeTargets(HashMap<Action, HashMap<String, ArrayList<Double>>> features, 
			HashMap<Action, ActionERTEnsemble> actionEnsembles, ArrayList<Action> actions,
			HashMap<Action, ArrayList<Double>> rewards)
			{
		HashMap<Action, ArrayList<Double>> targets =  new HashMap<Action,ArrayList<Double>>();
		HashMap<Action, Integer> actionCounters = new HashMap<Action, Integer>();
		intializeMapsForComputeTargets(targets, actionCounters, actions);
		HashMap<Action, ArrayList<ERTPoint>> points = intializeERTPoints(features);

		int inRunCount = 1;
		int runCount = 0;
		for(int i = 0; i < actions.size(); i++)
		{
			Action act1 = actions.get(i);
			int index = actionCounters.get(act1);
			actionCounters.put(act1, actionCounters.get(act1)+1);

			double maxNextState = 0;
			if(i < actions.size()-1 && !(inRunCount == data.getRunLengths().get(runCount)))
			{
				Action act2 = actions.get(i+1);
				maxNextState = Double.MIN_VALUE;

				for(Action act : data.getActionSet())
				{
					maxNextState = Math.max(actionEnsembles.get(act).getValueEstimate(points.get(act2).get(actionCounters.get(act2))), maxNextState);
				}

			}
			if(inRunCount == data.getRunLengths().get(runCount))
			{
				runCount++;
				inRunCount = 0;
			}
			inRunCount++;
			targets.get(actions.get(i)).add(rewards.get(act1).get(index)+aDiscount*maxNextState);

		}

		return targets;
			}

	/**
	 * Intializes maps for method computeTargets()
	 * 
	 * @param targets The targets map.
	 * @param actionCounters Action counter map.
	 * @param actions Ordered list of actions.
	 */
	private void intializeMapsForComputeTargets(HashMap<Action, ArrayList<Double>> targets, 
			HashMap<Action, Integer> actionCounters, ArrayList<Action> actions)
	{
		for(Action act : data.getActionSet())
		{

			actionCounters.put(act, 0);
			targets.put(act, new ArrayList<Double>());
		}
	}

	/**
	 * Initializes mapping of actions to list of ERT points
	 * 
	 * @param features Mapping of actions to list of features/
	 * @return Mapping of actions to list of ERT points
	 */
	private HashMap<Action, ArrayList<ERTPoint>> intializeERTPoints(HashMap<Action, HashMap<String, ArrayList<Double>>> features)
	{
		HashMap<Action, ArrayList<ERTPoint>> points = new HashMap<Action, ArrayList<ERTPoint>>();

		for(Action act : data.getActionSet())
		{
			points.put(act, ERTPoint.getERTPoints(features.get(act)));
		}

		return points;
	}



}
