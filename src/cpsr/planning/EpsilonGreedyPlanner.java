/*
 *   Copyright 2013 William Hamilton
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
package cpsr.planning;

import java.util.Random;

import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;

/**
 * An epsilon greedy planner that uses a predefied policy with prob (1-epsilon).
 * Otherwise a random policy is used.
 * Used for sample collection in planning.
 * 
 * @author William Hamilton
 */
public class EpsilonGreedyPlanner extends APSRPlanner
{

	RandomPlanner aRandPlanner;
	APSRPlanner aPSRPlanner;
	Random aRandGen;
	double aEpsilon;
	
	/**
	 * Constructs an epsilon greedy planner.
	 * 
	 * @param pRandPlanner A random policy/planner for the domain.
	 * @param pPlanner A planning object (non-random) for the domain.
	 * @param pEpsilon The epsilon value to use.
	 * @param pSeed Random seed to use.
	 */
	public EpsilonGreedyPlanner(RandomPlanner pRandPlanner, APSRPlanner pPlanner, double pEpsilon, int pSeed)
	{
		aRandPlanner = pRandPlanner;
		aPSRPlanner = pPlanner;
		aRandGen = new Random(pSeed);
		aEpsilon = pEpsilon;
	}
	
	@Override
	public void update(ActionObservation pAO)
	{
		aPSRPlanner.update(pAO);
	}
	
	@Override
	public void resetToStartState()
	{
		aPSRPlanner.resetToStartState();
	}
	
	@Override
	public Action getAction()
	{
		if(aRandGen.nextDouble() > aEpsilon)
		{
			return aPSRPlanner.getAction();
		}
		else
		{
			return aRandPlanner.getAction();
		}
	}
	
	@Override
	public IQFunction learnQFunction(TrainingDataSet data, int runs, int iterations,
			int treesPerEnsemble, int k, int nMin, double pDiscount) {
		return aPSRPlanner.learnQFunction(data, runs, iterations, treesPerEnsemble, k, nMin, pDiscount);
	}

}
