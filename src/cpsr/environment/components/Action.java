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
package cpsr.environment.components;

import java.io.Serializable;

import cpsr.environment.DataSet;
import cpsr.environment.exceptions.EnvironmentException;

/**
 * Class defines default actions.
 * Can be extended, but all children must still 
 * represent each unique action by a positive 
 * integer.
 * 
 * @author William Hamilton
 */
@SuppressWarnings("serial")
public class Action implements Serializable
{
	
	/**
	 * @serialField
	 * @deprecated
	 */
	protected DataSet dataSet;
	
	/**
	 * @serialField
	 */
	protected int id, maxID;
	
	/**
	 * Default constructor DO NOT USE.
	 */
	protected Action()
	{
		super();
	}
	
	/**
	 * Constructs Action using only id.
	 * Use this constructor if adding action to data set
	 * or if one plans on setting the DataSet at a later time.
	 * 
	 * @param id The action id.
	 */
	public Action(int id)
	{
		this.id = id;
	}
	

	/**
	 * Constructs Action using only id.
	 * Use this constructor if adding action to data set
	 * or if one plans on setting the DataSet at a later time.
	 * 
	 * @param id The action id.
	 */
	public Action(int id, int maxID)
	{
		this.id = id;
		this.maxID = maxID;
	}
	
	/**
	 * Constructs action associated with a particular
	 * DataSet using specified integer id.
	 * 
	 * @param id
	 * @param dataSet
	 * @deprecated
	 */
	public Action(int id, DataSet dataSet)
	{
		this.id = id;
		this.dataSet = dataSet;
	}
	
	/**
	 * Returns a binary string representing action with length equal
	 * to the max length specified by max action hash code of DataSet.
	 * 
	 * @return Binary representation of action. 
	 * @throws EnvironmentException
	 */
	public String toBinaryString() throws EnvironmentException
	{
		int targetLength = (Integer.toBinaryString(maxID)).length();
		String binaryRep = Integer.toBinaryString(this.hashCode());
		
		if(targetLength < binaryRep.length())
		{
			throw new EnvironmentException("Integer ID of action exceeds maximum specifed by data set");
		}
		else if(targetLength > binaryRep.length())
		{
			for(int i = 0; i < targetLength-binaryRep.length(); i++)
			{
				binaryRep = "0"+binaryRep;
			}
		}
		return binaryRep;
	}
	
	
	/**
	 * Returns DataSet associated with this action.
	 * 
	 * @return DataSet associated with this action. 
	 * @deprecated
	 */
	public DataSet getDataSet()
	{
		try
		{
			return dataSet;
		}
		catch(NullPointerException ex)
		{
			throw new EnvironmentException("Child classes of Observation must explicitly set dataSet field in constructor, or" +
					"by using setDataSet(DataSet) method");
		}
	}
	
	/**
	 * Return unique identifying integer ID for observation.
	 * 
	 * @return Unique identifying integer ID for observation
	 */
	public int getID()
	{
		try
		{
			return id;
		}
		catch(NullPointerException ex)
		{
			throw new EnvironmentException("Child classes of Observation must explicitly set id field in constructor");
		}
	}
	
	/**
	 * Sets the data set.
	 */
	public void setData(DataSet dataSet)
	{
		this.dataSet = dataSet;
	}
	
	public void setMaxID(int pMaxID)
	{
		maxID = pMaxID;
	}
	
	@Override 
	public int hashCode()
	{
		return getID();
	}
	
	@Override
	public boolean equals(Object ob)
	{
		return id == ((Action)ob).getID();
	}
}
