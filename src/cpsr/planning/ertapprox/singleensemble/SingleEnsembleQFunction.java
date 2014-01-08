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
package cpsr.planning.ertapprox.singleensemble;

import cpsr.environment.DataSet;
import cpsr.environment.components.Action;
import cpsr.model.APSR;
import cpsr.planning.IQFunction;

/**
 * Class for Q-Functions implicitly represented by
 * single ensemble of trees.
 * 
 * @author whamil3
 *
 */
public class SingleEnsembleQFunction implements IQFunction {
	SingleERTEnsemble erts;
	APSR aPSR;
	
	/**
	 * Constructs SingleEnsembleQFunction from DataSet, PSR, and an
	 * ERT ensemble.
	 * 
	 * @param env The associated DataSet.
	 * @param psr The associated PSR?
	 * @param erts The SingleERTEnsemble used to produce values.
	 */
	public SingleEnsembleQFunction(DataSet env, APSR psr, SingleERTEnsemble erts)
	{
		aPSR = psr;
		this.erts = erts;
	}
	
	@Override
	public double getQValue(APSR psr, Action act)
	{
		return erts.getValueEstimate(psr.getPredictionVector(), act);
	}
	

}
