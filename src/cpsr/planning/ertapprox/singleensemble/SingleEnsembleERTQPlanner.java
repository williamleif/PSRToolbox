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

import afest.datastructures.tree.decision.erts.ERTPoint;
import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.model.APSR;
import cpsr.planning.APSRPlanner;
import cpsr.planning.IQFunction;
import cpsr.planning.exceptions.PSRPlanningException;

/**
 * Class for planning with fitted q and single ensemble.
 * 
 * @author William Hamilton
 */
public class SingleEnsembleERTQPlanner extends APSRPlanner 
{
	
	double aDiscount;
	
	/**
	 * Constructs fittedQ from PSR and DataSet.
	 * 
	 * @param psr The predictive state representation used in planning.
	 * @param data The DataSet used.
	 * @param type Type of ERT ensemble(s). Either "ActionEnsembles" or "SingleEnsemble."
	 * In the SingleEnsemble case actions are treated as features, and in the 
	 * ActionEnsemble case separates ensembles are built for each action. 
	 */
	public SingleEnsembleERTQPlanner(APSR psr, TrainingDataSet data) 
	{
		if(psr.getDataSet().getClass() != data.getClass())
		{
			throw new PSRPlanningException("DataSets used to learn reward must be same type as one used to learn PSR");
		}
		
		this.psr = psr;
		this.data = data;
	}
	

	@Override
	public IQFunction learnQFunction(TrainingDataSet data, int runs, int iterations, int k, int nMin, int treesPerEnsemble, double pDiscount)
	{
		this.data = data;
		aDiscount = pDiscount;
		this.qFunction = learnQFunctionHelper(runs, iterations, treesPerEnsemble, k, nMin);
		return qFunction;
	}

	/**
	 * Returns an SingleEnsembleQFunction learned using specified PSR, DataSet, and tree ensemble parameters.  
	 * 
	 * @param runs Number of training runs to use when collecting initial data.
	 * @param iterations Number of iterations to use when training trees.
	 * @param treesPerEnsemble Number of trees per ensemble.
	 * @param k Number of splits to create at each inner node.  If k is null, then sqrt(number of attribute) will be used.
	 * @param nMin Specifies when trees stop growing if |set| < nMin at leaf, growing stops.
	 * @return
	 */
	public SingleEnsembleQFunction learnQFunctionHelper(int runs, int iterations, int treesPerEnsemble, int k, int nMin)
	{
		SingleEnsembleFeatureBuilder featBuilder = new SingleEnsembleFeatureBuilder(data, psr);	
		HashMap<String, ArrayList<Double>> features = featBuilder.buildFeatures(runs);

		SingleERTEnsemble ertEnsemble = learnERTEnsembleQFunction(features, featBuilder.getRewards(),
				iterations, treesPerEnsemble, k, nMin);

		return new SingleEnsembleQFunction(data, psr, ertEnsemble);
	}

	/**
	 * Returns an ERTEnsemble (used in QFunctions) learned using specified PSR, DataSet, and tree ensemble parameters.  
	 *
	 * @param features Features used to train regression ensemble.
	 * @param targets Targets (labels) used to train regression ensemble. 
	 * @param iterations Number of iterations to use when training trees.
	 * @param treesPerEnsemble Number of trees per ensemble.
	 * @param k Number of splits to create at each inner node.  If k is null, then sqrt(number of attribute) will be used.
	 * @param nMin Specifies when trees stop growing if |set| < nMin at leaf, growing stops.
	 * @return
	 */
	private SingleERTEnsemble learnERTEnsembleQFunction(HashMap<String, ArrayList<Double>> features, ArrayList<Double> rewards, int iterations, int treesPerEnsemble, int k, int nMin)
	{
		boolean firstIteration = true;
		SingleERTEnsemble qN = null;
		SingleERTEnsembleGrower grower = new SingleERTEnsembleGrower(data, psr);
		ArrayList<Double> targets = rewards;
		for(int i = 0; i < iterations; i++)
		{
			if(firstIteration)
			{
				firstIteration = false;
			}
			else
			{
				targets = computeTargets(features,rewards, qN);
			}
			grower.setTrainData(features, targets);
			qN = (SingleERTEnsemble)grower.growSingleERTEnsemble(k, treesPerEnsemble, nMin);
		}
		return qN;
	}

	/**
	 * Computes list of targets using ensemble.
	 * 
	 * @param features Features used to compute targets
	 * @param qN Ensemble used to compute targets.
	 * @return List of targets.
	 */
	private ArrayList<Double> computeTargets(HashMap<String, ArrayList<Double>> features, ArrayList<Double> rewards, SingleERTEnsemble qN)
	{

		ArrayList<Double> targets = new ArrayList<Double>();
		int count = 0;
		int runCount = 0;
		int inRunCount = 1;
		while(count < rewards.size()-1)
		{
			double maxNextState;
			if(count < rewards.size()-1 && !(data.getRunLengths().get(runCount) == inRunCount))
			{
				HashMap<Integer, ERTPoint> actPoints = createERTPointsForActions(features, count+1);

				maxNextState = Double.MIN_VALUE;

				for(ERTPoint point : actPoints.values())
				{
					maxNextState = Math.max(qN.getValueEstimate(point), maxNextState);
				}
			}
			else
			{
				maxNextState = 0;
			}
			
		
		
			targets.add(rewards.get(count)+aDiscount*maxNextState);
			
			if(data.getRunLengths().get(runCount) == inRunCount)
			{
				runCount++;
				inRunCount = 0;
			}
			
			count++;
			inRunCount++;
			
		}
		return targets;
	}

	private HashMap<Integer, ERTPoint> createERTPointsForActions(HashMap<String, ArrayList<Double>> features, int count)
	{
		HashMap<Integer, ERTPoint> actPoints = new HashMap<Integer, ERTPoint>();

		for(Action act : data.getActionSet())
		{
			HashMap<String, Double> ertPointData =  new HashMap<String, Double>();
			for(String attr : features.keySet())
			{
				if(!attr.equals("Action"))
				{
					ertPointData.put(attr, features.get(attr).get(count));
				}
				ertPointData.put("Action", ((double)act.getID()));
			}
			actPoints.put(act.getID(), new ERTPoint(ertPointData));
		}
		return actPoints;
	}

}
