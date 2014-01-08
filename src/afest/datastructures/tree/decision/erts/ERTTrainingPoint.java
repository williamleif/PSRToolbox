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
import java.util.ArrayList;
import java.util.HashMap;

import afest.datastructures.tree.decision.interfaces.ITrainingPoint;


/**
 * Class representing an ERTPoint using strings as feature names.
 * @param <O> Type of data contained in the ERTPoint.
 */
public class ERTTrainingPoint<O extends Serializable> extends ERTPoint implements ITrainingPoint<String, O>
{

	private static final long serialVersionUID = 1L;
	
	private O fContent;
	
	/**
	 * Create a ERTPoint with the corresponding values and feature names.
	 * @param values values of the features.
	 * @param content content of the data point.
	 */
	public ERTTrainingPoint(HashMap<String, Double> values, O content)
	{	
		super(values);
		fContent = content;
	}
	
	@Override
	public O getContent()
	{
		return fContent;
	}

	/**
	 * Return an ArrayList of ERTPoints created using the given HashMap of features and labels (point are in the same order as the MultiSignal).
	 * @param <O> Type of contents to be placed in the ERTPoints.
	 * @param features HashMap containing the features to create the ERTPoints with.
	 * @param contents contents used for the ERTPoints (must be in the same order as the feature values).
	 * @return an ArrayList of ERTPoints created using the given MultiSignal and labels.
	 */
	public static <O extends Serializable> ArrayList<ERTTrainingPoint<O>> getERTPoints(HashMap<String, ArrayList<Double>> features, 
																					   ArrayList<O> contents)
	{
		ArrayList<ERTTrainingPoint<O>> points = new ArrayList<ERTTrainingPoint<O>>();
		for (int i = 0; i < contents.size(); i++)
		{
			HashMap<String, Double> values = new HashMap<String, Double>(contents.size());
			for (String key : features.keySet())
			{
				values.put(key, features.get(key).get(i));
			}
			ERTTrainingPoint<O> point = new ERTTrainingPoint<O>(values, contents.get(i));
			points.add(point);
		}
		return points;
	}
	
}
