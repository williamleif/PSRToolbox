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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import afest.datastructures.tree.decision.erts.evaluator.interfaces.IForestEvaluator;

/**
 * Return the percentage of trees for that labeled the element in any of the given classes.
 * @param <C> Type of leaf content must implement equals and hashcode.
 */
public class ForestFractionEvaluator<C extends Serializable> implements IForestEvaluator<C, Double>
{

	private static final long serialVersionUID = 1L;

	private ArrayList<C> fClasses;
	
	/**
	 * Create a new Forest Fraction Evaluator that will return the fraction of trees 
	 * that classified the element in any of the given classes.
	 * @param classes will add a count of 1 if the element is in any of those classes.
	 */
	public ForestFractionEvaluator(Collection<C> classes)
	{
		fClasses = new ArrayList<C>(classes);
	}
	
	
	@Override
	public Double evaluate(ArrayList<C> leafsContent)
	{
		HashMap<C, Integer> counts = getCounts(leafsContent);
		Double value = 0.0;
		for (C clazz : fClasses)
		{
			Integer count = counts.get(clazz);
			if (count != null)
			{
				value += count;
			}
		}

		return value/leafsContent.size();
	}
	
	/**
	 * Return the counts for each of the classes from the leaf content.
	 * @param leafsContent the content of the leafs (when the forest is asked to classify an element)
	 * @return the counts for each of the classes from the leaf content.
	 */
	private HashMap<C, Integer> getCounts(ArrayList<C> leafsContent)
	{
		HashMap<C, Integer> counts = new HashMap<C, Integer>();
		
		// Compute the count
		for (C content : leafsContent)
		{
			Integer value = counts.get(content);
			if (value == null)
			{
				value = 0; 
			}
			value = value + 1;
			counts.put(content, value);
		}
		
		return counts;
	}
	
	@Override
	public String toString()
	{
		String str = fClasses.toString();
		str = str.substring(1, str.length()-1);
		str = str.replaceAll(" ", "");
		return "Fraction("+str+")";
	}
}
