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

package afest.datastructures.tree.decision.erts;

import java.io.Serializable;

import afest.datastructures.tree.decision.interfaces.IPoint;
import afest.datastructures.tree.decision.interfaces.ISplit;



/**
 * Split class used by the Extremely Randomized Trees.
 * @param <R> Type used to identify features contained in the IPoints.
 */
public class ERTSplit<R extends Serializable> implements ISplit<R>
{

	private static final long serialVersionUID = 1L;
	
	private R fAttribute;
	private double fThresold;
	
	/**
	 * Create a split that will return true if the attribute of the element is above the threshold.
	 * @param attribute attribute that the split will be comparing.
	 * @param threshold threshold to compare on the given attribute.
	 */
	public ERTSplit(R attribute, double threshold)
	{
		fAttribute = attribute;
		fThresold = threshold;
	}

	@Override
	public <T extends IPoint<R>> boolean contains(T element)
	{
		return element.getValue(fAttribute) > fThresold;
	}
	
	@Override
	public String toString()
	{
		String str = "split("+fAttribute.toString()+", "+fThresold+")";
		return str;
	}
}
