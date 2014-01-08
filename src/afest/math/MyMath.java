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

package afest.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math.complex.Complex;

import afest.math.exceptions.MathException;

/**
 * Provide useful functions not found in the Apache package...
 */
public final class MyMath
{
	
	private MyMath(){}
	
	/**
	 * Convert the specified list to complex numbers.
	 * @param values values to convert.
	 * @return the specified list represented as complex numbers.
	 * */
	public static Complex[] convertToComplex(double[] values)
	{
		Complex[] result = new Complex[values.length];
		for (int i = 0; i < values.length; i++)
		{
			result[i] = new Complex(values[i], 0);
		}
		return result;
	}
	
	/**
	 * Return the sum of u[i] * conjugate(v[i]) for all i. Throws an exception if the length of u and v differs.
	 * @param u array.
	 * @param v array.
	 * @return the sum of u[i] * conjugate(v[i]) for all i. Throws an exception if the length of u and v differs.
	 */
	public static double dot(Complex[] u, Complex[] v)
	{
		if (u.length != v.length)
		{
			throw new MathException("The length of u and v differs! u:"+u.length+", v:"+v.length);
		}
		double result = 0;
		for (int i = 0; i < u.length; i++)
		{
			result += u[i].multiply(v[i].conjugate()).getReal();
		}
		return result;
	}
	
	/**
	 * Return the sum of u[i] * v[i] for all i. Throws an exception if the length of u and v differs.
	 * @param u array.
	 * @param v array.
	 * @return the sum of u[i] * v[i] for all i. Throws an exception if the length of u and v differs.
	 */
	public static double dot(double[] u, double[] v)
	{
		if (u.length != v.length)
		{
			throw new MathException("The length of u and v differs! u:"+u.length+", v:"+v.length);
		}
		double result = 0;
		for (int i = 0; i < u.length; i++)
		{
			result += u[i] * v[i];
		}
		return result;
	}
	
	/**
	 * Return w[i] = u[i] + v[i]. Throws an exception if the length of u and v differs.
	 * @param u array.
	 * @param v array.
	 * @return the sum u[i] + v[i]. Throws an exception if the length of u and v differs.
	 */
	public static double[] ebeAdd(double[] u, double[] v)
	{
		if (u.length != v.length)
		{
			throw new MathException("The length of u and v differs! u:"+u.length+", v:"+v.length);
		}
		double[] result = new double[u.length];
		for (int i = 0; i < u.length; i++)
		{
			result[i] = u[i] + v[i];
		}
		return result;
	}
	
	/**
	 * Return w[i] = u[i] * v[i]. Throws an exception if the length of u and v differs.
	 * @param u array.
	 * @param v array.
	 * @return the sum u[i] * v[i]. Throws an exception if the length of u and v differs.
	 */
	public static Complex[] ebeMultiply(Complex[] u, Complex[] v)
	{
		if (u.length != v.length)
		{
			throw new MathException("The length of u and v differs! u:"+u.length+", v:"+v.length);
		}
		Complex[] result = new Complex[u.length];
		for (int i = 0; i < u.length; i++)
		{
			result[i] = u[i].multiply(v[i]);
		}
		return result;
	}
	
	/**
	 * Return w[i] = u[i] * v[i]. Throws an exception if the length of u and v differs.
	 * @param u array.
	 * @param v array.
	 * @return the sum u[i] * v[i]. Throws an exception if the length of u and v differs.
	 */
	public static double[] ebeMultiply(double[] u, double[] v)
	{
		if (u.length != v.length)
		{
			throw new MathException("The length of u and v differs! u:"+u.length+", v:"+v.length);
		}
		double[] result = new double[u.length];
		for (int i = 0; i < u.length; i++)
		{
			result[i] = u[i] * v[i];
		}
		return result;
	}
	
	/**
	 * Return w[i] = u[i] - v[i]. Throws an exception if the length of u and v differs.
	 * @param u array.
	 * @param v array.
	 * @return the sum u[i] - v[i]. Throws an exception if the length of u and v differs.
	 */
	public static double[] ebeSubtract(double[] u, double[] v)
	{
		if (u.length != v.length)
		{
			throw new MathException("The length of u and v differs! u:"+u.length+", v:"+v.length);
		}
		double[] result = new double[u.length];
		for (int i = 0; i < u.length; i++)
		{
			result[i] = u[i] - v[i];
		}
		return result;
	}
	
