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

package afest.datastructures.tree.decision.interfaces;

import java.io.Serializable;

/**
 * Interface implementing a point used by the Extremely Randomized Trees.
 * @param <R> Type used to identify features.
 * @param <T> Type object attached to the point.
 */
public interface ITrainingPoint<R extends Serializable, T extends Serializable> extends IPoint<R>
{
	/**
	 * Return the label of the Point.
	 * @return the label of the Point.
	 */
	T getContent();
	
}
