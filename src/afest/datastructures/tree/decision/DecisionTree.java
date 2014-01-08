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

import afest.datastructures.tree.decision.interfaces.IPoint;



/**
 * Class implementing a decision tree.
 * @param <R> Type used to identify features in points.
 * @param <C> Type of objects contained in the leafs.
 */
public class DecisionTree<R extends Serializable, C extends Serializable> implements Serializable
{

	private static final long serialVersionUID = 1L;
	
	private DTNode<R, C> fRoot;
	
	/**
	 * Create a Decision tree with the given root node.
	 * @param root root of the decision tree.
	 */
	public DecisionTree(DTNode<R, C> root)
	{
		fRoot = root;
	}
	
	/**
	 * Return the content of the leaf reached by the element when descending the decision tree.
	 * Convention is that if the split returns true, the right child is visited.
	 * @param <T> Type of points extending IPoint<R>.
	 * @param element element to get the label for.
	 * @return the label of the leaf reached by the element when descending the decision tree.
	 */
	public <T extends IPoint<R>> C classify(T element)
	{
		DTNode<R, C> leaf = getLeaf(element);
		return leaf.getContent();
	}
	
	/**
	 * Return the leaf node reached when descending the tree according to the given element.
	 * Convention is that if the split returns true, the right child is visited.
	 * @param <T> Type of points extending IPoint<R>.
	 * @param element element to reach the leaf node for.
	 * @return the leaf node reached when descending the tree according to the given element.
	 */
	public <T extends IPoint<R>> DTNode<R, C> getLeaf(T element)
	{
		DTNode<R, C> currentNode = fRoot;
		while (!currentNode.isLeaf())
		{
			if (currentNode.split(element))
			{
				currentNode = currentNode.getRightChild();
			}
			else
			{
				currentNode = currentNode.getLeftChild();
			}
		}
		return currentNode;
	}
}
