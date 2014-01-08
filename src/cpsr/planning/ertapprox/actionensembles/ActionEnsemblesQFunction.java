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
package cpsr.planning.ertapprox.actionensembles;

import java.util.HashMap;

import cpsr.environment.components.Action;
import cpsr.model.APSR;
import cpsr.planning.IQFunction;
import cpsr.planning.exceptions.PSRPlanningException;

/**
 * Class for Q-Functions implicilty represented by action ensembles
 * of trees.
 * 
 * @author William Hamilton
 */
public class ActionEnsemblesQFunction implements IQFunction {
	APSR psr;
	HashMap<Action, ActionERTEnsemble> erts;
	
	/**
	 * Constructs an ActionEnsembleQFunction for specified DataSet and PSR.
	 * Method addActionEnsemble(ActionERTEnsemble) must be called and ensemble for each
	 * action must be added before using this object.
	 * 
	 * @param psr The associated PSR.
	 */
	public ActionEnsemblesQFunction(APSR psr)
	{
		this.psr = psr;
		this.erts = new HashMap<Action, ActionERTEnsemble>();
	}
	
	/**
	 * Constructs an ActionEnsembleQFunction for PSR
	 * with specified collection of ActionERTEnsembles.
	 * 
	 * @param psr The associated PSR.
	 * @param actionEnsembles A complete mapping of actions to actionEnsembles
	 */
	public ActionEnsemblesQFunction(APSR psr, HashMap<Action, ActionERTEnsemble> actionEnsembles)
	{
		this.psr = psr;
		this.erts = actionEnsembles;
	}
	
	
	@Override
	public double getQValue(APSR psr, Action act)
	{
		validateConstruction();
		return erts.get(act).getValueEstimate(psr.getPredictionVector());
	}
		
	/**
	 * Adds an action ensemble to the set of action ensembles.
	 * 
	 * @param actionEnsemble The single action ensemble to add.
	 */
	public void addActionEnsemble(ActionERTEnsemble actionEnsemble)
	{
		erts.put(actionEnsemble.getAssociatedAction(), actionEnsemble);
	}
	
	/**
	 * Tests whether object has been fully created.
	 */
	private void validateConstruction()
	{
		if(erts.keySet().size() != psr.getActionSet().size())
		{
			throw new PSRPlanningException("ActionEnsembleQFunction must have ensemble for each action." +
					"Either construct with full ensembles or add using addActionEnsemble(ActionERTEnsemble) function");
		}
	}

}
