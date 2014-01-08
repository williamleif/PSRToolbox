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
 */
public interface IPoint<R extends Serializable> extends Serializable
{
	
	/**
	 * Return the value of the given feature.
	 * @param attribute the attribute for which to return the value for.
	 * @return the values of the given feature.
	 */
	double getValue(R attribute);
	
	/**
	 * Return the attributes contained in the Point.
	 * @return the attributes contained in the Point.
	 */
	R[] getAttributes();
	
	/**
	 * Return the number of features contained in the Point.
	 * @return the number of features contained in the Point.
	 */
	int size();
}
