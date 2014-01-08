package afest.datastructures.tree.decision.erts;

import java.util.ArrayList;
import java.util.HashMap;

import afest.datastructures.tree.decision.interfaces.IPoint;

/**
 * Creates a memory less point.  Uses a hashMap where each key is a feature and each value is an arraylist 
 * of feature values for the whole signal to return the values of the point at a given index.  Do not modify the HashMap
 * as it is only referenced.
 */
public class ERTMemoryLessPoint implements IPoint<String>
{

	private static final long serialVersionUID = 1L;
	
	private int fIndex;
	private HashMap<String, ArrayList<Double>> fSignalFeatures;
	
	/**
	 * Creates a new "memory less" point only storing the index of the point and a HashMap of all the features over the whole the signal.
	 * When querying the point the value of the given feature at the given index is returned.
	 * @param index index of the point to get values from.
	 * @param signalFeatures Map containing all features over the whole signal.
	 */
	public ERTMemoryLessPoint(int index, HashMap<String, ArrayList<Double>> signalFeatures)
	{
		fIndex = index;
		fSignalFeatures = signalFeatures;
	}
	
	@Override
	public double getValue(String attribute)
	{
		ArrayList<Double> feature = fSignalFeatures.get(attribute);
		double value = feature.get(fIndex);
		return value;
	}

	@Override
	public String[] getAttributes()
	{
		String[] names = new String[fSignalFeatures.size()];
		names = fSignalFeatures.keySet().toArray(names);
		return names;
	}

	@Override
	public int size()
	{
		return fSignalFeatures.size();
	}

	/**
	 * Return the index of the point.
	 * @return the index of the point.
	 */
	public int getIndex()
	{
		return fIndex;
	}
}
