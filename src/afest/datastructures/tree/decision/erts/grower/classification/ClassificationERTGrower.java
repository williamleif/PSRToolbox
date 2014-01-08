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

package afest.datastructures.tree.decision.erts.grower.classification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import afest.datastructures.tree.decision.DTNode;
import afest.datastructures.tree.decision.erts.grower.AERTGrower;
import afest.datastructures.tree.decision.erts.informationfunctions.interfaces.IERTScore;
import afest.datastructures.tree.decision.interfaces.ITrainingPoint;
import afest.math.MyMath;



/**
 * Class used to grow Extra Trees.
 * @param <R> Type of attributes contained in the Extra Trees.
 * @param <C> Type of content present in the leafs of the Extra Trees.
 */
public class ClassificationERTGrower<R extends Serializable, C extends Serializable> extends AERTGrower<R, C, C>
{

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a ERTGrower that will create ExtraTrees according to the following parameters.
	 * @param k number of splits to create at each inner node.
	 * @param nMin a leaf is created if |set| <= n_min.
	 * @param score score object used to calculate split scores.
	 */
	public ClassificationERTGrower(Integer k, int nMin, IERTScore<R, C> score)
	{
		super(k, nMin, score);
	}
	
	@Override
	protected <T extends ITrainingPoint<R, C>> DTNode<R, C> createLeaf(Collection<T> set)
	{
		ArrayList<C> contents = new ArrayList<C>(set.size());
		for (T aT : set)
		{
			contents.add(aT.getContent());
		}
		C majority = MyMath.majority(contents);
		
		DTNode<R, C> leaf = new DTNode<R, C>();
		leaf.setContent(majority);
		
		return leaf;
	}

}
