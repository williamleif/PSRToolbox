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

package afest.datastructures.tree.decision.erts.exceptions;

/**
 * Exception class to handle illegal operations in the Decision tree package. 
 */
@SuppressWarnings("serial")
public class ERTException extends RuntimeException
{
	/**
	 * Generates a standard error message.
	 * @param pMessage The message to store.
	 */
	public ERTException(String pMessage) 
	{
		super(pMessage);
	}
}
