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

package afest.datastructures.tree.decision.erts.evaluator.interfaces;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Return the evaluation of the element on the list of outputs received from a forest.
 *
 * @param <C> Type of output received from the forest.
 * @param <O> Type of output returned by the evaluator.
 */
public interface IForestEvaluator<C extends Serializable, O extends Serializable> extends Serializable
{

	/**
	 * Return the output once the element is evaluated over all leafs content.
	 * @param leafsContent forest output.
	 * @return the output once the element is evaluated over all leafs content.
	 */
	 O evaluate(ArrayList<C> leafsContent);
	
}
