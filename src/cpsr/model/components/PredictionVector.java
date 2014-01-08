/*
 *   Copyright 2012 William Hamilton
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

package cpsr.model.components;

import java.io.Serializable;

import org.jblas.DoubleMatrix;

import cpsr.model.exceptions.PVException;

/**
 * Simple class used to encapsulate prediction vector 
 * (i.e. the state vector of PSR)
 *  
 * @author William Hamilton
 */
@SuppressWarnings("serial")
public class PredictionVector implements Serializable
{
	/**
	 * @serialField
	 */
	private DoubleMatrix vector;
	
	/**
	 * Constructs a prediction vector of specified size with all
	 * values initialized to zero.
	 * 
	 * @param size Size (length) of prediction vector.
	 */
	public PredictionVector(int size)
	{
		vector = new DoubleMatrix(size, 1);
	}
	
	/**
	 * Constructs a prediction vector from a matrix (vector) passed as argument.
	 * This matrix passed must be vector and have second dimension equal to
	 * 1 or exception thrown.
	 * 
	 * @param vector
	 */
	public PredictionVector(DoubleMatrix vector)
	{
		if(vector.getColumns() != 1 )
		{
			throw new PVException("Constructor requires Matrix with column dimension equal to one (i.e. a vector)");
		}
		this.vector = vector;
	}
	
	/**
	 * Returns an entry of prediction vector.
	 * 
	 * @param index Index of entry to be returned
	 * @return The (double) entry specified by index.
	 */
	public double getEntry(int index)
	{
		return vector.get(index, 0);
	}
	
	/**
	 * Returns the size (length) of prediction vector.
	 * 
	 * @return The size (length) of prediction vector.
	 */
	public int getSize()
	{
		return vector.getRows();
	}
	
	/**
	 * Returns Jama matrix representation of prediction vector
	 * 
	 * @return Jama matrix representation of prediction vector
	 */
	public DoubleMatrix getVector()
	{
		return vector;
	}
}
