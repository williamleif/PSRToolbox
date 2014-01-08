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

package afest.datastructures.tree.decision.erts.informationfunctions.interfaces;

import java.io.Serializable;
import java.util.Collection;

import afest.datastructures.tree.decision.interfaces.ISplit;
import afest.datastructures.tree.decision.interfaces.ITrainingPoint;



/**
 * Interface for objects used to calculate the score of a split.
 * @param <R> Type used to identify features.
 * @param <C> Type of content present in the ITrainingPoint<R,C>.
 */
public interface IERTScore<R extends Serializable, C extends Serializable> extends Serializable
{
	/**
	 * Return the score of the split on the given set.
	 * @param <T> Type of ITrainingPoint to split.
	 * @param set set of elements to split.
	 * @param split split to separate the elements of the set.
	 * @return the score of the split on the given set.
	 */
	<T extends ITrainingPoint<R, C>> double getScore(Collection<T> set, ISplit<R> split);
}
