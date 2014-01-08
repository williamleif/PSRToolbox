/*
 *   Copyright 2011 Guillaume Saulnier-Comte
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

package afest.datastructures.tree.decision.erts.evaluator;

import java.util.ArrayList;

import org.apache.commons.math.stat.StatUtils;

import afest.datastructures.tree.decision.erts.evaluator.interfaces.IForestEvaluator;

/**
 * Return the mean of the regressions of the IPoint on the forest output.
 *
 * @param <C> Type of leaf content.
 */
public class ForestRegressionEvaluator<C extends Number> implements IForestEvaluator<C, Double>
{

	private static final long serialVersionUID = 1L;

	@Override
	public Double evaluate(ArrayList<C> leafsContent)
	{
		double[] evaluations = new double[leafsContent.size()];
		for (int i = 0; i < evaluations.length; i++)
		{
			double value = leafsContent.get(i).doubleValue();
			evaluations[i] = value;
		}
		double mean = StatUtils.mean(evaluations);
		return mean;
	}
	
	@Override
	public String toString()
	{
		return "Regression";
	}
}