	/**
	 * Return an array such that each entry i has a[i] = true iff values[i] >= threshold, a[i] = false otherwise.
	 * @param threshold the threshold value.
	 * @param values values to be thresholded.
	 * @return an array such that each entry i has a[i] = true iff values[i] >= threshold, a[i] = false otherwise.
	 */
	public static boolean[] threshold(double threshold, double[] values)
	{
		boolean[] thresholdedValues = new boolean[values.length];
		for (int i = 0; i < values.length; i++)
		{
			thresholdedValues[i] = values[i] >= threshold;
		}
		return thresholdedValues;
	}
	
	/**
	 * Return a list such that each entry i has a[i] = true iff values[i] >= threshold, a[i] = false otherwise.
	 * @param threshold the threshold value.
	 * @param values values to be thresholded.
	 * @return a list such that each entry i has a[i] = true iff values[i] >= threshold, a[i] = false otherwise.
	 */
	public static ArrayList<Boolean> threshold(double threshold, List<Double> values)
	{
		ArrayList<Boolean> thresholdedValues = new ArrayList<Boolean>(values.size());
		for (Double value : values)
		{
			thresholdedValues.add(value >= threshold);
		}
		return thresholdedValues;
	}
	
	/**
	 * Return true if more than majority of the values are equal to true, false otherwise.
	 * @param majority percentage of values that must be equal to true.
	 * @param values array of values equal to true or false.
	 * @return true if more than majority of the values are equal to true, false otherwise.
	 */
	public static boolean majority(double majority, boolean[] values)
	{
		int trueCount = 0;
		for (boolean value : values)
		{
			if (value)
			{
				trueCount++;
			}
		}
		
		double average = trueCount / (double) values.length;
		return average >= majority;
	}
	
	/**
	 * Return true if more than majority of the values are equal to true, false otherwise.
	 * @param majority percentage of values that must be equal to true.
	 * @param values array of values equal to true or false.
	 * @return true if more than majority of the values are equal to true, false otherwise.
	 */
	public static boolean majority(double majority, List<Boolean> values)
	{
		int trueCount = 0;
		for (boolean value : values)
		{
			if (value)
			{
				trueCount++;
			}
		}
		
		double average = trueCount / (double) values.size();
		return average >= majority;
	}
	
	/**
	 * Return the object that has majority (# of occurrences) in the collection. T must implement equals.  
	 * @param <T> Type of object to get the majority.
	 * @param collection collection to extract the object that is majoritary from.
	 * @return the object that occurs the most often in the collection.
	 */
	public static <T> T majority(Collection<T> collection)
	{
		HashMap<T, Integer> counts = new HashMap<T, Integer>();
		for (T aT : collection)
		{
			Integer count = counts.get(aT);
			if (count == null)
			{
				counts.put(aT, 0);
				count = counts.get(aT);
			}
			counts.put(aT, count+1);
		}
		T majority = null;
		int maxCount = -1;
		for (T aT : counts.keySet())
		{
			if (majority == null)
			{
				majority = aT;
				maxCount = counts.get(aT);
			}
			
			int count = counts.get(aT);
			if (maxCount < count)
			{
				majority = aT;
				maxCount = count;
			}
		}
		return majority;
	}
	
	/**
	 * Return the maximum element a such that for all b in the collection a > b according to the comparator.
	 * @param <T> Type of elements present in the list.
	 * @param collection collection of elements to get the largest from.
	 * @param comparator comparator used to order the elements in the collection.
	 * @return the maximum element a such that for all b in the collection a > b according to the comparator.
	 */
	public static <T> T max(Collection<T> collection, Comparator<T> comparator)
	{
		T max = null;
		for (T element : collection)
		{
			if (max == null)
			{
				max = element;
			}
			if (comparator.compare(max, element) < 0)
			{
				max = element;
			}
		}
		return max;
	}
	
	/**
	 * Return the largest element present in the collection.
	 * @param <T> Type of elements present in the collection.
	 * @param collection collection of elements to get the largest from.
	 * @return the largest element present in the collection.
	 */
	public static <T extends Comparable<T>> T max(Collection<T> collection)
	{
		return max(collection, new Comparator<T>()
		{
			@Override
			public int compare(T arg0, T arg1)
			{
				return arg0.compareTo(arg1);
			}
		});
	}
	
	/**
	 * Return the smallest element present in the collection.
	 * @param <T> Type of elements present in the collection.
	 * @param collection collection of elements to get the smallest from.
	 * @return the smallest element present in the collection.
	 */
	public static <T extends Comparable<T>> T min(Collection<T> collection)
	{
		return max(collection, new Comparator<T>()
		{
			@Override
			public int compare(T arg0, T arg1)
			{
				return -arg0.compareTo(arg1);
			}
		});
	}
	
}
