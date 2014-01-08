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
 * Class encapsulates action-observation pair.
 * 
 * @author William Hamilton
 */
@SuppressWarnings("serial")
public class ActionObservation implements Serializable
{
	/**
	 * @serialField
	 */
	private Observation ob;
	/**
	 * @serialField
	 */
	private Action act;
	/**
	 * @serialField
	 */
	protected int idCode;
	
	/**
	 * @serialField
	 * @deprecated
	 */
	protected DataSet data;
	
	/**
	 * Default constructor required for inheritance.
	 */
	protected ActionObservation()
	{
		super();
	}
	
	/**
	 * Constructs action-observation structure and computes new unique
	 * ID code.
	 * 
	 * @param act An action.
	 * @param ob An observation.
	 */
	public ActionObservation(Action act, Observation ob)
	{
		this.ob = ob;
		this.act = act;
		
		//part of deprecated interface.
//		this.data = ob.getDataSet();
		
		try
		{
			char[] idString = (ob.toBinaryString() + act.toBinaryString()).toCharArray();
			idCode = 1;
			
			for(int i = 0; i< idString.length; i++)
			{
				if(idString[i] == '1')
				{
					idCode += (int)(Math.pow(2, (idString.length-1)-i));
				}
			}
		}
		catch(EnvironmentException ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Returns the action associated with this action observation pair.
	 * 
	 * @return Action associated with this action-observation pair.
	 */
	public Action getAction()
	{
		return act;
	}
	
	/**
	 * Returns the observation associated with the action-observation pair.
	 * 
	 * @return Observation associated with this action-observation pair. 
	 */
	public Observation getObservation()
	{
		return ob;
	}
	
	/**
	 * Retrieve the unique ID code for this ActionObservation.
	 * 
	 * @return A unique ID code.
	 */
	public int getID()
	{
		return idCode;
	}
	
	/**
	 * Returns the maximum possible ID this type of action observation could have.
	 * 
	 * @return Max possible ID for this type of action observation.
	 */
	public int maxID()
	{
		int bitstringlength = Integer.toBinaryString(ob.maxID).length() + 
				Integer.toBinaryString(act.maxID).length();
		
		return (int)Math.pow(2, bitstringlength);
	}
	
	/**
	 * Returns DataSet associated with this action observation. 
	 * 
	 * @return DataSet associated with this action observation.
	 * @deprecated
	 */
	public DataSet getDataSet()
	{
		return this.data;
	}
		
	@Override
	public int hashCode()
	{
		return idCode;
	}
	
	@Override
	public boolean equals(Object actob)
	{
		return actob.hashCode() == this.hashCode();
	}
	
}
