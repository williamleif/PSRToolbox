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

package cpsr.planning;

import cpsr.environment.components.Action;
import cpsr.model.APSR;

/**
 * Top level interface defining a policy.
 *
 * @author William Hamilton
 */
public interface IQFunction 
{
	/**
	 * Returns Q value of given action.
	 * 
	 * @param psr The psr used to determine the best action.
	 * @return Best Action for current state. 
	 */
	public double getQValue(APSR psr, Action act);
}
