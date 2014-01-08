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

import org.apache.commons.math.util.MathUtils;

import afest.datastructures.tree.decision.erts.informationfunctions.interfaces.IERTScore;
import afest.datastructures.tree.decision.interfaces.ISplit;
import afest.datastructures.tree.decision.interfaces.ITrainingPoint;


/**
 * Computes the Normalized Shannon Entropy Information Gain for any number of classes. 
 * @param <R> Type used to identify features.
 * @param <C> Type of the content "classes" of the TrainingPoints.
 */
public class GeneralizedNormalizedShannonEntropy<R extends Serializable, 
												 C extends Serializable> 
												 implements IERTScore<R, C>
{

	private static final long serialVersionUID = 1L;

	@Override
	public <T extends ITrainingPoint<R, C>> double getScore(Collection<T> set, ISplit<R> split)
	{
		HashMap<Boolean, ArrayList<T>> splitSeparation = InformationFunctionsUtils.performSplit(set, split);
		
		HashMap<Boolean, Integer> countSeparation = new HashMap<Boolean, Integer>();
		for (Boolean key : splitSeparation.keySet())
		{
			ArrayList<T> elements = splitSeparation.get(key);
			countSeparation.put(key, elements.size());
		}
		
		HashMap<C, Integer> countContent = groupElementsByContent(set);
		HashMap<C, Integer> countContentTrue = groupElementsByContent(splitSeparation.get(true));
		HashMap<C, Integer> countContentFalse = groupElementsByContent(splitSeparation.get(false));
		
		double ht = getEntropy(countSeparation, set.size());
		double hc = getEntropy(countContent, set.size());
		
		double dSize = (double) set.size();
		double pTrue = countSeparation.get(true) / dSize;
		double hct = 0;
		for (Integer count : countContentTrue.values())
		{
			double prob1 = count / dSize;
			double prob2 = prob1 / pTrue;
			hct -= prob1 * MathUtils.log(2, prob2);
		}
		for (Integer count : countContentFalse.values())
		{
			double prob1 = count / dSize;
			double prob2 = 1- (prob1 / pTrue); // pFalse
			hct -= prob1 * MathUtils.log(2, prob2);
		}
		
		// Mutual Information
		double itc = hc - hct;
		
		// Normalization
		double ctc = 2 * itc / (hc + ht);
		
		return ctc;
	}
	
	/**
	 * Return the elements in the set grouped by their content.
	 * @param <T> Type of ITrainingPoint to split.
	 * @param set set to group the elements from.
	 * @return the elements in the set grouped by their content.
	 */
	protected <T extends ITrainingPoint<R, C>> HashMap<C, Integer> groupElementsByContent(Collection<T> set)
	{
		// Group the elements by content
		HashMap<C, Integer> contents = new HashMap<C, Integer>();
		for (T element : set)
		{
			C content = element.getContent();
			Integer count = contents.get(content);
			if (count == null)
			{
				contents.put(content, 0);
				count = contents.get(content);
			}
			contents.put(content, count+1);
		}
		return contents;
	}
	
	/**
	 * Return the entropy of a given set of classes.
	 * @param counts number of elements in each class.
	 * @param size total number of elements. (denominator of the probability)
	 * @return the entropy of a given set of classes.
	 */
	protected double getEntropy(HashMap<?, Integer> counts, int size)
	{
		double dSize = (double) size;
		double h = 0;
		for (Integer count : counts.values())
		{
			double prob = count/dSize;
			h -= prob * MathUtils.log(2, prob);
		}
		return h;
	}

	@Override
	public String toString()
	{
		return "GNSE";
	}
	
}
