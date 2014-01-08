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

package afest.datastructures.tree.decision;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import afest.datastructures.tree.decision.interfaces.IPoint;


/**
 * Forest class to create an Ensemble classifier from Decision trees.
 * @param <R> Type used to identify features.
 * @param <C> Type of objects returned by the decision trees.
 */
public class Forest<R extends Serializable, C extends Serializable> implements Serializable
{

	private static final long serialVersionUID = 1L;
	
	private ArrayList<DecisionTree<R, C>> fTrees;
	
	/**
	 * Creates a new empty forest.
	 */
	public Forest()
	{
		fTrees = new ArrayList<DecisionTree<R, C>>();
	}
	
	/**
	 * Add the tree to the forest.
	 * @param tree tree to add to the forest.
	 */
	public void add(DecisionTree<R, C> tree)
	{
		fTrees.add(tree);
	}
	
	/**
	 * Add all the trees to the forest.
	 * @param trees trees to add to the forest.
	 */
	public void addAll(Collection<DecisionTree<R, C>> trees)
	{
		fTrees.addAll(trees);
	}
	
	/**
	 * Return the size of the forest (number of trees contained in it).
	 * @return the size of the forest (number of trees contained in it).
	 */
	public int size()
	{
		return fTrees.size();
	}
	
	/**
	 * Return the contents of the reached leaf in each decision trees.
	 * @param <T> Type of points extending IPoint<R>.
	 * @param element for which to return the content.
	 * @return the contents of the reached leaf in each decision trees.
	 */
	public <T extends IPoint<R>> ArrayList<C> classify(T element)
	{
		ArrayList<C> contents = new ArrayList<C>(fTrees.size());
		for (DecisionTree<R, C> tree : fTrees)
		{
			C content = tree.classify(element);
			contents.add(content);
		}
		return contents;
	}
	
}
