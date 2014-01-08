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
import java.util.ArrayList;
import java.util.HashSet;

import cpsr.environment.DataSet;

/**
 * Class encapsulates collections of action-ActionObservation pairs
 * 
 * @author William Hamilton
 */
@SuppressWarnings("serial")
public class IndicativeEvent implements Serializable
{
	/**
	 * @serial
	 */
	private HashSet<ArrayList<ActionObservation>> indEvent;
	
	/**
	 * @serial
	 */
	DataSet data;
	
	boolean firstAdded;
	
	/**
	 * Default constructor creates empty indicative event.
	 */
	public IndicativeEvent()
	{
		this.indEvent = new HashSet<ArrayList<ActionObservation>>();
		firstAdded = false;
	}
	
	/**
	 * Constructor creates IndicativeEvent from ArrayList of ArrayLists of 
	 * action-ActionObservation pairs.
	 * 
	 * @param indEvent ArrayList of ArrayLists of action-ActionObservation pairs.
	 */
	public IndicativeEvent(HashSet<ArrayList<ActionObservation>> indEvent)
	{
		this.indEvent = indEvent;
	}
	
	/**
	 * Determines whether an action-ActionObservation history is present
	 * in a particular indicative event.
	 * 
	 * @param history A sequence of action-ActionObservation pairs
	 * @return A boolean representing whether or not a history is 
	 * present is a particular IndicativeEvent.
	 */
	public boolean contains(ArrayList<ActionObservation> history)
	{
		return indEvent.contains(history);
	}
	
	/**
	 * Add a history to an IndicativeEvent
	 * 
	 * @param history The history (sequence of action-ActionObservation pairs)
	 * to add. 
	 */
	public void addHistory(ArrayList<ActionObservation> history)
	{
		indEvent.add(history);
	}
	
	/**
	 * Remove a history from an IndicativeEvent
	 * 
	 * @param history The history (sequence of action-ActionObservation pairs)
	 * to remove. 
	 */
	public void removeHistory(ArrayList<ActionObservation> history)
	{
		indEvent.remove(history);
	}
	
	
	/**
	 * Returns the number of histories in this indicative event.
	 * 
	 * @return Number of histories in this indicative event.
	 */
	public int getNumberOfHistories()
	{
		return this.indEvent.size();
	}
	
	/**
	 * Make a deep copy of the IndicativeEvent.
	 * 
	 * @return A deep copy of the IndicativeEvent.
	 */
	public IndicativeEvent copy()
	{
		@SuppressWarnings("unchecked")
		IndicativeEvent copy = new IndicativeEvent((HashSet<ArrayList<ActionObservation>>)indEvent.clone());
		return copy;
	}
	
	@Override 
	public boolean equals(Object ob)
	{
		if(((IndicativeEvent)ob).getNumberOfHistories() != this.getNumberOfHistories()) 
			return false;
		
		for(ArrayList<ActionObservation> history : indEvent)
		{
			if(!((IndicativeEvent)ob).contains(history)) return false;
		}
		
		return true;
	}
	
}
