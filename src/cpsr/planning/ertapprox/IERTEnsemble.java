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
package cpsr.planning.ertapprox;

import afest.datastructures.tree.decision.erts.ERTPoint;
import cpsr.environment.components.Action;
import cpsr.model.components.PredictionVector;

/**
 * Class defines ensemble of extremely random trees used for planning
 * with Q-iteration. Serves as one interface between cpsr package and afest
 * package. 
 * 
 * @author William Hamilton
 */
public interface IERTEnsemble {
	
	
	/**
	 * Returns estimated value associated with this state/prediction vector
	 * and action.
	 * (NOTE: the semantic of this value depends on specific
	 * implementing class).
	 * 
	 * @param point The point in question
	 * @return Value estimate for the state.
	 */
	public double getValueEstimate(ERTPoint point);
	
	/**
	 * Returns estimated value associated with this state/prediction vector.
	 * (NOTE: the semantic of this value depends on specific
	 * implementing class).
	 * 
	 * @param predVec The state/prediction vector
	 * @return Value estimate for the state.
	 */
	public double getValueEstimate(PredictionVector predVec);
	
	/**
	 * Returns estimated value associated with this state/prediction vector
	 * and action.
	 * (NOTE: the semantic of this value depends on specific
	 * implementing class).
	 * 
	 * @param predVec Prediction vector/state.
	 * @param act Action
	 * @return Value estimate for the state.
	 */
	public double getValueEstimate(PredictionVector predVec, Action act);
	
}
