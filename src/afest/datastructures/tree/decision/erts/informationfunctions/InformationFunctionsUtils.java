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

package afest.datastructures.tree.decision.erts.informationfunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import afest.datastructures.tree.decision.interfaces.ISplit;
import afest.datastructures.tree.decision.interfaces.ITrainingPoint;


/**
 * Provides useful function for computing information functions.
 */
public final class InformationFunctionsUtils
{
	
	private InformationFunctionsUtils(){}
	
	/**
	 * Return the separated sets according to the split.
	 * @param <T> Type of ITrainingPoint used.
	 * @param <R> Type used to identify features.
 	* @param <C> Type of the content "classes" of the TrainingPoints.
	 * @param set set to split.
	 * @param split split used for the separation.
	 * @return the separated sets according to the split.
	 */
	public static <T extends ITrainingPoint<R, C>, 
				   R extends Serializable, 
				   C extends Serializable> 
						HashMap<Boolean, ArrayList<T>> performSplit(Collection<T> set, ISplit<R> split)
	{
		HashMap<Boolean, ArrayList<T>> separated = new HashMap<Boolean, ArrayList<T>>();
		separated.put(true, new ArrayList<T>());
		separated.put(false, new ArrayList<T>());
		for (T element : set)
		{
			Boolean splitResult = split.contains(element);
			ArrayList<T> elements = separated.get(splitResult);
			elements.add(element);
		}
		return separated;
	}

}
