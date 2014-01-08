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

import java.util.ArrayList;
import java.util.HashMap;

import afest.datastructures.tree.decision.erts.exceptions.ERTException;
import afest.datastructures.tree.decision.interfaces.IPoint;



/**
 * Class representing an ERTPoint using strings as feature names.
 */
public class ERTPoint implements IPoint<String>
{

	private static final long serialVersionUID = 1L;
	
	private HashMap<String, Float> fValues;
	
	/**
	 * Create a ERTPoint with the corresponding values and feature names.
	 * @param values values of the features.
	 */
	public ERTPoint(HashMap<String, Double> values)
	{	
		fValues = new HashMap<String, Float>(values.size());
		for (String key : values.keySet())
		{	
			fValues.put(key, values.get(key).floatValue());
		}
	}
	
	@Override
	public String[] getAttributes()
	{
		String[] names = new String[fValues.size()];
		names = fValues.keySet().toArray(names);
		return names;
	}

	@Override
	public double getValue(String attribute)
	{
		Float value = fValues.get(attribute);
		if (value == null)
		{
			throw new ERTException("Attribute name not present in point! name: "+attribute);
		}
		return value;
	}

	@Override
	public int size()
	{
		return fValues.size();
	}

	/**
	 * Return an ArrayList of ERTPoints created using the given HashMap of features.
	 * @param features HashMap containing the features to create the ERTPoints with.
	 * @return an ArrayList of ERTPoints created using the given features. 
	 */
	public static ArrayList<ERTPoint> getERTPoints(HashMap<String, ArrayList<Double>> features)
	{
		ArrayList<ERTPoint> points = new ArrayList<ERTPoint>();
		
		// ugly !!!
		int size = features.values().iterator().next().size();
		
		for (int i = 0; i < size; i++)
		{
			HashMap<String, Double> values = new HashMap<String, Double>(size);
			for (String key : features.keySet())
			{
				values.put(key, features.get(key).get(i));
			}
			ERTPoint point = new ERTPoint(values);
			points.add(point);
		}
		return points;
	}
	
}
