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

package afest.datastructures.tree.decision.erts.evaluator;

import java.io.Serializable;
import java.util.ArrayList;

import afest.datastructures.tree.decision.erts.evaluator.interfaces.IForestEvaluator;
import afest.math.MyMath;

/**
 * Return the leaf content who occurs the most often in the forest output.
 * @param <C> Type of leaf content.
 */
public class ForestMajorityEvaluator<C extends Serializable> implements IForestEvaluator<C, C>
{

	private static final long serialVersionUID = 1L;

	@Override
	public C evaluate(ArrayList<C> leafsContent)
	{
		C majorityElement = MyMath.majority(leafsContent);
		return majorityElement;
	}

	@Override
	public String toString()
	{
		return "Majority";
	}
	
}
