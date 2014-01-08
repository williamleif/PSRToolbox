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

/**
 * Simple wrapper class encapsulated reward and observation
 * into one object.
 * 
 * @author William Hamilton
 */
public class RewardObservation 
{
	private Observation obs;
	private double reward;
	
	/**
	 * Constructs RewardObservation from specified reward
	 * and obseravtion.
	 * 
	 * @param obs The observation.
	 * @param reward The reward.
	 */
	public RewardObservation(Observation obs, double reward)
	{
		this.obs = obs;
		this.reward = reward;
	}
	
	/**
	 * Returns the reward.
	 * 
	 * @return The reward.
	 */
	public double getReward()
	{
		return this.reward;
	}
	
	/**
	 * Returns the observation.
	 * 
	 * @return the observation. 
	 */
	public Observation getObservation()
	{
		return this.obs;
	}
	
	
}
