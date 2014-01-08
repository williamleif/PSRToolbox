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

package afest.datastructures.tree;

import java.io.Serializable;

import afest.datastructures.tree.interfaces.IBinaryTreeNode;


/**
 * Node object for binary trees. 
 * @param <N> Type of BinaryTreeNode.
 */
public class BinaryTreeNode<N extends BinaryTreeNode<N>> implements Serializable, IBinaryTreeNode<N>
{

	private static final long serialVersionUID = 1L;
	
	private N fLeftChild;
	private N fParent;
	private N fRightChild;
	
	/**
	 * Creates a binary node (max 2 children).
	 */
	public BinaryTreeNode()
	{
		fParent = null;
		fLeftChild = null;
		fRightChild = null;
	}
	
	@Override
	public N getLeftChild()
	{
		return fLeftChild;
	}
	
	@Override
	public N getParent()
	{
		return fParent;
	}
	
	@Override
	public N getRightChild()
	{
		return fRightChild;
	}
	
	@Override
	public boolean isLeaf()
	{
		return fLeftChild == null && fRightChild == null;
	}
	
	@Override
	public void setLeftChild(N leftChild)
	{
		fLeftChild = leftChild;
	}
	
	@Override
	public void setParent(N parent)
	{
		fParent = parent;
	}
	
	@Override
	public void setRightChild(N rightChild)
	{
		fRightChild = rightChild;
	}
	
	/**
	 * Return a string representation of the node.
	 * @return a string representation of the node.
	 */
	public String toString()
	{
		String str = "[";
		str += fParent.hashCode()+",";
		str += fLeftChild.hashCode()+",";
		str += fRightChild.hashCode()+",";
		str += "]";
		return str;
	}
	
}
