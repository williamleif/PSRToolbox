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

import java.util.HashMap;

import afest.datastructures.tree.decision.Forest;
import afest.datastructures.tree.decision.erts.ERTPoint;
import afest.datastructures.tree.decision.erts.evaluator.ForestRegressionEvaluator;
import cpsr.environment.components.Action;
import cpsr.model.components.PredictionVector;
import cpsr.planning.ertapprox.IERTEnsemble;
import cpsr.planning.exceptions.PSRPlanningException;

public class ActionERTEnsemble implements IERTEnsemble {

	ForestRegressionEvaluator<Double> eval;
	Forest<String, Double> forest;
	Action act;
	
	/**
	 * Creates and ERTEnsemble from a Forest object defined in afest package and
	 * from Action that will be associated with this ensemble.
	 * Use when seperate ensembles for each action are required. 
	 * 
	 * @param forest Forest object defined in afest package.
	 */
	public ActionERTEnsemble(Forest<String, Double> forest, Action act)
	{
		this.forest = forest;
		this.eval = new ForestRegressionEvaluator<Double>();
		this.act = act;
	}
	
	@Override
	public double getValueEstimate(ERTPoint point)
	{
		forest.classify(point);
		return eval.evaluate(forest.classify(point));
	}
	
	/**
	 * Returns Q-value estimate for this state.
	 * Q-value not utility since action ensemble implicitly
	 * associated with particular action.
	 * 
	 * @param predVec State.
	 * @return Q-value for state action pair.
	 */
	@Override
	public double getValueEstimate(PredictionVector predVec) 
	{
		ERTPoint point = createPointNoActions(predVec);
		forest.classify(point);		
		return eval.evaluate(forest.classify(point));
	}

	/**
	 * Returns Q-value iff action listed is action associated
	 * with this ensemble.
	 * Throws runtime exception otherwise.
	 * Use of this method should be limited. 
	 * 
	 * @param predVec State
	 * @param Action act
	 * @return Q-value for state action pair.
	 */
	@Override
	public double getValueEstimate(PredictionVector predVec, Action act) 
	{
		if(this.act.equals(act))
		{
			return getValueEstimate(predVec);
		}
		else
		{
			throw new PSRPlanningException("ActionEnsemble can only produce Q-value estimate for state-action pairs with" +
					"actions matching the action associated with the ActionEnsemble");
		}
	}
	
	/**
	 * Returns action associated with this ensemble
	 * 
	 * @return Action associated with this ensemble.
	 */
	public Action getAssociatedAction()
	{
		return act;
	}
	/**
	 * Helper method creates point used by afest methods.
	 * Points created by this method do not include actions as features.
	 * 
	 * @param predVec Prediction vector used to create point.
	 * @return ERTPoint used by afest methods. 
	 */
	private ERTPoint createPointNoActions(PredictionVector predVec)
	{
		HashMap<String, Double> features = new HashMap<String, Double>();
		for(int i = 0; i < predVec.getVector().getRows(); i++)
		{
			double[] stateFeatures = predVec.getVector().transpose().toArray();

			features.put(Integer.toString(i+1), stateFeatures[i]);
		}
		return new ERTPoint(features);
	}
	
}
