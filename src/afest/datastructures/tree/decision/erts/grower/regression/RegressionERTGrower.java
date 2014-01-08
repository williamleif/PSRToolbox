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

package afest.datastructures.tree.decision.erts.grower.regression;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.math.stat.StatUtils;

import afest.datastructures.tree.decision.DTNode;
import afest.datastructures.tree.decision.erts.grower.AERTGrower;
import afest.datastructures.tree.decision.erts.informationfunctions.interfaces.IERTScore;
import afest.datastructures.tree.decision.interfaces.ITrainingPoint;



/**
 * Class used to grow Extra Trees.
 * @param <R> Type of attributes contained in the Extra Trees.
 * @param <N> Type of number to do the regression on.
 */
public class RegressionERTGrower<R extends Serializable, 
								   N extends Number> 
								   extends AERTGrower<R, N, Double>
{

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a ERTGrower that will create ExtraTrees according to the following parameters.
	 * Takes the mean of the labels.
	 * @param k number of splits to create at each inner node.
	 * @param nMin a leaf is created if |set| <= n_min.
	 * @param score score object used to calculate split scores.
	 */
	public RegressionERTGrower(Integer k, int nMin, IERTScore<R, N> score)
	{
		super(k, nMin, score);
	}
	
	@Override
	protected <T extends ITrainingPoint<R, N>> DTNode<R, Double> createLeaf(Collection<T> set)
	{

		double[] values = new double[set.size()];
		int i = 0;
		for (T point : set)
		{
			values[i] = point.getContent().doubleValue();
			i++;
		}
		double mean = StatUtils.mean(values);
		
		// Create the leaf
		DTNode<R, Double> leaf = new DTNode<R, Double>();
		leaf.setContent(mean);
		
		return leaf;
	}

}
