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

import java.util.HashMap;

import afest.datastructures.tree.decision.Forest;
import afest.datastructures.tree.decision.erts.ERTPoint;
import afest.datastructures.tree.decision.erts.evaluator.ForestRegressionEvaluator;
import cpsr.environment.DataSet;
import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.Action;
import cpsr.model.components.PredictionVector;
import cpsr.planning.ertapprox.IERTEnsemble;

public class SingleERTEnsemble implements IERTEnsemble {

	TrainingDataSet data;
	ForestRegressionEvaluator<Double> eval;
	Forest<String, Double> forest;

	
	/**
	 * Creates and ERTEnsemble from a Forest object defined in afest package
	 * 
	 * @param forest Forest object defined in afest package.
	 */
	public SingleERTEnsemble(Forest<String, Double> forest, DataSet data)
	{
		this.forest = forest;
		this.eval = new ForestRegressionEvaluator<Double>();
	}
	
	@Override
	public double getValueEstimate(ERTPoint point)
	{
		forest.classify(point);
		return eval.evaluate(forest.classify(point));
	}
	
	/**
	 * Returns utility-estimate for a state.
	 * 
	 * @param predVec State.
	 * @return Utility estimate.
	 */
	@Override
	public double getValueEstimate(PredictionVector predVec) {
		double utilityEstimate = 0;
		for(Action act : data.getActionSet())
		{
			utilityEstimate += (getValueEstimate(predVec, act));
		}
		return utilityEstimate;
	}
	
	/**
	 * Returns Q-Value estimate for state-action pair.
	 * 
	 * @param predVec State.
	 * @param act Action.
	 * @return Q-value estimate.
	 */
	@Override
	public double getValueEstimate(PredictionVector predVec, Action act) {
		ERTPoint point = createPointWithActions(predVec, act);
		forest.classify(point);		
		return eval.evaluate(forest.classify(point));
	}
	
	/**
	 * Creates point used by afest methods.
	 * Points created by this method do include actions as features.
	 * 
	 * @param predVec Prediction vector used to create point.
	 * @return ERTPoint used by afest methods. 
	 */
	private ERTPoint createPointWithActions(PredictionVector predVec, Action act)
	{
		HashMap<String, Double> features = new HashMap<String, Double>();
		for(int i = 0; i < predVec.getVector().getRows(); i++)
		{
			double[] stateFeatures = predVec.getVector().transpose().toArray();

			features.put(Integer.toString(i+1), stateFeatures[i]);
		}
		features.put("Action", (double)act.hashCode());
		return new ERTPoint(features);
	}	
}


