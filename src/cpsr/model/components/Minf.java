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

import cpsr.model.exceptions.PSRParameterException;

/**
 * Class encapsulates Minf parameter vector.
 * Encapsulation restricts functionality. 
 * 
 * @author William Hamilton
 *
 */
@SuppressWarnings("serial")
public class Minf implements Serializable
{
	/**
	 * @serialField
	 */
	private DoubleMatrix vector;
	
	/**
	 * Creates Minf vector of specified length intialized to all zeroes.
	 * 
	 * @param size The length/size of the vector.
	 */
	public Minf(int size)
	{
		vector = new DoubleMatrix(size, 1);
	}
	
	/**
	 * Creates Minf vector from another Minf vector through
	 * a deep copy. 
	 * 
	 * @param minf The Minf vector to be copied.
	 */
	public Minf(Minf minf)
	{
		this.vector = minf.vector.dup();
	}
	
	/**
	 * Creates Minf vector from matrix with column dimension 1. 
	 * 
	 * @param vector The matrix(vector) used to create Minf vector.
	 */
	public Minf(DoubleMatrix vector) throws PSRParameterException
	{
		if(vector.getColumns() != 1)
		{
			throw new PSRParameterException("Minf vectors can only be constructed from matrices with column dimension of 1");
		}
		else
		{
			this.vector = vector;
		}
	}
	
	/**
	 * Returns the specified entry.
	 * 
	 * @param index Index of entry to be returned.
	 * @return The specified entry/
	 */
	public double getEntry(int index)
	{
		return vector.get(index, 1);
	}
	
	/**
	 * Returns a deep copy Jama matrix representation of Minf vector.
	 * 
	 * @return Deep copy Jama matrix representation of Minf vector.
	 */
	public DoubleMatrix getVector(){
		return vector;
	}
	
}
