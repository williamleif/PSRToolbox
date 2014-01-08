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

package afest.datastructures.tree.interfaces;

/**
 * Interface for a BinaryTreeNode.
 *
 * @param <N> Type of BinaryTreeNode implementing the Interface.
 */
public interface IBinaryTreeNode<N extends IBinaryTreeNode<N>>
{

	/**
	 * Return the left child of the node.
	 * @return the left child of the node.
	 */
	N getLeftChild();

	/**
	 * Return the parent of the node.
	 * @return the parent of the node.
	 */
	N getParent();

	/**
	 * Return the right child of the node.
	 * @return the right child of the node.
	 */
	N getRightChild();

	/**
	 * Return true if the node is a leaf (i.e. both children are null), false otherwise.
	 * @return true if the node is a leaf (i.e. both children are null), false otherwise.
	 */
	boolean isLeaf();

	/**
	 * Set the left child of the node. (Only a reference is stored)
	 * @param leftChild left child of the node.
	 */
	void setLeftChild(N leftChild);

	/**
	 * Set the parent of the node. (Only a reference is stored)
	 * @param parent parent of the node.
	 */
	void setParent(N parent);

	/**
	 * Set the right child of the node. (Only a reference is stored)
	 * @param rightChild right child of the node.
	 */
	void setRightChild(N rightChild);

}
