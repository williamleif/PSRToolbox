/*
 *   Copyright 2013 William Hamilton
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
package cpsr.planning;

import java.util.ArrayList;
import java.util.Random;

import cpsr.environment.components.Action;

/**
 * Random planner. Simple returns a random action.
 * @author William Hamilton
 */
public class RandomPlanner implements IPlanner {

	private Random aRando;
	private ArrayList<Action> aActList;
	
	/**
	 * Constructs a random planner using integer specified actions from given range.
	 * @param pSeed Random seed.
	 * @param pActRangeIncLowerLimit Inclusive lower limit of possible actions.
	 * @param pActRangeExcUpperLimit Exclusive upper limit of possible actions.
	 */
	public RandomPlanner(int pSeed, int pActRangeIncLowerLimit, int pActRangeExcUpperLimit)
	{
		aRando = new Random(pSeed);
		aActList = new ArrayList<Action>();
		
		for(int i = pActRangeIncLowerLimit; i < pActRangeExcUpperLimit; i++)
		{
			aActList.add(new Action(i));
		}
	}
	
	/**
	 * Constructs a random planner over given action list.
	 * @param pSeed Random seed.
	 * @param pActList List of possible actions.
	 */
	public RandomPlanner(int pSeed, ArrayList<Action> pActList)
	{
		aRando = new Random(pSeed);
		
		aActList = pActList;
	}
	
	@Override
	public Action getAction() 
	{
		return aActList.get(aRando.nextInt(aActList.size()));
	}

}
