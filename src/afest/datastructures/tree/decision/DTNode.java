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

import afest.datastructures.tree.BinaryTreeNode;
import afest.datastructures.tree.decision.exceptions.DecisionTreeException;
import afest.datastructures.tree.decision.interfaces.IPoint;
import afest.datastructures.tree.decision.interfaces.ISplit;

/**
 * Class to represent the data contained in a decision tree node. 
 * @param <R> Type used to identify features in points.
 * @param <C> Type of object contained in the Node.
 */
public class DTNode<R extends Serializable, C extends Serializable> extends BinaryTreeNode<DTNode<R, C>> implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private ISplit<R> fSplit;
	private C fContent;
	
	/**
	 * Construct a node with the following properties.
	 */
	public DTNode()
	{
		fSplit = null;
		fContent = null;
	}
	
	/**
	 * Return true if the element is contained in the split, false otherwise.
	 * @param <T> Type of points extending IPoint<R>.
	 * @param element element to verify membership.
	 * @return true if the element is contained in the split, false otherwise.
	 */
	public <T extends IPoint<R>> boolean split(T element)
	{
		if (fSplit == null)
		{
			throw new DecisionTreeException("No split contained in this node!");
		}
		return fSplit.contains(element);
	}
	
	/**
	 * Set the split of this node to the given split.
	 * @param <T> Type of split for IPoint<R>.
	 * @param split split used to separate elements in the node.
	 */
	public <T extends ISplit<R>> void setSplit(T split)
	{
		fSplit = split;
	}
	
	/**
	 * Return the content of the leaf.
	 * @return the content of the leaf.
	 */
	public C getContent()
	{
		if (fContent == null)
		{
			throw new DecisionTreeException("No label contained in this node!");
		}
		return fContent;
	}
	
	/**
	 * Set the label of the node to the given label.
	 * @param content content to be returned by this node. 
	 */
	public void setContent(C content)
	{
		fContent = content;
	}
	
}
