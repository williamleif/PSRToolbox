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

package afest.datastructures.tree.decision.erts.informationfunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.math.stat.StatUtils;

import afest.datastructures.tree.decision.erts.informationfunctions.interfaces.IERTScore;
import afest.datastructures.tree.decision.interfaces.ISplit;
import afest.datastructures.tree.decision.interfaces.ITrainingPoint;


/**
 * Computes the relative variance reduction.  Useful for regression trees.
 * @param <R> Type used to identify features.
 * @param <C> Type of the content "classes" of the TrainingPoints.
 */
public class RelativeVarianceReduction<R extends Serializable, 
									   C extends Number> 
									   implements IERTScore<R, C>
{

	private static final long serialVersionUID = 1L;

	@Override
	public <T extends ITrainingPoint<R, C>> double getScore(Collection<T> set, ISplit<R> split)
	{
		HashMap<Boolean, ArrayList<T>> splitSeparation = InformationFunctionsUtils.performSplit(set, split);
		
		double varS = extractVariance(set);
		double varSTrue = extractVariance(splitSeparation.get(true));
		double varSFalse = extractVariance(splitSeparation.get(false));
		
		double trueSize = splitSeparation.get(true).size();
		double falseSize = splitSeparation.get(false).size();
		double setSize = set.size();
		
		return (varS - trueSize/setSize * varSTrue - falseSize/setSize * varSFalse) / varS;
	}

	/**
	 * Return the variance of the content of the points in the set.
	 * @param <T> Type of ITrainingPoint to split.
	 * @param set set to compute the variance from.
	 * @return the variance of the content of the points in the set.
	 */
	protected <T extends ITrainingPoint<R, C>> double extractVariance(Collection<T> set)
	{		
		double[] values = new double[set.size()];
		int i = 0;
		for (T element : set)
		{
			C number = element.getContent();
			values[i] = number.doubleValue();
			i++;
		}
		return StatUtils.variance(values);
	}
	
	@Override
	public String toString()
	{
		return "RVR";
	}
	
}
