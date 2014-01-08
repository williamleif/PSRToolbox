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

import cpsr.environment.exceptions.EnvironmentException;

/**
 * Class defines a general reset action observation.
 * Users do not need to implement reset action observations
 * but they are added to data set to facilitate learning.
 * 
 * @author William Hamilton
 */
@SuppressWarnings("serial")
public class ResetActionObservation extends ActionObservation {

	/**
	 * Creates a reset Action Observation pair.
	 * 
	 * @param dataSet The data set that the reset is
	 * associated with.
	 */
	public ResetActionObservation() 
	{
		this.idCode = -1;
	}
	
	@Override
	public Action getAction()
	{
		throw new EnvironmentException("ResetActionObservations are placeholders." +
				" It is not possible to access actions or observations associated with" +
				"them.");
	}
	
	@Override
	public Observation getObservation()
	{
		throw new EnvironmentException("ResetActionObservations are placeholders." +
				" It is not possible to access actions or observations associated with" +
				"them.");
	}
	
	@Override
	public int maxID()
	{
		return 0;
	}

}
