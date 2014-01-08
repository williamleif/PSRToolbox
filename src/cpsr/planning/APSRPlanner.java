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

package cpsr.planning;

import cpsr.environment.DataSet;
import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.model.APSR;
import cpsr.planning.exceptions.PSRPlanningException;

/**
 * Top level class for performing planning.
 * 
 * @author William Hamilton
 */
public abstract class APSRPlanner implements IPlanner
{
	/**The PSR associated with this planner*/
	protected APSR psr;

	/**The Q function used for planning*/
	protected IQFunction qFunction;

	protected TrainingDataSet data;

	/**
	 * Constructs a planning object without any intialization.
	 * Plan must be learnt before getAction() and update() methods
	 * can be used.
	 */
	public APSRPlanner()
	{
		super();
	}

	public APSRPlanner(APSR psr, TrainingDataSet data)
	{
		this.psr = psr;
		this.data = data;
	}

	@Override
	public Action getAction()
	{
		if(qFunction == null)
		{
			throw new PSRPlanningException("Must learn a policy before asking for" +
					" best action!");
		}

		Action bestAction = null;
		double bestReward = Double.NEGATIVE_INFINITY;

		for(Action act : psr.getActionSet())
		{
			double currentReward = qFunction.getQValue(psr, act);
			if(currentReward > bestReward)
			{
				bestReward = currentReward;
				bestAction = act;
			}
		}
		return bestAction;
	}


	/**
	 * Updates the state with action-observation pair.
	 * 
	 * @param actob Action-observation pair.
	 */
	public void update(ActionObservation actob)
	{
		psr.update(actob);
	}

	/**
	 * Resets the PSR to its start state.
	 */
	public void resetToStartState()
	{
		psr.resetToStartState();
	}

	/**
	 * @return Reference to current dataset in use.
	 */
	public DataSet getCurrentData()
	{
		return data;
	}


	/**
	 * Returns a QFunction learned using specified PSR, DataSet and tree ensemble parameters.  
	 * 
	 * @param runs Number of training runs to use when collecting intial data.
	 * @param iterations Number of iterations to use when training trees.
	 * @param treesPerEnsemble Number of trees per ensemble.
	 * @param k Number of splits to create at each inner node.  If k is null, then sqrt(number of attribute) will be used.
	 * @param nMin Specifies when trees stop growing if |set| < nMin at leaf, growing stops.
	 * @param pDiscount the discount factor.
	 * @return Q function policy
	 */
	public abstract IQFunction learnQFunction(TrainingDataSet data, int runs, int iterations, int treesPerEnsemble, int k, int nMin, double pDiscount);


}
